package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.SourceType;

@SpringBootTest
public class ImportDirectServiceTest extends AbstractCommonTest {

    @Test
    public void testImportDirectService() throws IOException {
        FileUtils.copyDirectory(new File(DIR_FROM_DIRECT), new File(DIR_TO_DIRECT));
        importDirectService.processDirectory(Path.of(DIR_TO_DIRECT, DIRECT_DIR));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 1);

        ApuSource apuSource = apuSources.get(0);
        assertTrue(apuSource.getSourceType() == SourceType.DIRECT);

        List<Dao> daoFiles = daoFileRepository.findByApuSource(apuSource);
        assertTrue(daoFiles.size() > 0);

        // kontrola reimportu
        importDirectService.reimport(apuSource);
    }

}
