package cz.aron.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.aron.common.config.ConfigLoader;
import cz.aron.common.itemtypes.ItemTypeConfig;
import cz.aron.common.itemtypes.MetaDataConfig;
import cz.aron.common.itemtypes.PartTypeConfig;
import cz.aron.common.itemtypes.TypesConfiguration;

public class ConfigLoaderTest {

    private static final String CONFIG_TYPES_YAML = "config/types.yaml";

    @Test
    public void testConfigurationLoader() {
        String cfgFile = getClass().getClassLoader().getResource(CONFIG_TYPES_YAML).getFile();
        TypesConfiguration config = ConfigLoader.load(cfgFile);

        assertNotNull(config);

        List<PartTypeConfig> partTypes = config.getPartTypes();
        List<ItemTypeConfig> itemTypes = config.getItemTypes();
        List<MetaDataConfig> metaDataTypes = config.getMetaDataTypes();

        assertTrue(partTypes.size() == 2);
        assertTrue(itemTypes.size() == 3);
        assertTrue(metaDataTypes.size() == 1);
    }

}
