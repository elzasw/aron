package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetFindingAidFormTypeRequest;
import cz.aron.peva2.wsdl.ListFindingAidFormTypeRequest;
import cz.aron.transfagent.peva.PEvA2Connection;

public class FindingAidFormTypeProvider extends CodeProvider<String> {

	private static final Logger log = LoggerFactory.getLogger(FindingAidFormTypeProvider.class);

	public FindingAidFormTypeProvider(PEvA2Connection peva2, Map<String, String> cached) {
		super(peva2, cached);
	}

	@Override
	public String downloadItem(String id) {
		var gfaftReq = new GetFindingAidFormTypeRequest();
		gfaftReq.setId(id);
		var gfatfResp = peva2.getPeva().getFindingAidFormType(gfaftReq);
		return gfatfResp.getFindingAidFormType().getName();
	}

	public static FindingAidFormTypeProvider create(PEvA2Connection peva2) {
		Map<String, String> ret = new HashMap<>();
		var lfaftReq = new ListFindingAidFormTypeRequest();
		lfaftReq.setSize(100);
		var lfaftResp = peva2.getPeva().listFindingAidFormType(lfaftReq);
		for (var findingAidType : lfaftResp.getFindingAidFormTypes().getFindingAidFormType()) {
			ret.put(findingAidType.getId(), findingAidType.getName());
		}
		log.info("Finding aid form type downloaded.");
		return new FindingAidFormTypeProvider(peva2, ret);
	}

}
