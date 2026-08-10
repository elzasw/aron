package cz.aron.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.domain.Relation;

@Repository
public interface RelationRepository  extends JpaRepository<Relation, Long>  {
	
	@Query("SELECT r.id FROM Relation r WHERE r.target IN (:target)")
	List<Long> findIdsByTarget(@Param("target") Collection<UUID> target);

	/**
	 * Set reference to ApuEntity to NULL for all relations related to given ApuSource.
	 * @param apuSourceId id of ApuSource
	 * @return num disconnected records
	 */
	@Modifying
	@Query("UPDATE Relation r SET r.remove=true WHERE r.apuSource.id=:apuSourceId")
	long markToRemoveByApuSourceId(@Param("apuSourceId") long apuSourceId);
		
	List<Relation> findAllByApuSourceIdAndSourceIn(@Param("apuSourceId") long apuSourceId, @Param("sources") Collection<UUID> sources);
	
	/**
	 * Deletes the relations of the given ApuSource that were marked to be removed by
	 * {@link #markToRemoveByApuSourceId(long)} and were not re-created by the running import.
	 *
	 * @return num deleted records
	 */
	@Modifying(flushAutomatically = true)
	@Query("DELETE FROM Relation r WHERE r.apuSource.id=:apuSourceId AND r.remove=true")
	long deleteAllByApuSourceIdAndRemoveTrue(@Param("apuSourceId") long apuSourceId);

	@Modifying
	@Query("DELETE FROM Relation r WHERE r.apuSource.id=:apuSourceId")
	void deleteAllByApuSourceId(@Param("apuSourceId") long apuSourceId);

	@Query("SELECT r.id FROM Relation r WHERE r.id>:after AND r.remove=false ORDER BY r.id asc LIMIT :numItems")
	List<Long> findIds(@Param("after") long after, @Param("numItems") long numItems);

	@Query("SELECT COALESCE(MAX(r.id), 0) FROM Relation r")
	long findMaxId();

}
