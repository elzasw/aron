package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.IdProjection;

public interface DaoFileRepository extends JpaRepository<Dao, Integer> {

	List<IdProjection> findTop1000ByStateAndTransferredOrderById(DaoState state, boolean transfered);

	@EntityGraph(attributePaths = { "apuSource" })
	Optional<Dao> findById(int id);

	@EntityGraph(attributePaths = { "apuSource" })
	Optional<Dao> findByUuid(UUID uuid);

	@EntityGraph(attributePaths = { "apuSource" })
	List<Dao> findAllByUuidIn(List<UUID> uuids);

	List<Dao> findByApuSource(ApuSource apuSource);

    List<Dao> findTop1000ByStateOrderById(DaoState state);

}
