package cz.aron.transfagent.peva;

import java.util.HashMap;
import java.util.Map;

public class Peva2CodeLists {
    
    private final Map<String,String> accessibilities;
    private final Map<String,String> physicalStates;
    private final Map<String,String> integrity;    
    private final Map<String, String> findingAidTypes;
        
    public static final Map<String,String> findingAidTypesDefault = new HashMap<>();
    
    static {    	
    	findingAidTypesDefault.put("d54e798b-316b-4882-9d9a-bd302ba05a0c", "Dílčí inventář");
    	findingAidTypesDefault.put("fbbb1de2-0c54-40d4-a10f-890a9fdd2df8", "Inventář");
    	findingAidTypesDefault.put("67f26940-a3f6-479a-8e75-5c0159a9e620", "Katalog");
    	findingAidTypesDefault.put("6cc47792-9f7b-49f0-86db-c8cbefa166d9", "Rejstřík");
    	findingAidTypesDefault.put("031756ff-bdba-4d9d-91f6-05206ebc79a8", "Tematický katalog");
    	findingAidTypesDefault.put("3c4264dc-8483-4b53-b155-d450a8c67874", "Soupis archiválií");
    	findingAidTypesDefault.put("9bd707c6-d427-48b7-8ecc-4933a4c478df", "Tematický rejstřík");
    	findingAidTypesDefault.put("c7f59814-0d91-4c77-8113-d7cb6676f30b", "Manipulační seznam prvního typu");
    	findingAidTypesDefault.put("c61e1fe0-deee-4b9c-a643-b7e1f8423b55", "Manipulační seznam prvního typu");
    }
    
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
