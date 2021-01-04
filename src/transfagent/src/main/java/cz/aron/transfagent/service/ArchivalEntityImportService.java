package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportAp;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.transformation.DatabaseDataProvider;
import cz.tacr.elza.ws.types.v1.EntitiesRequest;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.IdentifierList;

@Service
public class ArchivalEntityImportService implements SmartLifecycle, ReimportProcessor {
	
	private static Logger log = LoggerFactory.getLogger(ArchivalEntityImportService.class);
	
	private final ElzaExportService elzaExportService;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final TransactionTemplate transactionTemplate;
	
	private final InstitutionRepository institutionRepository;
	
	private final StorageService storageService;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final ApuSourceService apuSourceService;
	
	private final ReimportService reimportService;
	
	private ThreadStatus status;
	
	public ArchivalEntityImportService(ElzaExportService elzaExportService,
			ArchivalEntityRepository archivalEntityRepository, TransactionTemplate transactionTemplate,
			StorageService storageService, ApuSourceRepository apuSourceRepository,
			InstitutionRepository institutionRepository,
			ApuSourceService apuSourceService,
			CoreQueueRepository coreQueueRepository,
			final ReimportService reimportService) {
		this.elzaExportService = elzaExportService;
		this.archivalEntityRepository = archivalEntityRepository;
		this.transactionTemplate = transactionTemplate;
		this.storageService = storageService;
		this.apuSourceRepository = apuSourceRepository;
		this.institutionRepository = institutionRepository;
		this.apuSourceService = apuSourceService;
		this.coreQueueRepository = coreQueueRepository;
		this.reimportService = reimportService;
	}
	
	@PostConstruct
	void init() {
		reimportService.registerReimportProcessor(this);
	}
	
	private void importEntities() {
		var ids = archivalEntityRepository.findTop1000ByStatusOrderById(EntityStatus.ACCESSIBLE);
		while (!ids.isEmpty()) {
			for (var id : ids) {
				importEntity(id.getId());
				if (status != ThreadStatus.RUNNING) {
					return;
				}
			}
			ids = archivalEntityRepository.findTop1000ByStatusOrderById(EntityStatus.ACCESSIBLE);
		}
	}
	
	private void importEntity(final ArchivalEntity ae) {
		Path tmpDir = downloadEntity(ae);
		ApuSourceBuilder apuSourceBuilder;
		final var importAp = new ImportAp();
		try {
			DatabaseDataProvider ddp = new DatabaseDataProvider(institutionRepository, archivalEntityRepository);
			
			apuSourceBuilder = importAp.importAp(tmpDir.resolve("ap.xml"), 
					(ae.getUuid()!=null)?ae.getUuid().toString():null,
							ddp);
			try (var os = Files.newOutputStream(tmpDir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os);
			}
						
		} catch (Exception e) {
			log.error("Fail to process downloaded ap.xml, dir={}", tmpDir, e);
			try {
				FileSystemUtils.deleteRecursively(tmpDir);
			} catch (IOException e1) {
				log.error("Fail to delete directory {}", tmpDir, e1);
			}
			throw new IllegalStateException(e);
		}

		Path dataDir;
		try {
			dataDir = storageService.moveToDataDir(tmpDir);
		} catch (Exception e) {
			log.error("Fail to move to data directory, src={}", tmpDir);
			try {
				FileSystemUtils.deleteRecursively(tmpDir);
			} catch (IOException e1) {
				log.error("Fail to delete directory {}", tmpDir, e1);
			}
			throw new IllegalStateException(e);
		}

		transactionTemplate.execute(t -> { 
			saveEntity(dataDir, importAp, apuSourceBuilder, ae);
			return null;
		});
		
	}

	private void saveEntity(Path dataDir, ImportAp importAp, ApuSourceBuilder apuSourceBuilder, ArchivalEntity ae) {
		// store subsequent hierarchical entities
		ArchivalEntity parentEntity;
		if (importAp.getParentElzaId() != null) {
			parentEntity = addAccessibleByElzaId(importAp.getParentElzaId());
		} else {
			parentEntity = null;
		}

		final ArchivalEntity srcArchivalEntity = archivalEntityRepository.getOne(ae.getId());
		if (srcArchivalEntity.getUuid() == null) {
			// we have to add uuid from downloaded data and check that it does not exists
			UUID uuid = UUID.fromString(importAp.getApUuid());
			var archivalEntity = archivalEntityRepository.findByUuid(uuid);
			if (archivalEntity.isPresent()) {
				ae.setElzaId(srcArchivalEntity.getElzaId());
				ae.setParentEntity(parentEntity);
				archivalEntityRepository.save(ae);
				// drop redundant
				archivalEntityRepository.delete(srcArchivalEntity);
				
				reindexAfterEntityChanged(ae);
				return;
			} else {
				srcArchivalEntity.setUuid(uuid);
			}
		}
		// update elza id
		if (srcArchivalEntity.getElzaId() == null) {
			srcArchivalEntity.setElzaId(importAp.getElzaId());
		}
		srcArchivalEntity.setParentEntity(parentEntity);
		saveApuSource(dataDir, srcArchivalEntity, apuSourceBuilder);

		// entity received new uuid
		// -> we have to reindex all entities having this entity as parent
		reindexAfterEntityChanged(srcArchivalEntity);
	}

	private void reindexAfterEntityChanged(ArchivalEntity archivalEntity) {
		List<ArchivalEntity> ents = this.archivalEntityRepository.findAllByParentEntity(archivalEntity);
		for(ArchivalEntity ent: ents) {
			apuSourceService.reimport(ent.getApuSource());			
		}
	}

	private void saveApuSource(Path dataDir, ArchivalEntity ae, ApuSourceBuilder apuSourceBuilder) {
		UUID apusrcUuid = UUID.fromString(apuSourceBuilder.getApusrc().getUuid());
		var apuSource = apuSourceService.createApuSource(apusrcUuid, SourceType.ARCH_ENTITY, dataDir, "");
		
		ae.setStatus(EntityStatus.AVAILABLE);
		ae.setApuSource(apuSource);
		archivalEntityRepository.save(ae);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);
		
	}

	private ArchivalEntity addAccessibleByElzaId(Integer elzaId) {
		Optional<ArchivalEntity> request = archivalEntityRepository.findByElzaId(elzaId);
		return request.orElseGet(()->{		
			ArchivalEntity ae = new ArchivalEntity();
			ae.setElzaId(elzaId);
			ae.setStatus(EntityStatus.ACCESSIBLE);
			
			return archivalEntityRepository.save(ae);
		});
	}

	private void importEntity(Integer archivalEntityId) {
		
		var archivalEntity = archivalEntityRepository.findById(archivalEntityId);				
		archivalEntity.ifPresentOrElse(
				ae -> {
					importEntity(ae);
					
					log.info("ArchivalEntity imported {}", archivalEntityId);					
		}, () -> {
			log.error("ArchivalEntity id={}, not exist",archivalEntityId);
		});				

	}
	
	private Path downloadEntity(ArchivalEntity ae) {
		
		var exportService = elzaExportService.get();		
		var identifierList = new IdentifierList();
		String ident;
		if(ae.getUuid()!=null) {
			ident = ae.getUuid().toString();
		} else 
		if(ae.getElzaId()!=null) {
			ident = ae.getElzaId().toString();
		} else {
			log.error("Missing identifier for archivale entity, id: {}", ae.getId());
			throw new RuntimeException("Failed to import archival entity: "+ae.getId());
		}
		identifierList.getIdentifier().add(ident);		
		var entitiesRequest = new EntitiesRequest();
		entitiesRequest.setIdentifiers(identifierList);		
		var exportRequest = new ExportRequest();
		exportRequest.setRequiredFormat("http://elza.tacr.cz/schema/v2");
		exportRequest.setEntities(entitiesRequest);				
		var response = exportService.exportData(exportRequest);
		
		Path tempDir;
		try {
			tempDir = storageService.createTempDir("ae-"+ae.getId().toString());		
		} catch (IOException e) {
			log.error("Fail to create temp directory",e);
			throw new IllegalStateException(e);
		}
		
		var dataHandler = response.getBinData();
		try(var os = Files.newOutputStream(tempDir.resolve("ap.xml"))) {
			dataHandler.writeTo(os);	
		} catch (IOException e) {
			log.error("Fail to write downloaded ap",e);
			try {
				FileSystemUtils.deleteRecursively(tempDir);
			} catch (IOException e1) {
				log.error("Fail to delete directory",e1);
			}
			throw new IllegalStateException(e);
		}
		return tempDir;		
	}

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                importEntities();
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("Error in import file. ", e);
                try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					return;
				}
            }
        }
        status = ThreadStatus.STOPPED;
    }


	@Override
	public void start() {
        status = ThreadStatus.RUNNING;
        new Thread(() -> {
            run();            
        }).start();
	}

	@Override
	public void stop() {
        status = ThreadStatus.STOP_REQUEST;
	}

	@Override
	public boolean isRunning() {
		return status == ThreadStatus.RUNNING;
	}

	@Override
	public boolean reimport(ApuSource apuSource) {
		if(apuSource.getSourceType()!=SourceType.ARCH_ENTITY)
			return false;
		
		ArchivalEntity archEntity = archivalEntityRepository.findByApuSource(apuSource);
		if(archEntity==null) {
			log.error("Missing archival entity: {}", apuSource.getId());
			return false;
		}
		if(archEntity.getStatus()!=EntityStatus.AVAILABLE) {
			log.warn("Archival entity {} cannot be reimported, status: {}", apuSource.getId(), archEntity.getStatus());
			return true;
		}
		
		Path apuDir = storageService.getApuDataDir(apuSource.getDataDir());		
		ApuSourceBuilder apuSourceBuilder;
		final var importAp = new ImportAp();
		try {
			DatabaseDataProvider ddp = new DatabaseDataProvider(institutionRepository, archivalEntityRepository);
			
			apuSourceBuilder = importAp.importAp(apuDir.resolve("ap.xml"), archEntity.getUuid().toString(), ddp);
			try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os);
			}
						
		} catch (Exception e) {
			log.error("Fail to process downloaded ap.xml, dir={}", apuDir, e);
			return false;
		}
		return true;
	}

}
