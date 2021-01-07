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
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportAp;
import cz.aron.transfagent.elza.ImportFundInfo;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFundService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFundService.class);

    private final ApuSourceService apuSourceService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final InstitutionRepository institutionRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final FundRepository fundRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationLoader configurationLoader;

    final private String FUND_DIR = "fund";

    public ImportFundService(ApuSourceService apuSourceService, ReimportService reimportService, StorageService storageService,
            InstitutionRepository institutionRepository, CoreQueueRepository coreQueueRepository,
            ApuSourceRepository apuSourceRepository, FundRepository fundRepository,
            DatabaseDataProvider databaseDataProvider, TransactionTemplate transactionTemplate,
            ConfigurationLoader configurationLoader) {
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.institutionRepository = institutionRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.fundRepository = fundRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.transactionTemplate = transactionTemplate;
        this.configurationLoader = configurationLoader;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
    }

    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(FUND_DIR);
    }

    /**
     * Zpracování adresářů s archivními soubory
     * 
     * @param dir zpracovavany adresar
     */
    @Override
    public boolean processDirectory(Path dir) {

        List<Path> xmls;
        try (var stream = Files.list(dir)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("fund")
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }

        var fundXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith("fund-")
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();

        if (fundXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        var fileName = fundXml.get().getFileName().toString();
        var tmp = fileName.substring("fund-".length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());

        var ifi = new ImportFundInfo();
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = ifi.importFundInfo(fundXml.get(), databaseDataProvider);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        var fund = fundRepository.findByCode(fundCode);
        if (fund != null) {
            apusrcBuilder.getApusrc().setUuid(fund.getApuSource().getUuid().toString());
            apusrcBuilder.getApusrc().getApus().getApu().get(0).setUuid(fund.getUuid().toString());
        }

        var institutionCode = ifi.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
            throw new NullPointerException("The entry Institution code={" + institutionCode + "} must exist.");
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

        if (fund == null) {
            createFund(institution, dataDir, dir, apusrcBuilder, fundCode);
        } else {
            updateFund(fund, dataDir, dir);
        }
        return true;
    }

    private void createFund(Institution institution, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String fundCode) {

        var fundUuid = UUID.randomUUID();
        var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

        transactionTemplate.execute(t -> {
            var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FUND, 
                    dataDir, origDir.getFileName().toString());

            var fund = new Fund();
            fund.setApuSource(apuSource);
            fund.setInstitution(institution);
            fund.setCode(fundCode);
            fund.setSource("source");
            fund.setUuid(fundUuid);
            fund = fundRepository.save(fund);

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("Fund created code={}, uuid={}", fundCode, fundUuid);
    }

    private void updateFund(Fund fund, Path dataDir, Path origDir) {

        var oldDir = fund.getApuSource().getDataDir();

        transactionTemplate.execute(t -> {
            var apuSource = fund.getApuSource();
            apuSource.setDataDir(dataDir.toString());
            apuSource.setOrigDir(origDir.getFileName().toString());

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);

            apuSourceRepository.save(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("Fund updated code={}, uuid={}, original data dir {}", fund.getCode(), fund.getUuid(), oldDir);
    }

    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.FUND)
            return Result.UNSUPPORTED;

        var fund = fundRepository.findByApuSource(apuSource);
        if (fund == null) {
            log.error("Missing fund: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        String fileName = "fund-" + fund.getCode() + ".xml";

        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        ApuSourceBuilder apuSourceBuilder;
        var ifi = new ImportFundInfo();
        try {
            apuSourceBuilder = ifi.importFundInfo(apuDir.resolve(fileName), databaseDataProvider);
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", apuDir, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
    }

}
