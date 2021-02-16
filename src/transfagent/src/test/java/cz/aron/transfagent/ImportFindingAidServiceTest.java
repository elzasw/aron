package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;

@Disabled
@SpringBootTest
public class ImportFindingAidServiceTest extends AbstractCommonTest {

    @Test
    @Transactional
    public void testImportFindingAidService() throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File(DIR_FROM_INSTITUTION), new File(DIR_TO_INSTITUTION));
        importInstitutionService.processDirectory(Path.of(DIR_TO_INSTITUTION, INSTITUTION_DIR));

        FileUtils.copyDirectory(new File(DIR_FROM_FUND), new File(DIR_TO_FUND));
        importFundService.processDirectory(Path.of(DIR_TO_FUND, FUND_DIR));

        FileUtils.copyDirectory(new File(DIR_FROM_FINDING_AID), new File(DIR_TO_FINDING_AID));
        importFindingAidService.processDirectory(Path.of(DIR_TO_FINDING_AID, FINDING_AID_DIR));

        List<ApuSource> apuSources = apuSourceRepository.findAll();
        assertTrue(apuSources.size() == 3);

        ApuSource apuSource = apuSources.get(2);
        assertTrue(apuSource.getSourceType() == SourceType.FINDING_AID);

        List<Institution> institutions = institutionRepository.findAll();
        assertTrue(institutions.size() == 1);

        List<Fund> funds = fundRepository.findAll();
        assertTrue(funds.size() == 1);

        List<FindingAid> findingAids = findingAidRepository.findAll();
        assertTrue(funds.size() == 1);

        List<Attachment> attachments = attachmentRepository.findAll();
        assertTrue(attachments.size() == 1);

        Institution institution = institutions.get(0);
        Fund fund = funds.get(0);
        FindingAid findingAid = findingAids.get(0);

        assertTrue(findingAid.getCode().equals(FUND_CODE));
        assertTrue(findingAid.getApuSource().equals(apuSource));
        assertTrue(findingAid.getInstitution().equals(institution));
        assertTrue(findingAid.getFund().equals(fund));

        // kontrola reimportu
        Path filePdf = storageService.getDataPath().resolve(apuSource.getDataDir()).resolve(apuSource.getOrigDir() + ".pdf");
        Files.delete(filePdf);
        importFindingAidService.reimport(apuSource);

        attachments = attachmentRepository.findAll();
        assertTrue(attachments.isEmpty());
    }

}
