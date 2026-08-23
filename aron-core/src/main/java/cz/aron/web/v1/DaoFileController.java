package cz.aron.web.v1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.net.UrlEscapers;

import cz.aron.api.v1.DaoApi;
import cz.aron.commons.HttpUtils;
import cz.aron.domain.DigitalObjectFile;
import cz.aron.domain.DigitalObjectType;
import cz.aron.repository.DaoFileRepository;
import cz.aron.service.FileManagerService;
import cz.aron.service.ReferencedFileResolver;
import cz.aron.service.TilesManager;

/**
 * Serves digital-object file content and Deep Zoom tiles for the new API. The
 * key is the file uuid ({@code FileInfo.id}) - the one identifier every file
 * has, stored or referenced. Bodies are {@link FileSystemResource}s with no
 * manual {@code Content-Length}, so Spring answers {@code Range} requests
 * (206) by itself.
 */
@RestController
public class DaoFileController implements DaoApi {

	/**
	 * A tile name is {@code {column}_{row}.{format}} and nothing else - the raw
	 * segment is never resolved against the filesystem.
	 */
	private static final Pattern TILE_NAME = Pattern.compile("(\\d{1,6})_(\\d{1,6})\\.([A-Za-z0-9]{1,8})");

	private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic();

	private final DaoFileRepository daoFileRepository;

	private final FileManagerService fileManager;

	private final ReferencedFileResolver referencedFiles;

	private final TilesManager tilesManager;

	private final String tileFormat;

	public DaoFileController(DaoFileRepository daoFileRepository, FileManagerService fileManager,
			ReferencedFileResolver referencedFiles, TilesManager tilesManager,
			@Value("${tile.format}") String tileFormat) {
		this.daoFileRepository = daoFileRepository;
		this.fileManager = fileManager;
		this.referencedFiles = referencedFiles;
		this.tilesManager = tilesManager;
		this.tileFormat = tileFormat;
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> daoFileGetContent(String id, Boolean download, String ifNoneMatch,
			String ifModifiedSince) {
		DigitalObjectFile file = findFile(id);
		Path path = switch (ReferencedFileResolver.locate(file)) {
			case STORED -> fileManager.resolve(file.getFileId());
			case LOCAL_PATH -> referencedFiles.resolveLocal(file.getReferencedFile());
			// externally hosted content is never proxied; a tile pyramid has no single content
			case EXTERNAL_URL, NONE -> null;
		};
		if (path == null || !Files.isRegularFile(path)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such file.");
		}

		LocalDateTime published = publishedOf(file);
		if (published != null) {
			var expireStatus = HttpUtils.computeExpired(published, ifNoneMatch, ifModifiedSince);
			if (!expireStatus.expired()) {
				return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
						.cacheControl(CACHE_CONTROL)
						.eTag(expireStatus.eTag())
						.lastModified(expireStatus.lastModified())
						.build();
			}
			return content(file, path, Boolean.TRUE.equals(download))
					.eTag(expireStatus.eTag())
					.lastModified(expireStatus.lastModified())
					.body(new FileSystemResource(path));
		}
		return content(file, path, Boolean.TRUE.equals(download)).body(new FileSystemResource(path));
	}

	private static ResponseEntity.BodyBuilder content(DigitalObjectFile file, Path path, boolean download) {
		MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
		if (file.getContentType() != null) {
			try {
				contentType = MediaType.parseMediaType(file.getContentType());
			} catch (org.springframework.http.InvalidMediaTypeException ignored) {
				// source-system data; an unparsable type falls back to octet-stream
			}
		}
		String name = file.getName() != null ? file.getName() : path.getFileName().toString();
		String disposition = (download ? "attachment" : "inline")
				+ "; filename=\"" + name.replace("\"", "") + "\""
				+ "; filename*=UTF-8''" + UrlEscapers.urlFragmentEscaper().escape(name);
		return ResponseEntity.ok()
				.cacheControl(CACHE_CONTROL)
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
				.contentType(contentType);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> daoFileGetDziDescriptor(String id) {
		Path descriptor = tilesManager.getDescriptor(tileFile(id).getUuid().toString());
		if (!Files.isRegularFile(descriptor)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No tiles for this file.");
		}
		return ResponseEntity.ok()
				.cacheControl(CACHE_CONTROL)
				.contentType(MediaType.APPLICATION_XML)
				.body(new FileSystemResource(descriptor));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> daoFileGetTileImage(String id, Integer level, String tile) {
		var matcher = TILE_NAME.matcher(tile);
		if (!matcher.matches() || !matcher.group(3).equals(tileFormat) || level < 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such tile.");
		}
		int column = Integer.parseInt(matcher.group(1));
		int row = Integer.parseInt(matcher.group(2));
		Path image = tilesManager.getTileImage(tileFile(id).getUuid().toString(), level, row, column);
		if (!Files.isRegularFile(image)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such tile.");
		}
		MediaType contentType = "png".equalsIgnoreCase(tileFormat) ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok()
				.cacheControl(CACHE_CONTROL)
				.contentType(contentType)
				.body(new FileSystemResource(image));
	}

	private DigitalObjectFile tileFile(String id) {
		DigitalObjectFile file = findFile(id);
		if (file.getType() != DigitalObjectType.TILE) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not a tile file.");
		}
		return file;
	}

	private DigitalObjectFile findFile(String id) {
		UUID uuid;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such file.");
		}
		DigitalObjectFile file = daoFileRepository.findByUuid(uuid);
		if (file == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such file.");
		}
		return file;
	}

	/** The moment the file's content was last published, when the entity model knows it. */
	private static LocalDateTime publishedOf(DigitalObjectFile file) {
		if (file.getDigitalObject() != null) {
			return file.getDigitalObject().getPublished();
		}
		if (file.getAttachment() != null && file.getAttachment().getApu() != null
				&& file.getAttachment().getApu().getSource() != null) {
			return file.getAttachment().getApu().getSource().getPublished();
		}
		return null;
	}

}
