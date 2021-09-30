package cz.aron.transfagent.peva;

import java.util.Map;

public class Peva2CodeLists {
    
    private final Map<String,String> accessibilities;
    private final Map<String,String> physicalStates;
    private final Map<String,String> integrity;    
    private final Map<String, String> findingAidTypes;
    
    public Peva2CodeLists(Map<String,String> accessibilities, Map<String,String> physicalStates, Map<String,String> integrity, Map<String,String> findingAidTypes) {
        this.accessibilities = accessibilities;
        this.physicalStates = physicalStates;
        this.integrity = integrity;
        this.findingAidTypes = findingAidTypes;
        
    }

    public String getAccessibilityName(String id) {
        return accessibilities.get(id);
    }

    public String getPhysicalStateName(String id) {
        return physicalStates.get(id);
    }

    public String getIntegrityName(String id) {
        return integrity.get(id);
    }

	public Map<String, String> getFindingAidTypes() {
		return findingAidTypes;
	}
    

}
