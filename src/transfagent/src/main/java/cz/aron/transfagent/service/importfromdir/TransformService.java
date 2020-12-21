package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.apux.ApuxFactory;
import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.apux._2020.DaoFile;
import cz.aron.apux._2020.Metadata;
import cz.aron.apux._2020.MetadataItem;
import cz.aron.transfagent.service.StorageService;
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
        files.sort((p1,p2)->p1.getFileName().compareTo(p2.getFileName()));

        var daoUuid = dir.getFileName().toString();

        var dao = new Dao();
        dao.setUuid(daoUuid);

        var published = new DaoBundle();
        published.setType(DaoBundleType.PUBLISHED);
        dao.getBndl().add(published);
        
        var hiResView = new DaoBundle();
        hiResView.setType(DaoBundleType.HIGH_RES_VIEW);
        dao.getBndl().add(hiResView);
        
        var thumbnail = new DaoBundle();
        thumbnail.setType(DaoBundleType.THUMBNAIL);
        dao.getBndl().add(thumbnail);
        
        // originalni soubory k presunu, klic je novy nazev 
        var filesToMove = new HashMap<String, Path>();
        
        var pos = 1;
        for (Path file : files) {            
            String mimeType = Files.probeContentType(file);                        
            processPublished(file, published, pos, filesToMove);
            if (mimeType!=null&&mimeType.startsWith("image/")) {
                log.info("Generating dzi and thumbnail for {}",file);
                processHiResView(file, filesDir, hiResView, pos);
                processThumbNail(file, filesDir, thumbnail, pos);
            }
            pos++;
        }

        // vytvoreni dao-uuid.xml
        Marshaller marshaller = ApuxFactory.createMarshaller();
        try (OutputStream os = Files.newOutputStream(dir.resolve("dao-" + daoUuid + ".xml"))) {
            marshaller.marshal(ApuxFactory.getObjFactory().createDao(dao), os);
        }
        
        // move original files
        for(var entry:filesToMove.entrySet()) {
            Files.move(entry.getValue(), filesDir.resolve(entry.getKey()));
        }

    }
    
    private void processThumbNail(Path file, Path filesDir, DaoBundle thumbnails, int pos) throws IOException {
        var uuid = UUID.randomUUID().toString();
        var daoFile = new DaoFile();
        daoFile.setPos(pos);
        daoFile.setUuid(uuid);
        addMimeTypeToDaoFile(daoFile, "image/jpeg");
        thumbnails.getFile().add(daoFile);
        try (OutputStream os = Files.newOutputStream(filesDir.resolve("file-" + uuid))) {
            Thumbnails.of(file.toFile())
                    .outputFormat("jpg")
                    .size(120, 120)
                    .toOutputStream(os);
        }
    }
    
    private void processHiResView(Path file, Path filesDir, DaoBundle hiResView, int pos) throws IOException {
        var uuid = UUID.randomUUID().toString();
        var daoFile = new DaoFile();
        daoFile.setPos(pos);
        daoFile.setUuid(uuid);
        addMimeTypeToDaoFile(daoFile,"image/jpeg");
        hiResView.getFile().add(daoFile);
        createDzi(file, filesDir.resolve("file-"+uuid));
    }
    
    private void processPublished(Path file, DaoBundle published, int pos, Map<String, Path> filesToMove) {
        var uuid = UUID.randomUUID().toString();
        var daoFile = new DaoFile();
        daoFile.setPos(pos);
        daoFile.setUuid(uuid);
        addMimeTypeToDaoFile(daoFile,"image/jpeg");
        filesToMove.put("file-" + uuid, file);
        published.getFile().add(daoFile);
    }
    
    private void addMimeTypeToDaoFile(DaoFile daoFile, String mimeType) {
        var metadata = new Metadata();
        var metadataItem = new MetadataItem();
        metadataItem.setCode("mimeType");
        metadataItem.setValue(mimeType);
        metadata.getItms().add(metadataItem);
        daoFile.setMtdt(metadata);
    }
    
    
    private void createDzi(Path sourceImage, Path targetFile) throws IOException {

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
