package cz.aron.transfagent.peva;

import java.util.Map;

public class Peva2CodeLists {
    
    private final Map<String,String> accessibilities;
    private final Map<String,String> physicalStates;
    private final Map<String,String> integrity;    
    private final Map<String, String> findingAidTypes;
    private final Map<String, Peva2Language> languages;
    private final Map<String, Peva2EvidenceUnitType> evidenceUnitTypes;
    private final Map<String, String> findingAidFormTypes;
    
	public Peva2CodeLists(Map<String, String> accessibilities, Map<String, String> physicalStates,
			Map<String, String> integrity, Map<String, String> findingAidTypes, Map<String, Peva2Language> languages,
			Map<String, Peva2EvidenceUnitType> evidenceUnitTypes, Map<String, String> findingAidFormTypes) {
		this.accessibilities = accessibilities;
		this.physicalStates = physicalStates;
		this.integrity = integrity;
		this.findingAidTypes = findingAidTypes;
		this.languages = languages;
		this.evidenceUnitTypes = evidenceUnitTypes;
		this.findingAidFormTypes = findingAidFormTypes;
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

    public String getFindingAidType(String id) {
    	return findingAidTypes.get(id);
    }
    
    public String getFindingAidFormType(String id) {
    	return findingAidFormTypes.get(id);
    }
    
	public Peva2Language getLanguage(String id) {
		return languages.get(id);
	}
	
	public Peva2EvidenceUnitType getEvidenceUnitType(String id) {
		return evidenceUnitTypes.get(id);
	}
	
	public static class Peva2Language {
		private final String code;
		private final String name;
		public Peva2Language(String code, String name) {
			super();
			this.code = code;
			this.name = name;
		}
		public String getCode() {
			return code;
		}
		public String getName() {
			return name;
		}		
	}

	public static class Peva2EvidenceUnitType {
		private final String name;

		public Peva2EvidenceUnitType(String name) {
			super();
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}	
	
}
