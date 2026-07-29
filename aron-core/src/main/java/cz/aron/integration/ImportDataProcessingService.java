package cz.aron.integration;

import org.springframework.stereotype.Service;

import cz.aron.ft.handling.TransferType;
import cz.aron.service.IdService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class ImportDataProcessingService {
    
    private final ReentrantLock apuLock = new ReentrantLock();
    
    private final ReentrantLock daoLock = new ReentrantLock();
    
    private final ApuProcessor apuProcessor;
    private final DaoInputProcessor daoInputProcessor;
    private final IdService idService;
    
    public ImportDataProcessingService(ApuProcessor apuProcessor, DaoInputProcessor daoInputProcessor, IdService idService) {
    	this.apuProcessor = apuProcessor;
    	this.daoInputProcessor = daoInputProcessor;
		this.idService = idService;
    }

    public void processData(Path path, TransferType transferType) {
        Map<String, Path> filesMap = loadFilesMap(path);
        if (transferType == TransferType.APUSRC) {
            try (var stream = Files.list(path)) {
                Path apuFilePath = stream.filter(child -> child.getFileName().toString().startsWith("apusrc-")).findFirst().orElseThrow();                
                if (!apuLock.tryLock()) {
                    throw new RuntimeException("Concurrent apu upload is running");
                }
                idService.initMetadataIds();
                try {                    
                    apuProcessor.processApuAndFiles(apuFilePath, filesMap);    
                } finally {
                    apuLock.unlock();
                }                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        if (transferType == TransferType.DAO) {
            try (var stream = Files.list(path)) {
                String metadata;
                Path apuFilePath = stream.filter(child -> child.getFileName().toString().startsWith("dao-")).findFirst().orElseThrow();
                metadata = Files.readString(apuFilePath, StandardCharsets.UTF_8);
                if (!daoLock.tryLock()) {
                    throw new RuntimeException("Concurrent dao upload is running");
                }
                try {
                    daoInputProcessor.processDaoAndFiles(metadata, filesMap);
                } finally {
                    daoLock.unlock();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, Path> loadFilesMap(Path basePath) {
        List<Path> files;
        if (!Files.exists(basePath.resolve("files"))) {
            return new HashMap<>();
        }
        try (var stream = Files.list(basePath.resolve("files"))) {
            files = stream.collect(Collectors.toList());
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
