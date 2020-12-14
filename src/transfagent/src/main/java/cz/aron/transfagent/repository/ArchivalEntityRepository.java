package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntityStatus;

@Repository
public interface ArchivalEntityRepository extends JpaRepository<ArchivalEntity, Integer> {

    List<ArchivalEntity> findByUuidInAndStatus(List<UUID> uuids, EntityStatus status);
    
    Optional<ArchivalEntity> findByUuid(UUID uuid);

}
