package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ArchivalEntity;

@Repository
public interface ArchivalEntityRepository extends JpaRepository<ArchivalEntity, Integer> {

}
