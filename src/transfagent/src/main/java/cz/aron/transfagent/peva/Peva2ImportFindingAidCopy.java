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

import cz.aron.peva2.wsdl.FindingAidCopy;
import cz.aron.peva2.wsdl.GetFindingAidCopyResponse;
import cz.aron.peva2.wsdl.ListFindingAidCopyRequest;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2ImportFindingAidCopy extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportFindingAidCopy.class);
	
	public Peva2ImportFindingAidCopy(PEvA peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService, @Value("${peva2.importFindingAidCopy:false}") boolean active) {
		super("FINDINGAID_COPY", peva2, propertyRepository, config, tt, storageService, active);	
	}

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider) {
		String searchAfter = searchAfterInitial;
		int numUpdated = 0;
		while (true) {
			var lfacReq = new ListFindingAidCopyRequest();
			lfacReq.setSize(config.getBatchSize());
			lfacReq.setUpdatedAfter(updateAfter);
			lfacReq.setSearchAfter(searchAfter);
			var lfacResp = peva2.listFindingAidCopy(lfacReq);
			searchAfter = lfacResp.getSearchAfter();
			long count = lfacResp.getFindingAidCopies().getFindingAidCopy().size();
			log.info("Downloaded {} finding aid copies to update after {}", count, updateAfter);
			if (count == 0) {
				break;
			}

			int numUpdatedFromBatch = 0;
			try {
				patchFindingAidCopiesBatch(lfacResp.getFindingAidCopies().getFindingAidCopy(),
						codeListProvider.getCodeLists());
				numUpdatedFromBatch += lfacResp.getFindingAidCopies().getFindingAidCopy().size();
			} catch (Exception e) {
				log.error("Fail to update finding aid copies batch", e);
				// zkusim to po jednom
				for (var findingAidCopy : lfacResp.getFindingAidCopies().getFindingAidCopy()) {
					try {
						patchFindingAidCopiesBatch(Collections.singletonList(findingAidCopy), codeListProvider.getCodeLists());
						numUpdatedFromBatch++;
					} catch (Exception e1) {
						log.error("Fail to update finding aid {}", findingAidCopy.getId(), e1);
					}
				}
			}

			storeSearchAfter(searchAfter);
			numUpdated += numUpdatedFromBatch;
			log.info("Updated {}/{} finding aid copies", numUpdated, lfacResp.getCount());
		}
		log.info("Finding aid copies synchronized");
		return numUpdated;
	}

	private void patchFindingAidCopiesBatch(List<FindingAidCopy> findingAidCopies, Peva2CodeLists codeLists) {
		var facInputDir = storageService.getInputPath().resolve("facopies");
		for (var findingAidCopy : findingAidCopies) {
			var findingAidId = findingAidCopy.getFindingAid();
			var gfacr = new GetFindingAidCopyResponse();
			gfacr.setFindingAidCopy(findingAidCopy);			
			var faDir = facInputDir.resolve(findingAidId);			
			try {
				Files.createDirectories(faDir);
				Path name = faDir.resolve(findingAidCopy.getId() + ".xml");
				Peva2XmlReader.marshalGetFindingAidCopyResponse(gfacr, name);
			} catch (Exception e) {
				log.error("Fail to store finding aid copy {} to input dir", findingAidCopy.getId(), e);
			}
		}
	}

}
