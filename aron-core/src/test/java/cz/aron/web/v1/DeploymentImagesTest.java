package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The deployment's result images: URL building (which must carry the deployment's
 * own prefix, so one artifact serves the URL root and any subpath) and the name
 * validation that is the whole risk surface of serving files from a configured
 * directory.
 */
class DeploymentImagesTest {

	@TempDir
	Path directory;

	/** A second configured directory: its own temp dir, not one made beside the first. */
	@TempDir
	Path ownDirectory;

	private DeploymentImages images;

	@BeforeEach
	void configure() throws IOException {
		Files.writeString(directory.resolve("record.svg"), "<svg/>");
		Files.writeString(directory.resolve("types.yaml"), "not an image");
		Files.createDirectory(directory.resolve("sub"));
		Files.writeString(directory.resolve("sub").resolve("nested.svg"), "<svg/>");
		Files.writeString(directory.getParent().resolve("outside.svg"), "<svg/>");
		var request = new MockHttpServletRequest();
		request.setContextPath("/aron");
		images = new DeploymentImages(request, directory.toString());
	}

	@Test
	void urlsCarryTheDeploymentsOwnPrefix() {
		assertThat(images.url("record.svg")).isEqualTo("/aron/api/v1/ui/images/record.svg");
	}

	@Test
	void thumbnailIsPassedThroughAsUrlOrResolvedAsAnImageName() {
		assertThat(images.thumbnailUrl("https://images.example.org/x.jp2"))
				.isEqualTo("https://images.example.org/x.jp2");
		assertThat(images.thumbnailUrl("//images.example.org/x.jp2")).isEqualTo("//images.example.org/x.jp2");
		assertThat(images.thumbnailUrl("record.svg")).isEqualTo("/aron/api/v1/ui/images/record.svg");

		// a name that could not be served is dropped rather than emitted as a URL
		// that answers 404
		assertThat(images.thumbnailUrl("sub/nested.svg")).isNull();
		assertThat(images.thumbnailUrl(null)).isNull();
		assertThat(images.thumbnailUrl(" ")).isNull();
		var unconfigured = new DeploymentImages(new MockHttpServletRequest(), "");
		assertThat(unconfigured.isConfigured()).isFalse();
		assertThat(unconfigured.thumbnailUrl("record.svg")).isNull();
		assertThat(unconfigured.thumbnailUrl("https://images.example.org/x.jp2"))
				.isEqualTo("https://images.example.org/x.jp2");
	}

	@Test
	void resolvesAcrossSeveralDirectoriesInOrder() throws IOException {
		// a deployment's own pictures need not live with the icons of a shared
		// display model - dev mode keeps the shipped result icons and adds its own
		Files.writeString(ownDirectory.resolve("tile.svg"), "<svg/>");
		// same name in both: the earlier directory wins
		Files.writeString(ownDirectory.resolve("record.svg"), "<svg id='own'/>");
		var both = new DeploymentImages(new MockHttpServletRequest(), directory + " , " + ownDirectory);

		assertThat(both.isConfigured()).isTrue();
		assertThat(both.resolve("tile.svg")).isEqualTo(ownDirectory.resolve("tile.svg"));
		assertThat(both.resolve("record.svg")).isEqualTo(directory.resolve("record.svg"));
		assertThat(both.resolve("neexistuje.svg")).isNull();
		// the traversal guard holds for every one of them
		assertThat(both.resolve("../outside.svg")).isNull();
	}

	@Test
	void resolvesOnlyPlainFileNamesInsideTheConfiguredDirectory() {
		assertThat(images.resolve("record.svg")).isEqualTo(directory.resolve("record.svg"));
		// a non-image file inside the directory still resolves; the media type
		// decides whether it can be served (UiController)
		assertThat(images.resolve("types.yaml")).isEqualTo(directory.resolve("types.yaml"));

		assertThat(images.resolve("neexistuje.svg")).isNull();
		assertThat(images.resolve("sub")).isNull();
		assertThat(images.resolve("sub/nested.svg")).isNull();
		assertThat(images.resolve("sub\\nested.svg")).isNull();
		assertThat(images.resolve("../outside.svg")).isNull();
		assertThat(images.resolve("..")).isNull();
		assertThat(images.resolve(directory.resolve("record.svg").toString())).isNull();
		assertThat(images.resolve("")).isNull();
		assertThat(images.resolve(null)).isNull();
	}

}
