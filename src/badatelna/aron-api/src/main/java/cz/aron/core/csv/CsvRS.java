package cz.aron.core.csv;

import cz.aron.core.model.DigitalObjectFile;
import cz.aron.core.model.DigitalObjectFileStore;
import cz.aron.core.model.DigitalObjectType;
import cz.aron.core.model.Metadatum;
import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.MetadataType;
import liquibase.util.csv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 10.12.2020.
 */
@RestController
@RequestMapping("/digitalObjectFile")
@Slf4j
public class CsvRS {
    @Inject private DigitalObjectFileStore digitalObjectFileStore;
    @Inject private TypesHolder typesHolder;

    @GetMapping(value = "/{id}/metadata/csv")
    public HttpEntity<byte[]> downloadCsv(@PathVariable String id) {
        DigitalObjectFile digitalObjectFile = digitalObjectFileStore.find(id);
        if (digitalObjectFile == null || digitalObjectFile.getType() != DigitalObjectType.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such object.");
        }
        byte[] data = createCsv(digitalObjectFile.getMetadata());
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.valueOf("text/csv"));
        header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=metadata.csv");
        header.setContentLength(data.length);
        return new HttpEntity<>(data, header);
    }

    private byte[] createCsv(List<Metadatum> metadata) {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            for (Metadatum metadatum : metadata) {
                MetadataType metadataType = typesHolder.getMetadataTypeForCode(metadatum.getType());
                String label;
                if (metadataType != null) {
                    label = metadataType.getName();
                }
                else {
                    label = metadatum.getType();
                }
                csvWriter.writeNext(new String[]{label, metadatum.getValue()});
            }
            return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

