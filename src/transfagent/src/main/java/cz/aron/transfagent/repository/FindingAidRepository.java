package cz.aron.transfagent.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.FindingAid;

public interface FindingAidRepository extends JpaRepository<FindingAid, Integer> {

    FindingAid findByCode(String code);

    FindingAid findByApuSource(ApuSource apuSource);

	FindingAid findByUuid(UUID uuid);
}
