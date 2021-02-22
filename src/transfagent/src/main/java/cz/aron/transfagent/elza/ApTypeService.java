package cz.aron.transfagent.elza;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.transfagent.elza.archentities.APTypeXml;
import cz.aron.transfagent.elza.archentities.APTypes;

public class ApTypeService {
	private static final Logger log = LoggerFactory.getLogger(ApTypeService.class);
	
    private Map<String, APTypeXml> apTypesMap = new HashMap<>();
    
    private Map<APTypeXml, APTypeXml> parentMap = new HashMap<>();

	private APTypes apTypesXml;
    
    private static JAXBContext jaxbContext;
    
    {
        try {
			jaxbContext = JAXBContext.newInstance(APTypes.class);
		} catch (JAXBException e) {
			log.error("Failed to prepare JAXB", e);
			throw new RuntimeException(e);
		}    	
    }
	
	public ApTypeService() {
		try {
		try(InputStream is = getClass().getClassLoader().getResourceAsStream("data/ap_type.xml")) {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            this.apTypesXml = (APTypes) unmarshaller.unmarshal(is);
			
            // fill lookup
            for(APTypeXml typeXml: apTypesXml.getRegisterTypes()) {
            	apTypesMap.put(typeXml.getCode(), typeXml);
            }
            // fill parent map
            for(APTypeXml typeXml: apTypesXml.getRegisterTypes()) {
            	String parentType = typeXml.getParentType();
            	if(parentType!=null) {
            		APTypeXml parent = apTypesMap.get(parentType);
            		Validate.notNull(parent, "Parent not found: %s", parentType);
            		this.parentMap.put(typeXml, parent);
            	}
            }
		}
		} catch(Exception e) {
			log.error("Faile to create APTypeService", e);
			throw new RuntimeException(e);
		}
	}

	public String getTypeName(String entityClass) {
		APTypeXml typeXml = apTypesMap.get(entityClass);
		if(typeXml!=null) {
			return typeXml.getName();
		}
		return null;
	}
	
	public String getParentName(String entityClass) {
		APTypeXml typeXml = apTypesMap.get(entityClass);
		if(typeXml!=null) {
			APTypeXml parentType = this.parentMap.get(typeXml);
			if(parentType!=null) {
				return parentType.getName();
			}
		}
		return null;
	}

    public String getParentCode(String entityClass) {
        APTypeXml typeXml = apTypesMap.get(entityClass);
        if(typeXml!=null) {
            APTypeXml parentType = this.parentMap.get(typeXml);
            if(parentType!=null) {
                return parentType.getCode();
            }
        }
        return null;
    }
}
