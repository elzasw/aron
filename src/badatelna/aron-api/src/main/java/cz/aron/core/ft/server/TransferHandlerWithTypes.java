package cz.aron.core.ft.server;

import com.lightcomp.ft.server.TransferHandler;

import java.util.Set;

public interface TransferHandlerWithTypes extends TransferHandler {

    /**
     * Return set of all handled types by this handler
     *
     * @return Set of String
     */
    Set<String> getHandledTypes();
}
