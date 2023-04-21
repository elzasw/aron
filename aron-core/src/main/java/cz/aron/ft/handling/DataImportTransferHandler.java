package cz.aron.ft.handling;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lightcomp.ft.server.TransferDataHandler;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import cz.aron.ft.server.FileTransferServer;
import cz.aron.ft.server.TransferHandlerWithTypes;
import cz.aron.integration.ImportDataProcessingService;
import jakarta.annotation.PostConstruct;

@Service
public class DataImportTransferHandler implements TransferHandlerWithTypes {
	
	private static final Logger log = LoggerFactory.getLogger(DataImportTransferHandler.class);

    @Value("${files.transfer.path}")
    private String filesPath;

    private final FileTransferServer fileTransferServer;
    private final ImportDataProcessingService importDataProcessingService;
    
    public DataImportTransferHandler(FileTransferServer fileTransferServer, ImportDataProcessingService importDataProcessingService) {
    	this.fileTransferServer = fileTransferServer;
    	this.importDataProcessingService = importDataProcessingService;
    }

    @PostConstruct
    public void init() {
        fileTransferServer.registerHandler(this);
    }

    @Override
    public synchronized TransferDataHandler onTransferBegin(String transferId, GenericDataType request) {
        Path transferBatchFolder = Paths.get(filesPath).resolve(transferId);
        try {
			Files.createDirectories(transferBatchFolder);
		} catch (IOException e) {
			log.error("Fail to create directory {}", transferBatchFolder, e);
			throw new UncheckedIOException(e);
		}             
        return new DataImportUploadHandler(
                transferId,
                transferBatchFolder,
                importDataProcessingService,
                TransferType.valueOf(request.getType()));
    }

    @Override
    public Set<String> getHandledTypes() {
        return Arrays.stream(TransferType.values()).map(Enum::name).collect(Collectors.toSet());
    }

}