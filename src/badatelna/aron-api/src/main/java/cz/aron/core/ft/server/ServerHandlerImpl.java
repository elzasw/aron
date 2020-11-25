package cz.aron.core.ft.server;


import com.lightcomp.ft.server.TransferDataHandler;
import com.lightcomp.ft.server.TransferHandler;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import java.util.HashMap;
import java.util.Map;

public class ServerHandlerImpl implements TransferHandler {

    final Map<String, TransferHandler> transferHandlers = new HashMap<>();

    @Override
    public TransferDataHandler onTransferBegin(String transferId, GenericDataType request) {
        TransferHandler transferHandler = transferHandlers.get(request.getType());
        if (transferHandler == null) {
            throw new IllegalStateException("Unknown type of request " + request.getType());
        }
        return transferHandler.onTransferBegin(transferId, request);
    }

    /**
     * Register request handler
     *
     * @param handler
     */
    public void registerHandler(TransferHandlerWithTypes handler) {
        for (String type : handler.getHandledTypes()) {
            if (transferHandlers.put(type, handler) != null) {
                throw new IllegalStateException("Duplicated handler for type " + type);
            }
        }
    }
}
