package cz.aron.transfagent;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.elza.ImportInstitution;

@SpringBootTest
public class ApuValidatorTest {

    private final String INST_DIR = "src/test/resources/files/institutions";

    private final String INST_FILE = "institution-215000010.xml";

    private final String INST_PREFIX = "institution-";

    private final String XML_EXT = ".xml";

    private final String APUSRC_XML = "apusrc.xml";

    private ApuValidator validator;

    private ApuSourceBuilder apusrcBuilder;

    @Autowired
    ConfigurationLoader configurationLoader;

    @BeforeEach
    public void initData() throws IOException, JAXBException {
        validator = new ApuValidator(configurationLoader.getConfig());

        String tmp = INST_FILE.substring(INST_PREFIX.length());
        String instCode = tmp.substring(0, tmp.length() - XML_EXT.length());

        ImportInstitution importInstitution = new ImportInstitution();
        apusrcBuilder = importInstitution.importInstitution(Path.of(INST_DIR, INST_FILE), instCode);
    }

    @Test
    public void testApuValidatorSucess() throws IOException, JAXBException {
        try (OutputStream fos = Files.newOutputStream(Path.of(INST_DIR, APUSRC_XML))) {
            apusrcBuilder.build(fos, validator);
        }
    }

    @Test
    public void testApuValidatorException() throws IOException, JAXBException {
        validator.getMapItems().remove("INST_CODE");

        Assertions.assertThrows(IllegalStateException.class, () -> {
            try (OutputStream fos = Files.newOutputStream(Path.of(INST_DIR, APUSRC_XML))) {
                apusrcBuilder.build(fos, validator);
            }
        });
    }

}
