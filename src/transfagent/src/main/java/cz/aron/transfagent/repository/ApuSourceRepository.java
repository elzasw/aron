package cz.aron.transfagent.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.transfagent.domain.ApuSource;

@Repository
public interface ApuSourceRepository extends JpaRepository<ApuSource, Integer> {
    
    Optional<ApuSource> findByUuid(UUID uuid);

	List<ApuSource> findByReimport(boolean reimport);
}
