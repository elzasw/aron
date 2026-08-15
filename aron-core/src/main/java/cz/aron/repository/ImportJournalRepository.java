package cz.aron.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.domain.ImportJournal;

public interface ImportJournalRepository extends JpaRepository<ImportJournal, Long> {

	Optional<ImportJournal> findByFolder(String folder);

}
