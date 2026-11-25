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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.ApuType;
import cz.aron.domain.types.TypesHolder;
import cz.aron.repository.ApuEntityRepository;
import jakarta.annotation.PostConstruct;

/**
 * Orders of archival material, as far as the portal takes part in them
 * ({@code webResources.order}): which records can be ordered, and what an order
 * says about one. Taking the order is not the portal's job - the reader's
 * browser sends it to the deployment's order service - so nothing here stores
 * or forwards one.
 *
 * <pre>
 * script: order-item-info.groovy   # resolved next to this file
 * apuTypes: [ARCH_DESC]            # record types that can be ordered
 * itemTypes: [STORAGE]             # optional: only records carrying one of these
 * </pre>
 *
 * What an order says about a record - which identifiers, how the storage unit
 * is named, which institution and reading room it goes to - is an agreement
 * between a deployment and its reading rooms, so the portal does not compose
 * it: it runs the deployment's script and publishes what that produces, as
 * with citations ({@link CitationService}). No configured file means the
 * portal offers no orders.
 * <p>
 * Everything the loader can check, it checks at startup - unknown keys, an
 * unknown record or item type, a missing script, a script that does not
 * compile - because an order is reached by a reader's click.
 * <p>
 * The script is given {@code id} (the record's uuid), {@code apuRepository},
 * {@code objectMapper} and {@code lang} - the bindings of a citation script -
 * and returns JSON carrying either {@code archivalie} + {@code institution}
 * (+ optional {@code badatelna}) or a diagnostic {@code error}.
 */
@Component
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private static final Set<String> KEYS = Set.of("script", "apuTypes", "itemTypes");

	private final String configFile;

	private final ScriptExecutor scriptExecutor;

	private final ApuEntityRepository apuEntityRepository;

	private final ObjectMapper objectMapper;

	private final TypesHolder typesHolder;

	/** {@code null} when the deployment configures no orders. */
	private Config config;

	/**
	 * The order configuration of one deployment. {@code itemTypes} are stored
	 * item-type codes (with {@code ~}); empty means every record of
	 * {@code apuTypes} can be ordered.
	 */
	public record Config(List<ApuType> apuTypes, List<String> itemTypes, String scriptFile, String script) {
	}

	/**
	 * A record no order can name: the diagnosis says which piece of its
	 * description was missing. A reader can do nothing about it; whoever looks
	 * into the data needs to know.
	 */
	public static class NotOrderableException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public NotOrderableException(String diagnostic) {
			super(diagnostic);
		}
	}

	public OrderService(@Value("${webResources.order:}") String configFile, ScriptExecutor scriptExecutor,
			ApuEntityRepository apuEntityRepository, ObjectMapper objectMapper, TypesHolder typesHolder) {
		this.configFile = configFile;
		this.scriptExecutor = scriptExecutor;
		this.apuEntityRepository = apuEntityRepository;
		this.objectMapper = objectMapper;
		this.typesHolder = typesHolder;
	}

	@PostConstruct
	void load() {
		if (configFile == null || configFile.isBlank()) {
			log.debug("No webResources.order configured - the portal offers no orders.");
			return;
		}
		config = parse(Paths.get(configFile), code -> typesHolder.getItemTypeForCode(code) != null);
		scriptExecutor.precompile(ScriptType.GROOVY, config.script());
		log.info("Orders are offered for {} (configured in {})", config.apuTypes(), configFile);
	}

	/** Whether this deployment offers orders at all. */
	public boolean isEnabled() {
		return config != null;
	}

	/**
	 * Whether the record can be put into an order: orders are on, its type is
	 * configured, and - where the deployment names item types - its description
	 * carries one of them. Invisible items count: invisible means "not shown on
	 * the record's page", not "absent".
	 */
	public boolean isOrderable(ApuEntity apu, List<ApuPart> parts) {
		if (config == null || !config.apuTypes().contains(apu.getType())) {
			return false;
		}
		return config.itemTypes().isEmpty() || carriesOneOf(parts, config.itemTypes());
	}

	private static boolean carriesOneOf(List<ApuPart> parts, List<String> itemTypes) {
		if (parts == null) {
			return false;
		}
		for (ApuPart part : parts) {
			if (part.getItems() != null) {
				for (ApuPartItem item : part.getItems()) {
					if (itemTypes.contains(item.getType())) {
						return true;
					}
				}
			}
			if (carriesOneOf(part.getChildParts(), itemTypes)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * What an order says about the record: the script's result, checked for the
	 * pieces an order cannot do without - the fund's number and the institution
	 * the order is addressed to. A script that reports an {@code error}, or
	 * leaves one of those out, is a {@link NotOrderableException}: the old portal
	 * put such an answer into the basket and failed later, on a record the reader
	 * had long left.
	 */
	@Transactional(readOnly = true)
	public JsonNode orderItem(ApuEntity apu, Locale locale) {
		if (config == null) {
			throw new NotOrderableException("orders are not configured");
		}
		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", apu.getUuid());
		bindings.put("apuRepository", apuEntityRepository);
		bindings.put("objectMapper", objectMapper);
		bindings.put("lang", locale != null ? locale.toLanguageTag() : null);
		Object result = scriptExecutor.evaluate(ScriptType.GROOVY, config.script(), true, bindings, null);
		if (result == null) {
			throw new IllegalStateException("order script " + config.scriptFile() + " returned nothing");
		}
		JsonNode item;
		try {
			item = objectMapper.readTree(result.toString());
		} catch (IOException e) {
			throw new IllegalStateException(
					"order script " + config.scriptFile() + " did not return JSON: " + result, e);
		}
		String diagnostic = item.path("error").asText(null);
		if (diagnostic == null) {
			diagnostic = missingPiece(item);
		}
		if (diagnostic != null) {
			log.warn("The order script described nothing for {}: {}", apu.getUuid(), diagnostic);
			throw new NotOrderableException(diagnostic);
		}
		return item;
	}

	private static String missingPiece(JsonNode item) {
		if (!item.path("archivalie").path("cisloNad").isIntegralNumber()) {
			return "the script returned no archivalie.cisloNad";
		}
		for (String field : List.of("name", "code")) {
			if (!item.path("institution").path(field).isTextual()
					|| item.path("institution").path(field).asText().isBlank()) {
				return "the script returned no institution." + field;
			}
		}
		return null;
	}

	/**
	 * Reads and validates the configuration. Package-private so the rejection
	 * cases are covered without a Spring context; {@code knownItemType} is asked
	 * about stored item-type codes.
	 */
	static Config parse(Path path, Predicate<String> knownItemType) {
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException("Configured webResources.order does not exist: " + path);
		}
		Map<String, Object> values = readConfig(path);
		for (String key : values.keySet()) {
			if (!KEYS.contains(key)) {
				throw new IllegalStateException(
						"order: unknown key '" + key + "' (known keys: " + String.join(", ", KEYS) + ")");
			}
		}
		if (!(values.get("script") instanceof String scriptName) || scriptName.isBlank()) {
			throw new IllegalStateException("order: 'script' is missing");
		}
		Path script = path.toAbsolutePath().getParent().resolve(scriptName);
		if (!Files.isRegularFile(script)) {
			throw new IllegalStateException("order: 'script' names a script that does not exist: " + script);
		}
		return new Config(apuTypes(values.get("apuTypes")), itemTypes(values.get("itemTypes"), knownItemType),
				script.toString(), readScript(script));
	}

	private static List<ApuType> apuTypes(Object value) {
		if (!(value instanceof List<?> values) || values.isEmpty()) {
			throw new IllegalStateException("order: 'apuTypes' must list the record types that can be ordered");
		}
		var types = new ArrayList<ApuType>();
		for (Object type : values) {
			try {
				types.add(ApuType.valueOf(String.valueOf(type)));
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("order: unknown record type '" + type + "' (known types: "
						+ List.of(ApuType.values()) + ")", e);
			}
		}
		return List.copyOf(types);
	}

	private static List<String> itemTypes(Object value, Predicate<String> knownItemType) {
		if (value == null) {
			return List.of();
		}
		if (!(value instanceof List<?> values)) {
			throw new IllegalStateException("order: 'itemTypes' must be a list of item type codes");
		}
		var codes = new ArrayList<String>();
		for (Object entry : values) {
			// written as in types.yaml, stored with '~' (see TypesLoader)
			String code = String.valueOf(entry).replace('_', '~');
			if (!knownItemType.test(code)) {
				throw new IllegalStateException(
						"order: 'itemTypes' names an item type types.yaml does not define: '" + entry + "'");
			}
			codes.add(code);
		}
		return List.copyOf(codes);
	}

	private static Map<String, Object> readConfig(Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			Map<String, Object> parsed = new Yaml().load(in);
			return parsed != null ? parsed : Map.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read order configuration " + path, e);
		}
	}

	private static String readScript(Path script) {
		try {
			return Files.readString(script, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read order script " + script, e);
		}
	}

}
