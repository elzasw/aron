package cz.aron.core.integration;

import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoFile;
import cz.aron.core.model.DigitalObject;
import cz.aron.core.model.DigitalObjectStore;
import cz.aron.core.model.DigitalObjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Lukas Jane (inQool) 4.11.2020.
 */
@Service
@Slf4j
public class DaoInputProcessor {
    @Inject private DigitalObjectStore digitalObjectStore;
    @Inject private FileInputProcessor fileInputProcessor;

    public void processDaoAndFiles(String metadata, Map<String, Path> filesMap) {
        Dao dao;
        try (StringReader reader = new StringReader(metadata)) {
            dao = JAXB.unmarshal(reader, Dao.class);
        }
        DigitalObject digitalObject = new DigitalObject();
        digitalObject.setId(dao.getUuid());
        digitalObject.setName(dao.getName());
        digitalObject.setPermalink(dao.getPrmLnk());
        for (DaoBundle daoBundle : dao.getBndl()) {
            DigitalObjectType digitalObjectType = DigitalObjectType.fromXmlType(daoBundle.getType());
            for (DaoFile daoFile : daoBundle.getFile()) {
                fileInputProcessor.processFile(
                        daoFile,
                        digitalObjectType,
                        null,
                        digitalObject,
                        filesMap);
            }
        }
        digitalObjectStore.create(digitalObject);
    }
}
