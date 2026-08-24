package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.AbstractTest;
import cz.aron.ft.handling.TransferType;
import cz.aron.integration.ApuxTransfers;
import cz.aron.integration.ImportDataProcessingService;
import cz.aron.test.api.v1.ApuApi;
import cz.aron.test.api.v1.model.FileInfo;
import cz.aron.test.api.v1.model.FileType;

/**
 * The serving half of digital objects: one DAO imported through the real
 * transfer mechanism, then read back over /api/v1 - the detail's server-built
 * URLs, the content/DZI/tile endpoints, and the boundaries (conditional and
 * range requests, the referenced-dirs allowlist, external content never
 * proxied, tile-name validation).
 */
class DaoServingTest extends AbstractTest {

	// fixture ids unique to this class (test data is additive, no resets)
	private static final String APU_SOURCE = "9da0f000-0000-4000-8000-000000000001";
	private static final String APU = "9da0f000-0000-4000-8000-000000000002";
	private static final String DAO = "9da0f000-0000-4000-8000-000000000003";
	private static final String PUBLISHED_STORED = "9da0f000-0000-4000-8000-000000000011";
	private static final String THUMBNAIL_STORED = "9da0f000-0000-4000-8000-000000000012";
	private static final String TILE_STORED = "9da0f000-0000-4000-8000-000000000013";
	private static final String PUBLISHED_REFERENCED = "9da0f000-0000-4000-8000-000000000021";
	private static final String PUBLISHED_OUTSIDE = "9da0f000-0000-4000-8000-000000000022";
	private static final String TILE_EXTERNAL = "9da0f000-0000-4000-8000-000000000023";
	private static final String ATTACHMENT_FILE = "9da0f000-0000-4000-8000-000000000031";

	private static final String EXTERNAL_DZI = "https://images.example.org/xyz/image.dzi";
	// stored with the leading slash: /redirectimage's tail keeps it (see RedirectApi.tail)
	private static final String FILE_PERMALINK = "/dao-serving-test/page-1";

	@Autowired
	private ImportDataProcessingService importService;

	private Path referencedFile;

	private Path outsideFile;

	/** Idempotent: the same uuids re-import as an update, so each test sees one fixture. */
	@BeforeEach
	void importDaoFixture(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
		// the allowlisted root of application-test.yml, and a file outside it
		Path referencedRoot = Files.createDirectories(Paths.get("./target/test-data/referencedFiles"));
		referencedFile = referencedRoot.resolve("dao-serving-page2.png").toAbsolutePath().normalize();
		Files.write(referencedFile, png());
		Path outsideRoot = Files.createDirectories(Paths.get("./target/test-data/outsideReferenced"));
		outsideFile = outsideRoot.resolve("dao-serving-secret.txt").toAbsolutePath().normalize();
		Files.writeString(outsideFile, "not served");

		ApuxTransfers.writeWithDao(tempDir.resolve("apusrc"), UUID.fromString(APU_SOURCE), UUID.fromString(APU),
				"Instituce s digitalizáty", UUID.fromString(DAO), "Priloha.pdf", UUID.fromString(ATTACHMENT_FILE),
				"pdf-bytes".getBytes(StandardCharsets.UTF_8), "application/pdf");
		importService.processData(tempDir.resolve("apusrc"), TransferType.APUSRC);

		new ApuxTransfers.DaoTransfer(UUID.fromString(DAO), "Kronika obce", "CC-BY-4.0")
				.addFile("Published", UUID.fromString(PUBLISHED_STORED), 1, png(), "image/png", "page-1.png", true)
				.withPermalink(FILE_PERMALINK)
				.addReference("Published", UUID.fromString(PUBLISHED_REFERENCED), 2,
						referencedFile.toString(), "image/png", "page-2.png", false)
				.addReference("Published", UUID.fromString(PUBLISHED_OUTSIDE), 3,
						outsideFile.toString(), "text/plain", "secret.txt", false)
				.addFile("Thumbnail", UUID.fromString(THUMBNAIL_STORED), 1, png(), "image/png", "thumb-1.png", false)
				.addFile("HighResView", UUID.fromString(TILE_STORED), 1, tileZip(), "application/zip", null, false)
				.addReference("HighResView", UUID.fromString(TILE_EXTERNAL), 2, EXTERNAL_DZI, null, null, false)
				.write(tempDir.resolve("dao"));
		importService.processData(tempDir.resolve("dao"), TransferType.DAO);
	}

	@Test
	void detailCarriesServerBuiltFileUrls() {
		var detail = new ApuApi(v1ApiClient()).apuGetDetail(APU, null, null, null);

		assertThat(detail.getDigitalObjects()).hasSize(1);
		var dao = detail.getDigitalObjects().get(0);
		assertThat(dao.getUuid()).isEqualTo(DAO);
		assertThat(dao.getName()).isEqualTo("Kronika obce");
		assertThat(dao.getLicense()).isEqualTo("CC-BY-4.0");
		assertThat(dao.getFiles()).hasSize(6);

		// a stored file: portal content URL, no dzi
		var stored = file(dao.getFiles(), PUBLISHED_STORED);
		assertThat(stored.getFileType()).isEqualTo(FileType.PUBLISHED);
		assertThat(stored.getPosition()).isEqualTo(1);
		assertThat(stored.getSelected()).isTrue();
		assertThat(stored.getUrl()).isEqualTo("/api/v1/daofile/" + PUBLISHED_STORED);
		assertThat(stored.getDziUrl()).isNull();

		// a referenced file under the allowed root: served by the portal too
		var referenced = file(dao.getFiles(), PUBLISHED_REFERENCED);
		assertThat(referenced.getPosition()).isEqualTo(2);
		assertThat(referenced.getName()).isEqualTo("page-2.png");
		assertThat(referenced.getUrl()).isEqualTo("/api/v1/daofile/" + PUBLISHED_REFERENCED);

		// a referenced file outside the allowed roots gets no URL at all
		var outside = file(dao.getFiles(), PUBLISHED_OUTSIDE);
		assertThat(outside.getUrl()).isNull();

		// a transferred tile pyramid: reached through the portal's dzi URL, never a content URL
		var tile = file(dao.getFiles(), TILE_STORED);
		assertThat(tile.getFileType()).isEqualTo(FileType.TILE);
		assertThat(tile.getUrl()).isNull();
		assertThat(tile.getDziUrl()).isEqualTo("/api/v1/daofile/" + TILE_STORED + "/tiles/image.dzi");

		// an external image server's tiles: its dzi URL travels verbatim
		var externalTile = file(dao.getFiles(), TILE_EXTERNAL);
		assertThat(externalTile.getDziUrl()).isEqualTo(EXTERNAL_DZI);
		assertThat(externalTile.getUrl()).isNull();

		// the attachment's file downloads through the same endpoint
		assertThat(detail.getAttachments()).hasSize(1);
		assertThat(detail.getAttachments().get(0).getFile().getUrl())
				.isEqualTo("/api/v1/daofile/" + ATTACHMENT_FILE);
	}

	@Test
	void storedContentIsServedInlineOrAsDownload() throws Exception {
		var response = getBytes("/api/v1/daofile/" + PUBLISHED_STORED);
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo(png());
		assertThat(contentType(response)).startsWith("image/png");
		assertThat(response.headers().firstValue("Content-Disposition").orElse("")).startsWith("inline");

		var download = getBytes("/api/v1/daofile/" + PUBLISHED_STORED + "?download=true");
		assertThat(download.headers().firstValue("Content-Disposition").orElse("")).startsWith("attachment");
	}

	@Test
	void conditionalRequestAnswers304() throws Exception {
		var first = getBytes("/api/v1/daofile/" + PUBLISHED_STORED);
		String eTag = first.headers().firstValue("ETag").orElseThrow();
		var second = getBytes("/api/v1/daofile/" + PUBLISHED_STORED, "If-None-Match", eTag);
		assertThat(second.statusCode()).isEqualTo(304);
		assertThat(second.body()).isEmpty();
	}

	@Test
	void rangeRequestAnswers206() throws Exception {
		var response = getBytes("/api/v1/daofile/" + PUBLISHED_STORED, "Range", "bytes=0-3");
		assertThat(response.statusCode()).isEqualTo(206);
		assertThat(response.body()).hasSize(4);
		assertThat(response.headers().firstValue("Content-Range").orElse("")).startsWith("bytes 0-3/");
	}

	@Test
	void referencedContentIsServedOnlyFromAllowedDirectories() throws Exception {
		var allowed = getBytes("/api/v1/daofile/" + PUBLISHED_REFERENCED);
		assertThat(allowed.statusCode()).isEqualTo(200);
		assertThat(allowed.body()).isEqualTo(png());

		// the file exists and the DB names it, but it is outside files.referenced-dirs
		assertThat(Files.exists(outsideFile)).isTrue();
		var outside = getBytes("/api/v1/daofile/" + PUBLISHED_OUTSIDE);
		assertThat(outside.statusCode()).isEqualTo(404);
	}

	@Test
	void externalContentIsNeverProxied() throws Exception {
		var response = getBytes("/api/v1/daofile/" + TILE_EXTERNAL);
		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void tilesAreServedAndTileNamesValidated() throws Exception {
		var descriptor = getBytes("/api/v1/daofile/" + TILE_STORED + "/tiles/image.dzi");
		assertThat(descriptor.statusCode()).isEqualTo(200);
		assertThat(contentType(descriptor)).startsWith("application/xml");
		assertThat(new String(descriptor.body(), StandardCharsets.UTF_8)).contains("TileSize");

		var tile = getBytes("/api/v1/daofile/" + TILE_STORED + "/tiles/image_files/0/0_0.jpg");
		assertThat(tile.statusCode()).isEqualTo(200);
		assertThat(contentType(tile)).startsWith("image/jpeg");

		// only {col}_{row}.{tile.format} is a tile name; nothing else touches the filesystem
		assertThat(getBytes("/api/v1/daofile/" + TILE_STORED + "/tiles/image_files/0/0_0.png").statusCode())
				.isEqualTo(404);
		assertThat(getBytes("/api/v1/daofile/" + TILE_STORED + "/tiles/image_files/0/%2E%2E_0.jpg").statusCode())
				.isEqualTo(404);
		// a non-tile file has no pyramid
		assertThat(getBytes("/api/v1/daofile/" + PUBLISHED_STORED + "/tiles/image.dzi").statusCode())
				.isEqualTo(404);
	}

	@Test
	void detailCarriesTheResolvedAttribution() {
		// the daoFooter configuration resolves per record and per language; the
		// client renders anchors and never matches license codes itself
		var czech = new ApuApi(v1ApiClient()).apuGetDetail(APU, "cs", null, null)
				.getDigitalObjects().get(0).getFooter();
		assertThat(czech.getDedication()).extracting(f -> f.getText())
				.containsExactly("Digitalizace probehla s podporou ", "NAKI II", ".");
		// the fixture's CC-BY-4.0 matches its own entry, image URL server-built
		assertThat(czech.getLicense()).singleElement().satisfies(run -> {
			assertThat(run.getText()).isEqualTo("CC BY 4.0");
			assertThat(run.getUrl()).isEqualTo("https://creativecommons.org/licenses/by/4.0/");
		});
		assertThat(czech.getLicenseImage()).isEqualTo("/api/v1/ui/images/record.svg");

		var english = new ApuApi(v1ApiClient()).apuGetDetail(APU, "en", null, null)
				.getDigitalObjects().get(0).getFooter();
		assertThat(english.getDedication()).extracting(f -> f.getText())
				.containsExactly("Digitized with the support of ", "NAKI II", ".");
	}

	@Test
	void oldApiServesTheSamePyramid() throws Exception {
		// the old UI's viewer URL shape (frozen): /api/aron/tile/{fileUuid}/...
		var descriptor = getBytes("/api/aron/tile/" + TILE_STORED + "/image.dzi");
		assertThat(descriptor.statusCode()).isEqualTo(200);
		assertThat(contentType(descriptor)).startsWith("text/xml");
		assertThat(new String(descriptor.body(), StandardCharsets.UTF_8)).contains("TileSize");

		var tile = getBytes("/api/aron/tile/" + TILE_STORED + "/image_files/0/0_0.jpg");
		assertThat(tile.statusCode()).isEqualTo(200);
		assertThat(contentType(tile)).startsWith("image/jpeg");

		// the id names a directory under the tile store, so only a real uuid may pass
		assertThat(getBytes("/api/aron/tile/not-a-uuid/image.dzi").statusCode()).isEqualTo(404);
		assertThat(getBytes("/api/aron/tile/" + TILE_STORED + "/image_files/-1/0_0.jpg").statusCode())
				.isEqualTo(404);
	}

	@Test
	void redirectImageLeadsToTheViewerUrl() throws Exception {
		var response = get("/api/aron/redirectimage" + FILE_PERMALINK);
		assertThat(response.statusCode()).isEqualTo(302);
		assertThat(response.headers().firstValue("Location").orElse(""))
				.isEqualTo("/apu/" + APU + "/dao/" + DAO + "?file=" + PUBLISHED_STORED);
	}

	@Test
	void reimportRemovingAFileMakesIt404(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
		// a re-delivery without the referenced page: the file disappears from the DAO
		new ApuxTransfers.DaoTransfer(UUID.fromString(DAO), "Kronika obce", "CC-BY-4.0")
				.addFile("Published", UUID.fromString(PUBLISHED_STORED), 1, png(), "image/png", "page-1.png", true)
				.write(tempDir.resolve("dao-less"));
		importService.processData(tempDir.resolve("dao-less"), TransferType.DAO);

		var detail = new ApuApi(v1ApiClient()).apuGetDetail(APU, null, null, null);
		assertThat(detail.getDigitalObjects().get(0).getFiles()).hasSize(1);
		assertThat(getBytes("/api/v1/daofile/" + PUBLISHED_REFERENCED).statusCode()).isEqualTo(404);
	}

	private static FileInfo file(java.util.List<FileInfo> files, String id) {
		return files.stream().filter(file -> id.equals(file.getId())).findFirst().orElseThrow();
	}

	/** A real 1x1 PNG, generated rather than committed. */
	private static byte[] png() {
		try {
			var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			var out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}

	/** A minimal Deep Zoom pyramid as Transfagent ships it: image.dzi + one jpg tile. */
	private static byte[] tileZip() {
		try {
			var jpg = new ByteArrayOutputStream();
			ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", jpg);
			var out = new ByteArrayOutputStream();
			try (var zip = new ZipOutputStream(out)) {
				zip.putNextEntry(new ZipEntry("image.dzi"));
				zip.write("""
						<?xml version="1.0" encoding="UTF-8"?>
						<Image xmlns="http://schemas.microsoft.com/deepzoom/2008" Format="jpg" Overlap="1" TileSize="254">
						 <Size Height="1" Width="1"/>
						</Image>
						""".getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
				zip.putNextEntry(new ZipEntry("image_files/0/0_0.jpg"));
				zip.write(jpg.toByteArray());
				zip.closeEntry();
			}
			return out.toByteArray();
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}

}
