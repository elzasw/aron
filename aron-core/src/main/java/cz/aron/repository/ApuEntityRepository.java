package cz.aron.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.api.rest.model.ApuEntityTreeViewDto;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.dto.IdStructuredResultDto;
import cz.aron.domain.dto.IdUuidNameDescriptionOrderDto;
import cz.aron.domain.dto.IdUuidNameDescriptionParentDto;
import cz.aron.domain.types.dto.ApuEntityView;
import cz.aron.domain.types.dto.ApuEntityViewType;


@Repository
public interface ApuEntityRepository extends JpaRepository<ApuEntity, Long> {

	@EntityGraph(attributePaths = {"source"})
	ApuEntity findByUuid(UUID uuid);

	/**
	 * Detaches all APUs of the given ApuSource from their parents, so that they can be deleted by
	 * {@link #deleteAllByApuSourceId(long)} in a single statement without ordering them
	 * child-before-parent (FK_apu_parent is a plain NO ACTION constraint).
	 *
	 * @return num updated records
	 */
	@Modifying
	@Query("UPDATE ApuEntity ae SET ae.parent=NULL WHERE ae.source.id=:apuSourceId")
	int clearParentsByApuSourceId(@Param("apuSourceId") long apuSourceId);

	/**
	 * Deletes all APUs of the given ApuSource. Everything referencing them has to be removed first:
	 * attachments (and their files), and {@code digital_object.apu_id} has to be NULLed — see
	 * {@link DaoRepository#disconnectDaosByApuSourceId(long)}.
	 *
	 * @return num deleted records
	 */
	@Modifying
	@Query("DELETE FROM ApuEntity ae WHERE ae.source.id=:apuSourceId")
	int deleteAllByApuSourceId(@Param("apuSourceId") long apuSourceId);

	@Query("SELECT cast(ae.uuid as string), ae.name, ae.description, ae.order FROM ApuEntity ae WHERE ae.uuid IN (:uuids)")
	List<ApuEntityView> findAllByUuids(@Param("uuids") Collection<UUID> uuids);

	List<ApuEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);

	List<ApuEntity> findAllByUuidIn(@Param("ids") Collection<UUID> ids);

	@Query("SELECT ae.id FROM ApuEntity ae WHERE ae.id>:after ORDER BY ae.id asc LIMIT :numItems")
	List<Long> findIds(@Param("after") long after, @Param("numItems") long numItems);

	@Query(name="entityTree", nativeQuery=true)
	List<ApuEntityViewType> findAllByParentUuid(@Param("uuid") UUID uuid);

	@Query(name="apuAncestors", nativeQuery=true)
	List<IdUuidNameDescriptionParentDto> findAncestors(@Param("id") long id);

	@Query(value="SELECT aps.published FROM ApuEntity ae JOIN ae.source aps WHERE ae.uuid=:uuid")
	LocalDateTime findPublishedByUuid(@Param("uuid") UUID uuid);

	@Query("SELECT new cz.aron.domain.dto.IdLabelDto(ae.id, ae.uuid, ae.name, ae.indexedName) FROM ApuEntity ae WHERE ae.uuid IN (:uuids)")
	List<IdLabelDto> listByUuids(@Param("uuids") Collection<UUID> uuids);

	// --- index synchronization (IndexSynchronizer) ---------------------------------------------

	/** Labels of every APU of the source - the snapshot a re-import compares its result with. */
	@Query("SELECT new cz.aron.domain.dto.IdLabelDto(ae.id, ae.uuid, ae.name, ae.indexedName) FROM ApuEntity ae WHERE ae.source.id=:apuSourceId")
	List<IdLabelDto> listLabelsByApuSourceId(@Param("apuSourceId") long apuSourceId);

	@Query("SELECT new cz.aron.api.rest.model.ApuEntityTreeViewDto(cast(ae.uuid as string), ae.name, ae.description, ae.depth, ae.pos, ae.childCnt) FROM ApuEntity ae WHERE ae.parent.id=:parentId AND ae.pos>:pos ORDER BY ae.pos asc LIMIT :maxItems ")
	List<ApuEntityTreeViewDto> listEntitiesAfter(@Param("parentId") long parentId, @Param("pos") int pos, @Param("maxItems") int maxItems);

	@Query("SELECT new cz.aron.api.rest.model.ApuEntityTreeViewDto(cast(ae.uuid as string), ae.name, ae.description, ae.depth, ae.pos, ae.childCnt) FROM ApuEntity ae WHERE ae.source.id=:apuSourceId AND ae.parent IS NULL AND ae.pos>:pos ORDER BY ae.pos asc LIMIT :maxItems")
	List<ApuEntityTreeViewDto> listRootEntitiesAfter(@Param("apuSourceId") long apuSourceId, @Param("pos") int pos, @Param("maxItems") int maxItems);

	@Query("SELECT new cz.aron.api.rest.model.ApuEntityTreeViewDto(cast(ae.uuid as string), ae.name, ae.description, ae.depth, ae.pos, ae.childCnt) FROM ApuEntity ae WHERE ae.parent.id=:parentId AND ae.pos<:pos ORDER BY ae.pos desc LIMIT :maxItems ")
	List<ApuEntityTreeViewDto> listEntitiesBefore(@Param("parentId") long parentId, @Param("pos") int pos, @Param("maxItems") int maxItems);

	@Query("SELECT new cz.aron.api.rest.model.ApuEntityTreeViewDto(cast(ae.uuid as string), ae.name, ae.description, ae.depth, ae.pos, ae.childCnt) FROM ApuEntity ae WHERE ae.source.id=:apuSourceId AND ae.parent IS NULL AND ae.pos<:pos ORDER BY ae.pos desc LIMIT :maxItems")
	List<ApuEntityTreeViewDto> listRootEntitiesBefore(@Param("apuSourceId") long apuSourceId, @Param("pos") int pos, @Param("maxItems") int maxItems);

	@Query("SELECT new cz.aron.api.rest.model.ApuEntityTreeViewDto(cast(ae.uuid as string), ae.name, ae.description, ae.depth, ae.pos, ae.childCnt) FROM ApuEntity ae WHERE ae.parent.id=:parentId ORDER BY ae.pos asc LIMIT :maxItems ")
	List<ApuEntityTreeViewDto> listEntitiesUnder(@Param("parentId") long parentId, @Param("maxItems") int maxItems);

	@Query("SELECT new cz.aron.domain.dto.IdStructuredResultDto(ae.uuid, ae.result) FROM ApuEntity ae WHERE ae.uuid IN (:ids)")
	List<IdStructuredResultDto> findAllResultsByUuidIn(@Param("ids") Collection<UUID> ids);

	@Query("SELECT new cz.aron.domain.dto.IdUuidNameDescriptionOrderDto(ae.id, ae.uuid, ae.name, ae.description, ae.order) FROM ApuEntity ae WHERE ae.uuid IN (:uuids)")
	List<IdUuidNameDescriptionOrderDto> findDtosByUuidIn(@Param("uuids") Collection<UUID> uuids);

	@Query("SELECT ae.uuid FROM ApuEntity ae WHERE ae.permalink=:permalink")
	List<UUID> findUuidsByPermalink(@Param("permalink") String permalink);

	@Query("SELECT COALESCE(MAX(ae.id), 0) FROM ApuEntity ae")
	long findMaxId();

}
