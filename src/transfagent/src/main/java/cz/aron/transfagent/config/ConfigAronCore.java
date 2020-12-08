package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aron-core")
public class ConfigAronCore {

    private String url;

    private String user;

    private String pass;

    private Boolean soapLogging;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public Boolean getSoapLogging() {
        return soapLogging;
    }

    public void setSoapLogging(Boolean soapLogging) {
        this.soapLogging = soapLogging;
    }
}
