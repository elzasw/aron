package cz.aron.transfagent.config;

public class ConfigPeva2FindingAidProperties {
	
	// nazev pomucky slozi z [nazevarchivu],[nazevfondu],[datace]
    private boolean composedFindingAidName;
    
    // neodesila pomucku do arona, pouze ji nacte
    private boolean dontSend;

	public boolean isComposedFindingAidName() {
		return composedFindingAidName;
	}

	public void setComposedFindingAidName(boolean composedFindingAidName) {
		this.composedFindingAidName = composedFindingAidName;
	}

	public boolean isDontSend() {
		return dontSend;
	}

	public void setDontSend(boolean dontSend) {
		this.dontSend = dontSend;
	}	
    
}
