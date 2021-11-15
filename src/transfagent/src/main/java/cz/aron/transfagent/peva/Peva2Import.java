package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2Import implements ImportProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2Import.class);

	private final FileImportService importService;

	private final ConfigPeva2 config;

	private final PEvA peva2;

	private final List<Peva2Downloader> downloaders = new ArrayList<>();
	
	private final ApplicationContext applicationContext;
	
	private final Peva2CodeListProvider codeListProvider;
	
	private final StorageService storageService;

	private OffsetDateTime nextRun = null;

	public Peva2Import(FileImportService importService, ConfigPeva2 config, PEvA peva2,
			Peva2CodeListProvider codeListProvider, ApplicationContext applicationContext,
			StorageService storageService) {
		this.importService = importService;
		this.config = config;
		this.peva2 = peva2;
		this.codeListProvider = codeListProvider;
		this.applicationContext = applicationContext;
		this.storageService = storageService;
		PEvA2Client.fillHeaders(peva2, config);		
	}

	@PostConstruct
	void init() {
		// create downloaders		
		try {
			// conditional bean
			downloaders.add(applicationContext.getBean(Peva2ImportOriginators.class));
		} catch (NoSuchBeanDefinitionException e) {
			log.info("Import originators disabled.");
		}
		try {
			// conditional bean
			downloaders.add(applicationContext.getBean(Peva2ImportFindingAidCopy.class));	
		} catch (NoSuchBeanDefinitionException e) {
			log.info("Import finding aid copies disabled.");
		}
		downloaders.add(applicationContext.getBean(Peva2ImportFunds.class));
		downloaders.add(applicationContext.getBean(Peva2ImportFindingAids.class));
		importService.registerImportProcessor(this);
	}

	@Override
	public void importData(ImportContext ic) {

		final OffsetDateTime nowTime = OffsetDateTime.now();

		processCommands();
		
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
	
	private void processCommands() {
		Path commandsPath = storageService.getInputPath().resolve("commands");
		if (Files.isDirectory(commandsPath)) {
			try (Stream<Path> stream = Files.list(commandsPath);) {
				stream.forEach(f -> {
					if (Files.isRegularFile(f)) {
						for (var downloader : downloaders) {
							try {
								if (downloader.processCommand(f, codeListProvider)) {
									try {
										Files.deleteIfExists(f);
									} catch (IOException e) {
										log.error("Fail to delete command file {}", f);
										throw new UncheckedIOException(e);
									}
									break;
								}
							} catch (Exception e) {
								log.error("Fail to process command file {}", f, e);
								throw new IllegalStateException(e);
							}
						}
					}
				});
			} catch (IOException ioEx) {
				log.error("Fail to read commands directory {}", commandsPath, ioEx);
				throw new UncheckedIOException(ioEx);
			}
		}
	}

	public Peva2CodeLists getCodeLists() {
		return codeListProvider.getCodeLists();
	}
	
}
