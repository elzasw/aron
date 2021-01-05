package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Institution;

public interface InstitutionRepository extends JpaRepository<Institution, Integer>{

    @EntityGraph(attributePaths = {"apuSource"})
    Institution findByCode(String code);

    Institution findByApuSource(ApuSource apuSource);

}
