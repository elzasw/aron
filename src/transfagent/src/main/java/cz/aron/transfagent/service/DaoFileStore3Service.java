package cz.aron.transfagent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.config.ConfigDaoFileStore3;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value="filestore3.path")
@Service
public class DaoFileStore3Service implements DaoImporter {
    
    private static final Logger log = LoggerFactory.getLogger(DaoFileStore3Service.class);

    private final TransformService transformService;

    private final ConfigDaoFileStore3 config;

    private final Path daosDir;

    public DaoFileStore3Service(TransformService transformService, ConfigDaoFileStore3 config) {
        this.transformService = transformService;
        this.config = config;
        daosDir = config.getPath();        
    }

    @Override
    public String getName() {
        return "file3";
    }

    @Override
    public void importDaoFile(Dao dao, Path daoDir) {
        Path dataDir = daosDir.resolve(dao.getHandle());
        try {
            // copy images
            try (Stream<Path> stream = Files.list(dataDir);) {
                stream.forEach(p -> {
                    if (isImage(p)) {
                        try {
                            Files.copy(p, daoDir.resolve(p.getFileName()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            }
            if (!transformService.transform(daoDir, dataDir)) {
                // delete empty dir
            }
            dao.setState(DaoState.READY);
        } catch (Exception e) {
            if (/*!config.isCheckExistency() &&*/ !Files.isDirectory(dataDir)) {
                dao.setState(DaoState.INACCESSIBLE);
                try {
                    FileSystemUtils.deleteRecursively(daoDir);
                } catch (IOException e1) {
                    log.warn("Fail to delete directory {}", daoDir);
                }
                log.warn("Dao handle={} not exist.", dao.getHandle());
            } else {
                log.error("Fail to import dao {}", dao.getUuid(), e);
            }
        }
    }
    
    public String getFundDaoHandle(String archiveCode, String fundCode, Integer subFundCode) {
        Path daoDir = daosDir.resolve(archiveCode).resolve("AS").resolve(fundCode);
        if (subFundCode != null) {
            daoDir = daoDir.resolve("" + subFundCode);
        }
        if (!Files.isDirectory(daoDir)) {
            return null;
        }
        if (isContainsImage(daoDir)) {
            return Paths.get(archiveCode, "AS", fundCode).toString();
        }
        return null;
    }
    
    public String getFindingAidDaoHandle(String archiveCode, String findingAidCode) {
        Path daoDir = daosDir.resolve(archiveCode).resolve("AP").resolve(findingAidCode);
        if (!Files.isDirectory(daoDir)) {
            return null;
        }
        if (isContainsImage(daoDir)) {
            return Paths.get(archiveCode, "AP", findingAidCode).toString();
        }
        return null;
    }

    private boolean isContainsImage(Path daoDir) {
        try (Stream<Path> stream = Files.list(daoDir);) {                                   
            return stream.anyMatch(p -> isImage(p));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Vrati true pokud jde o cestu k souboru. Zjistuje podle pripony.  
     * @param path cesta k souboru
     * @return true - obrazek, false - jinak
     */
    public static boolean isImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".tif") || fileName
                .endsWith(".tiff") || fileName.endsWith(".jp2")) {
            return true;
        } else {
            return false;
        }
    }
    
}
