package cz.aron.transfagent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntitySource;

@Repository
public interface EntitySourceRepository extends JpaRepository<EntitySource,Integer> {

	List<EntitySource> findByArchivalEntity(ArchivalEntity archivalEntity);
	
	EntitySource findByArchivalEntityAndApuSource(ArchivalEntity archivalEntity, ApuSource apuSource);

	@Query("select es from EntitySource es join fetch es.archivalEntity where es.apuSource = :apusrc")
    List<EntitySource> findByApuSourceJoinFetchArchivalEntity(@Param("apusrc") ApuSource apuSource);

}
