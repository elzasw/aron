package cz.aron.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.aron.domain.DigitalObject;

@Repository
public interface DaoRepository  extends JpaRepository<DigitalObject, Long> {

	@Query(value="UPDATE DigitalObject do SET do.apu=NULL WHERE do IN (SELECT do FROM DigitalObject do INNER JOIN ApuEntity ae ON do.apu=ae WHERE ae.source.id=?1)")
	@Modifying
	long disconnectDaosByApuSourceId(long id);

	DigitalObject findByUuid(UUID uuid);

	List<DigitalObject> findAllByUuidIn(Collection<UUID> uuids);

	@Query("SELECT COALESCE(MAX(dobj.id), 0) FROM DigitalObject dobj")
	long findMaxId();
}
