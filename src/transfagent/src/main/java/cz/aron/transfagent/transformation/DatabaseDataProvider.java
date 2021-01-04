package cz.aron.transfagent.transformation;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;

@Service
public class DatabaseDataProvider implements ContextDataProvider {

    final private InstitutionRepository institutionRepository;
    
    final private ArchivalEntityRepository entityRepository;
    
    final private FundRepository fundRepository;

    public DatabaseDataProvider(final InstitutionRepository institutionRepository,
    		final ArchivalEntityRepository entityRepository,
    		final FundRepository fundRepository) {
    	this.institutionRepository = institutionRepository;
    	this.entityRepository = entityRepository;
    	this.fundRepository = fundRepository;
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

	@Override
	public String getFundApu(String institutionCode, String fundCode) {
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
        	return null;
        }
		var fund = fundRepository.findByCodeAndInstitution(fundCode, institution);
		if(fund==null) {
			return null;
		}
		Validate.notNull(fund.getUuid());
		return fund.getUuid().toString();
	}

}
