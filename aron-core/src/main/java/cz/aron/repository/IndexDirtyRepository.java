package cz.aron.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.domain.IndexDirty;

/**
 * The dirty set of the search index (see {@link IndexDirty}). Producers are
 * native {@code INSERT ... SELECT} statements - the uuids come from rows the
 * running transaction can see, never from application memory - and duplicates
 * are allowed, which keeps every producer one portable statement.
 */
@Repository
public interface IndexDirtyRepository extends JpaRepository<IndexDirty, UUID> {

	/**
	 * Marks every APU of the source. On a re-import, called before the old rows go
	 * (their documents are then deleted or rewritten, whichever the new delivery
	 * decides) and again for the newly written rows; on a withdrawal, before the
	 * deletes.
	 */
	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO index_dirty(apu_uuid, requested_at) SELECT uuid, CURRENT_TIMESTAMP FROM apu WHERE apu_source_id = :apuSourceId", nativeQuery = true)
	int markDirtyBySource(@Param("apuSourceId") long apuSourceId);

	/**
	 * Marks every APU that references one of the given targets: its document
	 * carries the target's label, so it has to be rebuilt when that label appears,
	 * changes or disappears.
	 */
	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO index_dirty(apu_uuid, requested_at) SELECT DISTINCT source, CURRENT_TIMESTAMP FROM relation WHERE target IN (:targets)", nativeQuery = true)
	int markDirtyReferrers(@Param("targets") Collection<UUID> targets);

	/** {@link #markDirtyReferrers} for every APU of a source about to be withdrawn. */
	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO index_dirty(apu_uuid, requested_at) SELECT DISTINCT r.source, CURRENT_TIMESTAMP FROM relation r WHERE r.target IN (SELECT a.uuid FROM apu a WHERE a.apu_source_id = :apuSourceId)", nativeQuery = true)
	int markDirtyReferrersOfSource(@Param("apuSourceId") long apuSourceId);

	/** Marks one uuid - also the way a document with no database record behind it is removed. */
	@Transactional
	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO index_dirty(apu_uuid, requested_at) VALUES (:uuid, CURRENT_TIMESTAMP)", nativeQuery = true)
	void markDirty(@Param("uuid") UUID uuid);

	@Query("SELECT DISTINCT d.apuUuid FROM IndexDirty d ORDER BY d.apuUuid LIMIT :numItems")
	List<UUID> findDistinctUuids(@Param("numItems") int numItems);

	@Modifying(flushAutomatically = true)
	@Query("DELETE FROM IndexDirty d WHERE d.apuUuid IN (:uuids)")
	int deleteByUuids(@Param("uuids") Collection<UUID> uuids);

}
