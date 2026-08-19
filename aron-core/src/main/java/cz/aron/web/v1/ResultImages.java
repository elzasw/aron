package cz.aron.web.v1;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The deployment's images of the structured search results
 * ({@code webResources.resultImages}): the record and field icons of
 * {@code resultLayout.yaml}, and the thumbnails that arrive in the data as a
 * bare file name rather than a URL.
 * <p>
 * Two directions, one place: {@link #url(String)} builds the URL clients
 * receive, {@link #resolve(String)} turns a served name back into a file for
 * {@code /api/v1/ui/result-images/{name}}.
 * <p>
 * URLs are built here rather than by clients so nothing absolute is baked into
 * a build (the one-artifact rule): the name is prefixed with the request's own
 * context path, which already folds {@code X-Forwarded-Prefix}
 * ({@code forward-headers-strategy: framework}), so the same jar serves the URL
 * root and any subpath.
 */
@Component
public class ResultImages {

	private static final Logger log = LoggerFactory.getLogger(ResultImages.class);

	/**
	 * A served name is a file name, never a path. Everything else - path
	 * segments, traversal, absolute paths - is rejected before any file is
	 * touched; this is the whole risk surface of serving deployment files.
	 */
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]{1,128}");

	private static final String PATH = "/api/v1/ui/result-images/";

	private final HttpServletRequest request;

	/** The configured directory, absolute and normalized; {@code null} = unconfigured. */
	private final Path directory;

	public ResultImages(HttpServletRequest request,
			@Value("${webResources.resultImages:}") String directory) {
		this.request = request;
		this.directory = directory != null && !directory.isBlank()
				? Paths.get(directory).toAbsolutePath().normalize()
				: null;
	}

	public boolean isConfigured() {
		return directory != null;
	}

	/** URL of one deployment image, prefixed with this deployment's own context path. */
	public String url(String name) {
		return request.getContextPath() + PATH + UriUtils.encodePathSegment(name, StandardCharsets.UTF_8);
	}

	/**
	 * A thumbnail as delivered by the source system: an absolute URL passes
	 * through unchanged, a bare file name becomes {@link #url(String)}.
	 * <p>
	 * Returns {@code null} for a name that cannot be served - no image directory
	 * is configured, or the name is not a plain file name. Dropping the thumbnail
	 * is better than emitting a URL that answers 404.
	 */
	public String thumbnailUrl(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("//")) {
			return value;
		}
		if (directory == null) {
			log.debug("Thumbnail '{}' is a deployment image name but webResources.resultImages is not configured.",
					value);
			return null;
		}
		if (!NAME.matcher(value).matches()) {
			log.debug("Thumbnail '{}' is neither an absolute URL nor a plain image file name - ignored.", value);
			return null;
		}
		return url(value);
	}

	/**
	 * The file of a served name, or {@code null} when the name is not a plain
	 * file name, escapes the configured directory, or is not a readable file.
	 */
	public Path resolve(String name) {
		if (directory == null || name == null || !NAME.matcher(name).matches()) {
			return null;
		}
		Path file = directory.resolve(name).normalize();
		if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
			return null;
		}
		return file;
	}

}
