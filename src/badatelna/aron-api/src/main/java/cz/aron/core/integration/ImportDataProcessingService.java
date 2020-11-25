package cz.aron.core.integration;

import cz.aron.core.ft.handling.TransferType;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Lukas Jane (inQool) 04.11.2020.
 */
@Service
public class ImportDataProcessingService {
    @Inject private ApuProcessor apuProcessor;
    @Inject private DaoInputProcessor daoInputProcessor;

    public void processData(Path path, TransferType transferType) {
        Map<String, Path> filesMap = loadFilesMap(path);
        if (transferType == TransferType.APUSRC) {
            try {
                Path apuFilePath = Files.list(path).filter(child -> child.getFileName().startsWith("apusrc-")).findFirst().orElseThrow();
                String metadata = Files.readString(apuFilePath, StandardCharsets.UTF_8);
                apuProcessor.processApuAndFiles(metadata, filesMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        if (transferType == TransferType.DAO) {
            try {
                String metadata;
                Path apuFilePath = Files.list(path).filter(child -> child.getFileName().startsWith("dao-")).findFirst().orElseThrow();
                metadata = Files.readString(apuFilePath, StandardCharsets.UTF_8);
                daoInputProcessor.processDaoAndFiles(metadata, filesMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, Path> loadFilesMap(Path basePath) {
        List<Path> files;
        try {
            files = Files.list(basePath.resolve("files")).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, Path> filesMap = new HashMap<>();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String fileUuid = fileName.replace("file-", "");
            filesMap.put(fileUuid, file);
        }
        return filesMap;
    }
}
