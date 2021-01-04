package cz.aron.transfagent.transformation;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.InstitutionRepository;

public class DatabaseDataProvider implements ContextDataProvider {

    private InstitutionRepository institutionRepository;
    
    private ArchivalEntityRepository entityRepository;

    public DatabaseDataProvider(final InstitutionRepository institutionRepository,
    		final ArchivalEntityRepository entityRepository) {
    	this.institutionRepository = institutionRepository;
    	this.entityRepository = entityRepository;
    }

    @Override
    public String getInstitutionApu(String instCode) {
        Institution institution = institutionRepository.findByCode(instCode);
        if (institution != null) {
            return institution.getUuid().toString();
        }
        return null;
    }

	@Override
	public String getArchivalEntityApuByElzaId(Integer elzaId) {
		var entity = entityRepository.findByElzaId(elzaId);
		if(entity.isEmpty()) 
			return null;
		var entityRaw = entity.get();
		Validate.notNull(entityRaw.getUuid());
		return entityRaw.getUuid().toString();
	}

}
