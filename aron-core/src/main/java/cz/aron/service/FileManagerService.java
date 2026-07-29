package cz.aron.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileManagerService {

	@Value("${files.storage}")
	private String rootPath;

	/**
	 * Stores the given stream under a directory structure derived from the file UUID:
	 * {@code <rootPath>/<byte0>/<byte1>/<uuid>}, where {@code byte0}/{@code byte1} are the
	 * first two bytes of the UUID in hex. For example {@code 550e8400-e29b-41d4-a716-446655440000}
	 * is stored as {@code 55/0e/550e8400-e29b-41d4-a716-446655440000}.
	 * <p>
	 * The caller owns and closes {@code is}. An existing file with the same UUID is overwritten,
	 * so re-imports are idempotent.
	 *
	 * @param is   the file content
	 * @param uuid the file UUID; a random one is generated when {@code null}
	 * @return the UUID under which the file was stored
	 */
	public UUID storeFile(InputStream is, UUID uuid) throws IOException {
		var fileUuid = uuid != null ? uuid : UUID.randomUUID();
		Path path = resolve(fileUuid);
		Files.createDirectories(path.getParent());
		Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
		return fileUuid;
	}

	/**
	 * Resolves the absolute path where the file with the given UUID is (or would be) stored.
	 */
	public Path resolve(UUID fileUuid) {
		String id = fileUuid.toString();
		return Paths.get(rootPath, id.substring(0, 2), id.substring(2, 4), id);
	}

}
