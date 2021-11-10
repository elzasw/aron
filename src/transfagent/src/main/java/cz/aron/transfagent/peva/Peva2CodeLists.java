package cz.aron.transfagent.peva;

import java.util.Map;

import cz.aron.transfagent.peva.Peva2CodeLists.Peva2Geo;

public class Peva2CodeLists {
    
    private final Map<String,String> accessibilities;
    private final Map<String,String> physicalStates;
    private final Map<String,String> integrity;    
    private final Map<String, String> findingAidTypes;
    private final Map<String, Peva2Language> languages;
    private final Map<String, Peva2EvidenceUnitType> evidenceUnitTypes;
    private final Map<String, String> findingAidFormTypes;
    private final Map<String,Peva2OriginatorSubclass> originatorSubClasses;
    private final Map<String,Peva2DatingMethod> datingMethods;
    private final Map<String, Peva2ThematicGroup> thematicGroups;
    private final Map<String, Peva2Geo> geoTypes;
    
	public Peva2CodeLists(Map<String, String> accessibilities, Map<String, String> physicalStates,
			Map<String, String> integrity, Map<String, String> findingAidTypes, Map<String, Peva2Language> languages,
			Map<String, Peva2EvidenceUnitType> evidenceUnitTypes, Map<String, String> findingAidFormTypes,
			Map<String,Peva2OriginatorSubclass> originatorSubClasses, Map<String,Peva2DatingMethod> datingMethods,
			Map<String, Peva2ThematicGroup> thematicGroups, Map<String, Peva2Geo> geoTypes) {
		this.accessibilities = accessibilities;
		this.physicalStates = physicalStates;
		this.integrity = integrity;
		this.findingAidTypes = findingAidTypes;
		this.languages = languages;
		this.evidenceUnitTypes = evidenceUnitTypes;
		this.findingAidFormTypes = findingAidFormTypes;
		this.originatorSubClasses = originatorSubClasses;
		this.datingMethods = datingMethods;
		this.thematicGroups = thematicGroups;
		this.geoTypes = geoTypes;
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
	
	public Peva2OriginatorSubclass getOriginatorSubClass(String id) {
		return originatorSubClasses.get(id);
	}
	
	public Peva2DatingMethod getDatingMethod(String id) {
		return datingMethods.get(id);
	}
	
	public Peva2ThematicGroup getThematicGroup(String id) {
		return thematicGroups.get(id);
	}
	
	public Peva2Geo getGeoType(String id) {
		return geoTypes.get(id);
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
	
	public static class Peva2OriginatorSubclass {
		private final String oClass;
		private final String name;
		private final String camCode;

		public Peva2OriginatorSubclass(String oClass, String name, String camCode) {
			this.oClass = oClass;
			this.name = name;
			this.camCode = camCode;
		}

		public String getoClass() {
			return oClass;
		}

		public String getName() {
			return name;
		}

		public String getCamCode() {
			return camCode;
		}
		
	}
	
	public static class Peva2DatingMethod {
		private final String name;
		private final String type;
		private final String camCode;

		public Peva2DatingMethod(String name, String type, String camCode) {
			this.name = name;
			this.type = type;
			this.camCode = camCode;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getCamCode() {
			return camCode;
		}

	}
	
	public static class Peva2ThematicGroup {
		
		private final String name;
		
		private final String code;

		public Peva2ThematicGroup(String name, String code) {
			super();
			this.name = name;
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public String getCode() {
			return code;
		}

	}
	
	public static class Peva2Geo {
		
		private final String name;
		
		private final String oSubclass;
		
		public Peva2Geo(String name, String oSubclass) {
			this.name = name;
			this.oSubclass = oSubclass;
		}

		public String getName() {
			return name;
		}

		public String getoSubclass() {
			return oSubclass;
		}
		
	}

}
