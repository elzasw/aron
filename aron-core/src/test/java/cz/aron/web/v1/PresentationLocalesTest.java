package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of the presentation-language negotiation (no Spring): a request's
 * lang is matched against the deployment's configured localizations, and
 * anything unusable falls back to the default rather than failing the request.
 */
class PresentationLocalesTest {

	private static PresentationLocales locales(String... localizations) {
		var loader = new UiConfigLoader("unused", "", new FacetScope(List.of()),
				new DeploymentImages(new MockHttpServletRequest(), ""));
		// only the configured localizations matter here - the rest of the page
		// template is not read (no file behind this loader)
		ReflectionTestUtils.setField(loader, "localizations", List.of(localizations));
		return new PresentationLocales(loader);
	}

	@Test
	void firstConfiguredLocalizationIsTheDefault() {
		var locales = locales("cs_CZ", "en");

		assertThat(locales.getDefault()).isEqualTo(Locale.of("cs", "CZ"));
		assertThat(locales.resolve(null)).isEqualTo(Locale.of("cs", "CZ"));
		assertThat(locales.resolve("  ")).isEqualTo(Locale.of("cs", "CZ"));
	}

	@Test
	void requestedLanguageWinsWhenConfigured() {
		var locales = locales("cs_CZ", "en");

		assertThat(locales.resolve("en")).isEqualTo(Locale.ENGLISH);
		// the underscore spelling of the configuration is accepted from clients too
		assertThat(locales.resolve("cs_CZ")).isEqualTo(Locale.of("cs", "CZ"));
	}

	@Test
	void regionalRequestMatchesTheConfiguredLanguage() {
		// RFC 4647 lookup: en-GB falls back to the configured plain en
		assertThat(locales("cs_CZ", "en").resolve("en-GB")).isEqualTo(Locale.ENGLISH);
	}

	@Test
	void plainRequestMatchesARegionalConfiguration() {
		// the other direction, which RFC 4647 lookup alone does not cover: the UI
		// asks per language (its bundles have no regions), the deployment may have
		// written the region it thinks in
		var locales = locales("cs_CZ", "en_US");

		assertThat(locales.resolve("en")).isEqualTo(Locale.of("en", "US"));
		assertThat(locales.resolve("cs")).isEqualTo(Locale.of("cs", "CZ"));
		// a different region of a configured language is still that language
		assertThat(locales.resolve("en-GB")).isEqualTo(Locale.of("en", "US"));
		// an unconfigured language still falls back
		assertThat(locales.resolve("de")).isEqualTo(Locale.of("cs", "CZ"));
	}

	@Test
	void acceptLanguageStyleListIsHonoured() {
		// quality values decide; the unsupported language is skipped
		assertThat(locales("cs_CZ", "en").resolve("de;q=0.9,en;q=0.8")).isEqualTo(Locale.ENGLISH);
		// same order when the match is by language only
		assertThat(locales("cs_CZ", "en_US").resolve("de;q=0.9,en;q=0.8")).isEqualTo(Locale.of("en", "US"));
		// the wildcard is not a language - it takes the deployment's default
		assertThat(locales("cs_CZ", "en_US").resolve("*")).isEqualTo(Locale.of("cs", "CZ"));
	}

	@Test
	void exactMatchStillWinsOverTheLanguageOnlyFallback() {
		// with both en_GB and en_US configured, en-GB must not be answered by en_US
		var locales = locales("cs_CZ", "en_US", "en_GB");

		assertThat(locales.resolve("en-GB")).isEqualTo(Locale.of("en", "GB"));
		assertThat(locales.resolve("en-US")).isEqualTo(Locale.of("en", "US"));
		// a bare request takes the first configured region of that language
		assertThat(locales.resolve("en")).isEqualTo(Locale.of("en", "US"));
	}

	@Test
	void unknownOrMalformedLanguageFallsBackInsteadOfFailing() {
		var locales = locales("cs_CZ", "en");

		assertThat(locales.resolve("de")).isEqualTo(Locale.of("cs", "CZ"));
		assertThat(locales.resolve("!!!")).isEqualTo(Locale.of("cs", "CZ"));
	}

	@Test
	void invalidConfigurationFailsFast() {
		assertThatThrownBy(() -> locales("!!!"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("localizations");
	}

}
