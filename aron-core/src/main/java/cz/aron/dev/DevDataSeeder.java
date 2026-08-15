package cz.aron.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import cz.aron.ft.handling.TransferType;
import cz.aron.integration.ImportDataProcessingService;
import cz.aron.search.SearchIndexManager;

/**
 * Dev-mode seed (application-dev.yml): imports the sample transfers from
 * dev-data/import through ARON's internal import mechanism
 * ({@link ImportDataProcessingService} - identical to a real standalone
 * deployment). The seeder substitutes ONLY the transport: in production the
 * transfer folders are materialized by the SOAP file-transfer upload
 * (Transfagent), here they live in the source tree.
 * <p>
 * Each subdirectory of the seed path is one transfer in the standard layout:
 * an {@code apusrc-*.xml} (APUSRC) or {@code dao-*.xml} (DAO) descriptor plus an
 * optional {@code files/} directory with {@code file-<uuid>} payloads.
 */
@Profile("dev")
@Component
public class DevDataSeeder implements ApplicationListener<ApplicationReadyEvent>, Ordered {

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

	private final ImportDataProcessingService importDataProcessingService;

	private final String seedPath;

	public DevDataSeeder(ImportDataProcessingService importDataProcessingService,
			@Value("${dev.seed-path}") String seedPath) {
		this.importDataProcessingService = importDataProcessingService;
		this.seedPath = seedPath;
	}

	@Override
	public int getOrder() {
		// after SearchIndexManager prepared the search schema
		return SearchIndexManager.STARTUP_ORDER + 10;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		Path root = Paths.get(seedPath);
		if (!Files.isDirectory(root)) {
			log.warn("Dev seed directory {} not found - starting empty (dev mode expects the aron-core working directory).",
					root.toAbsolutePath());
			return;
		}
		int imported = 0;
		int failed = 0;
		try (var transfers = Files.list(root)) {
			for (Path transfer : transfers.filter(Files::isDirectory).sorted().toList()) {
				TransferType type = detectType(transfer);
				if (type == null) {
					log.warn("Dev seed: {} contains no apusrc-*/dao-* descriptor - skipped", transfer.getFileName());
					continue;
				}
				try {
					importDataProcessingService.processData(transfer, type);
					imported++;
				} catch (Exception e) {
					failed++;
					log.error("Dev seed: failed to import transfer {}", transfer.getFileName(), e);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log.info("Dev seed completed: {} transfers imported, {} failed, from {}", imported, failed,
				root.toAbsolutePath());
	}

	private static TransferType detectType(Path transfer) throws IOException {
		try (var files = Files.list(transfer)) {
			return files.map(f -> f.getFileName().toString())
					.map(name -> name.startsWith("apusrc-") ? TransferType.APUSRC
							: name.startsWith("dao-") ? TransferType.DAO : null)
					.filter(java.util.Objects::nonNull)
					.findFirst()
					.orElse(null);
		}
	}

}
