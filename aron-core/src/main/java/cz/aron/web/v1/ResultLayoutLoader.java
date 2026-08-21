package cz.aron.web.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.ResultFieldStyle;
import cz.aron.api.v1.model.ResultIcon;
import cz.aron.api.v1.model.ResultLayout;
import cz.aron.commons.LocalizationFile;
import cz.aron.domain.types.LocalizedText;
import cz.aron.domain.types.dto.LocalizedItem;
import jakarta.annotation.PostConstruct;

/**
 * Builds the typed {@link ResultLayout} of {@code /api/v1/search/result-layout}
 * from the deployment's {@code resultLayout.yaml}
 * ({@code webResources.resultLayout}).
 * <p>
 * The file says how the record and field codes carried by a structured result
 * are presented. The codes themselves come from the data - the source system
 * chose them - so the portal cannot know them and a code with no entry here
 * simply renders plain. The whole file is optional: without it structured
 * results still show, laid out by the client's own defaults.
 *
 * <pre>
 * fieldSeparator: " | "     # between fields of one row; client default " | "
 * iconSize: 35              # default width of the record icons, in CSS pixels
 * icons:                    # keyed by the record-shape code
 *   - code: A_IM
 *     image: archival-item-matrika.svg
 * fields:                   # keyed by the field code
 *   - code: N
 *     heading: true         # this field's text is the link to the record
 *     scale: 1.2
 *     bold: true
 *   - code: J_IC
 *     prefix: "Inv. č.: "
 *     image: J_IC.svg
 * </pre>
 *
 * {@code prefix} and {@code label} are display text, so they are translated in
 * the sibling {@code resultLayout_localization.yaml} ({@code fields:} section,
 * {@code language -> code -> {prefix, label}}); the configured file keeps the
 * source language and stays the fallback. An unknown key fails the startup -
 * a silently ignored styling key looks like a portal bug to whoever wrote it.
 */
@Component
public class ResultLayoutLoader {

	private static final Logger log = LoggerFactory.getLogger(ResultLayoutLoader.class);

	private static final Set<String> ROOT_KEYS = Set.of("fieldSeparator", "iconSize", "icons", "fields");

	private static final Set<String> FIELD_KEYS = Set.of("code", "heading", "prefix", "label", "valueSeparator",
			"color", "scale", "bold", "image");

	private static final Set<String> ICON_KEYS = Set.of("code", "image", "size");

	private final String layoutFile;

	private final DeploymentImages images;

	private String fieldSeparator;

	private List<FieldStyleConfig> fields = List.of();

	private List<IconConfig> icons = List.of();

	/** Parsed field styling; the localized texts are picked per request. */
	private record FieldStyleConfig(String code, Boolean heading, String prefix, List<LocalizedItem> prefixTexts,
			String label, List<LocalizedItem> labelTexts, String valueSeparator, String color, Float scale,
			Boolean bold, String image) {
	}

	private record IconConfig(String code, String image, Integer size) {
	}

	public ResultLayoutLoader(@Value("${webResources.resultLayout:}") String layoutFile, DeploymentImages images) {
		this.layoutFile = layoutFile;
		this.images = images;
	}

	@PostConstruct
	void load() {
		if (layoutFile == null || layoutFile.isBlank()) {
			log.debug("No webResources.resultLayout configured - structured results use the client defaults.");
			return;
		}
		Map<String, Object> layout = read();
		reject(layout.keySet(), ROOT_KEYS, "resultLayout");
		if (layout.get("fieldSeparator") instanceof String separator) {
			fieldSeparator = separator;
		}
		Integer defaultIconSize = intOf(layout.get("iconSize"), "resultLayout iconSize");
		var translations = LocalizationFile.byCode(LocalizationFile.besides(layoutFile), "fields");
		fields = readFields(layout.get("fields"), translations);
		icons = readIcons(layout.get("icons"), defaultIconSize);
		log.info("Loaded structured-result layout {} ({} fields, {} icons)", layoutFile, fields.size(), icons.size());
	}

	/** Typed layout for one reader's language; icon URLs are resolved per request. */
	public ResultLayout getLayout(Locale locale) {
		var styles = new ArrayList<ResultFieldStyle>();
		for (FieldStyleConfig field : fields) {
			var style = new ResultFieldStyle(field.code());
			style.setHeading(field.heading());
			style.setPrefix(LocalizedText.pick(field.prefixTexts(), field.prefix(), locale));
			// a field's own name for readers without the visual context; the visible
			// prefix is that name whenever one is configured
			String label = LocalizedText.pick(field.labelTexts(), field.label(), locale);
			style.setLabel(label != null ? label : style.getPrefix());
			style.setValueSeparator(field.valueSeparator());
			style.setColor(field.color());
			style.setScale(field.scale());
			style.setBold(field.bold());
			if (field.image() != null) {
				style.setIconUrl(images.url(field.image()));
			}
			styles.add(style);
		}
		var resolved = new ArrayList<ResultIcon>();
		for (IconConfig icon : icons) {
			var resultIcon = new ResultIcon(icon.code(), images.url(icon.image()));
			resultIcon.setSize(icon.size());
			resolved.add(resultIcon);
		}
		var layout = new ResultLayout(styles, resolved);
		layout.setFieldSeparator(fieldSeparator);
		return layout;
	}

	private List<FieldStyleConfig> readFields(Object entries, Map<String, Map<String, Object>> translations) {
		var parsed = new ArrayList<FieldStyleConfig>();
		for (Map<?, ?> entry : entriesOf(entries, "resultLayout fields")) {
			reject(entry.keySet(), FIELD_KEYS, "resultLayout fields");
			String code = requiredCode(entry, "resultLayout fields");
			String prefix = stringOf(entry.get("prefix"));
			String label = stringOf(entry.get("label"));
			Map<String, Object> byLanguage = translations.get(code);
			parsed.add(new FieldStyleConfig(code, booleanOf(entry.get("heading")), prefix,
					textsOf(byLanguage, "prefix", code), label, textsOf(byLanguage, "label", code),
					stringOf(entry.get("valueSeparator")), stringOf(entry.get("color")),
					floatOf(entry.get("scale"), "resultLayout fields scale"), booleanOf(entry.get("bold")),
					image(entry.get("image"), code)));
		}
		return parsed;
	}

	private List<IconConfig> readIcons(Object entries, Integer defaultSize) {
		var parsed = new ArrayList<IconConfig>();
		for (Map<?, ?> entry : entriesOf(entries, "resultLayout icons")) {
			reject(entry.keySet(), ICON_KEYS, "resultLayout icons");
			String code = requiredCode(entry, "resultLayout icons");
			String image = image(entry.get("image"), code);
			if (image == null) {
				throw new IllegalStateException("resultLayout icons: item '" + code + "' has no 'image'");
			}
			Integer size = intOf(entry.get("size"), "resultLayout icons size");
			parsed.add(new IconConfig(code, image, size != null ? size : defaultSize));
		}
		return parsed;
	}

	/**
	 * An image reference is a file name served from the configured image
	 * directory. Without that directory the reference cannot be honored, which is
	 * a configuration mistake worth stopping the startup for.
	 */
	private String image(Object value, String code) {
		String name = stringOf(value);
		if (name == null) {
			return null;
		}
		if (!images.isConfigured()) {
			throw new IllegalStateException("resultLayout: item '" + code + "' references the image '" + name
					+ "' but webResources.images is not configured");
		}
		return name;
	}

	private static List<LocalizedItem> textsOf(Map<String, Object> byLanguage, String key, String code) {
		if (byLanguage == null) {
			return List.of();
		}
		var texts = new ArrayList<LocalizedItem>();
		byLanguage.forEach((language, fields) -> {
			if (!(fields instanceof Map<?, ?> values)) {
				log.warn("resultLayout translations: {}/{} is not a mapping of prefix/label - ignored.", language,
						code);
				return;
			}
			if (values.get(key) instanceof String text && !text.isBlank()) {
				texts.add(new LocalizedItem(language, text));
			}
		});
		return texts;
	}

	private Map<String, Object> read() {
		var path = Paths.get(layoutFile);
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException("Configured webResources.resultLayout does not exist: " + layoutFile);
		}
		try (InputStream in = Files.newInputStream(path)) {
			Map<String, Object> parsed = new Yaml().load(in);
			return parsed != null ? parsed : Map.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read structured-result layout " + layoutFile, e);
		}
	}

	private static List<Map<?, ?>> entriesOf(Object value, String where) {
		if (value == null) {
			return List.of();
		}
		if (!(value instanceof List<?> entries)) {
			throw new IllegalStateException(where + ": expected a list");
		}
		var maps = new ArrayList<Map<?, ?>>();
		for (Object entry : entries) {
			if (!(entry instanceof Map<?, ?> map)) {
				throw new IllegalStateException(where + ": each item must be a mapping with a 'code' key");
			}
			maps.add(map);
		}
		return maps;
	}

	private static void reject(Set<?> present, Set<String> known, String where) {
		for (Object key : present) {
			if (!known.contains(String.valueOf(key))) {
				throw new IllegalStateException(
						where + ": unknown key '" + key + "' (known keys: " + String.join(", ", known) + ")");
			}
		}
	}

	private static String requiredCode(Map<?, ?> entry, String where) {
		String code = stringOf(entry.get("code"));
		if (code == null) {
			throw new IllegalStateException(where + ": an item has no 'code'");
		}
		return code;
	}

	private static String stringOf(Object value) {
		return value instanceof String text && !text.isBlank() ? text : null;
	}

	private static Boolean booleanOf(Object value) {
		return value instanceof Boolean flag ? flag : null;
	}

	private static Integer intOf(Object value, String where) {
		if (value == null) {
			return null;
		}
		if (!(value instanceof Number number)) {
			throw new IllegalStateException(where + ": expected a number, got '" + value + "'");
		}
		return number.intValue();
	}

	private static Float floatOf(Object value, String where) {
		if (value == null) {
			return null;
		}
		if (!(value instanceof Number number)) {
			throw new IllegalStateException(where + ": expected a number, got '" + value + "'");
		}
		return number.floatValue();
	}

}
