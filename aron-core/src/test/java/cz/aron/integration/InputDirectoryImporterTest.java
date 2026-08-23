package cz.aron.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.AbstractTest;
import cz.aron.repository.ApuSourceRepository;
import cz.aron.repository.ImportJournalRepository;

/**
 * Pins the input-directory import (a supported data input): the shared test
 * context starts with import.input-dir pointing at test-config/import, so the
 * sample transfer is imported through the real internal mechanism on startup.
 * Journal semantics: unchanged transfers are skipped on rescans, changed ones
 * are re-imported.
 */
class InputDirectoryImporterTest extends AbstractTest {

	/** apusrc uuid of the sample transfer in test-config/import/sample-transfer. */
	private static final UUID SAMPLE_APUSRC_UUID = UUID.fromString("7d3f9a10-2481-4a01-8f33-944455566677");

	@Autowired
	private InputDirectoryImporter importer;

	@Autowired
	private ImportDataProcessingService importDataProcessingService;

	@Autowired
	private ImportJournalRepository importJournalRepository;

	@Autowired
	private ApuSourceRepository apuSourceRepository;

	@Test
	void startupImportedTheSampleTransfer() {
		var journal = importJournalRepository.findByFolder("sample-transfer").orElseThrow();
		assertThat(journal.getContentHash()).isNotBlank();
		assertThat(apuSourceRepository.findByUuid(SAMPLE_APUSRC_UUID)).isNotNull();
	}

	@Test
	void rescanSkipsUnchangedTransfers() {
		var before = importJournalRepository.findByFolder("sample-transfer").orElseThrow();

		importer.scan();

		var after = importJournalRepository.findByFolder("sample-transfer").orElseThrow();
		assertThat(after.getImportedAt()).isEqualTo(before.getImportedAt());
		assertThat(after.getContentHash()).isEqualTo(before.getContentHash());
	}

	/**
	 * The journal half of a re-import: a changed transfer is scanned again and
	 * lands on the source it already created. That the re-imported content also
	 * replaces what the database and the index hold is the import mechanism's own
	 * promise, covered end to end by ApuManagementTest.
	 */
	@Test
	void changedTransferIsReimported(@TempDir Path tempDir) throws IOException {
		var apusrcUuid = UUID.randomUUID();
		var apuUuid = UUID.randomUUID();
		var transfer = tempDir.resolve("temp-transfer");
		ApuxTransfers.write(transfer, apusrcUuid, apuUuid, "První jméno");
		var tempImporter = new InputDirectoryImporter(importDataProcessingService, importJournalRepository,
				tempDir.toString());

		tempImporter.scan();
		var first = importJournalRepository.findByFolder("temp-transfer").orElseThrow();

		ApuxTransfers.write(transfer, apusrcUuid, apuUuid, "Změněné jméno");
		tempImporter.scan();

		var second = importJournalRepository.findByFolder("temp-transfer").orElseThrow();
		assertThat(second.getContentHash()).isNotEqualTo(first.getContentHash());
		// same apusrc uuid = reimport of the same source, not a duplicate
		assertThat(apuSourceRepository.findByUuid(apusrcUuid)).isNotNull();
	}

	@Test
	void transfersAreImportedInLexicographicFolderOrder(@TempDir Path tempDir) throws IOException {
		// deliberately created in reverse order - the scan must sort by name
		ApuxTransfers.write(tempDir.resolve("z2-second"), UUID.randomUUID(), UUID.randomUUID(), "Druhý");
		ApuxTransfers.write(tempDir.resolve("a1-first"), UUID.randomUUID(), UUID.randomUUID(), "První");
		var tempImporter = new InputDirectoryImporter(importDataProcessingService, importJournalRepository,
				tempDir.toString());

		tempImporter.scan();

		// journal ids are identity-generated, so they reflect the import order
		var first = importJournalRepository.findByFolder("a1-first").orElseThrow();
		var second = importJournalRepository.findByFolder("z2-second").orElseThrow();
		assertThat(first.getId()).isLessThan(second.getId());
	}

}
