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

    // import attachments stored inside xml
    public boolean importAttachments;
    
    // show ZP2015_APPLIED_RESTRICTION
    public boolean showAccessRestrictions;
    
    // nazev urovne slozen ze zkratky instituce a nazvu urovne
    public boolean composedShortName;

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

    public boolean isImportAttachments() {
        return importAttachments;
    }

    public void setImportAttachments(boolean importAttachments) {
        this.importAttachments = importAttachments;
    }

    public boolean isShowAccessRestrictions() {
        return showAccessRestrictions;
    }

    public void setShowAccessRestrictions(boolean showAccessRestrictions) {
        this.showAccessRestrictions = showAccessRestrictions;
    }

	public boolean isComposedShortName() {
		return composedShortName;
	}

	public void setComposedShortName(boolean composedShortName) {
		this.composedShortName = composedShortName;
	}

}
