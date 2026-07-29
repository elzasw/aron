package cz.aron.controller;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.size;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.net.UrlEscapers;

import cz.aron.api.rest.FileApi;
import cz.aron.repository.DaoFileRepository;
import cz.aron.service.FileManagerService;

@RestController
public class DownloadFile implements FileApi {

	private final DaoFileRepository daoFileRepository;
	
	private final FileManagerService fileManager;

	public DownloadFile(DaoFileRepository daoFileRepository, FileManagerService fileManager) {
		this.daoFileRepository = daoFileRepository;
		this.fileManager = fileManager;
	}

	@Override
	public ResponseEntity<Resource> downloadFile(UUID id) {
		var digitalObjectFile = daoFileRepository.findByFileId(id);
		if (digitalObjectFile == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such object.");
		}
		
		var path = fileManager.resolve(digitalObjectFile.getFileId());
		try {
			long size = size(path);

			String fileName = "filename=\"" + path.getFileName().toString() + "\"";
			String fileNameAsterisk = "filename*=UTF-8''"
					+ UrlEscapers.urlFragmentEscaper().escape(path.getFileName().toString());

			MediaType contentType = digitalObjectFile.getContentType() != null
					? MediaType.parseMediaType(digitalObjectFile.getContentType())
					: MediaType.APPLICATION_OCTET_STREAM;

			// open the stream last, so nothing that can throw leaves it dangling
			InputStream stream = newInputStream(path, StandardOpenOption.READ);

			return ResponseEntity.ok()
					.header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; " + fileName + "; " + fileNameAsterisk)
					.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size))
					.contentType(contentType)
					.body(new InputStreamResource(stream));
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}
	}

}
