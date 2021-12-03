package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetDatingMethodRequest;
import cz.aron.peva2.wsdl.ListDatingMethodRequest;
import cz.aron.peva2.wsdl.PEvA;

public class DatingMethodProvider extends CodeProvider<Peva2DatingMethod> {

	private static final Logger log = LoggerFactory.getLogger(DatingMethodProvider.class);

	public DatingMethodProvider(PEvA peva2, Map<String, Peva2DatingMethod> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2DatingMethod downloadItem(String id) {
		var gdmReq = new GetDatingMethodRequest();
		gdmReq.setId(id);
		var gdmResp = peva2.getDatingMethod(gdmReq);
		var dm = gdmResp.getDatingMethod();
		log.info("Dating method downloaded uuid={}, name={}", dm.getId(), dm.getName());
		return new Peva2DatingMethod(dm.getName(), dm.getType().toString(), dm.getCamCode());
	}

	public static DatingMethodProvider create(PEvA peva2) {
		Map<String, Peva2DatingMethod> ret = new HashMap<>();
		var ldmReq = new ListDatingMethodRequest();
		ldmReq.setSize(100);
		var ldmResp = peva2.listDatingMethod(ldmReq);
		for (var dm : ldmResp.getDatingMethods().getDatingMethod()) {
			ret.put(dm.getId(), new Peva2DatingMethod(dm.getName(), dm.getType().toString(), dm.getCamCode()));
		}
		log.info("Dating methods downloaded.");
		return new DatingMethodProvider(peva2, ret);
	}

}
