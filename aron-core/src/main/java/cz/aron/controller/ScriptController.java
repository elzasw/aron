package cz.aron.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.ScriptsApi;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.service.ScriptExecutor;
import cz.aron.service.ScriptType;

@RestController
public class ScriptController implements ScriptsApi {

	private final ScriptExecutor scriptExecutor;

	private final ObjectMapper objectMapper;

	private final ApuEntityRepository apuRepository;

	public ScriptController(ScriptExecutor scriptExecutor, ObjectMapper objectMapper,
			ApuEntityRepository apuRepository) {
		this.scriptExecutor = scriptExecutor;
		this.objectMapper = objectMapper;
		this.apuRepository = apuRepository;
	}

    @Override
    public ResponseEntity<Resource> runApuScript(String scriptName, UUID id) {
		String code = null;
		try {
			code = new String(Files.readAllBytes(Path.of(scriptName+".groovy")),"utf-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Map<String,Object> configuration = new HashMap<>();
		configuration.put("id", id);
		configuration.put("apuRepository", apuRepository);
		configuration.put("objectMapper", objectMapper);
		return scriptExecutor.executeScript(ScriptType.GROOVY, code, false, configuration, null);
    }
}
