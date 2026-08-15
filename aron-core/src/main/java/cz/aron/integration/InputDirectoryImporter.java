package cz.aron.integration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import cz.aron.domain.ImportJournal;
import cz.aron.ft.handling.TransferType;
import cz.aron.repository.ImportJournalRepository;
import cz.aron.search.SearchIndexManager;

/**
 * Supported data input: imports transfer folders from a configured directory at
 * application startup - the file-based alternative to the SOAP file-transfer
 * upload, sharing the same internal mechanism
 * ({@link ImportDataProcessingService}). Enabled by setting
 * {@code import.input-dir}; the property is empty by default (feature off).
 * <p>
 * Each subdirectory of the input directory is one transfer in the standard
 * layout: an {@code apusrc-*.xml} (APUSRC) or {@code dao-*.xml} (DAO) descriptor
 * plus an optional {@code files/} directory with {@code file-<uuid>} payloads.
 * <p>
 * Repeated startups are handled by a database journal keyed by folder name: a
 * transfer whose content hash is unchanged is skipped, a changed one is
 * re-imported (the import pipeline has reimport semantics), a failed one carries
 * no journal entry and is retried on the next startup. The input directory is
 * never modified. Transfers are read at startup only - adding files to a running
 * application requires a restart (or the SOAP transport).
 * <p>
 * Ordering: within one scan, transfers are imported in lexicographic order of
 * the folder name (platform-independent), so a numbering prefix
 * ({@code 01-...}, {@code 02-...}) gives full control over the initial-load
 * order. Across restarts no order is guaranteed (a changed transfer re-imports
 * after previously imported ones) - the import mechanism tolerates any arrival
 * order, exactly like transfers arriving over SOAP.
 */
@ConditionalOnProperty(name = "import.input-dir")
@Component
public class InputDirectoryImporter implements ApplicationListener<ApplicationReadyEvent>, Ordered {

	private static final Logger log = LoggerFactory.getLogger(InputDirectoryImporter.class);

	private final ImportDataProcessingService importDataProcessingService;

	private final ImportJournalRepository importJournalRepository;

	private final String inputDir;

	public InputDirectoryImporter(ImportDataProcessingService importDataProcessingService,
			ImportJournalRepository importJournalRepository, @Value("${import.input-dir}") String inputDir) {
		this.importDataProcessingService = importDataProcessingService;
		this.importJournalRepository = importJournalRepository;
		this.inputDir = inputDir;
	}

	@Override
	public int getOrder() {
		// after SearchIndexManager prepared the search schema
		return SearchIndexManager.STARTUP_ORDER + 10;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		scan();
	}

	/** Scans the input directory once; also invoked directly by tests. */
	public void scan() {
		Path root = Paths.get(inputDir);
		if (!Files.isDirectory(root)) {
			log.warn("Import input directory {} does not exist - nothing to import.", root.toAbsolutePath());
			return;
		}
		int imported = 0;
		int skipped = 0;
		int failed = 0;
		try (var transfers = Files.list(root)) {
			// lexicographic by folder NAME - Path.compareTo would be platform-dependent
			// (case-insensitive on Windows), breaking the documented ordering contract
			for (Path transfer : transfers.filter(Files::isDirectory)
					.sorted(java.util.Comparator.comparing(p -> p.getFileName().toString()))
					.toList()) {
				String folder = transfer.getFileName().toString();
				TransferType type = detectType(transfer);
				if (type == null) {
					log.warn("Import: {} contains no apusrc-*/dao-* descriptor - skipped", folder);
					continue;
				}
				String hash = contentHash(transfer);
				var journal = importJournalRepository.findByFolder(folder).orElse(null);
				if (journal != null && journal.getContentHash().equals(hash)) {
					skipped++;
					continue;
				}
				try {
					importDataProcessingService.processData(transfer, type);
					if (journal == null) {
						journal = new ImportJournal();
						journal.setFolder(folder);
					}
					journal.setContentHash(hash);
					journal.setImportedAt(LocalDateTime.now());
					importJournalRepository.save(journal);
					imported++;
				} catch (Exception e) {
					failed++;
					log.error("Import: failed to import transfer {}", folder, e);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Input directory import completed: {} imported, {} unchanged (skipped), {} failed, from {}",
				imported, skipped, failed, root.toAbsolutePath());
	}

	private static TransferType detectType(Path transfer) throws IOException {
		try (var files = Files.list(transfer)) {
			return files.map(f -> f.getFileName().toString())
					.map(name -> name.startsWith("apusrc-") ? TransferType.APUSRC
							: name.startsWith("dao-") ? TransferType.DAO : null)
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(null);
		}
	}

	/**
	 * Content hash of one transfer folder: the descriptor bytes plus name and size
	 * of every payload file (payload content is not hashed - it can be large; a
	 * replaced payload of identical name and size goes undetected, which is an
	 * accepted trade-off).
	 */
	private static String contentHash(Path transfer) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (var files = Files.list(transfer)) {
				for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
					digest.update(Files.readAllBytes(file));
				}
			}
			Path filesDir = transfer.resolve("files");
			if (Files.isDirectory(filesDir)) {
				try (var files = Files.list(filesDir)) {
					for (Path file : files.sorted().toList()) {
						digest.update(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));
						digest.update(Long.toString(Files.size(file)).getBytes(StandardCharsets.UTF_8));
					}
				}
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new IllegalStateException("Fail to hash transfer folder " + transfer, e);
		}
	}

}
