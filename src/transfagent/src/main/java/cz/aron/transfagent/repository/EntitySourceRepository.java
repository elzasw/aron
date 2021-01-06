package cz.aron.transfagent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntitySource;

@Repository
public interface EntitySourceRepository extends JpaRepository<EntitySource,Integer> {

	List<EntitySource> findByArchivalEntity(ArchivalEntity archivalEntity);

    List<EntitySource> findByApuSourceFetchJoinArchivalEntity(ApuSource apuSource);

}
