package cz.aron.web.v1;

import org.springframework.stereotype.Component;

import cz.aron.domain.DigitalObjectFile;
import cz.aron.domain.DigitalObjectType;
import cz.aron.service.ReferencedFileResolver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The file and tile URLs a detail response carries. Built server-side (the
 * {@link DeploymentImages} rule): the server knows whether a file is stored,
 * referenced locally, or hosted by an external image server, and the client
 * must not - it calls whatever URL it was handed. Portal URLs are prefixed
 * with the request's own context path, so the same jar serves the URL root and
 * any subpath.
 */
@Component
public class DaoFileUrls {

	private static final String PATH = "/api/v1/daofile/";

	private final HttpServletRequest request;

	private final ReferencedFileResolver referencedFiles;

	public DaoFileUrls(HttpServletRequest request, ReferencedFileResolver referencedFiles) {
		this.request = request;
		this.referencedFiles = referencedFiles;
	}

	/**
	 * Content URL of a file, or {@code null} when it has none: a tile pyramid
	 * is reached through {@link #dziUrl(DigitalObjectFile)}, and a local
	 * referenced file outside the allowed directories gets no URL rather than
	 * one that answers 404.
	 */
	public String contentUrl(DigitalObjectFile file) {
		if (file.getType() == DigitalObjectType.TILE) {
			return null;
		}
		return switch (ReferencedFileResolver.locate(file)) {
			case STORED -> portalUrl(file);
			case EXTERNAL_URL -> file.getReferencedFile();
			case LOCAL_PATH -> referencedFiles.isServable(file.getReferencedFile()) ? portalUrl(file) : null;
			case NONE -> null;
		};
	}

	/**
	 * Deep Zoom descriptor URL of a TILE file - the external image server's
	 * when the file references one, the portal's own otherwise. Tile images
	 * resolve relative to it, which is how deep-zoom viewers fetch them.
	 */
	public String dziUrl(DigitalObjectFile file) {
		if (file.getType() != DigitalObjectType.TILE) {
			return null;
		}
		if (ReferencedFileResolver.locate(file) == ReferencedFileResolver.Location.EXTERNAL_URL) {
			return file.getReferencedFile();
		}
		return portalUrl(file) + "/tiles/image.dzi";
	}

	private String portalUrl(DigitalObjectFile file) {
		return request.getContextPath() + PATH + file.getUuid();
	}

}
