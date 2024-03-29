package cz.aron.transfagent.config;

import java.util.List;

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
    
    public boolean processInternalSupplement;
    
    public boolean uniqueLevels;
    
    // mapování přístupových bodů na hodnoty
    private List<ConfigElzaArchDescApMapping> apMappings;

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

	public List<ConfigElzaArchDescApMapping> getApMappings() {
		return apMappings;
	}

	public void setApMappings(List<ConfigElzaArchDescApMapping> apMappings) {
		this.apMappings = apMappings;
	}

	public boolean isProcessInternalSupplement() {
		return processInternalSupplement;
	}

	public void setProcessInternalSupplement(boolean processInternalSupplement) {
		this.processInternalSupplement = processInternalSupplement;
	}

	public boolean isUniqueLevels() {
		return uniqueLevels;
	}

	public void setUniqueLevels(boolean uniqueLevels) {
		this.uniqueLevels = uniqueLevels;
	}

}
