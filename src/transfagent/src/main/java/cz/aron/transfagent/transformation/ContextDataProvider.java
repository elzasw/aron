package cz.aron.transfagent.transformation;

/**
 * Interface for reading specific data 
 *
 */
public interface ContextDataProvider {

	String getInstitutionApu(String instCode);
	
	String getArchivalEntityApuByElzaId(Integer elzaId);

	String getFundApu(String institutionCode, String fundCode);

}
