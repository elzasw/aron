package cz.aron.web.v1;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ResourceBundle;

import cz.aron.domain.UniversalDate;

/**
 * Formats a UNITDATE value ({@link UniversalDate}) into its display string in
 * the reader's language: per-side formats {@code C} (century), {@code Y}
 * (year), {@code YM} (month + year), {@code D} (date), {@code DT} (date-time);
 * estimated bounds in brackets; equal sides collapse to a single value.
 * <p>
 * Two sources of language, deliberately separated. Dates themselves come from
 * the JDK's CLDR data ({@link DateTimeFormatter#ofLocalizedDate} and localized
 * month names), so a new language needs no date patterns written by hand. The
 * archival vocabulary around them - how a century reads, how an estimate is
 * marked, what separates the sides of an interval - is not something CLDR
 * knows, so it lives in the {@code unitdate} resource bundles, one per
 * language. Adding a language is then a properties file, not code.
 * <p>
 * The bundle lookup is fallback-free on purpose: without it an untranslated
 * language would silently pick up the server's own default locale, making the
 * output depend on where the application happens to run. Untranslated falls
 * back to the base bundle (Czech, the source language) - the same rule the
 * display-model labels follow.
 */
final class UnitDateFormatter {

	private static final String BUNDLE = "cz/aron/web/v1/unitdate";

	private static final ResourceBundle.Control NO_FALLBACK = ResourceBundle.Control
			.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

	private UnitDateFormatter() {
	}

	/** Display string of the dating; {@code null} when the value has no bounds. */
	static String format(UniversalDate date, Locale locale) {
		ResourceBundle texts = ResourceBundle.getBundle(BUNDLE, locale, NO_FALLBACK);

		String from = formatBound(date.getFrom(), precisionOf(date, true), date.isValueFromEstimated(), locale, texts);
		String to = formatBound(date.getTo(), precisionOf(date, false), date.isValueToEstimated(), locale, texts);
		if (from == null) {
			return to;
		}
		if (to == null || to.equals(from)) {
			return from;
		}
		return message(texts, "range", locale, from, to);
	}

	/** Per-side format code of the stored {@code format} ("Y-Y", "C", "YM-D", …). */
	static String precisionOf(UniversalDate date, boolean lowerBound) {
		String format = date.getFormat();
		int separator = format != null ? format.indexOf('-') : -1;
		String side = separator < 0 ? format : (lowerBound ? format.substring(0, separator) : format.substring(separator + 1));
		return side != null && !side.isBlank() ? side : "D";
	}

	private static String formatBound(String value, String precision, boolean estimated, Locale locale,
			ResourceBundle texts) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String text = formatValue(value, precision, locale, texts);
		return estimated ? message(texts, "estimated", locale, text) : text;
	}

	private static String formatValue(String value, String precision, Locale locale, ResourceBundle texts) {
		try {
			LocalDateTime dateTime = LocalDateTime.parse(value);
			return switch (precision) {
				case "C" -> message(texts, "century", locale, (dateTime.getYear() + 99) / 100);
				case "Y" -> String.valueOf(dateTime.getYear());
				case "YM" -> message(texts, "monthYear", locale,
						Month.of(dateTime.getMonthValue()).getDisplayName(TextStyle.FULL_STANDALONE, locale),
						String.valueOf(dateTime.getYear()));
				// MEDIUM is the numeric-but-complete civil form in every locale
				case "D" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(dateTime);
				default -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
						.withLocale(locale).format(dateTime);
			};
		} catch (Exception e) {
			// unparseable bound - show the raw value rather than nothing
			return value;
		}
	}

	private static String message(ResourceBundle texts, String key, Locale locale, Object... arguments) {
		return new MessageFormat(texts.getString(key), locale).format(arguments);
	}

}
