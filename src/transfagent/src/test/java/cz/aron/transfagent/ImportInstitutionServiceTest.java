package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;

@SpringBootTest
public class ImportInstitutionServiceTest extends AbstractCommonTest {

    private final String DIR_FROM = "src/test/resources/files/institutions";

    private final String DIR_TO = "src/test/resources/input/institutions";

    private final String INSTITUTION_CODE = "225201010";

    @Autowired
    ImportInstitutionService importInstitutionService;

    @Test
    public void testImportInstitutionService() throws IOException, InterruptedException {

        FileUtils.copyDirectory(new File(DIR_FROM), new File(DIR_TO));
        do {
            Thread.sleep(1000);
        } while (!isEmpty(Path.of(DIR_TO)));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 1);

        SourceType sourceType = apuSources.get(0).getSourceType();
        assertTrue(sourceType == SourceType.INSTITUTION);

        Institution institution = institutionRepository.findByApuSource(apuSources.get(0));
        assertNotNull(institution);
        assertTrue(institution.getCode().equals(INSTITUTION_CODE));

    }
}
