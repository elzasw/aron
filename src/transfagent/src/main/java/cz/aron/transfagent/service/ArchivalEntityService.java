package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;

@Service
public class ArchivalEntityService {
	
	private static final Logger log = LoggerFactory.getLogger(ArchivalEntityService.class);

	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final EntitySourceRepository entitySourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final ApuSourceService apuSourceService;
	
	public ArchivalEntityService(ArchivalEntityRepository archivalEntityRepository,
			EntitySourceRepository entitySourceRepository, CoreQueueRepository coreQueueRepository,
			ApuSourceService apuSourceService) {
		super();
		this.archivalEntityRepository = archivalEntityRepository;
		this.entitySourceRepository = entitySourceRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.apuSourceService = apuSourceService;
	}
	
	@Transactional
	public void createOrUpdateArchivalEntity(Path dataDir, Path origDir, UUID uuid) {		
		Optional<ArchivalEntity> ae = archivalEntityRepository.findByUuid(uuid);
		if (ae.isPresent()) {
			updateArchivalEntity(ae.get(), dataDir, origDir, uuid);
		} else {								
			createArchivalEntity(dataDir, origDir, uuid);
		}				
	}
	
	/**
	 * Vytvori novou archivni entitu. Vytvori k ni EntitySource a ArchivalEntity a udalost do CoreQueue
	 * 
	 * @param dataDir       cesta k adresari/zipu s daty
	 * @param origDir       puvodni cesta k adresari/zipu s daty
	 * @param apusrcBuilder apu builder
	 */
	private void createArchivalEntity(Path dataDir, Path origDir, UUID apuSourceUuid) {
		
		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_ENTITY, dataDir,
				origDir.getFileName().toString());
		
		var archivalEntity = new ArchivalEntity();
		archivalEntity.setApuSource(null);
		archivalEntity.setDownload(false);
		archivalEntity.setEntityClass(null);
		archivalEntity.setStatus(EntityStatus.AVAILABLE);
		archivalEntity.setLastUpdate(ZonedDateTime.now());
		archivalEntity.setUuid(apuSourceUuid);		
		archivalEntity = archivalEntityRepository.save(archivalEntity);
		
		var entitySource = new EntitySource();
		entitySource.setApuSource(apuSource);
		entitySource.setArchivalEntity(archivalEntity);
		entitySourceRepository.save(entitySource);
		
		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);

		log.info("Archival entity created, uuid={}", apuSourceUuid);
	}
	
	
	/**
	 * Aktualizuje archivni entitu
	 * 
	 * Aktualizuje ApuSource a vlozi udalost do CoreQueue
	 * 
	 * @param archivalEntity aktualizovana entita
	 * @param dataDir adresar/zip s daty
	 * @param origDir puvodni adresar/zip s daty
	 */
	private void updateArchivalEntity(ArchivalEntity archivalEntity, Path dataDir, Path origDir, UUID apuSourceUuid) {
		
		var apuSource = archivalEntity.getApuSource();
		if (apuSource!=null) {
			// aktualizace apusource
			apuSource.setDataDir(dataDir.toString());
			apuSource.setOrigDir(origDir.getFileName().toString());
		} else {
			// TODO muze nastat?
			apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_ENTITY, dataDir,
					origDir.getFileName().toString());
		}

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);				
		log.info("Archival entity updated, uuid={}", apuSourceUuid);
	}

	
}
