package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;

public interface FindingAidRepository extends JpaRepository<FindingAid, Integer> {

    FindingAid findByCode(String code);

    FindingAid findByApuSource(ApuSource apuSource);

}
