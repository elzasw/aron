package cz.aron.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.commons.LocalizationFile;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.ApuType;
import cz.aron.domain.types.LocalizedText;
import cz.aron.domain.types.dto.LocalizedItem;
import cz.aron.repository.ApuEntityRepository;
import jakarta.annotation.PostConstruct;

/**
 * Citations of a record, rendered by the deployment's own citation scripts
 * ({@code webResources.citation}). A citation is archival grammar rather than
 * string formatting - which identifiers appear, in which order, with which
 * abbreviations is agreed between an archive and its archivists, per citation
 * norm - so the portal does not compose one: it runs the deployment's script and
 * publishes what that produces.
 *
 * <pre>
 * forms:
 *   - code: DEFAULT              # stable identity of the form
 *     label: Citace              # source language; translations in the sibling file
 *     script: citation.groovy    # resolved next to this file
 *     apuTypes: [ARCH_DESC, FUND]
 * </pre>
 *
 * A list, because a second citation norm is then deployment configuration rather
 * than a release. {@code apuTypes} reaches the UI through
 * {@code /api/v1/ui/config}, so a citation is offered only where one exists; no
 * configured file means none anywhere.
 * <p>
 * Everything the loader can check, it checks at startup - unknown keys, a
 * duplicate code, an unknown record type, a missing script, a script that does
 * not compile - because a citation is reached by a reader's click, and a
 * configuration mistake found then is found by the reader.
 * <p>
 * A script is given {@code id} (the record's uuid), {@code apuRepository},
 * {@code objectMapper} and {@code lang}, and returns JSON carrying either
 * {@code citation} or a diagnostic {@code error}. Those four names are the
 * contract deployment scripts are written against, so a test pins them. The
 * compiled script is cached: a citation costs one script run, not a compilation.
 */
@Component
public class CitationService {

	private static final Logger log = LoggerFactory.getLogger(CitationService.class);

	private static final Set<String> ROOT_KEYS = Set.of("forms");

	private static final Set<String> FORM_KEYS = Set.of("code", "label", "script", "apuTypes");

	private final String configFile;

	private final ScriptExecutor scriptExecutor;

	private final ApuEntityRepository apuEntityRepository;

	private final ObjectMapper objectMapper;

	private List<Form> forms = List.of();

	/**
	 * One configured citation form: its identity, its name (source language plus
	 * translations), the record types it covers and the script rendering it.
	 */
	public record Form(String code, String label, List<LocalizedItem> translations, List<ApuType> apuTypes,
			String scriptFile, String script) {
	}

	/** One rendered citation, ready to be copied verbatim. */
	public record Citation(String code, String label, String text) {
	}

	/**
	 * What the configured forms produced for one record. A form that cannot
	 * render it contributes no citation but a {@code diagnostic} instead: a
	 * reader can do nothing about a record whose fund is missing, while whoever
	 * looks into the data needs to know which piece was absent.
	 */
	public record Citations(List<Citation> items, String diagnostic) {
	}

	public CitationService(@Value("${webResources.citation:}") String configFile, ScriptExecutor scriptExecutor,
			ApuEntityRepository apuEntityRepository, ObjectMapper objectMapper) {
		this.configFile = configFile;
		this.scriptExecutor = scriptExecutor;
		this.apuEntityRepository = apuEntityRepository;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void load() {
		if (configFile == null || configFile.isBlank()) {
			log.debug("No webResources.citation configured - the portal offers no citations.");
			return;
		}
		forms = parse(Paths.get(configFile));
		for (Form form : forms) {
			scriptExecutor.precompile(ScriptType.GROOVY, form.script());
		}
		log.info("Loaded {} citation form(s) from {}", forms.size(), configFile);
	}

	/** The configured forms in configured order; empty when no citation is configured. */
	public List<Form> getForms() {
		return forms;
	}

	/** Display name of a form in the reader's language. */
	public static String label(Form form, Locale locale) {
		return LocalizedText.pick(form.translations(), form.label(), locale);
	}

	/**
	 * Citations of one record: every form covering its type, in configured order.
	 * An empty result without a diagnostic means no form covers this record type.
	 */
	@Transactional(readOnly = true)
	public Citations citations(ApuEntity apu, Locale locale) {
		var items = new ArrayList<Citation>();
		String diagnostic = null;
		for (Form form : forms) {
			if (!form.apuTypes().contains(apu.getType())) {
				continue;
			}
			String text = null;
			String error = null;
			try {
				JsonNode rendered = run(form, apu, locale);
				text = citationOf(rendered);
				error = rendered.path("error").asText(null);
				if (text == null && error == null) {
					error = "the script returned no citation";
				}
			} catch (RuntimeException e) {
				// a broken script must not take the record's page down with it
				log.error("Citation form {} failed on {}", form.code(), apu.getUuid(), e);
				error = e.getMessage();
			}
			if (text != null) {
				items.add(new Citation(form.code(), label(form, locale), text));
			} else {
				log.warn("Citation form {} rendered nothing for {}: {}", form.code(), apu.getUuid(), error);
				diagnostic = diagnostic != null ? diagnostic : form.code() + ": " + error;
			}
		}
		return new Citations(List.copyOf(items), items.isEmpty() ? diagnostic : null);
	}

	/**
	 * The configured form whose script is the file of this name - how the frozen
	 * old API's {@code /script/{scriptName}/{id}} endpoint finds the script to
	 * run, instead of reading a path named in the request.
	 */
	public Optional<Form> formByScript(String fileName) {
		return forms.stream().filter(form -> Paths.get(form.scriptFile()).getFileName().toString().equals(fileName))
				.findFirst();
	}

	/**
	 * What a form's script produced, verbatim - the JSON carrying either
	 * {@code citation} or {@code error}. The old API republishes it as it is, so
	 * a deployment's script keeps whatever shape it has always had.
	 */
	@Transactional(readOnly = true)
	public String renderRaw(Form form, UUID apuUuid, Locale locale) {
		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", apuUuid);
		bindings.put("apuRepository", apuEntityRepository);
		bindings.put("objectMapper", objectMapper);
		bindings.put("lang", locale != null ? locale.toLanguageTag() : null);
		Object result = scriptExecutor.evaluate(ScriptType.GROOVY, form.script(), true, bindings, null);
		if (result == null) {
			throw new IllegalStateException("citation script " + form.scriptFile() + " returned nothing");
		}
		return result.toString();
	}

	private JsonNode run(Form form, ApuEntity apu, Locale locale) {
		String rendered = renderRaw(form, apu.getUuid(), locale);
		try {
			return objectMapper.readTree(rendered);
		} catch (IOException e) {
			throw new IllegalStateException(
					"citation script " + form.scriptFile() + " did not return JSON: " + rendered, e);
		}
	}

	private static String citationOf(JsonNode rendered) {
		String citation = rendered.path("citation").asText(null);
		return citation != null && !citation.isBlank() ? citation : null;
	}

	/**
	 * Reads and validates the configuration. Package-private so the rejection
	 * cases are covered without a Spring context.
	 */
	static List<Form> parse(Path path) {
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException("Configured webResources.citation does not exist: " + path);
		}
		Map<String, Object> config = readConfig(path);
		reject(config.keySet(), ROOT_KEYS);
		if (!(config.get("forms") instanceof List<?> entries) || entries.isEmpty()) {
			throw new IllegalStateException("citation: 'forms' must be a non-empty list of citation forms");
		}
		var translations = LocalizationFile.byCode(LocalizationFile.besides(path.toString()), "forms");
		var parsed = new ArrayList<Form>();
		var codes = new LinkedHashSet<String>();
		for (Object entry : entries) {
			if (!(entry instanceof Map<?, ?> form)) {
				throw new IllegalStateException("citation forms: each item must be a mapping");
			}
			reject(form.keySet(), FORM_KEYS);
			String code = required(form, "code");
			if (!codes.add(code)) {
				throw new IllegalStateException("citation forms: duplicate code '" + code + "'");
			}
			// validated in the order the file is written, so the first mistake reported is the first one made
			String label = required(form, "label");
			Path script = path.toAbsolutePath().getParent().resolve(required(form, "script"));
			if (!Files.isRegularFile(script)) {
				throw new IllegalStateException(
						"citation forms: form '" + code + "' names a script that does not exist: " + script);
			}
			parsed.add(new Form(code, label, translationsOf(translations.get(code)),
					apuTypes(form.get("apuTypes"), code), script.toString(), readScript(script)));
		}
		return List.copyOf(parsed);
	}

	private static List<ApuType> apuTypes(Object value, String code) {
		if (!(value instanceof List<?> values) || values.isEmpty()) {
			throw new IllegalStateException(
					"citation forms: form '" + code + "' must list the record types it covers in 'apuTypes'");
		}
		var types = new ArrayList<ApuType>();
		for (Object type : values) {
			try {
				types.add(ApuType.valueOf(String.valueOf(type)));
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("citation forms: form '" + code + "' names an unknown record type '"
						+ type + "' (known types: " + List.of(ApuType.values()) + ")", e);
			}
		}
		return List.copyOf(types);
	}

	private static List<LocalizedItem> translationsOf(Map<String, Object> byLanguage) {
		if (byLanguage == null) {
			return List.of();
		}
		var texts = new ArrayList<LocalizedItem>();
		byLanguage.forEach((language, label) -> {
			if (label instanceof String text && !text.isBlank()) {
				texts.add(new LocalizedItem(language, text));
			} else {
				log.warn("citation translations: {} carries no label - ignored.", language);
			}
		});
		return texts;
	}

	private static Map<String, Object> readConfig(Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			Map<String, Object> parsed = new Yaml().load(in);
			return parsed != null ? parsed : Map.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read citation configuration " + path, e);
		}
	}

	private static String readScript(Path script) {
		try {
			return Files.readString(script, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read citation script " + script, e);
		}
	}

	private static void reject(Set<?> present, Set<String> known) {
		for (Object entry : present) {
			if (!known.contains(String.valueOf(entry))) {
				throw new IllegalStateException(
						"citation: unknown key '" + entry + "' (known keys: " + String.join(", ", known) + ")");
			}
		}
	}

	private static String required(Map<?, ?> form, String key) {
		if (form.get(key) instanceof String value && !value.isBlank()) {
			return value;
		}
		throw new IllegalStateException("citation forms: an item has no '" + key + "'");
	}

}
