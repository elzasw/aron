package cz.aron.transfagent.peva;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.GetNadSheetRequest;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.InstitutionReference;
import cz.aron.peva2.wsdl.ListNadSheetRequest;
import cz.aron.peva2.wsdl.ListNadSheetResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2ImportFunds extends Peva2Downloader {
    
    private static final Logger log = LoggerFactory.getLogger(Peva2ImportFunds.class);
        
    private final FileImportService importService;
    
    private final ArchivalEntityRepository archivalEntityRepository;

    private final ApuSourceRepository apuSourceRepository;
    
    private final TransactionTemplate tt;	
	
	private LRUMap<String,String> fundUUIDToJaFa = null;

    public Peva2ImportFunds(PEvA peva2, PropertyRepository propertyRepository, FileImportService importService,
            ArchivalEntityRepository archivalEntityRepository, ApuSourceRepository apuSourceRepository,ConfigPeva2 config,
            TransactionTemplate tt, StorageService storageService) {
    	
    	super("FUND",peva2,propertyRepository,config,tt,storageService);
        this.importService = importService;
        this.archivalEntityRepository = archivalEntityRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.tt = tt;
    }
    
	@Override
    protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial, Peva2CodeListProvider codeListProvider) {                        
        String searchAfter = searchAfterInitial;
        int numUpdated = 0;
        while (true) {
            ListNadSheetRequest lnsr = new ListNadSheetRequest();
            lnsr.setSize(config.getBatchSize());
            lnsr.setUpdatedAfter(updateAfter);
            lnsr.setSearchAfter(searchAfter);
            ListNadSheetResponse lnsResp = peva2.listNadSheet(lnsr);
            log.info("Downloaded {} funds to update", lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size());
            searchAfter = lnsResp.getSearchAfter();
            long count = lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size();
            if (count == 0) {
                break;
            }
                        
            fundUUIDToJaFa = new LRUMap<String,String>(10000);            
            int numUpdatedFromBatch = 0;
            try {
                patchFundBatch(lnsResp.getItems().getNadPrimarySheetOrNadSubsheet(), codeListProvider.getCodeLists());
                numUpdatedFromBatch+=lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size();
            } catch (Exception e) {
                log.error("Fail to update fund batch", e);
                // zkusim to po jednom
                for (NadSheet nadSheet : lnsResp.getItems().getNadPrimarySheetOrNadSubsheet()) {
                    try {
                        patchFundBatch(Collections.singletonList(nadSheet), codeListProvider.getCodeLists());
                        numUpdatedFromBatch++;
                    } catch (Exception e1) {
                        log.error("Fail to update fund {}", nadSheet.getId(), e1);
                    }
                }                
            }

            storeSearchAfter(searchAfter);            
            numUpdated += numUpdatedFromBatch;
            log.info("Updated {}/{} funds", numUpdated,
                     lnsResp.getCount());
        }
        log.info("Funds synchronized");
        fundUUIDToJaFa = null;
        return numUpdated;
    }
    
    private void patchFundBatch(List<NadSheet> nadSheets, Peva2CodeLists codeLists) {    	
    	Path fundsInputDir = storageService.getInputPath().resolve("fund");
		for (NadSheet nadSheet : nadSheets) {			
			if (nadSheet instanceof NadPrimarySheet) {
				NadPrimarySheet nps = (NadPrimarySheet)nadSheet;
				InstitutionReference ir = nps.getInstitution();
				String fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),nps.getEvidenceNumber(),null);				
				Path fundDir = fundsInputDir.resolve("pevafund-"+fundId);			
				try {
					Files.createDirectories(fundDir);
					Path name = fundDir.resolve("pevafund-"+fundId+".xml");					
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadPrimarySheet(nps);					
					Peva2XmlReader.marshalGetNadSheetResponse(gnsr, name);
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",nadSheet.getId(),e);
				}
				fundUUIDToJaFa.put(nps.getId(), nps.getEvidenceNumber());
			} else if (nadSheet instanceof NadSubsheet) {
				NadSubsheet nss = (NadSubsheet)nadSheet;
				var parentUUID = nss.getParent();				
				var evidenceNumber = fundUUIDToJaFa.get(parentUUID);
				if (evidenceNumber==null) {
					var gnsReq = new GetNadSheetRequest();
					gnsReq.setId(parentUUID);
					var gnsResp = peva2.getNadSheet(gnsReq);
					evidenceNumber = gnsResp.getNadPrimarySheet().getEvidenceNumber();
					fundUUIDToJaFa.put(gnsResp.getNadPrimarySheet().getId(), evidenceNumber);
				}												
				InstitutionReference ir = nss.getInstitution();
				var fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),evidenceNumber,""+nss.getNumber());				
				Path fundDir = fundsInputDir.resolve("pevafund-"+fundId);			
				try {
					Files.createDirectories(fundDir);
					Path name = fundDir.resolve("pevafund-"+fundId+".xml");					
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadSubsheet(nss);					
					Peva2XmlReader.marshalGetNadSheetResponse(gnsr, name);
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",nadSheet.getId(),e);
				}
			}
		}
    }
    
}
