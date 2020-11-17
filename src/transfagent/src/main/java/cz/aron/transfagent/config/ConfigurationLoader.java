package cz.aron.transfagent.config;

import java.io.IOException;
import java.io.Reader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import cz.aron.common.itemtypes.TypesConfiguration;

@Configuration
@EnableScheduling
public class ConfigurationLoader {

    @Value("${config.file}")
    private String configFile;

    @Scheduled(fixedRate = 10000)
    private void load() {
        TypesConfiguration config;
        FileSystemResource fileSystemResource = new FileSystemResource(configFile);
        try (Reader reader = new UnicodeReader(fileSystemResource.getInputStream())) {
            Yaml yaml = new Yaml();
            config = yaml.loadAs(reader, TypesConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
