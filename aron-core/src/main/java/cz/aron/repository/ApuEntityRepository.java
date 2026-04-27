package cz.aron.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.domain.ApuEntity;
import cz.aron.domain.dto.IdLabelDto;
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
	
	//@EntityGraph(attributePaths = {"parts.items"})
	List<ApuEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);
	
	@Query("SELECT ae.id FROM ApuEntity ae")
	List<Long> findAllIds();
	
	@Query(name="entityTree", nativeQuery=true)
	List<ApuEntityViewType> findAllByParentUuid(@Param("uuid") String uuid);
	
	@Query(value="SELECT aps.published FROM ApuEntity ae JOIN ae.source aps WHERE ae.uuid=:uuid")
	LocalDateTime findPublishedByUuid(@Param("uuid") String uuid);

	@Query("SELECT new cz.aron.domain.dto.IdLabelDto(ae.id, ae.uuid, ae.name) FROM ApuEntity ae WHERE ae.uuid IN (:uuids)")
	List<IdLabelDto> listByUuids(@Param("uuids") Collection<String> uuids);
	
	

}
