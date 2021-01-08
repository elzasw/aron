package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportInstitution;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

/**
 *  Import instituce ze vstupniho adresare
 */
@Service
public class ImportInstitutionService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportInstitutionService.class);

    private final ApuSourceService apuSourceService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final ArchivalEntityRepository archivalEntityRepository;

    private final EntitySourceRepository entitySourceRepository;

    private final InstitutionRepository institutionRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationLoader configurationLoader;
    
    final FileImportService fileImportService;

    final private String INSTITUTIONS_DIR = "institutions";

    public ImportInstitutionService(ApuSourceService apuSourceService, ReimportService reimportService, StorageService storageService,
            ArchivalEntityRepository archivalEntityRepository, EntitySourceRepository entitySourceRepository,
            InstitutionRepository institutionRepository, ApuSourceRepository apuSourceRepository,
            CoreQueueRepository coreQueueRepository, DatabaseDataProvider databaseDataProvider, TransactionTemplate transactionTemplate,
            ConfigurationLoader configurationLoader,
            final FileImportService fileImportService) {
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.archivalEntityRepository = archivalEntityRepository;
        this.entitySourceRepository = entitySourceRepository;
        this.institutionRepository = institutionRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.transactionTemplate = transactionTemplate;
        this.configurationLoader = configurationLoader;
        this.fileImportService = fileImportService;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }

    @Override
    public int getPriority() { return 9; }
    
    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(INSTITUTIONS_DIR);
    }

	/**
	 * Najde soubor institution-${kod_archivu}.xml, vytori z nej apusrc.xml, vytvori nebo aktualizuje zaznamy v databazi a vytvori odesilaci udalost.
	 * @param dir zpracovavany adresar
	 * @return true - continue processing next directory, false - stop processing 
	 */
	@Override
	public boolean processDirectory(Path dir) {
		List<Path> xmls;
		try (var stream = Files.list(dir)) {
			 xmls = stream
					.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("institution")
							&& f.getFileName().toString().endsWith(".xml"))
					.collect(Collectors.toList());
		} catch (IOException ioEx) {
			log.error("Fail to read directory {}", dir, ioEx);
			throw new UncheckedIOException(ioEx);
		}

		var inst = xmls.stream().filter(p -> p.getFileName().toString().startsWith("institution-")
				&& p.getFileName().toString().endsWith(".xml")).findFirst();

		if (inst.isEmpty()) {
			log.warn("Directory is empty {}", dir);
			return false;
		}

		var fileName = inst.get().getFileName().toString();
		var tmp = fileName.substring("institution-".length());
		var code = tmp.substring(0,tmp.length()-".xml".length());

		var ii = new ImportInstitution();
		ApuSourceBuilder apusrcBuilder;

		try {
			apusrcBuilder = ii.importInstitution(inst.get(), code);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		} catch (JAXBException e1) {
			throw new IllegalStateException(e1);
		}

		var institution = institutionRepository.findByCode(code);
		if (institution!=null) {
			apusrcBuilder.getApusrc().setUuid(institution.getApuSource().getUuid().toString());
			apusrcBuilder.getApusrc().getApus().getApu().get(0).setUuid(institution.getUuid().toString());
		}

		try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
			apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}
		
		Path dataDir;
	    try {
	        dataDir = storageService.moveToDataDir(dir);
	    } catch (IOException e) {
	        throw new IllegalStateException(e);
	    }

		if (institution == null) {
			createInstitution(dataDir, dir, apusrcBuilder, code, ii);
		} else {
			updateInstitution(institution, dataDir, dir, apusrcBuilder, code, ii);
		}
		return true;
	}

	private void createInstitution(Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String institutionCode,
			ImportInstitution ii) {

		var institutionUuid = UUID.fromString(apusrcBuilder.getApusrc().getApus().getApu().get(0).getUuid());

		transactionTemplate.execute(t -> {
			UUID apusrcUuid = UUID.fromString(apusrcBuilder.getApusrc().getUuid());
			// instituce neexistuje, vytvorim novou			
			var apuSource = apuSourceService.createApuSource(apusrcUuid, SourceType.INSTITUTION, 
					dataDir, origDir.getFileName().toString());

			var newInstitution = new Institution();
			newInstitution.setApuSource(apuSource);
			newInstitution.setCode(institutionCode);
			newInstitution.setSource("source");
			newInstitution.setUuid(institutionUuid);
			newInstitution = institutionRepository.save(newInstitution);

			if (ii.getApRefUuid() != null) {
				var apUuid = ii.getApRefUuid();
				createArchivalEntityIfNotExist(apUuid, apuSource, institutionCode, false);
			}

			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			coreQueueRepository.save(coreQueue);
			return null;
		});
		log.info("Institution created code={}, uuid={}", institutionCode, institutionUuid);
	}

	private void updateInstitution(Institution institution, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String institutionCode, ImportInstitution ii) {

		var origData = institution.getApuSource().getDataDir();

		transactionTemplate.execute(t -> {			
			// aktualizace, pouze zmenim datovy adresar
			var apuSource = institution.getApuSource();
			apuSource.setDataDir(dataDir.toString());
			apuSource.setOrigDir(origDir.getFileName().toString());
			
			if (ii.getApRefUuid() != null) {					
				var apUuid = ii.getApRefUuid();						
				createArchivalEntityIfNotExist(apUuid, apuSource, institutionCode, true);
			}

			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			
			apuSourceRepository.save(apuSource);
			institutionRepository.save(institution);
			coreQueueRepository.save(coreQueue);
			return null;
		});
		log.info("Institution updated code={}, uuid={}, original data dir {}", institutionCode, institution.getUuid(),
				origData);
	}

	private void createArchivalEntityIfNotExist(UUID apUuid, cz.aron.transfagent.domain.ApuSource apuSource,
			String institutionCode, boolean expectExist) {
		// overim existenci ArchivalEntity
		var existingArchivalEntity = archivalEntityRepository.findByUuid(apUuid);
		if (existingArchivalEntity.isEmpty()) {
			// vytvorim archivni entitu
			var archivalEntity = new ArchivalEntity();
			archivalEntity.setStatus(EntityStatus.ACCESSIBLE);
			archivalEntity.setUuid(apUuid);
			archivalEntity = archivalEntityRepository.save(archivalEntity);

			var entitySource = new EntitySource();
			entitySource.setApuSource(apuSource);
			entitySource.setArchivalEntity(archivalEntity);
			entitySourceRepository.save(entitySource);
			log.info("ArchivalEntity created {}", apUuid);
		} else {
			if (!expectExist) {
				// nemela by existovat, zaloguji warning
				log.warn("Import institution {}, archival entity {} already exist", institutionCode, apUuid);
			}
		}
	}

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.INSTITUTION)
            return Result.UNSUPPORTED;

        var institution = institutionRepository.findByApuSource(apuSource);
        if (institution == null) {
            log.error("Missing institution: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        String fileName = "institution-"+institution.getCode()+".xml";

        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        ApuSourceBuilder apuSourceBuilder;
        final var ii = new ImportInstitution();
        try {
            apuSourceBuilder = ii.importInstitution(apuDir.resolve(fileName), institution.getCode());
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileName, apuDir, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
    }

}
