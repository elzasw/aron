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
    
    List<ArchivalEntity> findByUuidIn(List<UUID> batch);

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

	@Query("select ae from ArchivalEntity ae where ae.elzaId in (:ids)")
    List<ArchivalEntity> findByElzaIds(@Param("ids") List<Integer> ids);
	
	@Query(nativeQuery = true, value="WITH RECURSIVE cte(uuid) as "
	        + "( "
	        + "SELECT uuid,parent_entity_id,1 as depth "
	        + "FROM archival_entity ae "
	        + "WHERE ae.elza_id=?1 "
	        + "UNION ALL "
	        + "SELECT ae2.uuid, ae2.parent_entity_id, cte.depth+1 "
	        + "FROM archival_entity ae2, cte "
	        + "WHERE ae2.entity_id=cte.parent_entity_id "
	        + ") "
	        + "SELECT CAST(uuid as VARCHAR(50)) "
	        + "FROM cte "
	        + "ORDER BY depth")
	List<UUID> findByElzaIdWithParents(Integer elzaId);
	
	@Query(nativeQuery = true, value="WITH RECURSIVE cte(uuid) as "
            + "( "
            + "SELECT uuid,parent_entity_id,1 as depth "
            + "FROM archival_entity ae "
            + "WHERE ae.uuid=?1 "
            + "UNION ALL "
            + "SELECT ae2.uuid, ae2.parent_entity_id, cte.depth+1 "
            + "FROM archival_entity ae2, cte "
            + "WHERE ae2.entity_id=cte.parent_entity_id "
            + ") "
            + "SELECT CAST(uuid as VARCHAR(50)) "
            + "FROM cte "
            + "ORDER BY depth")
	List<UUID> findByUUIDWithParents(UUID uuid);

    @Modifying
	@Query("update ArchivalEntity ae set ae.download = true where ae.elzaId in :ids")
    void setDownloadTrueByIds(@Param("ids") List<Integer> ids);

}
