package cz.aron.transfagent.peva;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.Dynasty;
import cz.aron.peva2.wsdl.Event;
import cz.aron.peva2.wsdl.GetOriginatorResponse;
import cz.aron.peva2.wsdl.ListOriginatorRequest;
import cz.aron.peva2.wsdl.Originator;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.peva2.wsdl.PartyGroup;
import cz.aron.peva2.wsdl.Person;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.importOriginators")
public class Peva2ImportOriginators extends Peva2Downloader {

	private static final Logger log = LoggerFactory.getLogger(Peva2ImportOriginators.class);

	public static final String PREFIX = "pevaoriginator-";

	public Peva2ImportOriginators(PEvA peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService) {
		super("ORIGINATOR", peva2, propertyRepository, config, tt, storageService);
	}

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider) {

		String searchAfter = searchAfterInitial;
		int numUpdated = 0;
		while (true) {
			var lor = new ListOriginatorRequest();
			lor.setSize(config.getBatchSize());
			lor.setUpdatedAfter(updateAfter);
			lor.setSearchAfter(searchAfter);
			var loResp = peva2.listOriginator(lor);
			searchAfter = loResp.getSearchAfter();
			long count = loResp.getOriginators().getDynastyOrEventOrPartyGroup().size();
			log.info("Downloaded {} originators to update", count);
			if (count == 0) {
				break;
			}

			int numUpdatedFromBatch = 0;
			try {
				patchOriginatorsBatch(loResp.getOriginators().getDynastyOrEventOrPartyGroup(),
						codeListProvider.getCodeLists());
				numUpdatedFromBatch += loResp.getOriginators().getDynastyOrEventOrPartyGroup().size();
			} catch (Exception e) {
				log.error("Fail to update finding aids batch", e);
				// zkusim to po jednom
				for (var originator : loResp.getOriginators().getDynastyOrEventOrPartyGroup()) {
					try {
						patchOriginatorsBatch(Collections.singletonList(originator), codeListProvider.getCodeLists());
						numUpdatedFromBatch++;
					} catch (Exception e1) {
						log.error("Fail to update finding aid {}", originator.getId(), e1);
					}
				}
			}

			storeSearchAfter(searchAfter);
			numUpdated += numUpdatedFromBatch;
			log.info("Updated {}/{} originators", numUpdated, loResp.getCount());
		}
		log.info("Originators synchronized");
		return numUpdated;
	}

	private void patchOriginatorsBatch(List<Originator> originators, Peva2CodeLists codeLists) {
		var faInputDir = storageService.getInputPath().resolve("originators");
		for (var originator : originators) {
			var gor = new GetOriginatorResponse();

			if (originator instanceof Dynasty) {
				gor.setDynasty((Dynasty) originator);
			} else if (originator instanceof Event) {
				// gor.setEvent((Event)originator);
				// ignoruji udalosti
				continue;
			} else if (originator instanceof Person) {
				gor.setPerson((Person) originator);
			} else if (originator instanceof PartyGroup) {
				gor.setPartyGroup((PartyGroup) originator);
			} else {
				log.error("Invalid type of originator id:{}, type:{}", originator.getId(), originator.getClass());
				continue;
			}

			/// gor.set
			String id = originator.getId();
			var oDir = faInputDir.resolve(PREFIX + id);
			try {
				Files.createDirectories(oDir);
				Path name = oDir.resolve(PREFIX + id + ".xml");
				Peva2XmlReader.marshalGetOriginatorResponse(gor, name);
			} catch (Exception e) {
				log.error("Fail to store peva originator {} to input dir", originator.getId(), e);
			}
		}
	}

}
