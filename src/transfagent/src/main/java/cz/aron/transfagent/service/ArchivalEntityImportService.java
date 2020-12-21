package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportAp;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.tacr.elza.ws.types.v1.EntitiesRequest;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.IdentifierList;

@Service
public class ArchivalEntityImportService implements SmartLifecycle {
	
	private static Logger log = LoggerFactory.getLogger(ArchivalEntityImportService.class);
	
	private final ElzaExportService elzaExportService;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final TransactionTemplate transactionTemplate;
	
	private final StorageService storageService;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private ThreadStatus status;
	
	public ArchivalEntityImportService(ElzaExportService elzaExportService,
			ArchivalEntityRepository archivalEntityRepository, TransactionTemplate transactionTemplate,
			StorageService storageService, ApuSourceRepository apuSourceRepository,
			CoreQueueRepository coreQueueRepository) {
		this.elzaExportService = elzaExportService;
		this.archivalEntityRepository = archivalEntityRepository;
		this.transactionTemplate = transactionTemplate;
		this.storageService = storageService;
		this.apuSourceRepository = apuSourceRepository;
		this.coreQueueRepository = coreQueueRepository;
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
			apuSourceBuilder = importAp.importAp(tmpDir.resolve("ap.xml"), 
					(ae.getUuid()!=null)?ae.getUuid().toString():null);
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
			// store subsequent hierarchical entities
			Set<Integer> elzaIds = importAp.getAccesileElzaIds();
			for(Integer elzaId: elzaIds) {
				addAccessibleByElzaId(elzaId);
			}
			final ArchivalEntity srcArchivalEntity = archivalEntityRepository.getOne(ae.getId());
			if(srcArchivalEntity.getUuid()==null) {
				// we have to add uuid from downloaded data and check that it does not exists
				UUID uuid = UUID.fromString(importAp.getApUuid());
				var archivalEntity = archivalEntityRepository.findByUuid(uuid);
				archivalEntity.ifPresentOrElse((ae2) -> {
					ae.setElzaId(srcArchivalEntity.getElzaId());
					archivalEntityRepository.save(ae);
					// drop redundant
					archivalEntityRepository.delete(srcArchivalEntity);
				},
			    ()-> {
			    	srcArchivalEntity.setUuid(uuid);
			    	saveApuSource(dataDir, srcArchivalEntity, apuSourceBuilder);
			    	archivalEntityRepository.save(srcArchivalEntity);
			    }
			    );
			} else {
				saveApuSource(dataDir, srcArchivalEntity, apuSourceBuilder);
			}
			
			return null;
		});
	}

	private void saveApuSource(Path dataDir, ArchivalEntity ae, ApuSourceBuilder apuSourceBuilder) {
		var apuSource = new cz.aron.transfagent.domain.ApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir("");
		apuSource.setSourceType(SourceType.INSTITUTION);
		apuSource.setUuid(UUID.fromString(apuSourceBuilder.getApusrc().getUuid()));
		apuSource.setDeleted(false);
		apuSource.setDateImported(ZonedDateTime.now());
		apuSource = apuSourceRepository.save(apuSource);

		ae.setStatus(EntityStatus.AVAILABLE);
		ae.setApuSource(apuSource);
		archivalEntityRepository.save(ae);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);
		
	}

	private void addAccessibleByElzaId(Integer elzaId) {
		Optional<ArchivalEntity> request = archivalEntityRepository.findByElzaId(elzaId);
		if(request.isEmpty()) {
			ArchivalEntity ae = new ArchivalEntity();
			ae.setElzaId(elzaId);
			ae.setStatus(EntityStatus.ACCESSIBLE);
			
			archivalEntityRepository.save(ae);
		}
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

}
