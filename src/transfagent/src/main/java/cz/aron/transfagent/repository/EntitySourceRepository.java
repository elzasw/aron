package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.EntitySource;

@Repository
public interface EntitySourceRepository extends JpaRepository<EntitySource,Integer> {

}
