package cz.aron.transfagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import cz.aron.common.config.ConfigLoader;
import cz.aron.common.itemtypes.TypesConfiguration;

@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "config")
public class ConfigurationLoader {

    final private static Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);

    private String file;

    private TypesConfiguration config;

    @Scheduled(fixedRate = 10000)
    private void load() {
        try {
            log.debug("Loading configuration {}", file);

            config = ConfigLoader.load(file);

            log.debug("Configuration reloaded");
        } catch(Exception e) {
            log.error("Failed to load configuration: " + file, e);
        }
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public TypesConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

}
