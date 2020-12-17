package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aron-core")
public class ConfigAronCore {

	ConfigWsdl ft;

	ConfigWsdl core;

	public ConfigWsdl getFt() {
		return ft;
	}

	public void setFt(ConfigWsdl ft) {
		this.ft = ft;
	}

	public ConfigWsdl getCore() {
		return core;
	}

	public void setCore(ConfigWsdl core) {
		this.core = core;
	}

}
