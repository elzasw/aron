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

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.elza.ImportFundInfo;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFindingAidService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFindingAidService.class);

    private final ApuSourceService apuSourceService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final FileImportService fileImportService;

    private final InstitutionRepository institutionRepository;

    private final FundRepository fundRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;

    final private String FINDINGAIDS_DIR = "findingaids";

    public ImportFindingAidService(ApuSourceService apuSourceService, ReimportService reimportService,
            StorageService storageService, FileImportService fileImportService, InstitutionRepository institutionRepository,
            FundRepository fundRepository, DatabaseDataProvider databaseDataProvider, ConfigurationLoader configurationLoader) {
        this.apuSourceService = apuSourceService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.fileImportService = fileImportService;
        this.institutionRepository = institutionRepository;
        this.fundRepository = fundRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.configurationLoader = configurationLoader;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }

    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(FINDINGAIDS_DIR);
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
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("findingaid")
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }

        var findingaidXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith("findingaid-")
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();

        if (findingaidXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        var fileName = findingaidXml.get().getFileName().toString();
        var tmp = fileName.substring("findingaid-".length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());

        var fund = fundRepository.findByCode(fundCode);
        UUID fundUuid = fund != null? fund.getUuid() : null;

        var ifai = new ImportFindingAidInfo(fundCode);
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = ifai.importFindingAidInfo(findingaidXml.get(), fundUuid, databaseDataProvider);
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

        log.info("FindingAid procesed fund={}, institution={}", fundCode, institutionCode);
        return true;
    }

    @Override
    public Result reimport(ApuSource apuSource) {
        // TODO Auto-generated method stub
        return null;
    }
}
