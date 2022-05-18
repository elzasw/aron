package cz.aron.transfagent.service.importfromdir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
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
import org.springframework.beans.factory.annotation.Value;
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
    
    private static final int DIR_NAME_LENGTH = 2;

    private final StorageService storageService;

    private final ConfigDspace configDspace;
    
    @Value("${tile.folder:#{NULL}}")
    private String tileFolder;

    @Value("${tile.level:2}")
    private int hierarchicalLevel;

    public TransformService(StorageService storageService, ConfigDspace configDspace) {
        this.storageService = storageService;
        this.configDspace = configDspace;
    }

    public boolean transform(Path dir) throws JAXBException, IOException {

        log.debug("Transforming data, path: {}", dir);

        if (!Files.isDirectory(dir)) {
            log.error("Directory does not exist {}", dir);
            throw new IOException();
        }

        Tika tika = new Tika();

        var daoUuid = dir.getFileName().toString();
        var daoUuidXmlFile = dir.resolve("dao-" + daoUuid + ".xml");
        var filesDir = dir.resolve("files");

        // mazání předchozích souborů
        log.debug("Deleting old files and folders in a directory {}", dir);
        FileSystemUtils.deleteRecursively(filesDir);
        Files.deleteIfExists(daoUuidXmlFile);

        log.debug("Preparing files list in directory {}", dir);
        List<Path> files = prepareFileList(dir);
        if (files.isEmpty()) {
            return false;
        }

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
            log.debug("Processing {} file {}", mimeType, file);
            processPublished(file, published, pos, filesToMove, mimeType);
            if (mimeType != null && mimeType.startsWith("image/")) {
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
        log.debug("Creating file {}", daoUuidXmlFile);
        Marshaller marshaller = ApuxFactory.createMarshaller();
        try (OutputStream os = Files.newOutputStream(daoUuidXmlFile)) {
            marshaller.marshal(daoBuilder.build(), os);
        }

        // move original files
        log.debug("Moving all files to {}", filesDir);
        for (var entry : filesToMove.entrySet()) {
            Files.move(entry.getValue(), filesDir.resolve(entry.getKey()));
        }
        return true;
    }

    private List<Path> prepareFileList(Path dir) {
        List<Path> files;
        var bitstreamJson = dir.resolve(DSpaceConsts.BITSTREAM_JSON);
        if (Files.exists(bitstreamJson)) {
            files = readBitstreamJson(dir, bitstreamJson);
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(f -> Files.isRegularFile(f) && !files.contains(f)).map(f -> f.toFile()).forEach(File::delete);
            } catch (IOException e) {
                log.error("Error getting file list from {} ", dir, e);
                throw new RuntimeException(e);
            }
        } else {
            try (Stream<Path> stream = Files.list(dir)) {
                files = stream.filter(f -> Files.isRegularFile(f)).collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error getting file list from {} ", dir, e);
                throw new RuntimeException(e);
            }
            files.sort((p1, p2)->p1.getFileName().compareTo(p2.getFileName()));
        }
        return files;
    }

    private List<Path> readBitstreamJson(Path dir, Path bitstreamJson) {
        var filterBundle = configDspace.getBundleName();
        Map<Integer, Path> filesMap = new HashMap<>();
        try {
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
        } catch (IOException e) {
            log.error("Error reading file {} ", bitstreamJson, e);
            throw new RuntimeException(e);
        }
        return filesMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private void processThumbNail(Path file, Path filesDir, DaoBundle thumbnails, int pos) {
        var daoFile = DaoBuilder.createDaoFile(pos, "image/jpeg");
        var uuid = daoFile.getUuid();

        thumbnails.getFile().add(daoFile);
        var thumbnailsFile = filesDir.resolve("file-" + uuid);

        log.debug("Creating thumbnails file {}", thumbnailsFile);

        try (OutputStream os = Files.newOutputStream(thumbnailsFile)) {
            Thumbnails.of(file.toFile())
                    .outputFormat("jpg")
                    .size(120, 120)
                    .toOutputStream(os);
        } catch (IOException e) {
            log.error("Error creating thumbnails {} ", thumbnailsFile, e);
            throw new RuntimeException(e);
        }
    }

    private void processHiResView(Path file, Path filesDir, DaoBundle hiResView, int pos) {
        var daoFile = DaoBuilder.createDaoFile(pos, "application/octetstream");
        var uuid = daoFile.getUuid();
        hiResView.getFile().add(daoFile);        
        if (tileFolder != null) {
        	createDziOut(file, uuid);
        	DaoBuilder.addReferenceFlag(daoFile);
        } else {
        	createDzi(file, filesDir.resolve("file-" + uuid));	
        }        
    }

    private void processPublished(Path file, DaoBundle published, int pos, Map<String, Path> filesToMove, String mimeType) {
        var fileName = file.getFileName().toString();
        long fileSize;
        try {
            fileSize = Files.size(file);
        } catch (IOException e) {
            log.error("Failed to read file size, path: {} ", file, e);
            throw new RuntimeException(e);
        }
        var daoFile = DaoBuilder.createDaoFile(fileName, fileSize, pos, mimeType);
        var uuid = daoFile.getUuid();

        filesToMove.put("file-" + uuid, file);
        published.getFile().add(daoFile);
    }

    private void createDzi(Path sourceImage, Path targetFile) {
        log.debug("Creating hiResView file {}", targetFile);

        var prefix = "dzi_" + sourceImage.getFileName().toString() + "_";
        Path tempDir;
        try {
            tempDir = storageService.createTempDir(prefix);
        } catch (IOException e) {
            log.error("Error creating tempDir prefix={} ", prefix, e);
            throw new RuntimeException(e);
        }

        boolean deleteCreated = true;
        try {
            ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
            FilesArchiver archiver = new DirectoryArchiver(tempDir.toFile());
            PartialImageReader pir = new BufferedImageReader(sourceImage.toFile());
            spb.buildPyramid(pir, "image", archiver, Runtime.getRuntime().availableProcessors());
            try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(targetFile))) {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        try {
                        	StringJoiner sj = new StringJoiner("/");
                            Path targetFile = tempDir.relativize(file);
                            targetFile.forEach(p->sj.add(p.toString()));                                                        
                            outputStream.putNextEntry(new ZipEntry(sj.toString()));
                            Files.copy(file, outputStream);
                            outputStream.closeEntry();
                        } catch (IOException e) {
                            log.error("Error in copying file, source: {}", file, e);
                            throw new RuntimeException(e);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            deleteCreated = false;
        } catch (IOException e) {
            log.error("Error in creating ZIP file {}", targetFile, e);
            throw new RuntimeException(e);
        } finally {
            try {
                log.debug("Deleting directory {}", tempDir);
                if (!FileSystemUtils.deleteRecursively(tempDir)) {
                    log.warn("Fail to delete temp directory");
                }
                if (deleteCreated) {
                    log.debug("Deleting file {}", tempDir);
                    Files.deleteIfExists(targetFile);
                }
            } catch (IOException e) {
                log.error("Error deleting unused file(s) ", e);
                throw new RuntimeException(e);
            }
        }
    }
    
    
    private void createDziOut(Path sourceImage, String id) {
    	log.debug("Creating hiResView file {}", id);
    	
    	Path path = getPath(id);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Error creating dir prefix={} ", path, e);
            throw new RuntimeException(e);
        }

        boolean deleteCreated = true;
        try {
            ScalablePyramidBuilder spb = new ScalablePyramidBuilder(254, 1, "jpg", "dzi");
            FilesArchiver archiver = new DirectoryArchiver(path.toFile());
            PartialImageReader pir = new BufferedImageReader(sourceImage.toFile());
            spb.buildPyramid(pir, "image", archiver, Runtime.getRuntime().availableProcessors());
            deleteCreated = false;
        } catch (IOException e) {
            log.error("Error in creating dzi {}", path, e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (deleteCreated) {
                    log.debug("Deleting file {}", path);
                    if (!FileSystemUtils.deleteRecursively(path)) {
                        log.warn("Fail to delete dzi directory", path);
                    }
                }
            } catch (IOException e) {
                log.error("Error deleting unused file(s) ", e);
                throw new RuntimeException(e);
            }
        }    	
    }

    private Path getPath(String id) {
        String[] path = new String[hierarchicalLevel + 1];
        path[hierarchicalLevel] = id;
        String uuid = id.replaceAll("-", "");
        for (int i = 0; i < hierarchicalLevel; i++) {
            path[i] = uuid.substring(i * DIR_NAME_LENGTH, i * DIR_NAME_LENGTH + DIR_NAME_LENGTH);
        }
        return Paths.get(tileFolder, path);
    }

}
