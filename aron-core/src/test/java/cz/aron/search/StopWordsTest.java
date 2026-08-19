package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of the stop words both engines share. The pairing itself is what
 * matters: a language either has a list on both sides or on neither, because a
 * query analyzed one way against terms indexed another way silently loses hits.
 */
class StopWordsTest {

	@ParameterizedTest
	@ValueSource(strings = { "cs", "de", "en", "fr", "ru", "sv" })
	void aSupportedLanguageHasBothSides(String language) {
		var locale = Locale.forLanguageTag(language);

		assertThat(StopWords.of(locale)).isNotEmpty();
		assertThat(StopWords.elasticsearchList(locale)).isNotEqualTo(StopWords.ELASTICSEARCH_NONE);
	}

	@Test
	void theListIsTheLanguagesOwn() {
		// Czech drops "a"/"v", German drops "und" - and neither drops the other's
		assertThat(StopWords.of(Locale.of("cs", "CZ")).contains("v")).isTrue();
		assertThat(StopWords.of(Locale.GERMAN).contains("und")).isTrue();
		assertThat(StopWords.of(Locale.GERMAN).contains("kronika")).isFalse();
		assertThat(StopWords.elasticsearchList(Locale.of("cs", "CZ"))).isEqualTo("_czech_");
		assertThat(StopWords.elasticsearchList(Locale.GERMAN)).isEqualTo("_german_");
	}

	@Test
	void aRegionalTagUsesItsLanguage() {
		assertThat(StopWords.elasticsearchList(Locale.forLanguageTag("de-AT"))).isEqualTo("_german_");
	}

	@Test
	void anUnknownLanguageRemovesNothingOnEitherSide() {
		// stricter matching is the safe direction; another language's list would drop real words
		var slovak = Locale.forLanguageTag("sk");

		assertThat(StopWords.isSupported(slovak)).isFalse();
		assertThat(StopWords.of(slovak)).isEmpty();
		assertThat(StopWords.elasticsearchList(slovak)).isEqualTo(StopWords.ELASTICSEARCH_NONE);
	}

	@Test
	void everySupportedLanguageNamesAnElasticsearchList() {
		// the two sides are declared together, so neither can be forgotten
		for (String language : List.of("cs", "da", "de", "en", "es", "fr", "hu", "it", "nl", "no", "ru", "sv")) {
			var locale = Locale.forLanguageTag(language);
			assertThat(StopWords.isSupported(locale)).as(language).isTrue();
			assertThat(StopWords.elasticsearchList(locale)).as(language).startsWith("_").endsWith("_");
		}
	}

}
