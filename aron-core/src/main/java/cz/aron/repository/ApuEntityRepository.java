package cz.aron.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.domain.ApuEntity;
import cz.aron.domain.types.dto.ApuEntityView;
import cz.aron.domain.types.dto.ApuEntityViewType;
import cz.aron.domain.types.dto.ApuIdParentId;


@Repository
public interface ApuEntityRepository extends JpaRepository<ApuEntity, Long> {

	ApuEntity findByUuid(String uuid);

	@Query("SELECT apu.id, apu.parent.id FROM ApuEntity apu WHERE apu.source.id=:apuSourceId")
	List<ApuIdParentId> findIdParentIdByApuSourceId(@Param("apuSourceId") long apuSourceId);
	
	@Query("SELECT ae.uuid, ae.name, ae.description, ae.order FROM ApuEntity ae WHERE ae.uuid IN (:uuids)")
	List<ApuEntityView> findAllByUuids(@Param("uuids") Collection<String> uuids); 

	/*
	@Query(value="""
WITH RECURSIVE cte(uuid, name, description, ordr, type, apu_id, parent_id) AS
(
	SELECT uuid, name, description, ordr, type, apu_id, parent_id
	FROM apu a
	WHERE a.uuid='073d6b0a-e659-4424-bdb1-d6e959f40dab'
	UNION ALL
	SELECT a.uuid, a.name, a.description, a.ordr, a.type, a.apu_id, a.parent_id
	FROM cte
	JOIN apu a ON a.parent_id=cte.apu_id			
)
SELECT uuid, name, description, ordr, type, apu_id, parent_id
FROM cte
""", nativeQuery=true)
*/
	
	@Query(name="entityTree", nativeQuery=true)
	List<ApuEntityViewType> findAllByParentUuid(@Param("uuid") String uuid);
	
	@Query(value="SELECT aps.published FROM ApuEntity ae JOIN ae.source aps WHERE ae.uuid=:uuid")
	LocalDateTime findPublishedByUuid(@Param("uuid") String uuid);

}
