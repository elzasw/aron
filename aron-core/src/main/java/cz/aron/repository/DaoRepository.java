package cz.aron.repository;

import java.util.Collection;
import java.util.List;

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

	DigitalObject findByUuid(String uuid);
	
	List<DigitalObject> findAllByUuidIn(Collection<String> uuids);
}
