package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.DaoFiles;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.IdProjection;

public interface DaoFileRepository extends JpaRepository<DaoFiles, Integer> {

	List<IdProjection> findTop1000ByStateAndTransferredOrderById(DaoState state, boolean transfered);

	@EntityGraph(attributePaths = { "apuSource" })
	Optional<DaoFiles> findById(int id);

	@EntityGraph(attributePaths = { "apuSource" })
	Optional<DaoFiles> findByUuid(UUID uuid);

	@EntityGraph(attributePaths = { "apuSource" })
	List<DaoFiles> findAllByUuidIn(List<UUID> uuids);

	List<DaoFiles> findByApuSource(ApuSource apuSource);

    List<DaoFiles> findTop1000ByStateOrderById(DaoState state);

}
