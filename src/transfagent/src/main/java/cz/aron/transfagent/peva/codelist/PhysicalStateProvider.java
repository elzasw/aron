package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetPhysicalStateRequest;
import cz.aron.peva2.wsdl.ListPhysicalStateRequest;
import cz.aron.peva2.wsdl.PhysicalState;
import cz.aron.transfagent.peva.PEvA2Connection;

public class PhysicalStateProvider extends CodeProvider<String> {
	
	private static Logger log = LoggerFactory.getLogger(PhysicalStateProvider.class);

	public PhysicalStateProvider(PEvA2Connection peva2, Map<String, String> cached) {
		super(peva2, cached);
	}

	@Override
	public String downloadItem(String id) {
		var gpsReq = new GetPhysicalStateRequest();
		gpsReq.setId(id);
		var gpsResp = peva2.getPeva().getPhysicalState(gpsReq);
		var ps = gpsResp.getPhysicalState();
		log.info("Physical state downloaded, uuid={}, name={}",ps.getId(),ps.getName());
		return ps.getName();
	}

	public static PhysicalStateProvider create(PEvA2Connection peva2) {
        Map<String, String> ret = new HashMap<>();
        var lpsReq = new ListPhysicalStateRequest();
        lpsReq.setSize(100);
        var lpsResp = peva2.getPeva().listPhysicalState(lpsReq);
        for (PhysicalState physicalSate : lpsResp.getPhysicalStates().getPhysicalState()) {
            ret.put(physicalSate.getId(), physicalSate.getName());
        }
        log.info("Physical states downloaded.");
        return new PhysicalStateProvider(peva2, ret);
	}
}
