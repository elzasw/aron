package cz.aron.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Translations of a deployment's configuration file, kept in a sibling file
 * rather than inline: {@code <config>.yaml} is accompanied by
 * {@code <config>_localization.yaml}. The configured file keeps one language -
 * the source language, which stays the fallback - and every further language is
 * added without touching it.
 * <p>
 * The file is optional throughout: a deployment that adds none behaves exactly
 * as before. A present but unreadable one fails the startup, because a
 * half-loaded translation set is worse than a missing one.
 */
public final class LocalizationFile {

	private static final Logger log = LoggerFactory.getLogger(LocalizationFile.class);

	private static final String SUFFIX = "_localization.yaml";

	private LocalizationFile() {
	}

	/**
	 * Contents of the translation file belonging to {@code configFile}; an empty
	 * map when there is none.
	 */
	public static Map<String, Object> besides(String configFile) {
		Path path = pathBesides(configFile);
		if (!Files.isRegularFile(path)) {
			return Map.of();
		}
		try (InputStream in = Files.newInputStream(path)) {
			Map<String, Object> parsed = new Yaml().load(in);
			log.info("Loaded translations {}", path);
			return parsed != null ? parsed : Map.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read translations " + path, e);
		}
	}

	/** {@code .../searchConfig.yaml} -> {@code .../searchConfig_localization.yaml}. */
	static Path pathBesides(String configFile) {
		Path path = Paths.get(configFile);
		String name = path.getFileName().toString();
		int extension = name.lastIndexOf('.');
		String base = extension > 0 ? name.substring(0, extension) : name;
		Path parent = path.getParent();
		return parent != null ? parent.resolve(base + SUFFIX) : Paths.get(base + SUFFIX);
	}

	/**
	 * One {@code language -> code -> value} section of a translation file, read
	 * as {@code code -> language -> value} so a caller can resolve one item.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Map<String, Object>> byCode(Map<String, Object> translations, String section) {
		if (!(translations.get(section) instanceof Map<?, ?> languages)) {
			return Map.of();
		}
		var byCode = new java.util.LinkedHashMap<String, Map<String, Object>>();
		for (var entry : ((Map<String, Object>) languages).entrySet()) {
			if (!(entry.getValue() instanceof Map<?, ?> codes)) {
				continue;
			}
			for (var coded : ((Map<String, Object>) codes).entrySet()) {
				byCode.computeIfAbsent(coded.getKey(), k -> new java.util.LinkedHashMap<>())
						.put(entry.getKey(), coded.getValue());
			}
		}
		return Collections.unmodifiableMap(byCode);
	}

}
