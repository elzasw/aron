package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "par")
public class ConfigPar {
	
	private String url;
	
    private String username;
    
    private String password;
	
	private boolean soapLogging;
	
	private long interval = 3600;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public boolean isSoapLogging() {
		return soapLogging;
	}

	public void setSoapLogging(boolean soapLogging) {
		this.soapLogging = soapLogging;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
