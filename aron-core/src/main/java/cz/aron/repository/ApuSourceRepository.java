package cz.aron.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.aron.domain.ApuSource;


@Repository
public interface ApuSourceRepository extends JpaRepository<ApuSource, Long> {

	ApuSource findByUuid(UUID uuid);

	@Query("SELECT COALESCE(MAX(aps.id), 0) FROM ApuSource aps")
	long findMaxId();

}
