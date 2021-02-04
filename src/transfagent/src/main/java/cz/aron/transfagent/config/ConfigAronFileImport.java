package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aron")
public class ConfigAronFileImport {

    boolean fileImportDisabled = false;

    public boolean isFileImportDisabled() {
        return fileImportDisabled;
    }

    public void setFileImportDisabled(boolean fileImportDisabled) {
        this.fileImportDisabled = fileImportDisabled;
    }

}
