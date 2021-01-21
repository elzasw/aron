package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.common.BulkOperation;
import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportAp;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.transformation.DatabaseDataProvider;
import cz.tacr.elza.ws.types.v1.EntitiesRequest;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.IdentifierList;

@Service
public class ArchivalEntityImportService implements /*SmartLifecycle,*/ ReimportProcessor, ImportProcessor {
	
	private static Logger log = LoggerFactory.getLogger(ArchivalEntityImportService.class);

	private final ElzaExportService elzaExportService;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final TransactionTemplate transactionTemplate;
		
	private final StorageService storageService;
			
	private final CoreQueueRepository coreQueueRepository;
	
	private final ApuSourceService apuSourceService;
	
	private final ReimportService reimportService;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final EntitySourceRepository entitySourceRepository;
	
	private final FileImportService importService;
	
	private final ConfigurationLoader configurationLoader;
	
	private final ConfigElza configElza;
	
	//private ThreadStatus status;
	
	public ArchivalEntityImportService(ElzaExportService elzaExportService,
			ArchivalEntityRepository archivalEntityRepository, TransactionTemplate transactionTemplate,
			StorageService storageService,
			ApuSourceService apuSourceService,
			CoreQueueRepository coreQueueRepository,
			final DatabaseDataProvider databaseDataProvider,
			final EntitySourceRepository entitySourceRepository,
			final ReimportService reimportService,
			final FileImportService importService,
			final ConfigurationLoader configurationLoader,
			final ConfigElza configElza) {
		this.elzaExportService = elzaExportService;
		this.archivalEntityRepository = archivalEntityRepository;
		this.transactionTemplate = transactionTemplate;
		this.storageService = storageService;
		this.apuSourceService = apuSourceService;
		this.coreQueueRepository = coreQueueRepository;
		this.databaseDataProvider = databaseDataProvider;
		this.entitySourceRepository = entitySourceRepository;
		this.reimportService = reimportService;
		this.importService = importService;
		this.configurationLoader = configurationLoader;
		this.configElza = configElza;
	}
	
	@PostConstruct
	void init() {
		reimportService.registerReimportProcessor(this);
		importService.registerImportProcessor(this);
	}
	
	/*
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
	}*/
	
	private void importEntityTrans(TransactionStatus t, final Path tmpDir, final ArchivalEntity ae) {
        ApuSourceBuilder apuSourceBuilder;
        final var importAp = new ImportAp();
        Path dataDir;
        try {                       
            apuSourceBuilder = importAp.importAp(tmpDir.resolve("ap.xml"), 
                    (ae.getUuid()!=null)?ae.getUuid():null,
                            databaseDataProvider);
            try (var os = Files.newOutputStream(tmpDir.resolve("apusrc.xml"))) {
                apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
            dataDir = storageService.moveToDataDir(tmpDir);
            
            saveEntity(dataDir, importAp, apuSourceBuilder, ae, importAp.getRequiredEntities(),
                       apuSourceBuilder.getReferencedEntities());
            
        } catch (Exception e) {         
            log.error("Fail to process downloaded ap.xml, dir={}, elzaId={}, uuid={}", tmpDir, 
                      ae.getElzaId(), ae.getUuid(), e);
            try {
                FileSystemUtils.deleteRecursively(tmpDir);
            } catch (IOException e1) {
                log.error("Fail to delete directory {}", tmpDir, e1);
            }
            throw new IllegalStateException(e);
        }
        
	}
	
	private void importEntity(final ArchivalEntity ae) {
		Path tmpDir = downloadEntity(ae);
		
		transactionTemplate.execute(t -> {
		    importEntityTrans(t, tmpDir, ae);
		    return null;
		});
		
		
	}

	private List<EntitySource> storeReqEnts(ApuSource apuSource, Set<Integer> reqEnts) {
	    List<EntitySource> storedEntSources = new ArrayList<>();
	    BulkOperation.run(reqEnts, 1000, batch -> storedEntSources.addAll(storeReqEntsInternal(apuSource, batch)));
	    return storedEntSources;
    }

    private List<EntitySource> storeReqEntsInternal(ApuSource apuSource, List<Integer> batch) {
        List<EntitySource> storedEntSources = new ArrayList<>();
        
        List<ArchivalEntity> ents = archivalEntityRepository.findByElzaIds(batch);
        List<Integer> newIds; 
        if(ents.size()>0) {
            Set<Integer> existingElzaIds = new HashSet<>();
                        
            newIds = new ArrayList<>(batch.size()-ents.size());
            
            for(var ent: ents) {
                existingElzaIds.add(ent.getElzaId());
                
                // entity in DB -> create only entity source
                storedEntSources.add( createEntitySource(apuSource, ent) );
            }
        } else {
            newIds = batch;
        }
        
        // insert into DB
        for(Integer elzaId: newIds) {
            ArchivalEntity ae = new ArchivalEntity();
            ae.setElzaId(elzaId);
            ae.setStatus(EntityStatus.ACCESSIBLE);
            ae = archivalEntityRepository.save(ae);
            
            storedEntSources.add( createEntitySource(apuSource, ae) );
        }
        
        return storedEntSources;
    }

    private EntitySource createEntitySource(ApuSource apuSource, ArchivalEntity ae) {
        // mark as required by master entity
        EntitySource es = new EntitySource();
        es.setApuSource(apuSource);
        es.setArchivalEntity(ae);
        return entitySourceRepository.save(es);
    }

    /**
	 * Save entity to DB
	 * 
	 * Method requires active transaction
	 * 
	 * @param dataDir
	 * @param importAp
	 * @param apuSourceBuilder
	 * @param ae Detached source entity
     * @param requiredEntities 
     * @param referencedEntities
	 */
	private void saveEntity(Path dataDir, ImportAp importAp, 
			ApuSourceBuilder apuSourceBuilder, ArchivalEntity ae, 
			Set<Integer> requiredEntities, 
			Set<UUID> referencedEntities) {
		// store subsequent hierarchical entities
		ArchivalEntity parentEntity;
		if (importAp.getParentElzaId() != null) {
			parentEntity = addAccessibleByElzaId(importAp.getParentElzaId());
		} else {
			parentEntity = null;
		}

		final ArchivalEntity srcArchivalEntity = archivalEntityRepository.getOne(ae.getId());
		boolean needReindex = false;
		if (srcArchivalEntity.getUuid() == null) {
			// we have to add uuid from downloaded data and check that it does not exists
			UUID uuid = importAp.getApUuid();
			var archivalEntity = archivalEntityRepository.findByUuid(uuid);
			if (archivalEntity.isPresent()) {
			    // update current db record with parent and elzaId
				ArchivalEntity dbArchEntity = archivalEntity.get(); 
				dbArchEntity.setElzaId(srcArchivalEntity.getElzaId());
				dbArchEntity.setParentEntity(parentEntity);
				archivalEntityRepository.save(dbArchEntity);
				
				// move all links from this entity to new ae
				List<EntitySource> ess = entitySourceRepository.findByArchivalEntity(srcArchivalEntity);
				for(EntitySource es: ess) {
					es.setArchivalEntity(dbArchEntity);
					entitySourceRepository.save(es);
				}
				
				// correct parent in connected entities
				var connectedEntities = archivalEntityRepository.findAllByParentEntity(srcArchivalEntity);
				for(var ce: connectedEntities) {
				    ce.setParentEntity(dbArchEntity);
				    archivalEntityRepository.save(ce);
				}
				
				// drop redundant
				archivalEntityRepository.delete(srcArchivalEntity);
				
				reindexAfterEntityChanged(ae);
				return;
			} else {
				srcArchivalEntity.setUuid(uuid);
				needReindex = true;
			}
		}
		// update elza id
		if (srcArchivalEntity.getElzaId() == null) {
			srcArchivalEntity.setElzaId(importAp.getElzaId());
		}
		srcArchivalEntity.setParentEntity(parentEntity);
		if(srcArchivalEntity.getStatus()!=EntityStatus.AVAILABLE) {
			needReindex = true;
		}
		saveApuSource(dataDir, srcArchivalEntity, apuSourceBuilder, requiredEntities, referencedEntities);
		
		// entity received new uuid
		if(needReindex) {
			// -> we have to reindex all entities having this entity as parent
			reindexAfterEntityChanged(srcArchivalEntity);
		}
	}

    private void reindexAfterEntityChanged(ArchivalEntity archivalEntity) {
		// reindex directly connected items
		List<ArchivalEntity> ents = archivalEntityRepository.findAllByParentEntity(archivalEntity);
		for(ArchivalEntity ent: ents) {
			apuSourceService.reimport(ent.getApuSource());			
		}
		// reindex all connected sources on whole subtree
		archivalEntityRepository.reimportConnected(archivalEntity.getId());
	}

	private ArchivalEntity saveApuSource(Path dataDir, ArchivalEntity ae, ApuSourceBuilder apuSourceBuilder, 
	                           Set<Integer> requiredEntities, 
	                           Set<UUID> referencedEntities) {
		UUID apusrcUuid = UUID.fromString(apuSourceBuilder.getApusrc().getUuid());
		var apuSource = apuSourceService.createApuSource(apusrcUuid, SourceType.ARCH_ENTITY, dataDir, "");

		ae.setStatus(EntityStatus.AVAILABLE);
		ae.setApuSource(apuSource);
		ArchivalEntity ret = archivalEntityRepository.save(ae);
		
		List<EntitySource> createdEntitySource;
		if(requiredEntities.size()>0) {
		    createdEntitySource = storeReqEnts(apuSource, requiredEntities);
		} else {
		    createdEntitySource = Collections.emptyList();
		}
		
		updateSourceEntityLinks(apuSource, referencedEntities, createdEntitySource);		

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);
		
		return ret;
	}

	/**
	 * 
	 * @param apuSource
	 * @param referencedEntities
	 * @param extraEntitySource List of already saved entitities
	 */
	public void updateSourceEntityLinks(ApuSource apuSource, Set<UUID> referencedEntities, 
	                                    List<EntitySource> extraEntitySource) {
	    Set<EntitySource> validEntSrcs;
	    if(extraEntitySource!=null&&extraEntitySource.size()>0) {
	        validEntSrcs = new HashSet<>();
	        validEntSrcs.addAll(extraEntitySource);
	    } else {
	        validEntSrcs = Collections.emptySet();
	    }
	    
	    // find existing links
	    List<EntitySource> entLinks = this.entitySourceRepository.findByApuSourceJoinFetchArchivalEntity(apuSource);
	    
	    Set<UUID> processedUuid = new HashSet<>();
	    List<EntitySource> deleteEntLinks = new ArrayList<>();
	    // 
	    for(var entLink: entLinks) {
	        // skip recently added entity sources
	        if(validEntSrcs.contains(entLink)) {
	            continue;
	        }
	        
	        if(referencedEntities.contains(entLink.getArchivalEntity().getUuid())) {
	            processedUuid.add(entLink.getArchivalEntity().getUuid());
	        } else {
	            deleteEntLinks.add(entLink);
	        }
	    }
	    if(deleteEntLinks.size()>0) {
	        BulkOperation.run(deleteEntLinks, 1000, batch -> entitySourceRepository.deleteInBatch(batch));
	    }
	    
	    // prepare new links
	    List<UUID> createEntLinks = new ArrayList<>();
	    for(var refEnt: referencedEntities) {
	        if(!processedUuid.contains(refEnt)) {
	            createEntLinks.add(refEnt);
	        }
	    }
	    
	    if(createEntLinks.size()>0)
	    {
	        // create source links
	        BulkOperation.run(createEntLinks, 1000,
                          batch -> {
                              // get referenced ents
                              var archEnts = this.archivalEntityRepository.findByUuidIn(batch);
                              Map<UUID, ArchivalEntity> lookup = archEnts.stream().
                                      collect(Collectors.toMap(ArchivalEntity::getUuid, Function.identity() ) );
                              for(var entUuid: batch) {
                                  var ent = lookup.get(entUuid);
                                  if(ent==null) {
                                      ent = new ArchivalEntity();
                                      ent.setStatus(EntityStatus.ACCESSIBLE);
                                      ent.setUuid(entUuid);
                                      ent = this.archivalEntityRepository.save(ent);
                                  }
                                  // create source link
                                  createEntitySource(apuSource, ent);
                              }
                          });
	    }
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
	
	private void downloadEntity(ArchivalEntity ae, Path dir) throws IOException {
	    log.debug("Downloading entity from Elza, uuid: {}, elzaId: {}", ae.getUuid(), ae.getElzaId());

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
        var exportService = elzaExportService.get();
        var response = exportService.exportData(exportRequest);

        var dataHandler = response.getBinData();
        
        var targetApPath = dir.resolve("ap.xml");
        if(Files.exists(targetApPath)) {
            var prevApPath = dir.resolve("apPrev.xml");
            Files.deleteIfExists(prevApPath);
            Files.move(targetApPath, prevApPath);
        }
        try(var os = Files.newOutputStream(targetApPath)) {
            dataHandler.writeTo(os);
        } catch (IOException e) {
            log.error("Fail to write downloaded ap",e);
            throw new IllegalStateException(e);
        }
}
	
	private Path downloadEntity(ArchivalEntity ae) {
	    Path tempDir = null;
		try {
			tempDir = storageService.createTempDir("ae-"+ae.getId().toString());
			downloadEntity(ae, tempDir);
			return tempDir;
		} catch (IOException e) {
            try {
                if(tempDir!=null) {
                    FileSystemUtils.deleteRecursively(tempDir);
                }
            } catch (IOException e1) {
                log.error("Fail to delete directory",e1);
            }
			log.error("Fail to download entity",e);
			throw new IllegalStateException(e);
		}
	}

	/*
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
    }*/

    /* @Override
    public void start() {
        if (configElza.isDisabled()) {
            status = ThreadStatus.STOPPED;
            return;
        }
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
	}*/

	@Override
	public Result reimport(ApuSource apuSource) {
		if(apuSource.getSourceType()!=SourceType.ARCH_ENTITY) {
			return Result.UNSUPPORTED;
		}

		ArchivalEntity archEntity = archivalEntityRepository.findByApuSource(apuSource);
		if(archEntity==null) {
			log.error("Missing archival entity: {}", apuSource.getId());
			return Result.UNSUPPORTED;
		}
		if(archEntity.getStatus()!=EntityStatus.AVAILABLE) {
			log.warn("Archival entity {} cannot be reimported, status: {}", apuSource.getId(), archEntity.getStatus());
			return Result.NOCHANGES;
		}

        Path apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        if(archEntity.isDownload()) {
            try {
                downloadEntity(archEntity, apuDir);
            } catch(IOException e) {
                log.error("Failed to download entity, targetDir: {}", apuDir.toString(), e);
                return Result.FAILED;
            }
            archEntity.setDownload(false);
            archivalEntityRepository.save(archEntity);
        }

		ApuSourceBuilder apuSourceBuilder;
		final var importAp = new ImportAp();
		try {
			apuSourceBuilder = importAp.importAp(apuDir.resolve("ap.xml"), archEntity.getUuid(), databaseDataProvider);
			apuSourceBuilder.setUuid(apuSource.getUuid());
			try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
			}
			List<EntitySource> ess = storeReqEnts(apuSource, importAp.getRequiredEntities());
			this.updateSourceEntityLinks(apuSource, apuSourceBuilder.getReferencedEntities(), ess);
		} catch (Exception e) {
			log.error("Fail to process downloaded ap.xml, dir={}", apuDir, e);
			return Result.FAILED;
		}
		return Result.REIMPORTED;
	}

    @Override
    public void importData(ImportContext ic) {
        if (configElza.isDisabled()) {
            return;
        }
        
        var ids = archivalEntityRepository.findTop1000ByStatusOrderById(EntityStatus.ACCESSIBLE);
        for(var id: ids) {
            try {
                importEntity(id.getId());
            } catch(Exception e) {
                ic.setFailed(true);
                log.error("Entity not imported: {}", id, e);
                return;
            }
            ic.addProcessed();
        }        
    }

}
