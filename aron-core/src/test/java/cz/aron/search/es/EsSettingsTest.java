package cz.aron.search.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/**
 * Validates es_settings.json against the Elasticsearch client's own schema
 * without a server.
 * <p>
 * The client rejects unknown keys, and index settings are otherwise only parsed
 * when an index is created - so a malformed analysis section used to surface
 * exclusively in the opt-in es-it run, long after the change that caused it.
 * This keeps that failure in the default suite.
 */
class EsSettingsTest {

	private static String settings(Locale locale) {
		try {
			// the adapter's own substitution, so the test validates what it sends
			return ElasticsearchSearchIndex.renderSettings(
					new ClassPathResource("elasticsearch/es_settings.json")
							.getContentAsString(StandardCharsets.UTF_8),
					locale);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static IndexSettings parse(String json) {
		var mapper = new JacksonJsonpMapper();
		try (var parser = mapper.jsonProvider().createParser(new StringReader(json))) {
			return IndexSettings._DESERIALIZER.deserialize(parser, mapper);
		}
	}

	@Test
	void theShippedSettingsAreWhatElasticsearchAccepts() {
		// the file nests everything under "index", which the client keeps nested too
		var analysis = parse(settings(Locale.of("cs", "CZ"))).index().analysis();

		assertThat(analysis).isNotNull();
		assertThat(analysis.analyzer()).containsKeys("folding_and_tokenizing",
				"folding_and_tokenizing_stop", "folding_stop_and_stem", "folding", "text_long_keyword");
		assertThat(analysis.filter()).containsKeys("stop_filter", "content_stem");
	}

	@ParameterizedTest
	@ValueSource(strings = { "cs", "de", "en", "ru", "sk" })
	void everyContentLocaleProducesSettingsThatParse(String language) {
		// including a language with no stop list and no stemmer (sk), whose
		// substitutions are _none_ and the valid-but-unused placeholder
		assertThatCode(() -> parse(settings(Locale.forLanguageTag(language)))).doesNotThrowAnyException();
	}

	@Test
	void noKeyPretendsToBeAComment() throws Exception {
		// JSON has no comments, and Elasticsearch rejects keys its analysis types do
		// not declare ("Unknown field '_comment'") - which only shows up against a
		// real server, since parsing the file offline tolerates the extra key
		var names = new java.util.ArrayList<String>();
		collectFieldNames(new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(settings(Locale.of("cs", "CZ"))), names);

		assertThat(names).allSatisfy(name -> assertThat(name)
				.as("es_settings.json may carry no explanatory keys - put the note in the Java code")
				.doesNotStartWith("_"));
	}

	private static void collectFieldNames(com.fasterxml.jackson.databind.JsonNode node, List<String> names) {
		node.properties().forEach(entry -> {
			names.add(entry.getKey());
			collectFieldNames(entry.getValue(), names);
		});
		node.forEach(child -> collectFieldNames(child, names));
	}

	@Test
	void thePlaceholdersAreAlwaysSubstituted() {
		// an unsubstituted token would reach Elasticsearch as a literal name
		assertThat(settings(Locale.of("cs", "CZ"))).doesNotContain("__STOP_WORDS__", "__STEMMER_LANGUAGE__");
		assertThat(settings(Locale.forLanguageTag("sk"))).doesNotContain("__STOP_WORDS__", "__STEMMER_LANGUAGE__");
	}

}
