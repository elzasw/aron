package cz.aron.transfagent.ead3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;
import org.archivists.ead3.schema.Control;
import org.archivists.ead3.schema.Subtitle;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;

public class ImportFindingAidInfo {

    private Ead3XmlReader ead3XmlReader;

    private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

    private ContextDataProvider dataProvider;

    private String institutionCode;

    private String fundCode;

    public ImportFindingAidInfo(String fundCode) {
        this.fundCode = fundCode;
    }

    public ApuSourceBuilder importFindingAidInfo(Path inputFile, String propFile) throws IOException, JAXBException {
        Validate.isTrue(propFile != null && propFile.length() > 0);

        PropertiesDataProvider pdp = new PropertiesDataProvider();
        Path propPath = Paths.get(propFile);
        pdp.load(propPath);

        return importFindingAidInfo(inputFile, null, pdp);
    }

    public ApuSourceBuilder importFindingAidInfo(final Path inputFile, UUID uuid, final ContextDataProvider cdp) throws IOException, JAXBException {
        this.dataProvider = cdp;

        try (InputStream is = Files.newInputStream(inputFile)) {
            ead3XmlReader = Ead3XmlReader.read(is);
            return importFindingAidInfo(uuid);
        }
    }

    public ApuSourceBuilder importFindingAidInfo(UUID uuid) {
        var ctrl = ead3XmlReader.getEad().getControl();

        var subtitules = ctrl.getFiledesc().getTitlestmt().getSubtitle();
        var contents = subtitules.get(0).getContent();
        String findingAidName = contents.get(0).toString();

        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        Apu apu = apusBuilder.createApu(findingAidName, ApuType.FINDING_AID, uuid);

        // add title part
        Part partTitle = apusBuilder.addPart(apu, CoreTypes.PT_TITLE);
        partTitle.setValue(findingAidName);
        apusBuilder.addString(partTitle, CoreTypes.TITLE, findingAidName);

        institutionCode = ctrl.getMaintenanceagency().getAgencycode().getContent();
        var instApuUuid = dataProvider.getInstitutionApu(institutionCode);
        Validate.notNull(instApuUuid, "Missing institution, code: %s", institutionCode);

        var fundApuUuid = dataProvider.getFundApu(institutionCode, fundCode);
        Validate.notNull(fundApuUuid, "Missing fund, code: %s, institution: %s", fundCode, institutionCode);

        // add references part
        Part partRef = apusBuilder.addPart(apu, CoreTypes.PT_ARCH_DESC_FUND);
        apusBuilder.addApuRef(partRef, "FUND_REF", fundApuUuid);
        apusBuilder.addApuRef(partRef, "FUND_INST_REF", instApuUuid);

        return apusBuilder;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }
}
