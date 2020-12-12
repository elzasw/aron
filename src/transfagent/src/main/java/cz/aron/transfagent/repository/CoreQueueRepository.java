package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.CoreQueue;

public interface CoreQueueRepository extends JpaRepository<CoreQueue, Integer> {

	@EntityGraph(attributePaths = {"apuSource"})
    CoreQueue findFirstByOrderById();

}
