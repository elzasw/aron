package cz.aron.transfagent.transformation;

import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.repository.InstitutionRepository;

public class DatabaseDataProvider implements ContextDataProvider {

    @Autowired
    InstitutionRepository institutionRepository;

    @Override
    public String getInstitutionApu(String instCode) {
        Institution institution = institutionRepository.findByCode(instCode);
        if (institution != null) {
            return institution.getUuid().toString();
        }
        return null;
    }

}
