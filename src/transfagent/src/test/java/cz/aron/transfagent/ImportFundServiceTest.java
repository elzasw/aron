package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;

@Disabled
@SpringBootTest
public class ImportFundServiceTest extends AbstractCommonTest {

    @Test
    @Transactional
    public void testImportFundService() throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File(DIR_FROM_INSTITUTION), new File(DIR_TO_INSTITUTION));
        importInstitutionService.processDirectory(Path.of(DIR_TO_INSTITUTION, INSTITUTION_DIR));

        FileUtils.copyDirectory(new File(DIR_FROM_FUND), new File(DIR_TO_FUND));
        importFundService.processDirectory(Path.of(DIR_TO_FUND, FUND_DIR));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 2);

        ApuSource apuSource = apuSources.get(1);
        assertTrue(apuSource.getSourceType() == SourceType.FUND);

        List<Institution> institutions = institutionRepository.findAll();
        assertTrue(institutions.size() == 1);

        List<Fund> funds = fundRepository.findAll();
        assertTrue(funds.size() == 1);

        Institution institution = institutions.get(0);
        Fund fund = funds.get(0);
        assertTrue(fund.getCode().equals(FUND_CODE));
        assertTrue(fund.getApuSource().equals(apuSource));
        assertTrue(fund.getInstitution().equals(institution));

        // kontrola reimportu
        importFundService.reimport(apuSource);
    }

}
