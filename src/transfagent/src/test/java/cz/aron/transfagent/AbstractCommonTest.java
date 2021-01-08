package cz.aron.transfagent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.importfromdir.ImportArchDescService;
import cz.aron.transfagent.service.importfromdir.ImportDirectService;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;

public abstract class AbstractCommonTest {

    final static String DIR_TEST_RESOURCES = "target/test-resources";

    final String DIR_DATA = DIR_TEST_RESOURCES + "/data";

    final String DIR_ERROR = DIR_TEST_RESOURCES + "/error";

    final String DIR_FROM_INSTITUTION = "src/test/resources/files/institutions";

    final String DIR_TO_INSTITUTION = DIR_TEST_RESOURCES + "/input/institutions";

    final String DIR_FROM_FUND = "src/test/resources/files/fund";

    final String DIR_TO_FUND = DIR_TEST_RESOURCES + "/input/fund";

    final String DIR_FROM_ARCH_DESC = "src/test/resources/files/archdesc";

    final String DIR_TO_ARCH_DESC = DIR_TEST_RESOURCES + "/input/archdesc";

    final String DIR_FROM_DIRECT = "src/test/resources/files/direct";

    final String DIRECT_ERR = "direct-err";

    final String DIR_FROM_DIRECT_ERR = "src/test/resources/files/" + DIRECT_ERR;

    final String DIR_TO_DIRECT = DIR_TEST_RESOURCES + "/input/direct";

    final static String INSTITUTION_CODE = "225201010";

    final String INSTITUTION_DIR = "institution-" + INSTITUTION_CODE;

    final static String FUND_CODE = "CR2303";

    final String FUND_DIR = "fund-" + FUND_CODE;

    final String ARCH_DESC_DIR = "archdesc-" + FUND_CODE;

    final String FILE_DIRECT = "apux-03.xml";

    final String FILE_DIRECT_EMPTY = "apux-00.xml";

    final String DIRECT_DIR = "direct-03";

    @Autowired
    ImportInstitutionService importInstitutionService;

    @Autowired
    ImportArchDescService importArchDescService;

    @Autowired
    ImportDirectService importDirectService;

    @Autowired
    ImportFundService importFundService;

    @Autowired
    FileImportService fileImportService;

    @Autowired
    ArchivalEntityRepository archivalEntityRepository;

    @Autowired
    EntitySourceRepository entitySourceRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    ApuSourceRepository apuSourceRepository;

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Autowired
    ArchDescRepository archDescRepository;

    @Autowired
    DaoFileRepository daoFileRepository;

    @Autowired
    FundRepository fundRepository;

    @BeforeEach
    protected void deleteAll() {
        entitySourceRepository.deleteAll();
        archivalEntityRepository.deleteAll();
        daoFileRepository.deleteAll();
        coreQueueRepository.deleteAll();
        archDescRepository.deleteAll();
        fundRepository.deleteAll();
        institutionRepository.deleteAll();
        apuSourceRepository.deleteAll();
    }

    /**
     * Přenos souboru XML ke zpracování
     * 
     * @param dirFrom
     * @param dirTo
     * @throws IOException
     * @throws InterruptedException
     */
    protected void processXmlFile(String dirFrom, String dirTo) throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File(dirFrom), new File(dirTo));
        do {
            Thread.sleep(1000);
        } while (!isEmpty(Path.of(dirTo)));
    }

    /**
     * Kontrola - je adresář prázdný?
     * 
     * @param path
     * @return
     * @throws IOException
     */
    protected boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }
}
