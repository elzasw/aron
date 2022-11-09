package cz.aron.transfagent.peva;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.GeoObject;
import cz.aron.peva2.wsdl.GetGeoObjectRequest;
import cz.aron.peva2.wsdl.GetGeoObjectResponse;
import cz.aron.peva2.wsdl.ListGeoObjectRequest;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;


/**
 * Stahuje a aktualizuje geograficke entity z PEvA 2
 */
@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2ImportGeo extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportGeo.class);

	public static final String PREFIX = "pevageo-";


	public Peva2ImportGeo(PEvA2Connection peva2, PropertyRepository propertyRepository, ConfigPeva2 config, TransactionTemplate tt,
			StorageService storageService, @Value("${peva2.importGeoObject:false}") boolean active) {
		super("GEO", peva2, propertyRepository, config, tt, storageService, active);
	}

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider) {
		String searchAfter = searchAfterInitial;
		int numUpdated = 0;
		while (true) {
			var lgor = new ListGeoObjectRequest();
			lgor.setSize(config.getBatchSize());
			lgor.setUpdatedAfter(updateAfter);
			lgor.setSearchAfter(searchAfter);
			var loResp = peva2.getPeva().listGeoObject(lgor);
			searchAfter = loResp.getSearchAfter();
			long count = loResp.getGeoObjects().getGeoObject().size();
			log.info("Downloaded {} geo objects to update after", count, updateAfter);
			if (count == 0) {
				break;
			}

			int numUpdatedFromBatch = 0;
			try {
				patchGeoBatch(loResp.getGeoObjects().getGeoObject(),
						codeListProvider.getCodeLists());
				numUpdatedFromBatch += loResp.getGeoObjects().getGeoObject().size();
			} catch (Exception e) {
				log.error("Fail to update geo objects batch", e);
				// zkusim to po jednom
				for (var originator : loResp.getGeoObjects().getGeoObject()) {
					try {
						patchGeoBatch(Collections.singletonList(originator), codeListProvider.getCodeLists());
						numUpdatedFromBatch++;
					} catch (Exception e1) {
						log.error("Fail to update geo object {}", originator.getId(), e1);
					}
				}
			}

			storeSearchAfter(searchAfter);
			numUpdated += numUpdatedFromBatch;
			log.info("Updated {}/{} geo objects", numUpdated, loResp.getCount());
		}
		log.info("Geo objects synchronized");
		return numUpdated;

	}
	
	private void patchGeoBatch(List<GeoObject> geoObjects, Peva2CodeLists codeLists) {
		var faInputDir = storageService.getInputPath().resolve("geos");
		for (var geoObject : geoObjects) {
			var ggor = new GetGeoObjectResponse();
			String id = geoObject.getId();
			ggor.setGeoObject(geoObject);
			var oDir = faInputDir.resolve(PREFIX + id);
			try {
				Files.createDirectories(oDir);
				Path name = oDir.resolve(PREFIX + id + ".xml");
				Peva2XmlReader.marshalGetGeoObjectResponse(ggor, name);
			} catch (Exception e) {
				log.error("Fail to store peva geo object {} to input dir", geoObject.getId(), e);
			}
		}
	}

	@Override
	protected boolean processCommand(Path path, Peva2CodeListProvider codeListProvider) {
		var fileName = path.getFileName().toString();
		if (!fileName.startsWith(PREFIX)) {
			// not my command
			return false;
		}
		var id = fileName.substring(PREFIX.length());		
		var ggoReq = new GetGeoObjectRequest();
		ggoReq.setId(id);
		var ggoResp = peva2.getPeva().getGeoObject(ggoReq);					
		patchGeoBatch(Collections.singletonList(ggoResp.getGeoObject()),codeListProvider.getCodeLists());		
		return true;
	}

}
