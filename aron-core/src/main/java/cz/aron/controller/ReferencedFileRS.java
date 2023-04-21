package cz.aron.controller;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.size;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.net.UrlEscapers;

import cz.aron.domain.DigitalObjectFile;
import cz.aron.repository.DaoFileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "ReferencedFiles", description = "Referenced files access API")
@RestController
@RequestMapping("/referencedfiles")
public class ReferencedFileRS {
    
    private final DaoFileRepository daoFileRepository;

    public ReferencedFileRS(DaoFileRepository daoFileRepository) {
		super();
		this.daoFileRepository = daoFileRepository;
	}

	/**
     * Get the content of a file with given ID.
     * <p>
     * Also the {@code Content-Length} and {@code Content-Disposition} HTTP headers are set.
     *
     * @param id ID of file to retrieve
     * @return content of a file in an input stream
     * @throws MissingObject if the file was not found
     */
    @Operation(summary = "Get the content of a upload with given ID.", description = "Returns content of an upload in input stream.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))
    @ApiResponse(responseCode = "404", description = "The file was not found.", content = @Content(schema = @Schema(implementation = Exception.class)))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InputStreamResource> download(@Parameter(description = "ID of file to download", required = true)
                                                        @PathVariable("id") String id) {

        DigitalObjectFile digitalObjectFile = daoFileRepository.findByUuid(id);        
        if (digitalObjectFile == null || digitalObjectFile.getReferencedFile() ==null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such object.");
        }
        Path path = Paths.get(digitalObjectFile.getReferencedFile());
        
        
        try {
        InputStream stream = newInputStream(path, StandardOpenOption.READ);
        long size = size(path);

        String fileName = "filename=\"" + path.getFileName().toString() + "\"";
        String fileNameAsterisk = "filename*=UTF-8''" + UrlEscapers.urlFragmentEscaper().escape(path.getFileName().toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; " + fileName + "; " + fileNameAsterisk)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size))
                .contentType(MediaType.parseMediaType(digitalObjectFile.getContentType()))
                .body(new InputStreamResource(stream));
        } catch (IOException ioEx) {
        	throw new UncheckedIOException(ioEx);
        }
    }

} 
