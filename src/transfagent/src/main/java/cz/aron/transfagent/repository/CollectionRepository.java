package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.Collection;

public interface CollectionRepository extends JpaRepository<Collection, Integer> {

}
