package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.aron.domain.types.LocalizedText;
import cz.aron.domain.types.dto.LocalizedItem;

/**
 * One piece of deployment-configured display text, written either as a plain
 * string (the source language) or as a language &rarr; text mapping:
 *
 * <pre>
 * label: Porta fontium
 * label: { cs: Matriky, en: Parish registers }
 * </pre>
 *
 * Inline rather than in a {@code _localization.yaml} sibling, which is the
 * documented exception for configuration that has no stable code to key a
 * sibling file on - the footer links' precedent (a link is identified by its
 * URL, a home-page tile by nothing at all).
 * <p>
 * The first entry of a mapping doubles as the fallback, so a reader whose
 * language the deployment did not translate still sees the text rather than
 * nothing.
 */
record ConfiguredText(String fallback, List<LocalizedItem> translations) {

	/** Parses a configured value; {@code null} when nothing usable is there. */
	static ConfiguredText of(Object node) {
		if (node == null) {
			return null;
		}
		if (!(node instanceof java.util.Map<?, ?> byLanguage)) {
			// a scalar is the source language; numbers included, because YAML turns
			// a label like 1820 into one on its own
			String value = String.valueOf(node).trim();
			return value.isEmpty() ? null : new ConfiguredText(value, List.of());
		}
		var translations = new ArrayList<LocalizedItem>();
		byLanguage.forEach((language, text) -> {
			if (text != null && !String.valueOf(text).isBlank()) {
				translations.add(new LocalizedItem(String.valueOf(language), String.valueOf(text)));
			}
		});
		if (translations.isEmpty()) {
			return null;
		}
		return new ConfiguredText(translations.get(0).text(), translations);
	}

	/** The text for one reader's language. */
	String pick(Locale locale) {
		return LocalizedText.pick(translations, fallback, locale);
	}

	/**
	 * Every string {@link #pick(Locale)} can return. Validation of configured
	 * text has to cover all of them: which one a reader gets depends on their
	 * language, so a mistake in one variant is a mistake in the configuration.
	 */
	List<String> variants() {
		var variants = new ArrayList<String>();
		variants.add(fallback);
		for (LocalizedItem translation : translations) {
			if (translation.text() != null && !variants.contains(translation.text())) {
				variants.add(translation.text());
			}
		}
		return variants;
	}

}
