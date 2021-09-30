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
import cz.aron.peva2.wsdl.ListFindingAidTypeRequest;
import cz.aron.peva2.wsdl.ListIntegrityRequest;
import cz.aron.peva2.wsdl.ListPhysicalStateRequest;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.peva2.wsdl.PhysicalState;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2CodeListDownloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2CodeListDownloader.class);
	
	private final PEvA peva2;
	
	public Peva2CodeListDownloader(PEvA peva2) {
		this.peva2 = peva2;
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
		/*
		var lfatReq = new ListFindingAidTypeRequest();
		lfatReq.setSize(100);
		var lfatResp = peva2.listFindingAidType(lfatReq);
		for (var findingAidType : lfatResp.getFindingAidTypes().getFindingAidType()) {
			ret.put(findingAidType.getId(), findingAidType.getName());
		}
		log.info("Finding aid type downloaded.");
		*/
		return ret;
	}

	public Peva2CodeLists downloadCodeLists() {
		return new Peva2CodeLists(getAccessibility(), getPhysicalState(), getIntegrity(), getFindingAidType());
	}

}
