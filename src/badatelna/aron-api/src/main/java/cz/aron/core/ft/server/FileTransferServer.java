package cz.aron.core.ft.server;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.server.Server;
import com.lightcomp.ft.server.ServerConfig;
import com.lightcomp.ft.simple.StatusStorageImpl;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;


@Component
public class FileTransferServer implements SmartLifecycle {

    private final Server server;
    private final ServerHandlerImpl serverHandler = new ServerHandlerImpl();
    private volatile boolean running = false;

    public FileTransferServer() {
        StatusStorageImpl statusStorage = new StatusStorageImpl();
        ServerConfig cfg = new ServerConfig(serverHandler, statusStorage);
        server = FileTransfer.createServer(cfg);
    }

    @Override
    public void start() {
        synchronized (this) {
            if (!this.running) {
                server.start();
                /*
                EndpointImpl ep = server.getEndpointFactory().createCxf(BusFactory.getThreadDefaultBus());
                ep.setAddress(config.getEndpoint());
                ep.publish();
                */
                this.running = true;
            }
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (this.running) {
                server.stop();
                this.running = false;
            }
        }
    }

    @Override
    public void stop(Runnable callback) {
        synchronized (this) {
            stop();
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (this) {
            return running;
        }
    }

    public void registerHandler(TransferHandlerWithTypes handler) {
        serverHandler.registerHandler(handler);
    }

    public Server getServer() {
        return server;
    }

}
