package cz.aron.web.v1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import cz.aron.domain.UniversalDate;

/**
 * Formats a UNITDATE value ({@link UniversalDate}) into its Czech display
 * string for the detail render model: per-side formats {@code C} (century),
 * {@code Y} (year), {@code YM} (month + year), {@code D} (date), {@code DT}
 * (date-time); estimated bounds in brackets; equal sides collapse to a single
 * value.
 */
final class UnitDateFormatter {

	private static final Locale CZECH = Locale.of("cs", "CZ");

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d. M. yyyy", CZECH);

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d. M. yyyy H:mm", CZECH);

	private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("LLLL yyyy", CZECH);

	private UnitDateFormatter() {
	}

	/** Display string of the dating; {@code null} when the value has no bounds. */
	static String format(UniversalDate date) {
		String fromFormat;
		String toFormat;
		String format = date.getFormat();
		int separator = format != null ? format.indexOf('-') : -1;
		if (separator > 0) {
			fromFormat = format.substring(0, separator);
			toFormat = format.substring(separator + 1);
		} else {
			fromFormat = format;
			toFormat = format;
		}

		String from = formatBound(date.getFrom(), fromFormat, date.isValueFromEstimated());
		String to = formatBound(date.getTo(), toFormat, date.isValueToEstimated());
		if (from == null) {
			return to;
		}
		if (to == null || to.equals(from)) {
			return from;
		}
		return from + "–" + to;
	}

	private static String formatBound(String value, String format, boolean estimated) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String text = formatValue(value, format != null && !format.isBlank() ? format : "D");
		return estimated ? "[" + text + "]" : text;
	}

	private static String formatValue(String value, String format) {
		try {
			LocalDateTime dateTime = LocalDateTime.parse(value);
			return switch (format) {
				case "C" -> ((dateTime.getYear() + 99) / 100) + ". století";
				case "Y" -> String.valueOf(dateTime.getYear());
				case "YM" -> MONTH_YEAR.format(dateTime);
				case "D" -> DATE.format(dateTime);
				default -> DATE_TIME.format(dateTime);
			};
		} catch (Exception e) {
			// unparseable bound - show the raw value rather than nothing
			return value;
		}
	}

}
