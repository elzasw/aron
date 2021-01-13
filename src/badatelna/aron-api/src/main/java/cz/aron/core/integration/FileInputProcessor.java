package cz.aron.core.integration;

import cz.aron.apux._2020.DaoFile;
import cz.aron.apux._2020.MetadataItem;
import cz.aron.core.model.*;
import cz.inqool.eas.common.storage.file.File;
import cz.inqool.eas.common.storage.file.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lukas Jane (inQool) 4.11.2020.
 */
@Service
@Slf4j
public class FileInputProcessor {
    @Inject private FileManager fileManager;

    public void processFile(DaoFile daoFile, DigitalObjectType digitalObjectType, ApuAttachment apuAttachment, DigitalObject digitalObject, Map<String, Path> filesMap) {
        DigitalObjectFile digitalObjectFile = new DigitalObjectFile();
        digitalObjectFile.setId(daoFile.getUuid());
        digitalObjectFile.setType(digitalObjectType);
        digitalObjectFile.setOrder(daoFile.getPos());
        digitalObjectFile.setPermalink(daoFile.getPrmLnk());
        String name;
        if (apuAttachment != null) {
            digitalObjectFile.setAttachment(apuAttachment);
            apuAttachment.setFile(digitalObjectFile);
            name = apuAttachment.getName();
        }
        else if (digitalObject != null) {
            digitalObjectFile.setDigitalObject(digitalObject);
            digitalObject.getFiles().add(digitalObjectFile);
            name = digitalObject.getName();
        }
        else {
            throw new RuntimeException("missing parent object");
        }
        String mimeType = "application/octet-stream";
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                Metadatum metadatum = new Metadatum();
                metadatum.setType(itm.getCode());
                metadatum.setValue(itm.getValue());
                metadatum.setFile(digitalObjectFile);
                digitalObjectFile.getMetadata().add(metadatum);
                if (metadatum.getType().equals("mimeType")) {
                    mimeType = metadatum.getValue();
                }
            }
        }
        Path uploadedFile = filesMap.get(digitalObjectFile.getId());
        if (name == null) {
            name = UUID.randomUUID().toString();
        }
        try (InputStream is = Files.newInputStream(uploadedFile)) {
            File file = fileManager.store(
                    name,
                    Files.size(uploadedFile),
                    mimeType,
                    is);
            digitalObjectFile.setFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
