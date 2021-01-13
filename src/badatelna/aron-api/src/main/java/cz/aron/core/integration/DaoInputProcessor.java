package cz.aron.core.integration;

import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoFile;
import cz.aron.core.model.DigitalObject;
import cz.aron.core.model.DigitalObjectStore;
import cz.aron.core.model.DigitalObjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Lukas Jane (inQool) 4.11.2020.
 */
@Service
@Slf4j
public class DaoInputProcessor {
    @Inject private DigitalObjectStore digitalObjectStore;
    @Inject private FileInputProcessor fileInputProcessor;

    @Value("${tile.folder}")
    private String tileFolder;

    public void processDaoAndFiles(String metadata, Map<String, Path> filesMap) {
        Dao dao;
        try (StringReader reader = new StringReader(metadata)) {
            dao = JAXB.unmarshal(reader, Dao.class);
        }
        DigitalObject digitalObject = digitalObjectStore.find(dao.getUuid());
        if (digitalObject == null) {
            throw new RuntimeException("Digital object base not found.");
        }
        digitalObject.setId(dao.getUuid());
        digitalObject.setName(dao.getName());
        digitalObject.setPermalink(dao.getPrmLnk());
        for (DaoBundle daoBundle : dao.getBndl()) {
            DigitalObjectType digitalObjectType = DigitalObjectType.fromXmlType(daoBundle.getType());
            if (digitalObjectType == DigitalObjectType.TILE) {  //unzip into folder, besides normal processing
                for (DaoFile daoFile : daoBundle.getFile()) {
                    Path uploadedFile = filesMap.get(daoFile.getUuid());
                    unzipTiles(uploadedFile, Path.of(tileFolder, daoFile.getUuid()));
                }
            }
            for (DaoFile daoFile : daoBundle.getFile()) {
                fileInputProcessor.processFile(
                        daoFile,
                        digitalObjectType,
                        null,
                        digitalObject,
                        filesMap);
            }
        }
        digitalObjectStore.update(digitalObject);
    }

    private void unzipTiles(Path uploadedFile, Path outputFolder) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(uploadedFile))) {
            if (Files.exists(outputFolder)) {   //delete it
                for (Path path : Files.walk(outputFolder).sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                    Files.delete(path);
                }
            }
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Files.createDirectories(outputFolder);
                Path targetPath = outputFolder.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(targetPath);
                }
                else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath);
                }
                zipEntry = zis.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
