package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetMainEvidenceUnitTypeRequest;
import cz.aron.peva2.wsdl.ListMainEvidenceUnitTypeRequest;
import cz.aron.peva2.wsdl.PEvA;

public class EvidenceUnitTypeProvider extends CodeProvider<Peva2EvidenceUnitType> {
	
	private static final Logger log = LoggerFactory.getLogger(EvidenceUnitTypeProvider.class);

	public EvidenceUnitTypeProvider(PEvA peva2, Map<String, Peva2EvidenceUnitType> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2EvidenceUnitType downloadItem(String id) {
		var gmeutReq = new GetMainEvidenceUnitTypeRequest();
		gmeutReq.setId(id);
		var gmeutResp = peva2.getMainEvidenceUnitType(gmeutReq);
		var meut = gmeutResp.getMainEvidenceUnitType();
		if (meut.getPartialEvidenceUnitTypes() != null) {
			// vysledkem muze byt vice hodnot, main evidence unit vratim jako vysledek,
			// podrizene hodnoty zapisu primo do cache
			// predpokladam, ze nejprve se zepta na main evidence unit type
			for (var partial : meut.getPartialEvidenceUnitTypes()) {
				cache.put(partial.getId(), new Peva2EvidenceUnitType(partial.getName(), meut.getId()));
			}
		}
		return new Peva2EvidenceUnitType(meut.getName(), meut.getId());
	}

	public static EvidenceUnitTypeProvider create(PEvA peva2) {
		Map<String, Peva2EvidenceUnitType> ret = new HashMap<>();
		var meutReq = new ListMainEvidenceUnitTypeRequest();
		meutReq.setSize(100);
		var meutResp = peva2.listMainEvidenceUnitType(meutReq);
		for (var meut : meutResp.getMainEvidenceUnitTypes().getMainEvidenceUnitType()) {
			ret.put(meut.getId(), new Peva2EvidenceUnitType(meut.getName(), null));
			if (meut.getPartialEvidenceUnitTypes() != null) {
				for (var partial : meut.getPartialEvidenceUnitTypes()) {
					ret.put(partial.getId(), new Peva2EvidenceUnitType(partial.getName(), meut.getId()));
				}
			}
		}
		log.info("Evidence unit types downloaded {}.", meutResp.getMainEvidenceUnitTypes().getMainEvidenceUnitType().size());
		return new EvidenceUnitTypeProvider(peva2,ret);
	}
	
}
