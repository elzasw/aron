package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHelper {

	public static void createDirIfNotExists(Path pathDir) throws IOException {
		if (Files.notExists(pathDir)) {
			Files.createDirectories(pathDir);
		}
	}

	public static List<Path> getOrderedDirectories(Path path) throws IOException {
		try (var stream = Files.list(path)) {
			List<Path> directories = stream.filter(f -> Files.isDirectory(f))
					.collect(Collectors.toCollection(ArrayList::new));
			directories.sort((p1, p2) -> p1.compareTo(p2));
			return directories;
		}
	}

}
