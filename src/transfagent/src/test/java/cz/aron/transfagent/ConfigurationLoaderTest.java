package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import cz.aron.common.itemtypes.ItemTypeConfig;
import cz.aron.common.itemtypes.MetaDataConfig;
import cz.aron.common.itemtypes.PartTypeConfig;
import cz.aron.common.itemtypes.TypesConfiguration;
import cz.aron.transfagent.config.ConfigurationLoader;

@SpringBootTest
public class ConfigurationLoaderTest {

    @Value("${config.file}")
    private String configFile;

    @Autowired
    ConfigurationLoader configurationLoader;

    @Test
    public void testConfigurationLoader() throws IOException {
        String cfgFile = new ClassPathResource(configFile).getFile().getAbsolutePath();
        TypesConfiguration config = configurationLoader.load(cfgFile);

        assertNotNull(config);

        List<PartTypeConfig> partTypes = config.getPartTypes();
        List<ItemTypeConfig> itemTypes = config.getItemTypes();
        List<MetaDataConfig> metaDataTypes = config.getMetaDataTypes();

        assertTrue(partTypes.size() == 2);
        assertTrue(itemTypes.size() == 3);
        assertTrue(metaDataTypes.size() == 1);
    }
}
