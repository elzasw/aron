package cz.inqool.eas.common.admin.console.stream;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.ByteArrayOutputStream;

/**
 * Byte stream that sends buffered values to defined web socket destination.
 */
public class WebSocketOutputStream extends ByteArrayOutputStream {

    private final SimpMessagingTemplate webSocket;
    private final String destination;


    public WebSocketOutputStream(SimpMessagingTemplate webSocket, String destination) {
        this.webSocket = webSocket;
        this.destination = destination;
    }


    @Override
    public void flush() {
        webSocket.convertAndSend(destination, toString());
        reset();
    }
}
