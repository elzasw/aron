package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.IdProjection;

@Repository
public interface ArchivalEntityRepository extends JpaRepository<ArchivalEntity, Integer> {

    List<ArchivalEntity> findByUuidInAndStatus(List<UUID> uuids, EntityStatus status);
    
    Optional<ArchivalEntity> findByUuid(UUID uuid);
    
    Optional<ArchivalEntity> findByElzaId(Integer elzaId);
    
    List<IdProjection> findTop1000ByStatusOrderById(EntityStatus status);

	List<ArchivalEntity> findAllByParentEntity(ArchivalEntity archivalEntity);

	ArchivalEntity findByApuSource(ApuSource apuSource);

	@Modifying
	@Query(nativeQuery = true, value = "with recursive subtree(entity_id) as (\r\n"
			+ " select ae.entity_id from archival_entity ae where ae.entity_id = :entId\r\n"
			+ " union \r\n"
			+ " select ae2.entity_id from archival_entity ae2 \r\n"
			+ " inner join subtree st on ae2.parent_entity_id = st.entity_id \r\n"
			+ " )\r\n"
			+ "update apu_source set reimport = true where \r\n"
			+ "  apusource_id in (\r\n"
			+ "select es.apusource_id from entity_source es\r\n"
			+ "join subtree as st on st.entity_id = es.entity_id );\r\n"
			)
	void reimportConnected(@Param("entId") Integer entId);

}
