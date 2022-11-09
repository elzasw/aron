package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetOriginatorSubClassRequest;
import cz.aron.peva2.wsdl.ListOriginatorSubClassRequest;
import cz.aron.transfagent.peva.PEvA2Connection;

public class OriginatorSubClassProvider extends CodeProvider<Peva2OriginatorSubclass> {

	private static final Logger log = LoggerFactory.getLogger(OriginatorSubClassProvider.class);

	public OriginatorSubClassProvider(PEvA2Connection peva2, Map<String, Peva2OriginatorSubclass> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2OriginatorSubclass downloadItem(String id) {
		var gosReq = new GetOriginatorSubClassRequest();
		gosReq.setId(id);
		var gosResp = peva2.getPeva().getOriginatorSubClass(gosReq);
		log.info("Originator subclass downloaded uuid={}. name={}", gosResp.getOriginatorSubClass().getId(),
				gosResp.getOriginatorSubClass().getName());
		var sc = gosResp.getOriginatorSubClass();
		return new Peva2OriginatorSubclass(sc.getOClass().toString(), sc.getName(), sc.getCamCode());
	}

	public static OriginatorSubClassProvider create(PEvA2Connection peva2) {
		Map<String, Peva2OriginatorSubclass> ret = new HashMap<>();
		var losReq = new ListOriginatorSubClassRequest();
		losReq.setSize(100);
		var losResp = peva2.getPeva().listOriginatorSubClass(losReq);
		for (var sc : losResp.getOriginatorSubClasses().getOriginatorSubClass()) {
			ret.put(sc.getId(), new Peva2OriginatorSubclass(sc.getOClass().toString(), sc.getName(), sc.getCamCode()));
		}
		log.info("Originator subclasses downloaded.");
		return new OriginatorSubClassProvider(peva2, ret);
	}

}
