package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetIntegrityRequest;
import cz.aron.peva2.wsdl.Integrity;
import cz.aron.peva2.wsdl.ListIntegrityRequest;
import cz.aron.transfagent.peva.PEvA2Connection;

public class IntegrityProvider extends CodeProvider<String> {
	
	private static final Logger log = LoggerFactory.getLogger(IntegrityProvider.class);

	public IntegrityProvider(PEvA2Connection peva2, Map<String, String> cached) {
		super(peva2, cached);
	}

	@Override
	public String downloadItem(String id) {
		var giReq = new GetIntegrityRequest();
		giReq.setId(id);
		var giResp = peva2.getPeva().getIntegrity(giReq);
		var i = giResp.getIntegrity();
		log.info("Integrity downloaded, uuid={}, name={}", i.getId(), i.getName());
		return i.getName();
	}

	public static IntegrityProvider create(PEvA2Connection peva2) {
		Map<String, String> ret = new HashMap<>();
        var liReq = new ListIntegrityRequest();
        liReq.setSize(100);
        var liResp = peva2.getPeva().listIntegrity(liReq);
        for (Integrity integrity : liResp.getIntegrities().getIntegrity()) {
            ret.put(integrity.getId(), integrity.getName());
        }
        log.info("Integrity downloaded.");
        return new IntegrityProvider(peva2,ret);		
	}

}
