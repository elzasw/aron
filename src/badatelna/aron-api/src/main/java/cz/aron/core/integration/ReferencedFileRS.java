package cz.aron.core.integration;

import cz.aron.core.model.DigitalObjectFile;
import cz.aron.core.model.DigitalObjectFileStore;
import cz.inqool.eas.common.exception.dto.RestException;

import com.google.common.net.UrlEscapers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import lombok.Setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static cz.inqool.eas.common.exception.ExceptionUtils.checked;
import static java.nio.file.Files.*;

@Tag(name = "ReferencedFiles", description = "Referenced files access API")
@RestController
@RequestMapping("/referencedfiles")
public class ReferencedFileRS {

    @Inject private DigitalObjectFileStore digitalObjectFileStore;

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
    @ApiResponse(responseCode = "404", description = "The file was not found.", content = @Content(schema = @Schema(implementation = RestException.class)))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InputStreamResource> download(@Parameter(description = "ID of file to download", required = true)
                                                        @PathVariable("id") String id) {

        DigitalObjectFile digitalObjectFile = digitalObjectFileStore.find(id);
        if (digitalObjectFile == null || digitalObjectFile.getReferencedFile() ==null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such object.");
        }
        Path path = Paths.get(digitalObjectFile.getReferencedFile());
        InputStream stream = checked(() -> newInputStream(path, StandardOpenOption.READ));
        long size = checked(()-> size(path));

        String fileName = "filename=\"" + path.getFileName().toString() + "\"";
        String fileNameAsterisk = "filename*=UTF-8''" + UrlEscapers.urlFragmentEscaper().escape(path.getFileName().toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; " + fileName + "; " + fileNameAsterisk)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size))
                .contentType(MediaType.parseMediaType(digitalObjectFile.getContentType()))
                .body(new InputStreamResource(stream));
    }


} 
