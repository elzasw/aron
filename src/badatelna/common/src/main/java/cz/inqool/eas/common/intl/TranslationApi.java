package cz.inqool.eas.common.intl;

import cz.inqool.eas.common.dictionary.DictionaryApi;
import cz.inqool.eas.common.exception.MissingObject;
import cz.inqool.eas.common.exception.dto.RestException;
import cz.inqool.eas.common.storage.file.File;
import cz.inqool.eas.common.storage.file.OpenedFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.InputStream;

@Tag(name = "Translations", description = "Translations CRUD API")
@ResponseBody
@RequestMapping("${intl.translation.url}")
public class TranslationApi extends DictionaryApi<
        Translation,
        TranslationDetail,
        TranslationList,
        TranslationCreate,
        TranslationUpdate,
        TranslationService> {

    @Operation(summary = "Get the content of a translation", description = "Returns content of an upload in input stream.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))
    @ApiResponse(responseCode = "404", description = "The file was not found.", content = @Content(schema = @Schema(implementation = RestException.class)))
    @GetMapping(value = "/load/{lang}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InputStreamResource> load(@PathVariable("lang") Language lang) {
        try {
            OpenedFile openedFile = service.load(lang);

            if (openedFile != null) {
                File file = openedFile.getDescriptor();
                InputStream stream = openedFile.getStream();

                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                        .header("Content-Length", String.valueOf(file.getSize()))
                        .contentType(MediaType.parseMediaType(file.getContentType()))
                        .body(new InputStreamResource(stream));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MissingObject ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
