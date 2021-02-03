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
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFindingAidService extends ImportDirProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFindingAidService.class);

    private final StorageService storageService;

    private final FileImportService fileImportService;

    private final InstitutionRepository institutionRepository;

    private final FindingAidRepository findingAidRepository;

    private final FundRepository fundRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationLoader configurationLoader;

    final private String FINDING_AID = "findingaid";

    final private String FINDING_AID_DASH = FINDING_AID + "-";

    final private String FINDING_AIDS_DIR = FINDING_AID + "s";

    public ImportFindingAidService(StorageService storageService, FileImportService fileImportService, InstitutionRepository institutionRepository,
            FindingAidRepository findingAidRepository, FundRepository fundRepository, DatabaseDataProvider databaseDataProvider,
            TransactionTemplate transactionTemplate, ConfigurationLoader configurationLoader) {
        this.storageService = storageService;
        this.fileImportService = fileImportService;
        this.institutionRepository = institutionRepository;
        this.findingAidRepository = findingAidRepository;
        this.fundRepository = fundRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.transactionTemplate = transactionTemplate;
        this.configurationLoader = configurationLoader;
    }

    @PostConstruct
    void register() {
        fileImportService.registerImportProcessor(this);
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
    protected boolean processDirectory(Path dir) {

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
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = ifai.importFindingAidInfo(findingaidXml.get(), findingaidUuid, databaseDataProvider);
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

        if (findingAid == null) {
            createFindingAid(findingaidCode, fund, institution, apusrcBuilder);
        } else {
            updateFindingAid(findingAid, fund, institution);
        }
        return true;
    }

    private void createFindingAid(String findingaidCode, Fund fund, Institution institution, ApuSourceBuilder apusrcBuilder) {

        var findingaidUuid = apusrcBuilder.getApusrc().getApus().getApu().get(0).getUuid();
        Validate.notNull(findingaidUuid, "FindingAid UUID is null");

        transactionTemplate.execute(t -> {
            var findingAid = new FindingAid();
            findingAid.setCode(findingaidCode);
            findingAid.setUuid(UUID.fromString(findingaidUuid));
            findingAid.setInstitution(institution);
            findingAid.setFund(fund);
            findingAidRepository.save(findingAid);
            return null;
        });
        log.info("FindingAid created fund={}, institution={}", fund.getCode(), institution.getCode());
    }

    private void updateFindingAid(FindingAid findingAid, Fund fund, Institution institution) {

        transactionTemplate.execute(t -> {
            findingAid.setInstitution(institution);
            findingAid.setFund(fund);
            findingAidRepository.save(findingAid);
            return null;
        });
        log.info("FindingAid updated fund={}, institution={}", fund.getCode(), institution.getCode());
    }

}
