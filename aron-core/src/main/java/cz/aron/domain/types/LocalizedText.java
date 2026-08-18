package cz.aron.domain.types;

import java.util.List;
import java.util.Locale;

import cz.aron.domain.types.dto.LocalizedItem;

/**
 * Picks the display text of a types.yaml element for a presentation language.
 * The translations come from the optional {@code types_localization.yaml} next
 * to types.yaml; the element's own name is the source language and the last
 * resort, so an untranslated deployment keeps working unchanged.
 */
public final class LocalizedText {

	private LocalizedText() {
	}

	/**
	 * Translation for {@code locale}: an exact tag match wins, then a match on
	 * the bare language ({@code cs-CZ} accepts a {@code cs} translation), then
	 * {@code fallback}.
	 */
	public static String pick(List<LocalizedItem> translations, String fallback, Locale locale) {
		if (translations == null || translations.isEmpty() || locale == null) {
			return fallback;
		}
		String wanted = locale.toLanguageTag();
		String text = firstMatching(translations, tag -> tag.equalsIgnoreCase(wanted));
		if (text == null) {
			String language = locale.getLanguage();
			text = firstMatching(translations, tag -> tag.equalsIgnoreCase(language));
		}
		return text != null ? text : fallback;
	}

	private static String firstMatching(List<LocalizedItem> translations, java.util.function.Predicate<String> matches) {
		for (LocalizedItem translation : translations) {
			if (translation.lang() == null || translation.text() == null || translation.text().isBlank()) {
				continue;
			}
			if (matches.test(translation.lang().trim().replace('_', '-'))) {
				return translation.text();
			}
		}
		return null;
	}

}
