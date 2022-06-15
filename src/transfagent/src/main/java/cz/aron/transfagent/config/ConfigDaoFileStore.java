package cz.aron.transfagent.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "filestore")
public class ConfigDaoFileStore {

	private Path path;
	
	private String mappingName;

	public Path getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = Paths.get(path);
	}

	public String getMappingName() {
		return mappingName;
	}

	public void setMappingName(String mappingName) {
		this.mappingName = mappingName;
	}

}
