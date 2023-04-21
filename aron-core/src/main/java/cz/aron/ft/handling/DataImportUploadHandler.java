package cz.aron.ft.handling;

import com.lightcomp.ft.server.ErrorDesc;
import com.lightcomp.ft.server.TransferStatus;
import com.lightcomp.ft.server.UploadHandler;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import cz.aron.integration.ImportDataProcessingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;

public class DataImportUploadHandler implements UploadHandler {
	
	private static final Logger log = LoggerFactory.getLogger(DataImportUploadHandler.class);

    private final String transferId;
    private final Path transferLocation;
    private final ImportDataProcessingService importDataProcessingService;
    private final TransferType transferType;

    public DataImportUploadHandler(String transferId, Path transferLocation,
			ImportDataProcessingService importDataProcessingService, TransferType transferType) {
		this.transferId = transferId;
		this.transferLocation = transferLocation;
		this.importDataProcessingService = importDataProcessingService;
		this.transferType = transferType;
	}

	@Override
    public Mode getMode() {
        return Mode.UPLOAD;
    }

    @Override
    public String getRequestId() {
        return transferId;
    }

    @Override
    public GenericDataType finishTransfer() {
        try {
            log.debug("File transfer {} finished, processing of transferred data started.", transferId);
            importDataProcessingService.processData(transferLocation, transferType);
        }
        finally {
        	try {
				FileSystemUtils.deleteRecursively(transferLocation);
			} catch (IOException e) {
				log.warn("Fail to delete dir {}", transferLocation, e);
			}
        }
        log.debug("File transfer {} processing of transferred data succeeded.", transferId);
        GenericDataType response = new GenericDataType();
        response.setId(transferId);
        response.setType(transferType.name());
        return response;
    }

    @Override
    public void onTransferProgress(TransferStatus status) {
    }

    @Override
    public void onTransferCanceled() {
    }

    @Override
    public void onTransferFailed(ErrorDesc errorDesc) {
    }

    @Override
    public Path getUploadDir() {
        return transferLocation;
    }
}