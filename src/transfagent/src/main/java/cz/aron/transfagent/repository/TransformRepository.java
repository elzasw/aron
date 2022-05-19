package cz.aron.transfagent.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.Transform;
import cz.aron.transfagent.domain.TransformState;

public interface TransformRepository extends JpaRepository<Transform, Integer> {
	
	List<Transform> findTop1000ByStateOrderById(TransformState state);
	
	List<Transform> findAllByDaoUuid(UUID daoUuid);

}
