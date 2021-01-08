package cz.aron.transfagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import cz.aron.common.config.ConfigLoader;
import cz.aron.common.itemtypes.TypesConfiguration;

@Configuration
@EnableScheduling
public class ConfigurationLoader {

    final private static Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);

    @Value("${aron.typesConfig}")
    private String types;

    private TypesConfiguration config;

    @Scheduled(fixedRate = 10000)
    private void load() {
        try {
            log.debug("Loading configuration {}", types);

            config = ConfigLoader.load(types);

            log.debug("Configuration reloaded");
        } catch(Exception e) {
            log.error("Failed to load configuration: " + types, e);
        }
    }

    public String getTypes() {
        return types;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public TypesConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

}
