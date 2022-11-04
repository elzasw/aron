package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "level-enrichment")
public class ConfigLevelEnrichment {
    
    private String levelUrls;
    
    private String levelUrlsPrefix;
    
    private String levelUrlsLabel;
    
    public String getLevelUrls() {
        return levelUrls;
    }

    public void setLevelUrls(String levelUrls) {
        this.levelUrls = levelUrls;
    }

    public String getLevelUrlsPrefix() {
        return levelUrlsPrefix;
    }

    public void setLevelUrlsPrefix(String levelUrlsPrefix) {
        this.levelUrlsPrefix = levelUrlsPrefix;
    }

    public String getLevelUrlsLabel() {
        return levelUrlsLabel;
    }

    public void setLevelUrlsLabel(String levelUrlsLabel) {
        this.levelUrlsLabel = levelUrlsLabel;
    }    

}
