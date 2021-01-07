package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;

@SpringBootTest
public class ImportInstitutionServiceTest extends AbstractCommonTest {

    @Test
    @Transactional
    public void testImportInstitutionService() throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File(DIR_FROM_INSTITUTION), new File(DIR_TO_INSTITUTION));
        importInstitutionService.processDirectory(Path.of(DIR_TO_INSTITUTION, INSTITUTION_DIR));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 1);

        ApuSource apuSource = apuSources.get(0);
        assertTrue(apuSource.getSourceType() == SourceType.INSTITUTION);

        List<ArchivalEntity> archivalEntities = archivalEntityRepository.findAll();
        assertTrue(apuSources.size() == 1);

        List<EntitySource> entitySources = entitySourceRepository.findAll();
        assertTrue(entitySources.size() == 1);

        ArchivalEntity archivalEntity = archivalEntities.get(0);
        EntitySource entitySource = entitySources.get(0);
        assertTrue(entitySource.getArchivalEntity().equals(archivalEntity));
        assertTrue(entitySource.getApuSource().equals(apuSource));

        Institution institution = institutionRepository.findByApuSource(apuSources.get(0));
        assertNotNull(institution);
        assertTrue(institution.getCode().equals(INSTITUTION_CODE));

        // kontrola reimportu
        importInstitutionService.reimport(apuSource);
    }

}
