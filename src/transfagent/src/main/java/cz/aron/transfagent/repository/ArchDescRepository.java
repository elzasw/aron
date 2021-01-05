package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchDesc;
import cz.aron.transfagent.domain.Fund;

public interface ArchDescRepository extends JpaRepository<ArchDesc, Integer> {

    ArchDesc findByFund(Fund fund);

    ArchDesc findByApuSource(ApuSource apuSource);

}
