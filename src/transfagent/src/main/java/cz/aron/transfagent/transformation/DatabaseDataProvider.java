package cz.aron.transfagent.transformation;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;

@Service
public class DatabaseDataProvider implements ContextDataProvider {

    final private InstitutionRepository institutionRepository;
    
    final private ArchivalEntityRepository entityRepository;
    
    final private FundRepository fundRepository;
    
    final private DaoFileRepository daoRepository;

    public DatabaseDataProvider(final InstitutionRepository institutionRepository,
    		final ArchivalEntityRepository entityRepository,
    		final FundRepository fundRepository,
    		final DaoFileRepository daoRepository) {
    	this.institutionRepository = institutionRepository;
    	this.entityRepository = entityRepository;
    	this.fundRepository = fundRepository;
    	this.daoRepository = daoRepository;
    }

    @Override
    public UUID getInstitutionApu(String instCode) {
        Institution institution = institutionRepository.findByCode(instCode);
        if (institution != null) {
            return institution.getUuid();
        }
        return null;
    }

	@Override
    public List<UUID> getArchivalEntityApuWithParentsByElzaId(Integer elzaId) {
        return entityRepository.findByElzaIdWithParents(elzaId).stream().filter(uuid -> uuid != null)
                .collect(Collectors.toList());
    }

	@Override
	public UUID getFundApu(String institutionCode, String fundCode) {
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
        	return null;
        }
		var fund = fundRepository.findByCodeAndInstitution(fundCode, institution);
		if(fund==null) {
			return null;
		}
		Validate.notNull(fund.getUuid());
		return fund.getUuid();
	}

    @Override
    public List<UUID> findByUUIDWithParents(UUID apUuid) {        
        return entityRepository.findByUUIDWithParents(apUuid);
    }

    @Override
    public UUID getDao(String daoHandle) {
        var dao = daoRepository.findByHandle(daoHandle);
        if(dao.isPresent()) {
            return dao.get().getUuid();
        } else {
            return null;
        }
    }

}
