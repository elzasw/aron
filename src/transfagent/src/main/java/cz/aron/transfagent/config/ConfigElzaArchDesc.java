package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "archdesc")
public class ConfigElzaArchDesc {
    
    // add date to level name
    public boolean addDateToName;
    
    // inherit name+date field from parent when name is empty
    public boolean inheritNameDate;

    public boolean isAddDateToName() {
        return addDateToName;
    }

    public void setAddDateToName(boolean addDateToName) {
        this.addDateToName = addDateToName;
    }

    public boolean isInheritNameDate() {
        return inheritNameDate;
    }

    public void setInheritNameDate(boolean inheritNameDate) {
        this.inheritNameDate = inheritNameDate;
    }    

}
