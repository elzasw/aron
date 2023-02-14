package cz.aron.transfagent.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "filestore3")
public class ConfigDaoFileStore3 {
    
    private Path path;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
    
}
