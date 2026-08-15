package cz.aron.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

	@Test
	void changedTransferIsReimported(@TempDir Path tempDir) throws IOException {
		var transfer = Files.createDirectories(tempDir.resolve("temp-transfer"));
		var apusrcUuid = UUID.randomUUID();
		var apuUuid = UUID.randomUUID();
		writeApusrc(transfer, apusrcUuid, apuUuid, "První jméno");
		var tempImporter = new InputDirectoryImporter(importDataProcessingService, importJournalRepository,
				tempDir.toString());

		tempImporter.scan();
		var first = importJournalRepository.findByFolder("temp-transfer").orElseThrow();

		writeApusrc(transfer, apusrcUuid, apuUuid, "Změněné jméno");
		tempImporter.scan();

		var second = importJournalRepository.findByFolder("temp-transfer").orElseThrow();
		assertThat(second.getContentHash()).isNotEqualTo(first.getContentHash());
		// same apusrc uuid = reimport of the same source, not a duplicate
		assertThat(apuSourceRepository.findByUuid(apusrcUuid)).isNotNull();
	}

	@Test
	void transfersAreImportedInLexicographicFolderOrder(@TempDir Path tempDir) throws IOException {
		// deliberately created in reverse order - the scan must sort by name
		writeApusrc(Files.createDirectories(tempDir.resolve("z2-second")), UUID.randomUUID(), UUID.randomUUID(),
				"Druhý");
		writeApusrc(Files.createDirectories(tempDir.resolve("a1-first")), UUID.randomUUID(), UUID.randomUUID(),
				"První");
		var tempImporter = new InputDirectoryImporter(importDataProcessingService, importJournalRepository,
				tempDir.toString());

		tempImporter.scan();

		// journal ids are identity-generated, so they reflect the import order
		var first = importJournalRepository.findByFolder("a1-first").orElseThrow();
		var second = importJournalRepository.findByFolder("z2-second").orElseThrow();
		assertThat(first.getId()).isLessThan(second.getId());
	}

	private static void writeApusrc(Path transfer, UUID apusrcUuid, UUID apuUuid, String name) throws IOException {
		var xml = """
				<?xml version="1.0"?>
				<apusrc xmlns="http://www.aron.cz/apux/2020" uuid="%s">
				 <apus>
				  <apu type="Institution" uuid="%s">
				   <name>%s</name>
				  </apu>
				 </apus>
				</apusrc>
				""".formatted(apusrcUuid, apuUuid, name);
		Files.writeString(transfer.resolve("apusrc-temp.xml"), xml, StandardCharsets.UTF_8);
	}

}
