package cz.aron.transfagent.transformation;

import java.util.UUID;

/**
 * Interface for reading specific data 
 *
 */
public interface ContextDataProvider {

	UUID getInstitutionApu(String instCode);
	
	UUID getArchivalEntityApuByElzaId(Integer elzaId);

	UUID getFundApu(String institutionCode, String fundCode);

}
