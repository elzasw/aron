package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.service.FileImportService;

@SpringBootTest
public class FileImportServiceTest {

    private final String DIR_FROM = "src/test/resources/files/xml";

    private final String FILE_XML = "apux-03.xml";

    private final String DIR_FROM_ERR = "src/test/resources/files/xml_err";

    private final String FILE_XML_EMPTY = "apux-00.xml";

    private final String DIR_TO = "src/test/resources/input/direct";

    private final String DIR_DATA = "src/test/resources/data";

    private final String DIR_ERROR = "src/test/resources/error";

    private final String DIRECT_ERR = "direct-err";

    @Autowired
    FileImportService fileImportService;

    @Autowired
    ApuSourceRepository apuSourceRepository;

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Autowired
    DaoFileRepository daoFileRepository;

    @BeforeEach
    public void deleteAll() {
        daoFileRepository.deleteAll();
        coreQueueRepository.deleteAll();
        apuSourceRepository.deleteAll();
    }

    @Test
    public void testImportDirectDirSuccess() throws IOException, InterruptedException {

        FileUtils.copyDirectory(new File(DIR_FROM), new File(DIR_TO));
        do {
            Thread.sleep(1000);
        } while (!isEmpty(Path.of(DIR_TO)));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 1);

        SourceType sourceType = apuSources.get(0).getSourceType();
        assertTrue(sourceType == SourceType.DIRECT);

        String dataDir = apuSources.get(0).getDataDir();
        assertTrue(Files.exists(Path.of(DIR_DATA, dataDir, FILE_XML)));
    }

    @Test
    public void testImportDirectDirError() throws IOException, InterruptedException {

        FileSystemUtils.deleteRecursively(new File(DIR_ERROR));
        FileUtils.copyDirectory(new File(DIR_FROM_ERR), new File(DIR_TO));
        do {
            Thread.sleep(1000);
        } while (!isEmpty(Path.of(DIR_TO)));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.isEmpty());

        String dirDate = String.format("%1$tY%1$tm%1$td", new Date());
        assertTrue(Files.exists(Path.of(DIR_ERROR, dirDate, DIRECT_ERR, FILE_XML_EMPTY)));
    }

    /**
     * Kontrola - je adresář prázdný?
     * 
     * @param path
     * @return
     * @throws IOException
     */
    private boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }
}
