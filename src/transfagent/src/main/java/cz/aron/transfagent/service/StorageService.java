package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StorageService {
	
	private final Path inputFolder;
	
	private final Path dataPath;
	
	private final Path inputPath;
	
	private final AtomicLong counter = new AtomicLong(0);
	
	public StorageService(@Value("${aron.inputFolder}") String inputFolderStr) {
		this.inputFolder = Path.of(inputFolderStr);
		this.dataPath = inputFolder.resolve("data");
		this.inputPath = inputFolder.resolve("input");
	}
	
	public Path getDataPath() {
		return dataPath;
	}
	
	public Path getInputPath() {
		return inputPath;
	}
	
	/**
	 * Move directory to data directory
	 * @param sourceDir dir to move
	 * @return {@link Path} relative to data directory 
	 * @throws IOException
	 */
	public Path moveToDataDir(Path sourceDir) throws IOException {
		return moveToDir(dataPath,sourceDir);
	}
	
	
	private Path moveToDir(Path targetDir, Path movedDir) throws IOException {
		Path parentDir = targetDir.resolve(String.format("%1$tY%1$tm%1$td", new Date()));
		Files.createDirectories(parentDir);
		String dirName = movedDir.getFileName().toString();
		Path dirPath = parentDir.resolve(dirName);
		int count = 0;
		while (Files.exists(dirPath)) {
			dirPath.resolve(dirName + "_" + counter.incrementAndGet());
			count++;
			if (count > 1000) {
				throw new IOException("Fail to create data directory for source " + dirName);
			}
		}
		Files.move(movedDir, dirPath);
		return dataPath.relativize(dirPath);
	}	 
	
	public Path moveToErrorDir(Path sourceDir) throws IOException {
		return moveToDir(inputPath.resolve("error"),sourceDir);
	}
	
	public Path moveToProcessed(Path sourceDir) throws IOException {
		return moveToDir(inputPath.resolve("processed"),sourceDir);
	}
	
	/**
	 * Return absolute path to apu datadir
	 * @param dataDir relative path of directory
	 * @return {@link Path}
	 */
	public Path getApuDataDir(String dataDir) {
		return dataPath.resolve(dataDir);
	}
	
}
