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

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.elza.ImportFundInfo;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFindingAidService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFindingAidService.class);

    private final ApuSourceService apuSourceService;

    private final FileImportService fileImportService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final InstitutionRepository institutionRepository;

    private final FindingAidRepository findingAidRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final FundRepository fundRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationLoader configurationLoader;

    final private String FINDING_AID = "findingaid";

    final private String FINDING_AID_DASH = FINDING_AID + "-";

    final private String FINDING_AIDS_DIR = FINDING_AID + "s";

    public ImportFindingAidService(ApuSourceService apuSourceService, FileImportService fileImportService,
            ReimportService reimportService, StorageService storageService, InstitutionRepository institutionRepository,
            FindingAidRepository findingAidRepository, ApuSourceRepository apuSourceRepository,
            CoreQueueRepository coreQueueRepository, FundRepository fundRepository,
            DatabaseDataProvider databaseDataProvider, TransactionTemplate transactionTemplate,
            ConfigurationLoader configurationLoader) {
        this.apuSourceService = apuSourceService;
        this.fileImportService = fileImportService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.institutionRepository = institutionRepository;
        this.findingAidRepository = findingAidRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.fundRepository = fundRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.transactionTemplate = transactionTemplate;
        this.configurationLoader = configurationLoader;
    }

    @PostConstruct
    void register() {
        fileImportService.registerImportProcessor(this);
        reimportService.registerReimportProcessor(this);
    }

    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(FINDING_AIDS_DIR);
    }

    /**
     * Zpracování adresářů s archivní pomucky
     * 
     * @param dir zpracovavany adresar
     */
    @Override
    public boolean processDirectory(Path dir) {

        List<Path> xmls;
        try (var stream = Files.list(dir)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith(FINDING_AID)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }

        var findingaidXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(FINDING_AID_DASH)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();

        if (findingaidXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        var fileName = findingaidXml.get().getFileName().toString();
        var tmp = fileName.substring(FINDING_AID_DASH.length());
        var findingaidCode = tmp.substring(0, tmp.length() - ".xml".length());

        var fund = fundRepository.findByCode(findingaidCode);
        if (fund == null) {
            throw new NullPointerException("The entry Fund code={" + findingaidCode + "} must exist.");
        }

        var findingAid = findingAidRepository.findByCode(findingaidCode);
        var findingaidUuid = findingAid != null? findingAid.getUuid() : null;

        var ifai = new ImportFindingAidInfo(findingaidCode);
        ApuSourceBuilder builder;

        try {
            builder = ifai.importFindingAidInfo(findingaidXml.get(), findingaidUuid, databaseDataProvider);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        var institutionCode = ifai.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
            throw new NullPointerException("The entry Institution code={" + institutionCode + "} must exist.");
        }

        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            builder.build(fos, new ApuValidator(configurationLoader.getConfig()));
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

        if (findingAid == null) {
            createFindingAid(findingaidCode, fund, institution, dataDir, dir, builder);
        } else {
            updateFindingAid(findingAid, dataDir, dir);
        }
        return true;
    }

    private void createFindingAid(String findingaidCode, Fund fund, Institution institution, Path dataDir, Path origDir, ApuSourceBuilder builder) {

        var findingaidUuid = builder.getApusrc().getApus().getApu().get(0).getUuid();
        Validate.notNull(findingaidUuid, "FindingAid UUID is null");

        var apuSourceUuidStr = builder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

        transactionTemplate.execute(t -> {
            var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FINDING_AID, 
                    dataDir, origDir.getFileName().toString());

            var findingAid = new FindingAid();
            findingAid.setCode(findingaidCode);
            findingAid.setUuid(UUID.fromString(findingaidUuid));
            findingAid.setApuSource(apuSource);
            findingAid.setInstitution(institution);
            findingAid.setFund(fund);
            findingAid = findingAidRepository.save(findingAid);

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("FindingAid created code={}, uuid={}", findingaidCode, findingaidUuid);
    }

    private void updateFindingAid(FindingAid findingAid, Path dataDir, Path origDir) {

        var oldDir = findingAid.getApuSource().getDataDir();

        transactionTemplate.execute(t -> {
            var apuSource = findingAid.getApuSource();
            apuSource.setDataDir(dataDir.toString());
            apuSource.setOrigDir(origDir.getFileName().toString());

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);

            apuSourceRepository.save(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("FindingAid updated code={}, uuid={}, original data dir {}", findingAid.getCode(), findingAid.getUuid(), oldDir);
    }

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.FINDING_AID)
            return Result.UNSUPPORTED;

        var findingAid = findingAidRepository.findByApuSource(apuSource);
        if (findingAid == null) {
            log.error("Missing findingAid: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        String fileName = FINDING_AID_DASH + findingAid.getCode() + ".xml";

        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        var ifai = new ImportFindingAidInfo(findingAid.getCode());
        ApuSourceBuilder builder;
        try {
            builder = ifai.importFindingAidInfo(apuDir.resolve(fileName), findingAid.getUuid(), databaseDataProvider);
            builder.setUuid(apuSource.getUuid());
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                builder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileName, apuDir, e);
            return Result.FAILED;
        }

        return Result.REIMPORTED;
    }

}
