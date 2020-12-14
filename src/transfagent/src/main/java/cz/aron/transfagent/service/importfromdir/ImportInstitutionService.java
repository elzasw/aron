package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
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
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportInstitution;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
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
	
	public ImportInstitutionService(StorageService storageService, InstitutionRepository institutionRepository,
			TransactionTemplate transactionTemplate, ApuSourceRepository apuSourceRepository,
			CoreQueueRepository coreQueueRepository) {
		this.storageService = storageService;
		this.institutionRepository = institutionRepository;
		this.transactionTemplate = transactionTemplate;
		this.apuSourceRepository = apuSourceRepository;
		this.coreQueueRepository = coreQueueRepository;
	}
	
	/**
	 * Najde soubor institution-${kod_archivu}.xml, vytori z nej apusrc.xml, vytvori nebo aktualizuje zaznamy v databazi a vytvori odesilaci udalost.
	 * @param dir zpracovavany adresar
	 */
	public void processDirectory(Path dir) {
		
		List<Path> xmls;
		try (var stream = Files.list(dir)) {
			 xmls = stream
					.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("institution")
							&& f.getFileName().toString().endsWith(".xml"))
					.collect(Collectors.toList());								
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}
		
		Optional<Path> inst = xmls.stream().filter(p -> p.getFileName().toString().startsWith("institution-")
				&& p.getFileName().toString().endsWith(".xml")).findFirst();
		
		if (inst.isEmpty()) {
			log.warn("Directory is empty {}", dir);
			return;
		}
		
		String fileName = inst.get().getFileName().toString();			
		String tmp = fileName.substring("institution-".length());
		String code = tmp.substring(0,tmp.length()-".xml".length());
		
		ImportInstitution ii = new ImportInstitution();
		ApuSourceBuilder apusrcBuilder;
		
		try {
			apusrcBuilder = ii.importInstitution(inst.get(), code);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		} catch (JAXBException e1) {
			throw new IllegalStateException(e1);
		}			
		
		Institution institution = institutionRepository.findByCode(code);
		if (institution!=null) {
			apusrcBuilder.getApusrc().setUuid(institution.getApuSource().getUuid().toString());
			apusrcBuilder.getApusrc().getApus().getApu().get(0).setUuid(institution.getUuid().toString());
		}
		
		Path dataDir;
		try(OutputStream fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
			apusrcBuilder.build(fos);
			
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}
		
		try {
			dataDir = storageService.moveToDataDir(dir);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		if (institution == null) {    			
			// instituce neexistuje, vytvorim novou
			cz.aron.transfagent.domain.ApuSource apuSource = new cz.aron.transfagent.domain.ApuSource();
			apuSource.setOrigDir(dir.getFileName().toString());
			apuSource.setDataDir(dataDir.toString());
			apuSource.setSourceType(SourceType.INSTITUTION);
			apuSource.setUuid(UUID.fromString(apusrcBuilder.getApusrc().getUuid()));
			apuSource.setDeleted(false);
			apuSource.setDateImported(ZonedDateTime.now());

			Institution newInstitution = new Institution();
			newInstitution.setApuSource(apuSource);
			newInstitution.setCode(code);
			newInstitution.setSource("source");
			newInstitution.setUuid(UUID.fromString(apusrcBuilder.getApusrc().getApus().getApu().get(0).getUuid()));

			/*
			// vytvorim archivni entitu
			ArchivalEntity archivalEntity = new ArchivalEntity();
			archivalEntity.setApuSource(apuSource);
			archivalEntity.setStatus(EntityStatus.AVAILABLE);
			archivalEntity.setUuid(apuSrcBuilder.);
			*/

			CoreQueue coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			transactionTemplate.execute(t -> {
				apuSourceRepository.save(apuSource);
				institutionRepository.save(newInstitution);
				coreQueueRepository.save(coreQueue);
				return null;
			});
		} else {									
			
			// aktualizace, pouze zmenim datovy adresar
			cz.aron.transfagent.domain.ApuSource apuSource = institution.getApuSource();
			apuSource.setDataDir(dataDir.toString());
			apuSource.setOrigDir(dir.getFileName().toString());

			CoreQueue coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			transactionTemplate.execute(t -> {
				apuSourceRepository.save(apuSource);
				institutionRepository.save(institution);
				coreQueueRepository.save(coreQueue);
				return null;
			});    			
		}

		
	}
	
	

}
