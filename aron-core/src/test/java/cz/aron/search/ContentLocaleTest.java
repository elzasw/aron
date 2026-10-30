package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the configurable search locale (no Spring): the alphabet that
 * orders results comes from {@code search.content-locale}, and a change of it has to be
 * visible in the schema fingerprint so the startup bootstrap reindexes.
 */
class ContentLocaleTest {

	@Test
	void czechSortKeyFollowsTheCzechAlphabet() {
		var czech = new ContentLocale("cs-CZ");
		// Czech files the digraph "ch" between h and i
		assertThat(czech.sortKey("Cibule")).isLessThan(czech.sortKey("Hrad"));
		assertThat(czech.sortKey("Hrad")).isLessThan(czech.sortKey("Chalupa"));
		assertThat(czech.sortKey("Chalupa")).isLessThan(czech.sortKey("Ivan"));
		// diacritics do not derail the ordering
		assertThat(czech.sortKey("Čáp")).isGreaterThan(czech.sortKey("Cibule"))
				.isLessThan(czech.sortKey("Hrad"));
	}

	@Test
	void anotherLocaleOrdersByItsOwnAlphabet() {
		var english = new ContentLocale("en-GB");
		// no digraph in English: "ch" is just c + h, so Chalupa precedes Cibule
		assertThat(english.sortKey("Chalupa")).isLessThan(english.sortKey("Cibule"));
		assertThat(english.sortKey("Cibule")).isLessThan(english.sortKey("Hrad"));
	}

	@Test
	void nullNameHasNoSortKey() {
		assertThat(new ContentLocale("cs-CZ").sortKey(null)).isNull();
	}

	@Test
	void longNameSortKeyStaysWithinTheTermLimit() {
		var czech = new ContentLocale("cs-CZ");
		String longName = "Příliš žluťoučký kůň úpěl ďábelské ódy. ".repeat(150);
		String key = czech.sortKey(longName);
		// Lucene rejects a keyword term / sorted doc value over 32766 bytes
		assertThat(key.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThan(32766);
		// only the prefix counts, so the name is cut, not the key
		assertThat(key).isEqualTo(czech.sortKey(longName.substring(0, ContentLocale.SORT_KEY_MAX_LENGTH)));
		// and long names still order by their prefix
		assertThat(czech.sortKey("A" + longName)).isLessThan(czech.sortKey("B" + longName));
	}

	@Test
	void invalidLanguageTagFailsFast() {
		assertThatThrownBy(() -> new ContentLocale("!!!"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("search.content-locale");
	}

	@Test
	void languageTagIsPartOfTheSchemaFingerprint() {
		long czech = SearchIndexManager.schemaCrc(42L, new ContentLocale("cs-CZ").getLanguageTag());
		long english = SearchIndexManager.schemaCrc(42L, new ContentLocale("en-GB").getLanguageTag());
		// same types.yaml, different alphabet - the bootstrap must rebuild and reindex
		assertThat(czech).isNotEqualTo(english);
		// and the types.yaml side still counts
		assertThat(czech).isNotEqualTo(SearchIndexManager.schemaCrc(43L, "cs-CZ"));
	}

}
