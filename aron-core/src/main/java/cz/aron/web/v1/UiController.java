package cz.aron.web.v1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.api.v1.UiApi;
import cz.aron.api.v1.model.UiConfig;

/**
 * Implements the /api/v1/ui endpoints: typed UI configuration
 * ({@link UiConfigLoader}) and the deployment-supplied logo
 * ({@code webResources.logo}; format by file extension). Unlike the old API's
 * /pageTemplate, clients never receive raw server configuration files.
 */
@RestController
public class UiController implements UiApi {

	private final UiConfigLoader uiConfigLoader;

	private final PresentationLocales presentationLocales;

	private final String logoFile;

	private byte[] logoData;

	public UiController(UiConfigLoader uiConfigLoader, PresentationLocales presentationLocales,
			@Value("${webResources.logo}") String logoFile) {
		this.uiConfigLoader = uiConfigLoader;
		this.presentationLocales = presentationLocales;
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

	private MediaType logoMediaType() {
		String file = logoFile.toLowerCase(Locale.ROOT);
		if (file.endsWith(".svg")) {
			return MediaType.parseMediaType("image/svg+xml");
		}
		if (file.endsWith(".jpg") || file.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG;
		}
		if (file.endsWith(".png")) {
			return MediaType.IMAGE_PNG;
		}
		throw new IllegalStateException("Unsupported logo format (svg/png/jpeg): " + logoFile);
	}

}
