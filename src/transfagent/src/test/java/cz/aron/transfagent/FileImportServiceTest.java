package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;

@SpringBootTest
public class FileImportServiceTest extends AbstractCommonTest {

    @Test
    public void testImportDirectDirSuccess() throws IOException, InterruptedException {
        processXmlFile(DIR_FROM_DIRECT, DIR_TO_DIRECT);

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 1);

        SourceType sourceType = apuSources.get(0).getSourceType();
        assertTrue(sourceType == SourceType.DIRECT);

        String dataDir = apuSources.get(0).getDataDir();
        assertTrue(Files.exists(Path.of(DIR_DATA, dataDir, FILE_DIRECT)));
    }

    @Test
    public void testImportDirectDirError() throws IOException, InterruptedException {
        FileSystemUtils.deleteRecursively(new File(DIR_ERROR));
        processXmlFile(DIR_FROM_DIRECT_ERR, DIR_TO_DIRECT);

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.isEmpty());

        String dirDate = String.format("%1$tY%1$tm%1$td", new Date());
        assertTrue(Files.exists(Path.of(DIR_ERROR, dirDate, DIRECT_ERR, FILE_DIRECT_EMPTY)));
    }

}
