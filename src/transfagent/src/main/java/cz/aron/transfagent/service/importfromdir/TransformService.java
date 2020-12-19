package cz.aron.transfagent.service.importfromdir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.Marshaller;

import org.springframework.stereotype.Service;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.apux._2020.DaoFile;
import cz.aron.apux._2020.Metadata;
import cz.aron.apux._2020.MetadataItem;
import cz.aron.transfagent.service.StorageService;

@Service
public class TransformService {

    private final StorageService storageService;

    public TransformService(StorageService storageService) {
        this.storageService = storageService;
    }

    public void transform(Path dir) throws Exception {

        // TODO otestovat jestli jsou v adresari jenom soubory

        var filesDir = dir.resolve("files");
        Files.createDirectories(filesDir);

        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(f -> Files.isRegularFile(f)).collect(Collectors.toList());
        }

        var daoUuid = dir.getFileName().toString();

        var dao = new Dao();
        dao.setUuid(daoUuid);

        var published = new DaoBundle();
        published.setType(DaoBundleType.PUBLISHED);
        dao.getBndl().add(published);

        var pos = 1;

        for (Path file : files) {

            var uuid = UUID.randomUUID().toString();
            var daoFile = new DaoFile();
            daoFile.setPos(pos);
            daoFile.setUuid(uuid);

            var metadata = new Metadata();
            var metadataItem = new MetadataItem();
            metadataItem.setCode("mimetype");
            metadataItem.setValue("image/jpeg");
            metadata.getItms().add(metadataItem);

            daoFile.setMtdt(metadata);
            daoFile.setUuid(uuid);

            Files.move(file, filesDir.resolve("file-" + uuid));

            published.getFile().add(daoFile);
            pos++;
        }

        Marshaller marshaller = ApuxFactory.createMarshaller();
        try (OutputStream os = Files.newOutputStream(dir.resolve("dao-" + daoUuid + ".xml"))) {
            marshaller.marshal(ApuxFactory.getObjFactory().createDao(dao), os);
        }

    }

}
