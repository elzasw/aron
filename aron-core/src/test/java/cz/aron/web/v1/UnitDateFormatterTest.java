package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cz.aron.domain.UniversalDate;

/**
 * The rendering table of archival datings, per language - readable as a
 * specification rather than as assertions, because this is the output
 * archivists review. Dates come from CLDR, the vocabulary around them from the
 * unitdate resource bundles.
 */
class UnitDateFormatterTest {

	private static final String NBSP = "\u00A0";

	private static final String EN_DASH = "\u2013";

	private static UniversalDate dating(String from, String to, String format,
			boolean fromEstimated, boolean toEstimated) {
		var date = new UniversalDate();
		date.setFrom(from);
		date.setTo(to);
		date.setFormat(format);
		date.setValueFromEstimated(fromEstimated);
		date.setValueToEstimated(toEstimated);
		return date;
	}

	@ParameterizedTest(name = "[{index}] {2} {0}..{1} in cs = {3}")
	@CsvSource(delimiter = '|', value = {
			// from                 | to                    | format | expected (cs)
			" 1850-01-01T00:00:00 | 1910-12-31T23:59:59 | Y-Y  | 1850#1910          ",
			" 1850-01-01T00:00:00 | 1850-12-31T23:59:59 | Y-Y  | 1850               ",
			" 1850-05-01T00:00:00 | 1850-05-01T00:00:00 | D    | 1. 5. 1850         ",
			" 1850-05-01T00:00:00 | 1850-05-31T23:59:59 | YM   | kveten 1850        ",
			" 1801-01-01T00:00:00 | 1900-12-31T23:59:59 | C-C  | 19.@stoleti        ",
			" 1801-01-01T00:00:00 | 1950-12-31T23:59:59 | C-Y  | 19.@stoleti#1950   ",
			" 1850-05-01T14:30:00 | 1850-05-01T14:30:00 | DT   | 1. 5. 1850 14:30   ",
	})
	void czechRendering(String from, String to, String format, String expected) {
		assertThat(UnitDateFormatter.format(dating(from.trim(), to.trim(), format.trim(), false, false),
				Locale.of("cs", "CZ")))
				.isEqualTo(expand(expected.trim()));
	}

	@ParameterizedTest(name = "[{index}] {2} {0}..{1} in en = {3}")
	@CsvSource(delimiter = '|', value = {
			" 1850-01-01T00:00:00 | 1910-12-31T23:59:59 | Y-Y  | 1850#1910           ",
			" 1850-05-01T00:00:00 | 1850-05-01T00:00:00 | D    | May 1, 1850         ",
			" 1850-05-01T00:00:00 | 1850-05-31T23:59:59 | YM   | May 1850            ",
			" 1801-01-01T00:00:00 | 1900-12-31T23:59:59 | C-C  | 19th century        ",
			" 0001-01-01T00:00:00 | 0100-12-31T23:59:59 | C-C  | 1st century         ",
			" 2001-01-01T00:00:00 | 2100-12-31T23:59:59 | C-C  | 21st century        ",
	})
	void englishRendering(String from, String to, String format, String expected) {
		assertThat(UnitDateFormatter.format(dating(from.trim(), to.trim(), format.trim(), false, false),
				Locale.ENGLISH))
				.isEqualTo(expand(expected.trim()));
	}

	@Test
	void estimatedBoundsAreBracketed() {
		var date = dating("1850-01-01T00:00:00", "1910-12-31T23:59:59", "Y-Y", true, false);
		assertThat(UnitDateFormatter.format(date, Locale.of("cs", "CZ"))).isEqualTo("[1850]" + EN_DASH + "1910");

		var bothEstimated = dating("1850-01-01T00:00:00", "1910-12-31T23:59:59", "Y-Y", true, true);
		assertThat(UnitDateFormatter.format(bothEstimated, Locale.ENGLISH))
				.isEqualTo("[1850]" + EN_DASH + "[1910]");
	}

	@Test
	void untranslatedLanguageFallsBackToTheSourceLanguage() {
		// no unitdate_de bundle: German gets the base (Czech) vocabulary, but CLDR
		// still renders the German date - the fallback is the words, not the format
		var date = dating("1850-05-01T00:00:00", "1850-05-01T00:00:00", "D", false, false);
		assertThat(UnitDateFormatter.format(date, Locale.GERMAN)).isEqualTo("01.05.1850");

		var century = dating("1801-01-01T00:00:00", "1900-12-31T23:59:59", "C-C", false, false);
		assertThat(UnitDateFormatter.format(century, Locale.GERMAN)).isEqualTo("19." + NBSP + "stolet\u00ED");
	}

	@Test
	void oneSidedAndUnparseableDatingsSurvive() {
		assertThat(UnitDateFormatter.format(dating(null, "1910-12-31T23:59:59", "Y-Y", false, false),
				Locale.of("cs", "CZ"))).isEqualTo("1910");
		assertThat(UnitDateFormatter.format(dating("1850-01-01T00:00:00", null, "Y-Y", false, false),
				Locale.of("cs", "CZ"))).isEqualTo("1850");
		// a bound that is not an ISO date-time is shown as stored rather than dropped
		assertThat(UnitDateFormatter.format(dating("nekdy davno", null, "Y", false, false),
				Locale.of("cs", "CZ"))).isEqualTo("nekdy davno");
	}

	/** The table stays ASCII-readable: # = en dash, @ = non-breaking space, and cs month names are folded. */
	private static String expand(String expected) {
		return expected.replace("#", EN_DASH).replace("@", NBSP).replace("kveten", "kv\u011Bten")
				.replace("stoleti", "stolet\u00ED");
	}

}
