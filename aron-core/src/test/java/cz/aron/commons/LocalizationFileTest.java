package cz.aron.commons;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the configuration-translation convention: the sibling file's
 * name is derived from the configured one, and its {@code language -> code}
 * sections are read per item.
 */
class LocalizationFileTest {

	@Test
	void translationsSitNextToTheirConfigFile() {
		assertThat(LocalizationFile.pathBesides("./config/searchConfig.yaml").getFileName())
				.hasToString("searchConfig_localization.yaml");
		// a deployment that renames its config file gets the matching sibling
		assertThat(LocalizationFile.pathBesides("/opt/aron/facets.yml").getFileName())
				.hasToString("facets_localization.yaml");
		assertThat(LocalizationFile.pathBesides("types.yaml").getFileName())
				.hasToString("types_localization.yaml");
	}

	@Test
	void missingTranslationsAreNotAnError() {
		assertThat(LocalizationFile.besides("./config/does-not-exist.yaml")).isEmpty();
	}

	@Test
	void sectionsAreRegroupedFromLanguageFirstToCodeFirst() {
		Map<String, Object> translations = Map.of("facets", Map.of(
				"en", Map.of("LANG_CODE", Map.of("title", "Language"), "TEST_FACET", Map.of("title", "Test")),
				"de", Map.of("LANG_CODE", Map.of("title", "Sprache"))));

		var byCode = LocalizationFile.byCode(translations, "facets");

		assertThat(byCode).containsOnlyKeys("LANG_CODE", "TEST_FACET");
		assertThat(byCode.get("LANG_CODE")).containsOnlyKeys("en", "de");
		assertThat(byCode.get("TEST_FACET")).containsOnlyKeys("en");
	}

	@Test
	void anAbsentOrMalformedSectionYieldsNothing() {
		assertThat(LocalizationFile.byCode(Map.of(), "facets")).isEmpty();
		assertThat(LocalizationFile.byCode(Map.of("facets", "not a mapping"), "facets")).isEmpty();
	}

}
