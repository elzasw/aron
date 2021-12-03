package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetThematicEvidenceGroupRequest;
import cz.aron.peva2.wsdl.ListThematicEvidenceGroupRequest;
import cz.aron.peva2.wsdl.PEvA;

public class ThematicEvidenceGroupProvider extends CodeProvider<Peva2ThematicEvidenceGroup> {

	private static final Logger log = LoggerFactory.getLogger(ThematicEvidenceGroupProvider.class);

	public ThematicEvidenceGroupProvider(PEvA peva2, Map<String, Peva2ThematicEvidenceGroup> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2ThematicEvidenceGroup downloadItem(String id) {
		var gtegReq = new GetThematicEvidenceGroupRequest();
		gtegReq.setId(id);
		var gtegResp = peva2.getThematicEvidenceGroup(gtegReq);
		var teg = gtegResp.getThematicEvidenceGroup();
		log.info("Thematic evidence group downloaded, uuid={}, name={}", teg.getId(), teg.getName());
		return new Peva2ThematicEvidenceGroup(teg.getName(), teg.getCode());
	}

	public static ThematicEvidenceGroupProvider create(PEvA peva2) {
		Map<String, Peva2ThematicEvidenceGroup> ret = new HashMap<>();
		var ltegReq = new ListThematicEvidenceGroupRequest();
		ltegReq.setSize(1000);
		var ltegResp = peva2.listThematicEvidenceGroup(ltegReq);
		for (var teg : ltegResp.getThematicEvidenceGroups().getThematicEvidenceGroup()) {
			ret.put(teg.getId(), new Peva2ThematicEvidenceGroup(teg.getName(), teg.getCode()));
		}
		log.info("Thematic groups downloaded.");
		return new ThematicEvidenceGroupProvider(peva2, ret);
	}

}
