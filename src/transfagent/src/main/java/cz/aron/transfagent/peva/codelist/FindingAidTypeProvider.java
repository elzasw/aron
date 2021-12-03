package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetFindingAidTypeRequest;
import cz.aron.peva2.wsdl.ListFindingAidTypeRequest;
import cz.aron.peva2.wsdl.PEvA;

public class FindingAidTypeProvider extends CodeProvider<String> {

	private static final Logger log = LoggerFactory.getLogger(FindingAidTypeProvider.class);

	public FindingAidTypeProvider(PEvA peva2, Map<String, String> cached) {
		super(peva2, cached);
	}

	@Override
	public String downloadItem(String id) {
		var gfatReq = new GetFindingAidTypeRequest();
		gfatReq.setId(id);
		var gfatResp = peva2.getFindingAidType(gfatReq);
		log.info("Finding aid type downloaded uuid={}, type={}", gfatResp.getFindingAidType().getId(),
				gfatResp.getFindingAidType().getName());
		return gfatResp.getFindingAidType().getName();
	}

	public static FindingAidTypeProvider create(PEvA peva2) {
		Map<String, String> ret = new HashMap<>();
		var lfatReq = new ListFindingAidTypeRequest();
		lfatReq.setSize(100);
		var lfatResp = peva2.listFindingAidType(lfatReq);
		for (var findingAidType : lfatResp.getFindingAidTypes().getFindingAidType()) {
			ret.put(findingAidType.getId(), findingAidType.getName());
		}
		log.info("Finding aid type downloaded.");
		return new FindingAidTypeProvider(peva2, ret);
	}

}
