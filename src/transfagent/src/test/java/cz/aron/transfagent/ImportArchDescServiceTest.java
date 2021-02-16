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
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;

@Disabled
@SpringBootTest
public class ImportArchDescServiceTest extends AbstractCommonTest {

    @Test
    @Transactional
    public void testImportArchDescService() throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File(DIR_FROM_INSTITUTION), new File(DIR_TO_INSTITUTION));
        importInstitutionService.processDirectory(Path.of(DIR_TO_INSTITUTION, INSTITUTION_DIR));

        FileUtils.copyDirectory(new File(DIR_FROM_FUND), new File(DIR_TO_FUND));
        importFundService.processDirectory(Path.of(DIR_TO_FUND, FUND_DIR));

        FileUtils.copyDirectory(new File(DIR_FROM_ARCH_DESC), new File(DIR_TO_ARCH_DESC));
        importArchDescService.processDirectory(Path.of(DIR_TO_ARCH_DESC, ARCH_DESC_DIR));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 3);

        ApuSource apuSource = apuSources.get(2);
        assertTrue(apuSource.getSourceType() == SourceType.ARCH_DESCS);

        List<Fund> funds = fundRepository.findAll();
        assertTrue(funds.size() == 1);

        List<ArchDesc> archDescs = archDescRepository.findAll();
        assertTrue(archDescs.size() == 1);

        ArchDesc archDesc = archDescs.get(0);
        Fund fund = funds.get(0);
        assertTrue(archDesc.getApuSource().equals(apuSource));
        assertTrue(archDesc.getFund().equals(fund));

        // kontrola reimportu
        importArchDescService.reimport(apuSource);
    }

}
