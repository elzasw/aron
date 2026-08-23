package cz.aron.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cz.aron.domain.DigitalObjectFile;

/**
 * Where a digital-object file's content actually lives. APUX delivers files
 * either as transferred binaries (stored by {@link FileManagerService}) or as
 * references, whose {@code path} may be a local filesystem path or an external
 * URL - one string, two very different things. This is the single place that
 * tells them apart; clients never see the raw value, only the URL the server
 * built from it.
 * <p>
 * Local referenced paths are served only under the directories a deployment
 * allows ({@code files.referenced-dirs}, one directory or a comma-separated
 * list). The value is data delivered by a source system, so serving it
 * unchecked would let an import expose any file the server can read; unset
 * means local referenced files are not served at all.
 */
@Component
public class ReferencedFileResolver {

	private static final Logger log = LoggerFactory.getLogger(ReferencedFileResolver.class);

	/** What a file's content is, decided from the entity alone. */
	public enum Location {
		/** Transferred binary in the portal's own storage. */
		STORED,
		/** Externally hosted - the reference is a URL, the portal never touches the bytes. */
		EXTERNAL_URL,
		/** Referenced local file - servable only under the allowed directories. */
		LOCAL_PATH,
		/** No content at all (a placeholder, or a tile pyramid reached through its descriptor). */
		NONE,
	}

	/** The allowed roots, absolute and normalized; empty = local references unserved. */
	private final List<Path> allowedRoots;

	public ReferencedFileResolver(@Value("${files.referenced-dirs:}") String allowedRoots) {
		this.allowedRoots = Stream.of((allowedRoots == null ? "" : allowedRoots).split(","))
				.map(String::trim)
				.filter(root -> !root.isEmpty())
				.map(root -> Paths.get(root).toAbsolutePath().normalize())
				.toList();
	}

	public static Location locate(DigitalObjectFile file) {
		if (file.getFileId() != null) {
			return Location.STORED;
		}
		String reference = file.getReferencedFile();
		if (reference == null || reference.isBlank()) {
			return Location.NONE;
		}
		if (isUrl(reference)) {
			return Location.EXTERNAL_URL;
		}
		return Location.LOCAL_PATH;
	}

	public static boolean isUrl(String reference) {
		return reference.startsWith("http://") || reference.startsWith("https://");
	}

	/**
	 * The local file a reference may be served from: an existing regular file
	 * under one of the allowed roots, {@code null} otherwise. The reference is
	 * source-system data, so everything else - no roots configured, escaping
	 * traversal, a path outside every root, a missing file - is rejected here,
	 * before any file is touched.
	 */
	public Path resolveLocal(String reference) {
		if (reference == null || reference.isBlank() || isUrl(reference)) {
			return null;
		}
		Path file;
		try {
			file = Paths.get(reference).toAbsolutePath().normalize();
		} catch (java.nio.file.InvalidPathException e) {
			log.debug("Referenced file '{}' is not a valid path.", reference);
			return null;
		}
		for (Path root : allowedRoots) {
			if (file.startsWith(root) && Files.isRegularFile(file)) {
				return file;
			}
		}
		log.debug("Referenced file '{}' is outside the allowed directories (files.referenced-dirs) or missing.",
				reference);
		return null;
	}

	/** Whether {@link #resolveLocal(String)} could ever succeed - drives whether URLs are emitted. */
	public boolean isServable(String reference) {
		return resolveLocal(reference) != null;
	}

}
