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
class ResultImagesTest {

	@TempDir
	Path directory;

	private ResultImages images;

	@BeforeEach
	void configure() throws IOException {
		Files.writeString(directory.resolve("record.svg"), "<svg/>");
		Files.writeString(directory.resolve("types.yaml"), "not an image");
		Files.createDirectory(directory.resolve("sub"));
		Files.writeString(directory.resolve("sub").resolve("nested.svg"), "<svg/>");
		Files.writeString(directory.getParent().resolve("outside.svg"), "<svg/>");
		var request = new MockHttpServletRequest();
		request.setContextPath("/aron");
		images = new ResultImages(request, directory.toString());
	}

	@Test
	void urlsCarryTheDeploymentsOwnPrefix() {
		assertThat(images.url("record.svg")).isEqualTo("/aron/api/v1/ui/result-images/record.svg");
	}

	@Test
	void thumbnailIsPassedThroughAsUrlOrResolvedAsAnImageName() {
		assertThat(images.thumbnailUrl("https://images.example.org/x.jp2"))
				.isEqualTo("https://images.example.org/x.jp2");
		assertThat(images.thumbnailUrl("//images.example.org/x.jp2")).isEqualTo("//images.example.org/x.jp2");
		assertThat(images.thumbnailUrl("record.svg")).isEqualTo("/aron/api/v1/ui/result-images/record.svg");

		// a name that could not be served is dropped rather than emitted as a URL
		// that answers 404
		assertThat(images.thumbnailUrl("sub/nested.svg")).isNull();
		assertThat(images.thumbnailUrl(null)).isNull();
		assertThat(images.thumbnailUrl(" ")).isNull();
		var unconfigured = new ResultImages(new MockHttpServletRequest(), "");
		assertThat(unconfigured.isConfigured()).isFalse();
		assertThat(unconfigured.thumbnailUrl("record.svg")).isNull();
		assertThat(unconfigured.thumbnailUrl("https://images.example.org/x.jp2"))
				.isEqualTo("https://images.example.org/x.jp2");
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
