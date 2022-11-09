package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.Accessibility;
import cz.aron.peva2.wsdl.GetAccessibilityRequest;
import cz.aron.peva2.wsdl.ListAccessibilityRequest;
import cz.aron.transfagent.peva.PEvA2Connection;

public class AccessibilityProvider extends CodeProvider<String> {

	private static final Logger log = LoggerFactory.getLogger(AccessibilityProvider.class);

	public AccessibilityProvider(PEvA2Connection peva2, Map<String, String> cache) {
		super(peva2, cache);
	}

	@Override
	public String downloadItem(String id) {
		var gaReq = new GetAccessibilityRequest();
		gaReq.setId(id);
		var gaResp = peva2.getPeva().getAccessibility(gaReq);
		log.info("Accessibility downloaded uuid={}, name={}", gaResp.getAccessibility().getId(),
				gaResp.getAccessibility().getName());
		return gaResp.getAccessibility().getName();
	}

	public static AccessibilityProvider create(PEvA2Connection peva2) {
		Map<String, String> ret = new HashMap<>();
		var laReq = new ListAccessibilityRequest();
		laReq.setSize(100);
		var laResp = peva2.getPeva().listAccessibility(laReq);
		for (Accessibility accessibility : laResp.getAccessibilities().getAccessibility()) {
			ret.put(accessibility.getId(), accessibility.getName());
		}
		log.info("Accessibilities list downloaded.");
		return new AccessibilityProvider(peva2, ret);
	}

}
