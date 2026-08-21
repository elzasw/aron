package cz.aron.web.v1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.api.v1.UiApi;
import cz.aron.api.v1.model.UiConfig;

/**
 * Implements the /api/v1/ui endpoints: typed UI configuration
 * ({@link UiConfigLoader}), the deployment-supplied logo
 * ({@code webResources.logo}) and the images of the structured search results
 * ({@link DeploymentImages}); image formats follow the file extension. Unlike the
 * old API's /pageTemplate, clients never receive raw server configuration files.
 */
@RestController
public class UiController implements UiApi {

	private final UiConfigLoader uiConfigLoader;

	private final PresentationLocales presentationLocales;

	private final DeploymentImages deploymentImages;

	private final String logoFile;

	private byte[] logoData;

	public UiController(UiConfigLoader uiConfigLoader, PresentationLocales presentationLocales,
			DeploymentImages deploymentImages, @Value("${webResources.logo}") String logoFile) {
		this.uiConfigLoader = uiConfigLoader;
		this.presentationLocales = presentationLocales;
		this.deploymentImages = deploymentImages;
		this.logoFile = logoFile;
	}

	@Override
	public ResponseEntity<UiConfig> uiGetConfig(String lang) {
		return ResponseEntity.ok(uiConfigLoader.getConfig(presentationLocales.resolve(lang)));
	}

	@Override
	public ResponseEntity<Resource> uiGetLogo() {
		if (logoData == null) {
			try {
				logoData = Files.readAllBytes(Paths.get(logoFile));
			} catch (IOException e) {
				throw new UncheckedIOException("Fail to read logo " + logoFile, e);
			}
		}
		return ResponseEntity.ok()
				.contentType(logoMediaType())
				.body(new ByteArrayResource(logoData));
	}

	/**
	 * One image of the structured search results. The name is a file name within
	 * the configured directory; anything else - an unknown name, a path, an
	 * unsupported format, no directory configured - is 404, because the file set
	 * is deployment data rather than part of the application.
	 */
	@Override
	public ResponseEntity<Resource> uiGetImage(String name) {
		Path file = deploymentImages.resolve(name);
		MediaType mediaType = file != null ? mediaTypeOf(name) : null;
		if (mediaType == null) {
			return ResponseEntity.notFound().build();
		}
		try {
			return ResponseEntity.ok()
					.contentType(mediaType)
					// deployment branding: stable for the lifetime of a deployment's config
					.cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
					.body(new ByteArrayResource(Files.readAllBytes(file)));
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read result image " + file, e);
		}
	}

	private MediaType logoMediaType() {
		MediaType mediaType = mediaTypeOf(logoFile);
		if (mediaType == null) {
			throw new IllegalStateException("Unsupported logo format (svg/png/jpeg): " + logoFile);
		}
		return mediaType;
	}

	/** Image media type by file extension; {@code null} for anything else. */
	private static MediaType mediaTypeOf(String fileName) {
		String file = fileName.toLowerCase(Locale.ROOT);
		if (file.endsWith(".svg")) {
			return MediaType.parseMediaType("image/svg+xml");
		}
		if (file.endsWith(".jpg") || file.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG;
		}
		if (file.endsWith(".png")) {
			return MediaType.IMAGE_PNG;
		}
		return null;
	}

}
