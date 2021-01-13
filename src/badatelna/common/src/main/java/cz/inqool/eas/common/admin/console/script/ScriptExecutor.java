package cz.inqool.eas.common.admin.console.script;

import cz.inqool.eas.common.admin.console.api.msg.ExecuteScriptRequest;
import cz.inqool.eas.common.admin.console.stream.MultiOutputStream;
import cz.inqool.eas.common.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.inqool.eas.common.utils.AssertionUtils.ifPresent;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

/**
 * Executes provided script and return the result.
 * <p>
 * The provided script has access to all spring beans and therefore can do nearly anything.
 */
@Slf4j
@Service
public class ScriptExecutor {

    private ApplicationContext context;

    private Map<ScriptType, ScriptEngine> scriptEngines = Map.of();


    public List<ScriptType> getSupportedScriptEngines() {
        return List.copyOf(scriptEngines.keySet());
    }

    @Transactional
    public Object executeScriptWithTransaction(ExecuteScriptRequest request) {
        return executeScript(request);
    }

    public Object executeScriptNoTransaction(ExecuteScriptRequest request) {
        return executeScript(request);
    }

    private Object executeScript(ExecuteScriptRequest request) {
        ScriptEngine scriptEngine = scriptEngines.get(request.getScriptType());
        notNull(scriptEngine, () -> new UnsupportedOperationException("Unsupported script engine '" + request.getScriptType() + "'"));

        SimpleBindings bindings = new SimpleBindings();
        ifPresent(request.getParams(), bindings::putAll);
        bindings.put("spring", context);

        if (request.getOutputStream() != null) {
            OutputStream outputStream = request.getOutputStream();

            scriptEngine.getContext().setWriter(new OutputStreamWriter(outputStream));
            scriptEngine.getContext().setErrorWriter(new OutputStreamWriter(outputStream));

            PrintStream sOut = System.out;
            PrintStream sErr = System.err;

            System.setOut(new PrintStream(new MultiOutputStream(sOut, outputStream)));
            System.setErr(new PrintStream(new MultiOutputStream(sErr, outputStream)));

            try {
                return scriptEngine.eval(request.getScript(), bindings);
            } catch (ScriptException e) {
                log.error("Script execution failed. ", e);
                throw new GeneralException(e);
            } finally {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    log.error("Failed to flush", e);
                }

                System.setOut(sOut);
                System.setErr(sErr);
            }
        } else {
            try {
                return scriptEngine.eval(request.getScript(), bindings);
            } catch (ScriptException e) {
                log.error("Script execution failed. ", e);
                throw new GeneralException(e);
            }
        }
    }


    @Autowired
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    @Autowired(required = false)
    public void setScriptEngines(List<ScriptEngine> scriptEngines) {
        this.scriptEngines = scriptEngines.stream()
                .collect(Collectors.toMap(scriptEngine -> ScriptType.obtainFrom(scriptEngine.getFactory()), Function.identity()));
    }
}
