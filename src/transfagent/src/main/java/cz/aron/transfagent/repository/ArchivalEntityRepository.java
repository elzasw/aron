package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.IdProjection;

@Repository
public interface ArchivalEntityRepository extends JpaRepository<ArchivalEntity, Integer> {

    List<ArchivalEntity> findByUuidInAndStatus(List<UUID> uuids, EntityStatus status);
    
    Optional<ArchivalEntity> findByUuid(UUID uuid);
    
    Optional<ArchivalEntity> findByElzaId(Integer elzaId);
    
    List<IdProjection> findTop1000ByStatusOrderById(EntityStatus status);

	List<ArchivalEntity> findAllByParentEntity(ArchivalEntity archivalEntity);

	ArchivalEntity findByApuSource(ApuSource apuSource);

}
