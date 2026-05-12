package cz.aron.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.domain.Relation;

@Repository
public interface RelationRepository  extends JpaRepository<Relation, Long>  {
	
	@Query("SELECT r.id FROM Relation r WHERE r.target IN (:target)")
	List<Long> findIdsByTarget(@Param("target") Collection<String> target);

	/**
	 * Set reference to ApuEntity to NULL for all relations related to given ApuSource.
	 * @param apuSourceId id of ApuSource
	 * @return num disconnected records
	 */
	@Modifying
	@Query("UPDATE Relation r SET r.remove=true WHERE r.apuSource.id=:apuSourceId")
	long markToRemoveByApuSourceId(@Param("apuSourceId") long apuSourceId);
		
	List<Relation> findAllByApuSourceIdAndSourceIn(@Param("apuSourceId") long apuSourceId, @Param("sources") Collection<String> sources);
	
	@Query("SELECT r.id FROM Relation r WHERE r.apuSource.id=:apuSourceId AND r.remove=true")
	List<Long> findAllIdByApuSourceIdAndRemoveTrue(@Param("apuSourceId") long apuSourceId);

	@Modifying
	@Query("DELETE FROM Relation r WHERE r.apuSource.id=:apuSourceId")
	void deleteAllByApuSourceId(@Param("apuSourceId") long apuSourceId);

	@Query("SELECT r.id FROM Relation r WHERE r.id>:after AND r.remove=false ORDER BY r.id asc LIMIT :numItems")
	List<Long> findIds(@Param("after") long after, @Param("numItems") long numItems);

}
