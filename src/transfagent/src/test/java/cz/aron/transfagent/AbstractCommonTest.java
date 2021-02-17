package cz.aron.transfagent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchDescRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportArchDescService;
import cz.aron.transfagent.service.importfromdir.ImportDirectService;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;
import cz.aron.transfagent.service.importfromdir.TransformService;

public abstract class AbstractCommonTest {

    final static String DIR_TEST_RESOURCES = "target/test-resources";

    final static String DIR_DATA = DIR_TEST_RESOURCES + "/data";

    final static String DIR_ERROR = DIR_TEST_RESOURCES + "/error";

    final static String SRC_TEST_RESOURCES_FILES = "src/test/resources/files";

    final String DIR_FROM_INSTITUTION = SRC_TEST_RESOURCES_FILES + "/institutions";

    final String DIR_TO_INSTITUTION = DIR_TEST_RESOURCES + "/input/institutions";

    final String DIR_FROM_FUND = SRC_TEST_RESOURCES_FILES + "/fund";

    final String DIR_TO_FUND = DIR_TEST_RESOURCES + "/input/fund";

    final String DIR_FROM_ARCH_DESC = SRC_TEST_RESOURCES_FILES + "/archdesc";

    final String DIR_TO_ARCH_DESC = DIR_TEST_RESOURCES + "/input/archdesc";

    final String DIR_FROM_FINDING_AID = SRC_TEST_RESOURCES_FILES + "/findingaids";

    final String DIR_TO_FINDING_AID = DIR_TEST_RESOURCES + "/input/findingaids";

    final String DIR_FROM_DIRECT = SRC_TEST_RESOURCES_FILES + "/direct";

    final String DIRECT_ERR = "direct-err";

    final String DIR_FROM_DIRECT_ERR = SRC_TEST_RESOURCES_FILES + "/" + DIRECT_ERR;

    final String DIR_TO_DIRECT = DIR_TEST_RESOURCES + "/input/direct";

    final String DIR_TO_DAO = SRC_TEST_RESOURCES_FILES + "/dao";

    final static String INSTITUTION_CODE = "225201010";

    final String INSTITUTION_DIR = "institution-" + INSTITUTION_CODE;

    final static String FUND_CODE = "CR2303";

    final String FUND_DIR = "fund-" + FUND_CODE;

    final String ARCH_DESC_DIR = "archdesc-" + FUND_CODE;

    final String FINDING_AID_DIR = "findingaid-" + FUND_CODE;

    final String FILE_DIRECT = "apux-03.xml";

    final String FILE_DIRECT_EMPTY = "apux-00.xml";

    final String DIRECT_DIR = "direct-03";

    @Autowired
    ImportInstitutionService importInstitutionService;

    @Autowired
    ImportFindingAidService importFindingAidService;

    @Autowired
    ImportArchDescService importArchDescService;
    
    @Autowired
    ImportDirectService importDirectService;

    @Autowired
    ImportFundService importFundService;

    @Autowired
    FileImportService fileImportService;

    @Autowired
    TransformService transformService;

    @Autowired
    StorageService storageService;

    @Autowired
    ArchivalEntityRepository archivalEntityRepository;

    @Autowired
    EntitySourceRepository entitySourceRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    FindingAidRepository findingAidRepository;

    @Autowired
    AttachmentRepository attachmentRepository;

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

    @AfterAll
    public static void deleteApusrcXml() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of(DIR_DATA));
        FileSystemUtils.deleteRecursively(Path.of(DIR_ERROR));
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

    /**
     * Aktuální datum jako řetězec znaků
     * 
     * @return YYYYMMDD
     */
    protected String getDateDir() {
        return String.format("%1$tY%1$tm%1$td", new Date()); 
    }

}
