package cz.aron.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One imported transfer folder of the input-directory import
 * ({@code import.input-dir}): the stored content hash lets unchanged transfers
 * be skipped on subsequent startups while changed ones are re-imported.
 */
@Entity
@Table(name = "import_journal")
public class ImportJournal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "import_journal_id")
	private Long id;

	@Column(nullable = false, unique = true)
	private String folder;

	@Column(name = "content_hash", nullable = false)
	private String contentHash;

	@Column(name = "imported_at", nullable = false)
	private LocalDateTime importedAt;

	public Long getId() {
		return id;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	public LocalDateTime getImportedAt() {
		return importedAt;
	}

	public void setImportedAt(LocalDateTime importedAt) {
		this.importedAt = importedAt;
	}

}
