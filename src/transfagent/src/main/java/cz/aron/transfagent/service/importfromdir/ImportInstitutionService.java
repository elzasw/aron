package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
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
import cz.aron.transfagent.service.StorageService;

/**
 *  Import instituce ze vstupniho adresare
 */
@Service
public class ImportInstitutionService {
	
	private static final Logger log = LoggerFactory.getLogger(ImportInstitutionService.class);
	
	private final StorageService storageService;
	
	private final InstitutionRepository institutionRepository;
	
	private final TransactionTemplate transactionTemplate;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final EntitySourceRepository entitySourceRepository;
	
	public ImportInstitutionService(StorageService storageService, InstitutionRepository institutionRepository,
			TransactionTemplate transactionTemplate, ApuSourceRepository apuSourceRepository,
			CoreQueueRepository coreQueueRepository, ArchivalEntityRepository archivalEntityRepository,
			EntitySourceRepository entitySourceRepository) {
		this.storageService = storageService;
		this.institutionRepository = institutionRepository;
		this.transactionTemplate = transactionTemplate;
		this.apuSourceRepository = apuSourceRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.archivalEntityRepository = archivalEntityRepository;
		this.entitySourceRepository = entitySourceRepository;
	}
	
	/**
	 * Najde soubor institution-${kod_archivu}.xml, vytori z nej apusrc.xml, vytvori nebo aktualizuje zaznamy v databazi a vytvori odesilaci udalost.
	 * @param dir zpracovavany adresar
	 * @return true - continue processing next directory, false - stop processing 
	 */
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
			apusrcBuilder.build(fos);
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
			// instituce neexistuje, vytvorim novou
			var apuSource = new cz.aron.transfagent.domain.ApuSource();
			apuSource.setOrigDir(origDir.getFileName().toString());
			apuSource.setDataDir(dataDir.toString());
			apuSource.setSourceType(SourceType.INSTITUTION);
			apuSource.setUuid(UUID.fromString(apusrcBuilder.getApusrc().getUuid()));
			apuSource.setDeleted(false);
			apuSource.setDateImported(ZonedDateTime.now());
			apuSource = apuSourceRepository.save(apuSource);

			var newInstitution = new Institution();
			newInstitution.setApuSource(apuSource);
			newInstitution.setCode(institutionCode);
			newInstitution.setSource("source");
			newInstitution.setUuid(institutionUuid);
			newInstitution = institutionRepository.save(newInstitution);

			if (ii.getApRefUuid() != null) {
				var apUuid = UUID.fromString(ii.getApRefUuid());
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
				var apUuid = UUID.fromString(ii.getApRefUuid());						
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

}
