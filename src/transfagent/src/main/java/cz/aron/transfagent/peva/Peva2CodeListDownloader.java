package cz.aron.transfagent.peva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.peva.codelist.AccessibilityProvider;
import cz.aron.transfagent.peva.codelist.DatingMethodProvider;
import cz.aron.transfagent.peva.codelist.EvidenceUnitTypeProvider;
import cz.aron.transfagent.peva.codelist.FindingAidFormTypeProvider;
import cz.aron.transfagent.peva.codelist.FindingAidTypeProvider;
import cz.aron.transfagent.peva.codelist.GeoObjectTypeProvider;
import cz.aron.transfagent.peva.codelist.IntegrityProvider;
import cz.aron.transfagent.peva.codelist.LanguageProvider;
import cz.aron.transfagent.peva.codelist.OriginatorSubClassProvider;
import cz.aron.transfagent.peva.codelist.PhysicalStateProvider;
import cz.aron.transfagent.peva.codelist.ThematicEvidenceGroupProvider;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2CodeListDownloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2CodeListDownloader.class);
	
	private final PEvA peva2;
	
	private final StorageService storageService;
	
	public Peva2CodeListDownloader(PEvA peva2, StorageService storageService) {
		this.peva2 = peva2;
		this.storageService = storageService;
	}
	
	public Peva2CodeLists downloadCodeLists() {
		return new Peva2CodeLists(AccessibilityProvider.create(peva2), PhysicalStateProvider.create(peva2), IntegrityProvider.create(peva2),
				FindingAidTypeProvider.create(peva2), LanguageProvider.create(peva2),
				EvidenceUnitTypeProvider.create(peva2), FindingAidFormTypeProvider.create(peva2),
				OriginatorSubClassProvider.create(peva2), DatingMethodProvider.create(peva2),
				ThematicEvidenceGroupProvider.create(peva2), GeoObjectTypeProvider.create(peva2));
	}

}
