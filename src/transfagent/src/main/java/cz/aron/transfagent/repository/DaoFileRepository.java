package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.DaoFile;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.IdProjection;

public interface DaoFileRepository extends JpaRepository<DaoFile, Integer> {

	List<IdProjection> findTop1000ByStateAndTransferredOrderById(DaoState state, boolean transfered);
	
	@EntityGraph(attributePaths = { "apuSource" })
	Optional<DaoFile> findById(int id);
	
}