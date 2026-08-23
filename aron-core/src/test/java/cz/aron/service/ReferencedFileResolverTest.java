package cz.aron.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.aron.domain.DigitalObjectFile;

/** Pure logic of the referenced-file classification and allowlist - no Spring. */
class ReferencedFileResolverTest {

	@Test
	void locationIsDecidedFromTheEntityAlone() {
		var stored = new DigitalObjectFile();
		stored.setFileId(UUID.randomUUID());
		assertThat(ReferencedFileResolver.locate(stored)).isEqualTo(ReferencedFileResolver.Location.STORED);

		var external = new DigitalObjectFile();
		external.setReferencedFile("https://images.example.org/a/image.dzi");
		assertThat(ReferencedFileResolver.locate(external)).isEqualTo(ReferencedFileResolver.Location.EXTERNAL_URL);

		var local = new DigitalObjectFile();
		local.setReferencedFile("/data/dao/page-1.jpg");
		assertThat(ReferencedFileResolver.locate(local)).isEqualTo(ReferencedFileResolver.Location.LOCAL_PATH);

		var none = new DigitalObjectFile();
		assertThat(ReferencedFileResolver.locate(none)).isEqualTo(ReferencedFileResolver.Location.NONE);
	}

	@Test
	void localPathsResolveOnlyUnderAllowedRoots(@TempDir Path tempDir) throws IOException {
		Path allowed = Files.createDirectories(tempDir.resolve("allowed"));
		Path inside = Files.writeString(allowed.resolve("page.jpg"), "x");
		Path outside = Files.writeString(tempDir.resolve("secret.txt"), "x");
		var resolver = new ReferencedFileResolver(allowed.toString());

		assertThat(resolver.resolveLocal(inside.toString())).isEqualTo(inside.toAbsolutePath().normalize());
		assertThat(resolver.resolveLocal(outside.toString())).isNull();
		// traversal cannot escape a root
		assertThat(resolver.resolveLocal(allowed.resolve("../secret.txt").toString())).isNull();
		// a directory is not a servable file
		assertThat(resolver.resolveLocal(allowed.toString())).isNull();
		// URLs have no local content
		assertThat(resolver.resolveLocal("https://example.org/page.jpg")).isNull();
	}

	@Test
	void noConfiguredRootsMeansNothingResolves(@TempDir Path tempDir) throws IOException {
		Path file = Files.writeString(tempDir.resolve("page.jpg"), "x");
		assertThat(new ReferencedFileResolver("").resolveLocal(file.toString())).isNull();
		assertThat(new ReferencedFileResolver(null).resolveLocal(file.toString())).isNull();
	}

	@Test
	void rootListIsCommaSeparated(@TempDir Path tempDir) throws IOException {
		Path first = Files.createDirectories(tempDir.resolve("first"));
		Path second = Files.createDirectories(tempDir.resolve("second"));
		Path inSecond = Files.writeString(second.resolve("page.jpg"), "x");
		var resolver = new ReferencedFileResolver(first + ", " + second);
		assertThat(resolver.resolveLocal(inSecond.toString())).isEqualTo(inSecond.toAbsolutePath().normalize());
	}

}
