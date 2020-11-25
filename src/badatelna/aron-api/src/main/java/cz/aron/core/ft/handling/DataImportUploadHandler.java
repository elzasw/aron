package cz.aron.core.ft.handling;

import com.lightcomp.ft.server.ErrorDesc;
import com.lightcomp.ft.server.TransferStatus;
import com.lightcomp.ft.server.UploadHandler;
import com.lightcomp.ft.xsd.v1.GenericDataType;
import cz.aron.core.integration.ImportDataProcessingService;
import cz.inqool.eas.common.exception.ExceptionUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class DataImportUploadHandler implements UploadHandler {

    private final String transferId;
    private final Path transferLocation;
    private final ImportDataProcessingService importDataProcessingService;
    private final TransferType transferType;

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
            ExceptionUtils.checked(() -> FileSystemUtils.deleteRecursively(transferLocation));
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