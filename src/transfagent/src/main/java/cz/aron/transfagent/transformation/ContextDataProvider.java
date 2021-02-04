package cz.aron.transfagent.transformation;

import java.util.List;
import java.util.UUID;

/**
 * Interface for reading specific data 
 *
 */
public interface ContextDataProvider {

	UUID getInstitutionApu(String instCode);
	
	List<UUID> getArchivalEntityApuWithParentsByElzaId(Integer elzaId);

	UUID getFundApu(String institutionCode, String fundCode);

    List<UUID> findByUUIDWithParents(UUID apUuid);

    UUID getDao(String daoHandle);

}
