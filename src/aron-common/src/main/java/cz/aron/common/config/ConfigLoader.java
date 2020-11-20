package cz.aron.common.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import cz.aron.common.itemtypes.TypesConfiguration;

public class ConfigLoader {

    /**
     * Čtení dat pro konfigurační třídu ze souboru yaml
     * 
     * @param configFile
     * @return TypesConfiguration
     */
    public static TypesConfiguration load(String configFile) {
        try (Reader reader = new UnicodeReader(new FileInputStream(configFile))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(reader, TypesConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
