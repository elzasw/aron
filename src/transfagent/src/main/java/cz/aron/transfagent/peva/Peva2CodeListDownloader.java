package cz.aron.transfagent.peva;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.peva2.wsdl.Accessibility;
import cz.aron.peva2.wsdl.Integrity;
import cz.aron.peva2.wsdl.ListAccessibilityRequest;
import cz.aron.peva2.wsdl.ListDatingMethodRequest;
import cz.aron.peva2.wsdl.ListFindingAidFormTypeRequest;
import cz.aron.peva2.wsdl.ListFindingAidTypeRequest;
import cz.aron.peva2.wsdl.ListGeoObjectTypeRequest;
import cz.aron.peva2.wsdl.ListIntegrityRequest;
import cz.aron.peva2.wsdl.ListLanguageRequest;
import cz.aron.peva2.wsdl.ListMainEvidenceUnitTypeRequest;
import cz.aron.peva2.wsdl.ListOriginatorSubClassRequest;
import cz.aron.peva2.wsdl.ListPhysicalStateRequest;
import cz.aron.peva2.wsdl.ListThematicEvidenceGroupRequest;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.peva2.wsdl.PhysicalState;
import cz.aron.transfagent.peva.Peva2CodeLists.Peva2DatingMethod;
import cz.aron.transfagent.peva.Peva2CodeLists.Peva2EvidenceUnitType;
import cz.aron.transfagent.peva.Peva2CodeLists.Peva2Geo;
import cz.aron.transfagent.peva.Peva2CodeLists.Peva2Language;
import cz.aron.transfagent.peva.Peva2CodeLists.Peva2OriginatorSubclass;
import cz.aron.transfagent.peva.Peva2CodeLists.Peva2ThematicGroup;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2CodeListDownloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2CodeListDownloader.class);
	
	private final PEvA peva2;
	
	private final StorageService storageService;
	
	public Peva2CodeListDownloader(PEvA peva2, StorageService storageService) {
		this.peva2 = peva2;
		this.storageService = storageService;
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
    
	private Map<String, String> getFindingAidType() {
		Map<String, String> ret = new HashMap<>();
		var lfatReq = new ListFindingAidTypeRequest();
		lfatReq.setSize(100);
		var lfatResp = peva2.listFindingAidType(lfatReq);
		for (var findingAidType : lfatResp.getFindingAidTypes().getFindingAidType()) {
			ret.put(findingAidType.getId(), findingAidType.getName());
		}
		log.info("Finding aid type downloaded.");
		return ret;
	}
	
	public Map<String,Peva2Language> getLanguages() {
		Map<String,Peva2Language> ret = new HashMap<>();
		var llReq = new ListLanguageRequest();
		llReq.setSize(1000);
		var llResp = peva2.listLanguage(llReq);
		for(var lang:llResp.getLanguages().getLanguage()) {
			ret.put(lang.getId(), new Peva2Language(lang.getCode(), lang.getName()));
		}
		log.info("Languages downloaded.");
		return ret;
	}
	
	public Map<String,Peva2EvidenceUnitType> getEvidenceUnitTypes() {
		Map<String,Peva2EvidenceUnitType> ret = new HashMap<>();
		var meutReq = new ListMainEvidenceUnitTypeRequest();
		meutReq.setSize(100);
		var meutResp = peva2.listMainEvidenceUnitType(meutReq);
		for(var meut:meutResp.getMainEvidenceUnitTypes().getMainEvidenceUnitType()) {
			ret.put(meut.getId(), new Peva2EvidenceUnitType(meut.getName()));
			if (meut.getPartialEvidenceUnitTypes()!=null) {
				for(var partial:meut.getPartialEvidenceUnitTypes()) {
					ret.put(partial.getId(), new Peva2EvidenceUnitType(partial.getName()));
				}
			}
		}
		log.info("Evidence unit types downloaded.");
		return ret;
	}
	
	private Map<String, String> getFindingAidFormType() {
		Map<String, String> ret = new HashMap<>();
		var lfaftReq = new ListFindingAidFormTypeRequest();
		lfaftReq.setSize(100);
		var lfaftResp = peva2.listFindingAidFormType(lfaftReq);
		for (var findingAidType : lfaftResp.getFindingAidFormTypes().getFindingAidFormType()) {
			ret.put(findingAidType.getId(), findingAidType.getName());
		}
		log.info("Finding aid form type downloaded.");
		return ret;
	}
	
	private Map<String, Peva2OriginatorSubclass> getOriginatorSubclass() {
		Map<String, Peva2OriginatorSubclass> ret = new HashMap<>();
		var losReq = new ListOriginatorSubClassRequest();
		losReq.setSize(100);
		var losResp = peva2.listOriginatorSubClass(losReq);
		for (var sc : losResp.getOriginatorSubClasses().getOriginatorSubClass()) {
			ret.put(sc.getId(), new Peva2OriginatorSubclass(sc.getOClass().toString(), sc.getName(), sc.getCamCode()));
		}
		log.info("Originator subclasses downloaded.");
		return ret;
	}
	
	private Map<String,Peva2DatingMethod> getDatingMethod() {		
		Map<String,Peva2DatingMethod> ret = new HashMap<>(); 
		var ldmReq = new ListDatingMethodRequest();
		ldmReq.setSize(100);
		var ldmResp = peva2.listDatingMethod(ldmReq);
		for(var dm:ldmResp.getDatingMethods().getDatingMethod()) {
			ret.put(dm.getId(), new Peva2DatingMethod(dm.getName(),dm.getType().toString(), dm.getCamCode()));
		}
		log.info("Dating methods downloaded.");
		return ret;
	}
	
	private Map<String, Peva2ThematicGroup> getThematicGroup() {
		Map<String, Peva2ThematicGroup> ret = new HashMap<>();
		var ltegReq = new ListThematicEvidenceGroupRequest();
		ltegReq.setSize(1000);
		var ltegResp = peva2.listThematicEvidenceGroup(ltegReq);
		for (var teg : ltegResp.getThematicEvidenceGroups().getThematicEvidenceGroup()) {
			ret.put(teg.getId(), new Peva2ThematicGroup(teg.getName(), teg.getCode()));
		}
		log.info("Thematic groups downloaded.");
		return ret;
	}
	
	private Map<String, Peva2Geo> getGeoTypes() {
		Map<String, Peva2Geo> ret = new HashMap<>();
		var lgotReq = new ListGeoObjectTypeRequest();
		lgotReq.setSize(1000);
		var lgotResp = peva2.listGeoObjectType(lgotReq);
		for(var lgot : lgotResp.getGeoObjectTypes().getGeoObjectType()) {
			ret.put(lgot.getId(), new Peva2Geo(lgot.getName(),lgot.getOSubClass()));
		}
		return ret;
	}

	public Peva2CodeLists downloadCodeLists() {
		return new Peva2CodeLists(getAccessibility(), getPhysicalState(), getIntegrity(), getFindingAidType(),
				getLanguages(), getEvidenceUnitTypes(), getFindingAidFormType(), getOriginatorSubclass(),
				getDatingMethod(), getThematicGroup(), getGeoTypes());
	}

}
