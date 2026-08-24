package cz.aron.web.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.DaoFooter;

/**
 * The deployment's attribution of its digital objects
 * ({@code webResources.daoFooter}): a standing dedication line and the license
 * statements, matched to an object's license code with {@code default} as the
 * fallback - the gen-1 portal's daoFooter, moved server-side. Resolved per
 * record into the detail response, so clients render anchors the portal built
 * and never see raw configuration or match codes themselves.
 *
 * <pre>
 * dedication:
 *   text:
 *     cs: "Digitalizace probehla s podporou {donor}."
 *     en: "Digitized with the support of {donor}."
 *   links:
 *     donor: { label: NAKI II, url: "https://..." }
 * licenses:
 *   - code: default
 *     text: "Licence: {owner} | {terms}"
 *     links:
 *       owner: { label: { cs: Literarni archiv PNP, en: Literary Archives PNP }, url: "https://..." }
 *       terms: { label: { cs: Smluvni podminky, en: Terms of use }, url: "https://..." }
 *   - code: CC-BY-4.0
 *     text: "{license}"
 *     image: cc-by.svg          # a file of webResources.images
 *     links:
 *       license: { label: CC BY 4.0, url: "https://creativecommons.org/licenses/by/4.0/" }
 * </pre>
 *
 * Prose follows the home footer's rule - {@code {placeholder}}s over a link
 * map, never markup - and everything the configuration can get wrong fails the
 * startup: unknown keys, a duplicate or missing code, a placeholder mismatch in
 * any language, an image no configured directory serves. A footer is reached
 * with every digitized record, so a mistake found later is found by a reader.
 * No configured file = no attribution anywhere, which is what turns the
 * feature off.
 */
@Component
public class DaoFooterConfig {

	private static final Set<String> ROOT_KEYS = Set.of("dedication", "licenses");

	private static final Set<String> LICENSE_KEYS = Set.of("code", "text", "links", "image");

	/** The license code an object without one (or with an unknown one) falls back to. */
	private static final String DEFAULT_CODE = "default";

	private final ConfiguredParagraph dedication;

	private final Map<String, LicenseConfig> licenses;

	private record LicenseConfig(ConfiguredParagraph text, String image) {
	}

	public DaoFooterConfig(@Value("${webResources.daoFooter:}") String configFile, DeploymentImages images) {
		if (configFile == null || configFile.isBlank()) {
			dedication = null;
			licenses = Map.of();
			return;
		}
		Map<String, Object> root = readConfig(configFile);
		ConfigNodes.rejectAt(root.keySet(), ROOT_KEYS, "daoFooter");
		Predicate<String> imageIsServable = name -> images.resolve(name) != null;

		dedication = root.get("dedication") instanceof Map<?, ?> entry
				? ConfiguredParagraph.parse(entry, "daoFooter dedication")
				: null;
		if (root.get("dedication") != null && dedication == null) {
			throw new IllegalStateException("daoFooter dedication: must be a mapping with 'text' (and 'links')");
		}

		var parsed = new LinkedHashMap<String, LicenseConfig>();
		for (Map<?, ?> entry : ConfigNodes.mappingsAt(root.get("licenses"), "daoFooter licenses")) {
			ConfigNodes.rejectAt(entry.keySet(), LICENSE_KEYS, "daoFooter licenses");
			String code = ConfigNodes.text(entry.get("code"));
			if (code == null) {
				throw new IllegalStateException("daoFooter licenses: an entry has no 'code'");
			}
			if (parsed.containsKey(code)) {
				throw new IllegalStateException("daoFooter licenses: duplicate code '" + code + "'");
			}
			// the paragraph reader sees only its own keys; code and image are this entry's
			var paragraphNode = new LinkedHashMap<String, Object>();
			if (entry.get("text") != null) {
				paragraphNode.put("text", entry.get("text"));
			}
			if (entry.get("links") != null) {
				paragraphNode.put("links", entry.get("links"));
			}
			var text = ConfiguredParagraph.parse(paragraphNode, "daoFooter license '" + code + "'");
			String image = ConfigNodes.text(entry.get("image"));
			if (image != null && !imageIsServable.test(image)) {
				throw new IllegalStateException("daoFooter license '" + code + "': image '" + image
						+ "' is not served by any webResources.images directory");
			}
			parsed.put(code, new LicenseConfig(text, image));
		}
		licenses = parsed;

		if (dedication == null && licenses.isEmpty()) {
			throw new IllegalStateException(
					"daoFooter: the configured file declares neither a dedication nor licenses");
		}
	}

	/**
	 * The attribution of one digital object in the reader's language, or
	 * {@code null} when the deployment configures none. Image URLs are built per
	 * request (the one-jar rule), hence the operator.
	 */
	public DaoFooter footerFor(String licenseCode, Locale locale, UnaryOperator<String> imageUrl) {
		LicenseConfig license = licenses.get(licenseCode != null ? licenseCode : DEFAULT_CODE);
		if (license == null) {
			license = licenses.get(DEFAULT_CODE);
		}
		if (dedication == null && license == null) {
			return null;
		}
		var footer = new DaoFooter();
		// the generated model defaults its lists to empty; absent must be absent
		footer.setDedication(dedication != null ? dedication.runs(locale) : null);
		footer.setLicense(license != null ? license.text().runs(locale) : null);
		if (license != null && license.image() != null) {
			footer.setLicenseImage(imageUrl.apply(license.image()));
		}
		return footer;
	}

	private static Map<String, Object> readConfig(String configFile) {
		var path = Paths.get(configFile);
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException("Configured webResources.daoFooter does not exist: " + path);
		}
		try (InputStream in = Files.newInputStream(path)) {
			Map<String, Object> parsed = new Yaml().load(in);
			return parsed != null ? parsed : Map.of();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to read daoFooter " + configFile, e);
		}
	}

}
