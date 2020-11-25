package cz.aron.core.ft.handling;

import com.lightcomp.ft.server.TransferDataHandler;
import com.lightcomp.ft.xsd.v1.GenericDataType;
import cz.aron.core.ft.server.FileTransferServer;
import cz.aron.core.ft.server.TransferHandlerWithTypes;
import cz.aron.core.integration.ImportDataProcessingService;
import cz.inqool.eas.common.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataImportTransferHandler implements TransferHandlerWithTypes {

    @Value("${files.transfer.path}")
    private String filesPath;

    @Inject private FileTransferServer fileTransferServer;
    @Inject private ImportDataProcessingService importDataProcessingService;

    @PostConstruct
    public void init() {
        fileTransferServer.registerHandler(this);
    }

    @Override
    public synchronized TransferDataHandler onTransferBegin(String transferId, GenericDataType request) {
        Path transferBatchFolder = Paths.get(filesPath).resolve(transferId);
        ExceptionUtils.checked(() -> Files.createDirectories(transferBatchFolder));
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