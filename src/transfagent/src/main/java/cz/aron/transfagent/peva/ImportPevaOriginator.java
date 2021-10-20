package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.service.ArchivalEntityImportService;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportOriginatorService.OriginatorImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportPevaOriginator implements OriginatorImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaOriginator.class);
	
	private static final String PREFIX = "pevaoriginator-";
	
	private final ApTypeService apTypeService;
	
	private final Peva2CodeListProvider codeLists;
	
	private final DatabaseDataProvider contextDataProvider;
	
	private final ConfigurationLoader configurationLoader;
	
	private final StorageService storageService;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final EntitySourceRepository entitySourceRepository;
	
	private final ArchivalEntityImportService archivalEntityImportService;
	
	private final ArchivalEntityService archivalEntityService;

	public ImportPevaOriginator(ApTypeService apTypeService, Peva2CodeListProvider codeLists,
			DatabaseDataProvider contextDataProvider, ConfigurationLoader configurationLoader,
			StorageService storageService, ArchivalEntityRepository archivalEntityRepository,
			EntitySourceRepository entitySourceRepository, ArchivalEntityImportService archivalEntityImportService,
			ArchivalEntityService archivalEntityService) {
		this.apTypeService = apTypeService;
		this.codeLists = codeLists;
		this.contextDataProvider = contextDataProvider;
		this.configurationLoader = configurationLoader;
		this.storageService = storageService;
		this.archivalEntityRepository = archivalEntityRepository;
		this.entitySourceRepository = entitySourceRepository;
		this.archivalEntityImportService = archivalEntityImportService;
		this.archivalEntityService = archivalEntityService;
	}

	@Override
	public ImportResult processPath(Path path) {
		
		if (!path.getFileName().toString().startsWith(PREFIX)) {
			return ImportResult.UNSUPPORTED;
		}
		
		List<Path> xmls;
		try (var stream = Files.list(path)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
		
		var originatorXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();
		
		if (originatorXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", path);
            return ImportResult.FAIL;
		}
		
		if (processOriginator(path,originatorXml.get())) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;	
		}
	}

	private boolean processOriginator(Path dir, Path originatorXml) {
		ApuSourceBuilder apuSourceBuilder = null;
		var ipfi = new ImportPevaOriginatorInfo(apTypeService, codeLists);
		try {
			apuSourceBuilder = ipfi.importOriginator(originatorXml, contextDataProvider);
			try (var os = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
			}			
			var dataDir = storageService.moveToDataDir(dir);			
			archivalEntityService.createOrUpdateArchivalEntity(dataDir, dir, UUID.fromString(apuSourceBuilder.getMainApu().getUuid()));						
		} catch (Exception e) {
			log.error("Fail to import originator", e);
			throw new IllegalStateException(e);
		}
		return false;
	}
	
	@Override
	public Result reimport(ApuSource apuSource) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
