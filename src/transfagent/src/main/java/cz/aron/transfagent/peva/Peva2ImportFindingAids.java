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

import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.peva2.wsdl.GetFindingAidResponse;
import cz.aron.peva2.wsdl.ListFindingAidRequest;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2ImportFindingAids extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportFindingAids.class);

	public Peva2ImportFindingAids(PEvA peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService) {
		super("FINDINGAID", peva2, propertyRepository, config, tt, storageService);
	}

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial, Peva2CodeListProvider codeListProvider) {
		PEvA2Client.fillHeaders(peva2, config);
        
        String searchAfter = searchAfterInitial;
        int numUpdated = 0;
        while (true) {
            var lfar = new ListFindingAidRequest();
            lfar.setSize(config.getBatchSize());
            lfar.setUpdatedAfter(updateAfter);
            lfar.setSearchAfter(searchAfter);
            var lfaResp = peva2.listFindingAid(lfar);            
            searchAfter = lfaResp.getSearchAfter();
            long count = lfaResp.getFindingAids().getFindingAid().size();
            log.info("Downloaded {} finding aids to update", count);
            if (count == 0) {
                break;
            }
            
            int numUpdatedFromBatch = 0;
            try {
                patchFindingAidsBatch(lfaResp.getFindingAids().getFindingAid(), codeListProvider.getCodeLists());
                numUpdatedFromBatch+=lfaResp.getFindingAids().getFindingAid().size();
            } catch (Exception e) {
                log.error("Fail to update finding aids batch", e);
                // zkusim to po jednom
                for (var findingAid : lfaResp.getFindingAids().getFindingAid()) {
                    try {
                        patchFindingAidsBatch(Collections.singletonList(findingAid), codeListProvider.getCodeLists());
                        numUpdatedFromBatch++;
                    } catch (Exception e1) {
                        log.error("Fail to update finding aid {}", findingAid.getId(), e1);
                    }
                }                
            }

            storeSearchAfter(searchAfter);            
            numUpdated += numUpdatedFromBatch;
            log.info("Updated {}/{} finding aids", numUpdated,
                     lfaResp.getCount());
        }
        log.info("Finding aids synchronized");
        return numUpdated;
	}

	private void patchFindingAidsBatch(List<FindingAid> findingAids, Peva2CodeLists codeLists) {
		var faInputDir = storageService.getInputPath().resolve("findingAids");
		for (var findingAid : findingAids) {			
			if (findingAid.getNadSheets() == null || findingAid.getNadSheets().getNadSheet() == null
					|| findingAid.getNadSheets().getNadSheet().isEmpty()) {
				log.warn("Finding aid without NadSheet institution:{}, finding aid:{}", findingAid.getInstitution().getExternalId(), findingAid.getEvidenceNumber());
				continue;
			}			
			var gfar = new GetFindingAidResponse();
			gfar.setFindingAid(findingAid);			
			String id = findingAid.getInstitution().getExternalId() + "_" + findingAid.getId();			
			var faDir = faInputDir.resolve("pevafa-"+id);
			try {
				Files.createDirectories(faDir);
				Path name = faDir.resolve("pevafa-"+id+".xml");
				Peva2XmlReader.marshalGetFindingAidResponse(gfar, name);			
			} catch (Exception e) {
				log.error("Fail to store peva finding aid {} to input dir",findingAid.getId(),e);			
			}
		}
	}

}
