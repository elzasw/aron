package cz.aron.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.service.TilesManager;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Deep Zoom tiles of the old API - the URL shape the old UI has always
 * requested ({@code /tile/{id}/image.dzi} and the tile images relative to it).
 * Like {@link ReferencedFileRS} it is hand-annotated, not part of the frozen
 * OpenAPI contract. The id is the TILE file's uuid and is validated as one
 * before any path is built from it (the gen-1 endpoint resolved the raw
 * string). New consumers use {@code /api/v1/daofile/{id}/tiles/**} instead.
 */
@Tag(name = "Tiles", description = "Deep-zoom tile access API")
@RestController
@RequestMapping("/tile")
public class TileRS {

	private final TilesManager tilesManager;

	public TileRS(TilesManager tilesManager) {
		this.tilesManager = tilesManager;
	}

	@GetMapping(value = "/{id}/image.dzi", produces = MediaType.TEXT_XML_VALUE)
	public ResponseEntity<Resource> getDescriptor(@PathVariable("id") String id) {
		Path descriptor = tilesManager.getDescriptor(validatedId(id));
		if (!Files.isRegularFile(descriptor)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No tiles for this file.");
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
				.contentType(MediaType.TEXT_XML)
				.body(new FileSystemResource(descriptor));
	}

	@GetMapping(value = "/{id}/image_files/{level}/{column}_{row}.jpg", produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<Resource> getTileImage(@PathVariable("id") String id, @PathVariable("level") int level,
			@PathVariable("row") int row, @PathVariable("column") int column) {
		if (level < 0 || row < 0 || column < 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such tile.");
		}
		Path image = tilesManager.getTileImage(validatedId(id), level, row, column);
		if (!Files.isRegularFile(image)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such tile.");
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
				.contentType(MediaType.IMAGE_JPEG)
				.body(new FileSystemResource(image));
	}

	/** The id names a directory under the tile store, so only a real uuid may pass. */
	private static String validatedId(String id) {
		try {
			return UUID.fromString(id).toString();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such file.");
		}
	}

}
