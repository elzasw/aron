package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportArchDescService extends ImportDirProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportArchDescService.class);

    private final StorageService storageService;

    private final FundRepository fundRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final InstitutionRepository institutionRepository;

    private final ArchDescRepository archDescRepository;

    private final ArchivalEntityRepository archivalEntityRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final TransactionTemplate transactionTemplate;
    
    private final ApuSourceService apuSourceService;
    
    final private String ARCHDESC_DIR = "archdesc";

    public ImportArchDescService(StorageService storageService, FundRepository fundRepository,
                             ApuSourceRepository apuSourceRepository, InstitutionRepository institutionRepository,
                             ArchDescRepository archDescRepository, ArchivalEntityRepository archivalEntityRepository,
                             CoreQueueRepository coreQueueRepository, TransactionTemplate transactionTemplate,
                             ApuSourceService apuSourceService) {
        this.storageService = storageService;
        this.fundRepository = fundRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.institutionRepository = institutionRepository;
        this.archDescRepository = archDescRepository;
        this.archivalEntityRepository = archivalEntityRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.transactionTemplate = transactionTemplate;
        this.apuSourceService = apuSourceService; 
    }
    
	@Override
	protected Path getInputDir() {
		return storageService.getInputPath().resolve(ARCHDESC_DIR);
	}    
    

    /**
     * Zpracování adresářů s archdesc.xml soubory
     * 
     * @param dir zpracovavany adresar
     */
    @Override
    public boolean processDirectory(Path dir) {

        List<Path> xmls;
        try (var stream = Files.list(dir)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("archdesc")
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }

        var archdescXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith("archdesc-")
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();

        if (archdescXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        var fileName = archdescXml.get().getFileName().toString();
        var tmp = fileName.substring("archdesc-".length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());

        var iad = new ImportArchDesc();
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = iad.importArchDesc(archdescXml.get(), new DatabaseDataProvider(institutionRepository, archivalEntityRepository));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        var institution = institutionRepository.findByCode(iad.getInstitutionCode());
        if (institution == null) {
        	throw new NullPointerException("The entry Institution code={" + iad.getInstitutionCode() + "} must exist.");
        }

        var fund = fundRepository.findByCodeAndInstitution(fundCode, institution);
        if (fund == null) {
        	throw new NullPointerException("The entry Fund code={" + fundCode + "} must exist.");
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

        var archDesc = archDescRepository.findByFund(fund);
        if (archDesc == null) {
            createArchDesc(fund, dataDir, dir, apusrcBuilder);
        } else {
            updateArchDesc(archDesc, dataDir, dir);
        }

        Set<String> uuids = iad.getApRefs();
        for (String uuid : uuids) {
        	var archivalEntity = archivalEntityRepository.findByUuid(UUID.fromString(uuid)).orElse(null);
        	if (archivalEntity == null) {
        		archivalEntity = new ArchivalEntity();
        		archivalEntity.setUuid(UUID.fromString(uuid));
            	archivalEntity.setApuSource(fund.getApuSource());
            	archivalEntity.setStatus(EntityStatus.ACCESSIBLE);
            	archivalEntity.setLastUpdate(ZonedDateTime.now());
        		archivalEntityRepository.save(archivalEntity);
        	}
        }
        return true;
    }

    private void createArchDesc(Fund fund, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder) {
    	
        var archDescUuid = UUID.randomUUID();
        var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

        transactionTemplate.execute(t -> {
            var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.ARCH_DESCS, 
            		dataDir, origDir.getFileName().toString());

            var archDesc = new ArchDesc();
            archDesc.setApuSource(apuSource);
            archDesc.setFund(fund);
            archDesc.setUuid(archDescUuid);
            archDesc = archDescRepository.save(archDesc);

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("ArchDesc created uuid={}", archDescUuid);
    }

    private void updateArchDesc(ArchDesc archDesc, Path dataDir, Path origDir) {
    	
        var oldDir = archDesc.getApuSource().getDataDir();

        transactionTemplate.execute(t -> {
            var apuSource = archDesc.getApuSource();
            apuSource.setDataDir(dataDir.toString());
            apuSource.setOrigDir(origDir.getFileName().toString());

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);

            apuSourceRepository.save(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("ArchDesc updated uuid={}, original data dir {}", archDesc.getUuid(), oldDir);
    }

}
