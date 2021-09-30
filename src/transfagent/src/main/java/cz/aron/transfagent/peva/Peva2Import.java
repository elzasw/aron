package cz.aron.transfagent.peva;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2Import implements ImportProcessor {

	private final FileImportService importService;

	private final ConfigPeva2 config;

	private final PEvA peva2;

	private final List<Peva2Downloader> downloaders = new ArrayList<>();
	
	private final ApplicationContext applicationContext;
	
	private final Peva2CodeListProvider codeListProvider;

	private OffsetDateTime nextRun = null;

	public Peva2Import(FileImportService importService, ConfigPeva2 config, PEvA peva2,
			Peva2CodeListDownloader codeListDownloader, ApplicationContext applicationContext) {
		this.importService = importService;
		this.config = config;
		this.peva2 = peva2;
		this.codeListProvider = new Peva2CodeListProvider(codeListDownloader);
		this.applicationContext = applicationContext;
		
		PEvA2Client.fillHeaders(peva2, config);		
	}

	@PostConstruct
	void init() {
		// create downloaders
		//downloaders.add(applicationContext.getBean(Peva2ImportOriginators.class));
		downloaders.add(applicationContext.getBean(Peva2ImportFunds.class));
		downloaders.add(applicationContext.getBean(Peva2ImportFindingAids.class));
		importService.registerImportProcessor(this);
	}

	@Override
	public void importData(ImportContext ic) {

		final OffsetDateTime nowTime = OffsetDateTime.now();

		if (nextRun != null && nextRun.isAfter(OffsetDateTime.now())) {
			// TODO nejake lepsi planovani
			return;
		}
		
		codeListProvider.reset();
		for (var downloader : downloaders) {
			downloader.importDataInternal(ic, codeListProvider);
		}

		// po uspesne synchronizaci nastavim dalsi cas synchronizace
		nextRun = nowTime.plusSeconds(config.getInterval());
	}

	public Peva2CodeLists getCodeLists() {
		return codeListProvider.getCodeLists();
	}
	
}
