package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetGeoObjectTypeRequest;
import cz.aron.peva2.wsdl.ListGeoObjectTypeRequest;
import cz.aron.peva2.wsdl.PEvA;

public class GeoObjectTypeProvider extends CodeProvider<Peva2GeoObjectType> {

	private static final Logger log = LoggerFactory.getLogger(GeoObjectTypeProvider.class);

	public GeoObjectTypeProvider(PEvA peva2, Map<String, Peva2GeoObjectType> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2GeoObjectType downloadItem(String id) {
		var ggotReq = new GetGeoObjectTypeRequest();
		ggotReq.setId(id);
		var ggotResp = peva2.getGeoObjectType(ggotReq);
		var got = ggotResp.getGeoObjectType();
		log.info("Geo object type downloaded, uuid={}, name={}", got.getId(), got.getName());
		return new Peva2GeoObjectType(got.getName(), got.getOSubClass());
	}

	public static GeoObjectTypeProvider create(PEvA peva2) {
		Map<String, Peva2GeoObjectType> ret = new HashMap<>();
		var lgotReq = new ListGeoObjectTypeRequest();
		lgotReq.setSize(1000);
		var lgotResp = peva2.listGeoObjectType(lgotReq);
		for (var lgot : lgotResp.getGeoObjectTypes().getGeoObjectType()) {
			ret.put(lgot.getId(), new Peva2GeoObjectType(lgot.getName(), lgot.getOSubClass()));
		}
		log.info("Geo object types downloaded");
		return new GeoObjectTypeProvider(peva2, ret);
	}

}
