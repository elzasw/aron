package cz.aron.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Executes scripts through the JSR-223 {@link ScriptEngine} API.
 */
@Component
public class ScriptExecutor {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    private final Map<String, CompiledScript> compiledCache = new ConcurrentHashMap<>();

    /**
     * Evaluates {@code code} with the engine for the given {@link ScriptType} and turns its result
     * into a binary HTTP response.
     *
     * @param type          script language (selects the JSR-223 engine)
     * @param code          script source
     * @param cache         when {@code true} the compiled script is cached and reused (only for
     *                      engines implementing {@link Compilable})
     * @param configuration variables exposed to the script as global bindings
     * @param input         optional value bound as the {@code input} variable (ignored when null)
     */
    public ResponseEntity<Resource> executeScript(ScriptType type, String code, boolean cache,
            Map<String, Object> configuration, Object input) {

        ScriptEngine engine = scriptEngineManager.getEngineByName(type.getEngineName());
        if (engine == null) {
            throw new IllegalStateException("No script engine available for " + type
                    + " (engine '" + type.getEngineName() + "' is not on the classpath)");
        }

        Bindings bindings = engine.createBindings();
        if (configuration != null) {
            bindings.putAll(configuration);
        }
        if (input != null) {
            bindings.put("input", input);
        }

        try {
            Object result;
            if (cache && engine instanceof Compilable compilable) {
                CompiledScript compiled = compiledCache.computeIfAbsent(cacheKey(type, code), key -> compile(compilable, code));
                result = compiled.eval(bindings);
            } else {
                result = engine.eval(code, bindings);
            }
            return toResponse(result);
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to execute " + type + " script", e);
        }
    }

    private CompiledScript compile(Compilable compilable, String code) {
        try {
            return compilable.compile(code);
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to compile script", e);
        }
    }

    private String cacheKey(ScriptType type, String code) {
        return type.name() + ':' + code.hashCode();
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Resource> toResponse(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return (ResponseEntity<Resource>) responseEntity;
        }
        if (result instanceof Resource resource) {
            return ResponseEntity.ok(resource);
        }
        byte[] bytes;
        if (result == null) {
            bytes = new byte[0];
        } else if (result instanceof byte[] b) {
            bytes = b;
        } else {
            bytes = result.toString().getBytes(StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(bytes));
    }
}
