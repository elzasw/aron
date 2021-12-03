package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetLanguageRequest;
import cz.aron.peva2.wsdl.ListLanguageRequest;
import cz.aron.peva2.wsdl.PEvA;

public class LanguageProvider extends CodeProvider<Peva2Language> {

	private static final Logger log = LoggerFactory.getLogger(LanguageProvider.class);

	public LanguageProvider(PEvA peva2, Map<String, Peva2Language> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2Language downloadItem(String id) {
		var glReq = new GetLanguageRequest();
		glReq.setId(id);
		var glResp = peva2.getLanguage(glReq);
		var lang = glResp.getLanguage();
		log.info("Language downloaded uuid={}, name={}", lang.getId(), lang.getName());
		return new Peva2Language(lang.getCode(), lang.getName());
	}

	public static LanguageProvider create(PEvA peva2) {
		Map<String, Peva2Language> ret = new HashMap<>();
		var llReq = new ListLanguageRequest();
		llReq.setSize(1000);
		var llResp = peva2.listLanguage(llReq);
		for (var lang : llResp.getLanguages().getLanguage()) {
			ret.put(lang.getId(), new Peva2Language(lang.getCode(), lang.getName()));
		}
		log.info("Languages downloaded.");
		return new LanguageProvider(peva2, ret);
	}

}
