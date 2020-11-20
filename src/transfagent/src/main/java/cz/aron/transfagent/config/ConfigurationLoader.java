package cz.aron.transfagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import cz.aron.common.config.ConfigLoader;
import cz.aron.common.itemtypes.TypesConfiguration;

@Configuration
@EnableScheduling
public class ConfigurationLoader {

    @Value("${config.file}")
    private String configFile;

    @Scheduled(fixedRate = 10000)
    private void load() {
        TypesConfiguration config = ConfigLoader.load(configFile);
    }

}
