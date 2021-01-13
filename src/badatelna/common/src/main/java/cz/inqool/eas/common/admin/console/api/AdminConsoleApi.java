package cz.inqool.eas.common.admin.console.api;

import cz.inqool.eas.common.admin.console.api.msg.ExecuteScriptRequest;
import cz.inqool.eas.common.admin.console.api.msg.ExecuteScriptResponse;
import cz.inqool.eas.common.admin.console.script.ScriptExecutor;
import cz.inqool.eas.common.admin.console.script.ScriptType;
import cz.inqool.eas.common.admin.console.stream.WebSocketOutputStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Management web socket endpoint for admin script execution. Provides own HTML interface.
 * <p>
 * If application wants to use admin console API, it needs to extend this class and add {@link Controller} (to expose
 * web socket mappings) or {@link RestController} (to expose web socket mappings and REST endpoints) annotation.
 */
@Slf4j
@Tag(name = "Admin Console", description = "Admin console API")
public class AdminConsoleApi {

    private static final String CONSOLE_OUT_MESSAGE_DESTINATION = "/topic/script/execute/out";

    private ScriptExecutor scriptExecutor;
    private SimpMessagingTemplate webSocket;


    @Operation(summary = "List of supported script engines")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(value = "/supported")
    @MessageMapping("/script/supported/request")
    @SendTo("/topic/script/supported/response")
    @PreAuthorize("this.canAccess(#root)")
    public List<ScriptType> getSupportedScriptTypes() {
        return scriptExecutor.getSupportedScriptEngines();
    }

    @MessageMapping("/script/execute/request")
    @SendTo("/topic/script/execute/response")
    @PreAuthorize("this.canAccess(#root)")
    public ExecuteScriptResponse executeScript(ExecuteScriptRequest message) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            message.setOutputStream(new WebSocketOutputStream(webSocket, CONSOLE_OUT_MESSAGE_DESTINATION));

            Object result = (message.getTransaction()) ?
                    scriptExecutor.executeScriptWithTransaction(message) : scriptExecutor.executeScriptNoTransaction(message);
            stopWatch.stop();

            return ExecuteScriptResponse.success(result, stopWatch.getLastTaskTimeMillis());
        } catch (Exception ex) {
            log.error("Error during script execution", ex);
            stopWatch.stop();
            return ExecuteScriptResponse.failure("Error, see console output", stopWatch.getLastTaskTimeMillis());
        }
    }

    public boolean canAccess(SecurityExpressionRoot root) {
        return true;
    }


    @Autowired
    public void setScriptExecutor(ScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    @Autowired
    public void setWebSocket(SimpMessagingTemplate webSocket) {
        this.webSocket = webSocket;
    }
}
