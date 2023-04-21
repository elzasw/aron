package cz.aron.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.aron.domain.DigitalObjectFile;

@Repository
public interface DaoFileRepository  extends JpaRepository<DigitalObjectFile, Long> {

	DigitalObjectFile findByUuid(String uuid);
	
}
