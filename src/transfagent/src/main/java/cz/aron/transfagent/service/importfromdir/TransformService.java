package cz.aron.transfagent.service.importfromdir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.json.Json;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.tika.Tika;
import org.jsoup.helper.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux.DaoBuilder;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.transfagent.config.ConfigDspace;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DSpaceConsts;
import gov.nist.isg.archiver.DirectoryArchiver;
import gov.nist.isg.archiver.FilesArchiver;
import gov.nist.isg.pyramidio.BufferedImageReader;
import gov.nist.isg.pyramidio.PartialImageReader;
import gov.nist.isg.pyramidio.ScalablePyramidBuilder;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class TransformService {

    private static final Logger log = LoggerFactory.getLogger(TransformService.class);

    private final StorageService storageService;

    private final ConfigDspace configDspace;

    public TransformService(StorageService storageService, ConfigDspace configDspace) {
        this.storageService = storageService;
        this.configDspace = configDspace;
    }

    public boolean transform(Path dir) throws JAXBException, IOException {

        Tika tika = new Tika();

        var daoUuid = dir.getFileName().toString();
        var daoUuidXmlFile = dir.resolve("dao-" + daoUuid + ".xml");
        var filesDir = dir.resolve("files");

        // mazání předchozích souborů
        FileSystemUtils.deleteRecursively(filesDir);
        Files.deleteIfExists(daoUuidXmlFile);

        List<Path> files = prepareFileList(dir);
        if (!files.isEmpty()) {
            Files.createDirectories(filesDir);

            DaoBuilder daoBuilder = new DaoBuilder();
            daoBuilder.setUuid(daoUuid);

            DaoBundle published = daoBuilder.createDaoBundle(DaoBundleType.PUBLISHED);
            DaoBundle hiResView = null;
            DaoBundle thumbnail = null;

            // originalni soubory k presunu, klic je novy nazev 
            var filesToMove = new HashMap<String, Path>();

            var pos = 1;
            for (Path file : files) {
                String mimeType = tika.detect(file);
                processPublished(file, published, pos, filesToMove, mimeType);
                if (mimeType!=null&&mimeType.startsWith("image/")) {
                    log.info("Generating dzi and thumbnail for {}", file);
                    if (hiResView == null) {
                        hiResView = daoBuilder.createDaoBundle(DaoBundleType.HIGH_RES_VIEW);
                        thumbnail = daoBuilder.createDaoBundle(DaoBundleType.THUMBNAIL);
                    }
                    processHiResView(file, filesDir, hiResView, pos);
                    processThumbNail(file, filesDir, thumbnail, pos);
                }
                pos++;
            }

            // vytvoreni dao-uuid.xml
            Marshaller marshaller = ApuxFactory.createMarshaller();
            try (OutputStream os = Files.newOutputStream(daoUuidXmlFile)) {
                marshaller.marshal(daoBuilder.build(), os);
            }
    
            // move original files
            for (var entry : filesToMove.entrySet()) {
                Files.move(entry.getValue(), filesDir.resolve(entry.getKey()));
            }
            return true;
        }

        Files.delete(dir);
        return false;
    }

    private List<Path> prepareFileList(Path dir) throws IOException {
        List<Path> files;
        var bitstreamJson = dir.resolve(DSpaceConsts.BITSTREAM_JSON);
        if (Files.exists(bitstreamJson)) {
            files = readBitstreamJson(dir, bitstreamJson);
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(f -> Files.isRegularFile(f) && !files.contains(f)).map(f -> f.toFile()).forEach(File::delete);
            }
        } else {
            try (Stream<Path> stream = Files.list(dir)) {
                files = stream.filter(f -> Files.isRegularFile(f)).collect(Collectors.toList());
            }
            files.sort((p1, p2)->p1.getFileName().compareTo(p2.getFileName()));
        }
        return files;
    }

    private List<Path> readBitstreamJson(Path dir, Path bitstreamJson) throws IOException {
        var filterBundle = configDspace.getBundleName();
        Map<Integer, Path> filesMap = new HashMap<>();
        var jsonReader = Json.createReader(Files.newInputStream(bitstreamJson));
        var jsonValue = jsonReader.readValue();
        for (var value : jsonValue.asJsonArray()) {
            var object = value.asJsonObject();

            if (filterBundle == null || filterBundle.equals(object.getString(DSpaceConsts.BUNDLE_NAME))) {
                var name = object.getString(DSpaceConsts.NAME);
                var id = object.getInt(DSpaceConsts.SEQUENCE_ID);

                Validate.notNull(name, "Název souboru nesmí být prázdný");
                Validate.notNull(id, "SequenceId nesmí být prázdný");

                filesMap.put(id, dir.resolve(name));
            }
        }
        return filesMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private void processThumbNail(Path file, Path filesDir, DaoBundle thumbnails, int pos) throws IOException {
        var daoFile = DaoBuilder.createDaoFile(pos, "image/jpeg");
        var uuid = daoFile.getUuid();

        thumbnails.getFile().add(daoFile);
        try (OutputStream os = Files.newOutputStream(filesDir.resolve("file-" + uuid))) {
            Thumbnails.of(file.toFile())
                    .outputFormat("jpg")
                    .size(120, 120)
                    .toOutputStream(os);
        }
    }

    private void processHiResView(Path file, Path filesDir, DaoBundle hiResView, int pos) throws IOException {
        var daoFile = DaoBuilder.createDaoFile(pos, "application/octetstream");
        var uuid = daoFile.getUuid();

        hiResView.getFile().add(daoFile);
        createDzi(file, filesDir.resolve("file-" + uuid));
    }

    private void processPublished(Path file, DaoBundle published, int pos, Map<String, Path> filesToMove, String mimeType) throws IOException {
        var fileName = file.getFileName().toString();
        var fileSize = Files.size(file);
        var daoFile = DaoBuilder.createDaoFile(fileName, fileSize, pos, mimeType);
        var uuid = daoFile.getUuid();

        filesToMove.put("file-" + uuid, file);
        published.getFile().add(daoFile);
    }

    private void createDzi(Path sourceImage, Path targetFile) throws IOException {
        log.info("Creating file {}", targetFile);
        Path tempDir = storageService.createTempDir("dzi_" + sourceImage.getFileName().toString() + "_");
        boolean deleteCreated = true;
        try {
            ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
            FilesArchiver archiver = new DirectoryArchiver(tempDir.toFile());
            PartialImageReader pir = new BufferedImageReader(sourceImage.toFile());
            spb.buildPyramid(pir, "image", archiver, 1);
            try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(targetFile))) {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        try {
                            Path targetFile = tempDir.relativize(file);
                            outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                            Files.copy(file, outputStream);
                            outputStream.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            deleteCreated = false;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!FileSystemUtils.deleteRecursively(tempDir)) {
                log.warn("Fail to delete temp directory");
            }
            if (deleteCreated) {
                Files.deleteIfExists(targetFile);
            }
        }
    }

}
