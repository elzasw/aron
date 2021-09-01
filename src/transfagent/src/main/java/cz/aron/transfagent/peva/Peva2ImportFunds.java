package cz.aron.transfagent.peva;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.Accessibility;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.InstitutionReference;
import cz.aron.peva2.wsdl.Integrity;
import cz.aron.peva2.wsdl.ListAccessibilityRequest;
import cz.aron.peva2.wsdl.ListIntegrityRequest;
import cz.aron.peva2.wsdl.ListNadSheetRequest;
import cz.aron.peva2.wsdl.ListNadSheetResponse;
import cz.aron.peva2.wsdl.ListPhysicalStateRequest;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.peva2.wsdl.PhysicalState;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.domain.Property;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
public class Peva2ImportFunds implements ImportProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(Peva2ImportFunds.class);
    
    private static final String PEVA2_UPDATE_AFTER = "PEVA2_UPDATE_AFTER";
    
    private static final String PEVA2_SEARCH_AFTER = "PEVA2_SEARCH_AFTER";
    
    private final PEvA peva2;
    
    private final PropertyRepository propertyRepository;
    
    private final FileImportService importService;
    
    private final ArchivalEntityRepository archivalEntityRepository;

    private final ApuSourceRepository apuSourceRepository;
    
    private final ConfigPeva2 config;
    
    private final TransactionTemplate tt;
    
    private final StorageService storageService;

    public Peva2ImportFunds(PEvA peva2, PropertyRepository propertyRepository, FileImportService importService,
            ArchivalEntityRepository archivalEntityRepository, ApuSourceRepository apuSourceRepository,ConfigPeva2 config,
            TransactionTemplate tt, StorageService storageService) {
        this.peva2 = peva2;
        this.propertyRepository = propertyRepository;
        this.importService = importService;
        this.archivalEntityRepository = archivalEntityRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.config = config;
        this.tt = tt;
        this.storageService = storageService;
    }
    
    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }

    @Override
    public void importData(ImportContext ic) {
       
        var updateAfterProp = propertyRepository.findByName(PEVA2_UPDATE_AFTER);
        var searchAfterProp = propertyRepository.findByName(PEVA2_SEARCH_AFTER);
        
        final OffsetDateTime nowTime = OffsetDateTime.now();
        OffsetDateTime nextTime = nowTime.plusSeconds(config.getInterval());        
        
        XMLGregorianCalendar od = null;
        if (updateAfterProp != null&&StringUtils.isNotBlank(updateAfterProp.getValue())) {
            try {
                od = DatatypeFactory.newInstance().newXMLGregorianCalendar(updateAfterProp.getValue());
                // chyba v PEvA2, pokud je zadana casova zona, tak je updatedAfter ignorovano 
                od.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException();
            }            
        }
 
        String searchAfter = searchAfterProp!=null&&StringUtils.isNotBlank(searchAfterProp.getValue())?searchAfterProp.getValue():null;
        
        if (synchronizeFunds(od,0L,searchAfter)>0) {
        	// TODO reportovat realnou hodnotu
        	ic.addProcessed();
        }

        tt.execute(t -> {
            var sa = propertyRepository.findByName(PEVA2_SEARCH_AFTER);
            if (sa != null) {
                sa.setValue("");
                propertyRepository.save(sa);
            }
            var ua = propertyRepository.findByName(PEVA2_UPDATE_AFTER);
            if (ua != null) {
                ua.setValue(nowTime.toString());
            } else {
                ua = new Property();
                ua.setName(PEVA2_UPDATE_AFTER);
                ua.setValue(nowTime.toString());                
            }
            propertyRepository.save(ua);
            return null;
        });
        
    }

	class CodeListHolder {
		Peva2CodeLists codeLists = null;

		public Peva2CodeLists getCodeLists() {
			if (codeLists == null) {
				codeLists = downloadCodeLists();
			}
			return codeLists;
		}
	}
    
    private int synchronizeFunds(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial) {
        
        PEvA2Client.fillHeaders(peva2, config);
        CodeListHolder clh = new CodeListHolder();
        
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
            
            int numUpdatedFromBatch = 0;
            try {
                patchFundBatch(lnsResp.getItems().getNadPrimarySheetOrNadSubsheet(), clh.getCodeLists());
                numUpdatedFromBatch+=lnsResp.getItems().getNadPrimarySheetOrNadSubsheet().size();
            } catch (Exception e) {
                log.error("Fail to update fund batch", e);
                // zkusim to po jednom
                for (NadSheet nadSheet : lnsResp.getItems().getNadPrimarySheetOrNadSubsheet()) {
                    try {
                        patchFundBatch(Collections.singletonList(nadSheet), clh.getCodeLists());
                        numUpdatedFromBatch++;
                    } catch (Exception e1) {
                        log.error("Fail to update fund {}", nadSheet.getId(), e1);
                    }
                }                
            }

            String searchAfterFinal = searchAfter;
            tt.execute(t->{
                var sa = propertyRepository.findByName(PEVA2_SEARCH_AFTER);
                if (sa!=null) {
                    sa.setValue(searchAfterFinal!=null?searchAfterFinal:"");
                } else {
                    sa = new Property();
                    sa.setName(PEVA2_SEARCH_AFTER);
                    sa.setValue(searchAfterFinal);                    
                }
                propertyRepository.save(sa);
                return null;
            });
            
            numUpdated += numUpdatedFromBatch;
            log.info("Updated {}/{} funds", numUpdated,
                     lnsResp.getCount());
        }
        log.info("Funds synchronized");
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
			}
		}
    }

    private Map<String, String> getAccessibility() {
        Map<String, String> ret = new HashMap<>();
        var laReq = new ListAccessibilityRequest();
        laReq.setSize(100);
        var laResp = peva2.listAccessibility(laReq);
        for (Accessibility accessibility : laResp.getAccessibilities().getAccessibility()) {
            ret.put(accessibility.getId(), accessibility.getName());
        }
        log.info("Accessibilities list downloaded.");
        return ret;
    }

    private Map<String, String> getPhysicalState() {
        Map<String, String> ret = new HashMap<>();
        var lpsReq = new ListPhysicalStateRequest();
        lpsReq.setSize(100);
        var lpsResp = peva2.listPhysicalState(lpsReq);
        for (PhysicalState physicalSate : lpsResp.getPhysicalStates().getPhysicalState()) {
            ret.put(physicalSate.getId(), physicalSate.getName());
        }
        log.info("Physical states downloaded.");
        return ret;
    }

    private Map<String, String> getIntegrity() {
        Map<String, String> ret = new HashMap<>();
        var liReq = new ListIntegrityRequest();
        liReq.setSize(100);
        var liResp = peva2.listIntegrity(liReq);
        for (Integrity integrity : liResp.getIntegrities().getIntegrity()) {
            ret.put(integrity.getId(), integrity.getName());
        }
        log.info("Integrity downloaded.");
        return ret;
    }

    private Peva2CodeLists downloadCodeLists() {
        return new Peva2CodeLists(getAccessibility(), getPhysicalState(), getIntegrity());
    }
    
}
