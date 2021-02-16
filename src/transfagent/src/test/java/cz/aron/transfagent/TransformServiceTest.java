package cz.aron.transfagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.transfagent.config.ConfigDspace;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.TransformService;

public class TransformServiceTest {

    private final static String DAO_UUID = "61259486-3786-4877-85b5-f845ee038132";

    private final static String DAO_DIR = "src/test/resources/files/dao/" + DAO_UUID;

    private final static String TEST_RESOURCES_DIR = "target/test-resources";

    @Test
    public void testTransformService() throws IOException, JAXBException {
        StorageService storageService = new StorageService(TEST_RESOURCES_DIR, TEST_RESOURCES_DIR + "/daos");
        TransformService service = new TransformService(storageService, new ConfigDspace());
        Path daoInputDir = storageService.getInputPath().resolve("dao").resolve(DAO_UUID);

        FileUtils.copyDirectory(new File(DAO_DIR), daoInputDir.toFile());
        try (Stream<Path> stream = Files.list(daoInputDir)) {
            List<Path> files = stream.filter(f -> Files.isRegularFile(f)).collect(Collectors.toList());
            assertTrue(files.size() == 5);
        }

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

    @AfterAll
    public static void deleteApusrcXml() throws IOException {
        //FileSystemUtils.deleteRecursively(Path.of(TEST_RESOURCES_DIR, "input/dao", DAO_UUID));
    }
}
