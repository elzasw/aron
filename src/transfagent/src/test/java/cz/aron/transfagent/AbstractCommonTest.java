package cz.aron.transfagent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;

public abstract class AbstractCommonTest {

    final String DIR_FROM_INSTITUTION = "src/test/resources/files/institutions";

    final String DIR_TO_INSTITUTION = "src/test/resources/input/institutions";

    final String DIR_FROM_FUND = "src/test/resources/files/fund";

    final String DIR_TO_FUND = "src/test/resources/input/fund";

    final String INSTITUTION_CODE = "225201010";

    final String INSTITUTION_DIR = "institution-" + INSTITUTION_CODE;

    final String FUND_CODE = "CR2303";

    final String FUND_DIR = "fund-" + FUND_CODE;

    @Autowired
    ImportInstitutionService importInstitutionService;

    @Autowired
    ImportFundService importFundService;

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
    DaoFileRepository daoFileRepository;

    @Autowired
    FundRepository fundRepository;

    @BeforeEach
    protected void deleteAll() {
        entitySourceRepository.deleteAll();
        archivalEntityRepository.deleteAll();
        daoFileRepository.deleteAll();
        coreQueueRepository.deleteAll();
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
