package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2ImportFunds extends Peva2Downloader {
    
    private static final Logger log = LoggerFactory.getLogger(Peva2ImportFunds.class);
    
    private static final String PREFIX_DASH = "pevafund-";
	
	private LRUMap<String,String> fundUUIDToJaFa = null;

	public Peva2ImportFunds(PEvA peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService, @Value("${peva2.importFund:false}") boolean active) {
		super("FUND", peva2, propertyRepository, config, tt, storageService, active);
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
		for (NadSheet nadSheetId : nadSheets) {			
			var gnsReqMain = new GetNadSheetRequest();
			gnsReqMain.setId(nadSheetId.getId());
			var gnsRespMain = peva2.getNadSheet(gnsReqMain);									
			if (gnsRespMain.getNadPrimarySheet()!=null) {
				NadPrimarySheet nps = gnsRespMain.getNadPrimarySheet();
				InstitutionReference ir = nps.getInstitution();
				String fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),nps.getEvidenceNumber(),null);				
				Path fundDir = fundsInputDir.resolve(PREFIX_DASH+fundId);			
				try {
					Files.createDirectories(fundDir);
					Path name = fundDir.resolve(PREFIX_DASH+fundId+".xml");					
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadPrimarySheet(nps);					
					Peva2XmlReader.marshalGetNadSheetResponse(gnsr, name);
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",gnsRespMain.getNadPrimarySheet().getId(),e);
					throw new IllegalStateException(e);
				}
				fundUUIDToJaFa.put(nps.getId(), nps.getEvidenceNumber());
			} else if (gnsRespMain.getNadSubsheet()!=null) {
				NadSubsheet nss = gnsRespMain.getNadSubsheet();
				var parentUUID = nss.getParent();				
				var evidenceNumber = fundUUIDToJaFa.get(parentUUID);
				if (evidenceNumber==null) {
					var gnsReqParent = new GetNadSheetRequest();
					gnsReqParent.setId(parentUUID);
					var gnsRespParent = peva2.getNadSheet(gnsReqParent);
					evidenceNumber = gnsRespParent.getNadPrimarySheet().getEvidenceNumber();
					fundUUIDToJaFa.put(gnsRespParent.getNadPrimarySheet().getId(), evidenceNumber);
				}												
				InstitutionReference ir = nss.getInstitution();
				var fundId = ArchiveFundId.createJaFaId(ir.getExternalId(),evidenceNumber,""+nss.getNumber());				
				Path fundDir = fundsInputDir.resolve(PREFIX_DASH+fundId);			
				try {
					Files.createDirectories(fundDir);
					Path name = fundDir.resolve(PREFIX_DASH+fundId+".xml");					
					GetNadSheetResponse gnsr = new GetNadSheetResponse();
					gnsr.setNadSubsheet(nss);					
					Peva2XmlReader.marshalGetNadSheetResponse(gnsr, name);
				} catch (Exception e) {
					log.error("Fail to store pevafund {} to input dir",gnsRespMain.getNadSubsheet().getId(),e);
					throw new IllegalStateException(e);
				}
			}
		}
    }
    
    private void createCommands(List<NadSheet> nadSheets) {
    	Path commandsInputDir = storageService.getInputPath().resolve("commands");
    	for (NadSheet nadSheet : nadSheets) {
    		var commandFile = commandsInputDir.resolve(PREFIX_DASH+nadSheet.getId());
    		if (!Files.isRegularFile(commandFile)) {
    			try {
    				Files.createFile(commandFile);
    			} catch (IOException e) {
    				log.error("Fail to create command file {}",commandFile,e);
    			}
    		}
    	}    	
    }

	@Override
	protected boolean processCommand(Path path, Peva2CodeListProvider codeListProvider) {		
		var fileName = path.getFileName().toString();
		if (!fileName.startsWith(PREFIX_DASH)) {
			// not my command
			return false;
		}
		
		var id = fileName.substring(PREFIX_DASH.length());		
		var gnsReq = new GetNadSheetRequest();
		gnsReq.setId(id);
		var gnsResp = peva2.getNadSheet(gnsReq);
		
		NadSheet nadSheet = gnsResp.getNadPrimarySheet();
		if (nadSheet==null) {
			nadSheet = gnsResp.getNadSubsheet();
		}
		
		fundUUIDToJaFa = new LRUMap<String,String>(10000);
		try {
			patchFundBatch(Collections.singletonList(nadSheet), codeListProvider.getCodeLists());
		} finally {
			fundUUIDToJaFa = null;
		}
		return true;
	}
    
    
    
    
}
