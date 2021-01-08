package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportArchDesc;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ArchivalEntityImportService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportArchDescService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportArchDescService.class);

    private final ApuSourceService apuSourceService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final FundRepository fundRepository;

    private final InstitutionRepository institutionRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final ArchDescRepository archDescRepository;

    private final TransactionTemplate transactionTemplate;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final ArchivalEntityImportService archivalEntityImportService;
    
    private final FileImportService fileImportService;

    final private String ARCHDESC_DIR = "archdesc";

    public ImportArchDescService(ApuSourceService apuSourceService, ReimportService reimportService, StorageService storageService,
            FundRepository fundRepository,
            InstitutionRepository institutionRepository, ApuSourceRepository apuSourceRepository,
            CoreQueueRepository coreQueueRepository, ArchDescRepository archDescRepository,
            TransactionTemplate transactionTemplate, DatabaseDataProvider databaseDataProvider,
            ConfigurationLoader configurationLoader,
            final ArchivalEntityImportService archivalEntityImportService,
            final FileImportService fileImportService) {
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.fundRepository = fundRepository;
        this.institutionRepository = institutionRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.archDescRepository = archDescRepository;
        this.transactionTemplate = transactionTemplate;
        this.databaseDataProvider = databaseDataProvider;
        this.configurationLoader = configurationLoader;
        this.archivalEntityImportService = archivalEntityImportService;
        this.fileImportService = fileImportService;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }
    
    @Override
    public int getPriority() { return 7; }    

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
        
        transactionTemplate.execute(t -> {
           importArchDesc(t, dir, fundCode, archdescXml.get());
           return null;
        });
        return true;
    }

    /**
     * Real import
     * 
     * Has to be run in transaction
     * @param t 
     * @param dir
     * @param fundCode
     * @param archdescXmlPath
     */
    private void importArchDesc(TransactionStatus t, Path dir, String fundCode, Path archdescXmlPath) {

        var iad = new ImportArchDesc();
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = iad.importArchDesc(archdescXmlPath, databaseDataProvider);
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

        var archDesc = archDescRepository.findByFund(fund);
        if (archDesc == null) {
            archDesc = createArchDesc(fund, dataDir, dir, apusrcBuilder);
        } else {
            updateArchDesc(archDesc, dataDir, dir);
        }

        // Request entities and store refs
        Set<UUID> uuids = iad.getApRefs();        
        archivalEntityImportService.updateSourceEntityLinks(archDesc.getApuSource(), uuids, null);        
    }

    private ArchDesc createArchDesc(Fund fund, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder) {
    	
        var archDescUuid = UUID.randomUUID();
        var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

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

        log.info("ArchDesc created uuid={}", archDescUuid);
        
        return archDesc;
    }

    private void updateArchDesc(ArchDesc archDesc, Path dataDir, Path origDir) {
    	
        var oldDir = archDesc.getApuSource().getDataDir();

        var apuSource = archDesc.getApuSource();
        apuSource.setDataDir(dataDir.toString());
        apuSource.setOrigDir(origDir.getFileName().toString());

        var coreQueue = new CoreQueue();
        coreQueue.setApuSource(apuSource);

        apuSourceRepository.save(apuSource);
        coreQueueRepository.save(coreQueue);

        log.info("ArchDesc updated uuid={}, original data dir {}", archDesc.getUuid(), oldDir);
    }

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.ARCH_DESCS)
            return Result.UNSUPPORTED;

        var archDesc = archDescRepository.findByApuSource(apuSource);
        if (archDesc == null) {
            log.error("Missing archive description: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        var fund = archDesc.getFund();
        var fileName = "archdesc-"+fund.getCode()+".xml";
        
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        var inputFile = apuDir.resolve(fileName);
             
        ApuSourceBuilder apuSourceBuilder;
        var iad = new ImportArchDesc();
        try {
            apuSourceBuilder = iad.importArchDesc(inputFile, databaseDataProvider);
            apuSourceBuilder.setUuid(apuSource.getUuid());
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
            // Request entities and store refs
            Set<UUID> uuids = iad.getApRefs();        
            archivalEntityImportService.updateSourceEntityLinks(archDesc.getApuSource(), uuids, null);        
        } catch (Exception e) {
            log.error("Fail to process, file: {}", inputFile, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
    }

}
