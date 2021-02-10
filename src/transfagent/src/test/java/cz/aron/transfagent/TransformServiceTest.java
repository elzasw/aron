package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.TransformService;

public class TransformServiceTest {

    private final static String DAO_UUID = "61259486-3786-4877-85b5-f845ee038132";

    private final static String DAO_DIR = "src/test/resources/files/dao/" + DAO_UUID;

    @Test
    public void testTransformService() throws Exception {
        StorageService storageService = new StorageService("target/test-resources", "target/test-resources/daos");
        TransformService service = new TransformService(storageService);
        Path daoInputDir = storageService.getInputPath().resolve("dao").resolve(DAO_UUID);

        FileUtils.copyDirectory(new File(DAO_DIR), daoInputDir.toFile());
        service.transform(daoInputDir);

        Path daoUuidXml = daoInputDir.resolve("dao-" + DAO_UUID + ".xml");

        Dao dao;
        Unmarshaller unmarshaller = ApuxFactory.createUnmarshaller();
        try (InputStream is = Files.newInputStream(daoUuidXml)) {
            dao = ((JAXBElement<Dao>) unmarshaller.unmarshal(is)).getValue();
        }
        assertTrue(dao.getUuid().equals(DAO_UUID));

        int size;
        for (DaoBundle bundle : dao.getBndl()) {
            switch (bundle.getType().value()) {
            case "Published":
                size = 5;
                break;
            case "HighResView":
            case "Thumbnail":
                size = 4;
                break;
            default:
                size = 0;
            }
            assertTrue(bundle.getFile().size() == size);
        }

    }

}