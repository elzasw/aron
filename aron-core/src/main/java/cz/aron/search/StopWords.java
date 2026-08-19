package cz.aron.search;

import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stop words of the described material's language ({@code search.content-locale}).
 * <p>
 * Both engines must drop the same words, or a query analyzed one way would be
 * matched against terms indexed another way. This class is the single place
 * where that pairing is decided: one entry names the Lucene set (used by the
 * query planner and, through it, the embedded engine) and Elasticsearch's
 * predefined list of the same language. The two are the same word list -
 * Elasticsearch's {@code _czech_} is Lucene's Czech set - so adding a language
 * means adding one line, and it is not possible to add it to one side only.
 * <p>
 * A language nobody has a list for gets none: matching is then stricter than
 * ideal (every word of the query must occur), which is the safe direction -
 * removing another language's stop words would silently drop real words.
 */
public final class StopWords {

	private static final Logger log = LoggerFactory.getLogger(StopWords.class);

	/** One language's stop words on both engines. */
	private record Language(CharArraySet lucene, String elasticsearchList) {
	}

	/** Elasticsearch's way of saying "do not remove anything". */
	public static final String ELASTICSEARCH_NONE = "_none_";

	private static final Map<String, Language> BY_LANGUAGE = Map.ofEntries(
			Map.entry("cs", new Language(CzechAnalyzer.getDefaultStopSet(), "_czech_")),
			Map.entry("da", new Language(DanishAnalyzer.getDefaultStopSet(), "_danish_")),
			Map.entry("de", new Language(GermanAnalyzer.getDefaultStopSet(), "_german_")),
			Map.entry("en", new Language(EnglishAnalyzer.getDefaultStopSet(), "_english_")),
			Map.entry("es", new Language(SpanishAnalyzer.getDefaultStopSet(), "_spanish_")),
			Map.entry("fr", new Language(FrenchAnalyzer.getDefaultStopSet(), "_french_")),
			Map.entry("hu", new Language(HungarianAnalyzer.getDefaultStopSet(), "_hungarian_")),
			Map.entry("it", new Language(ItalianAnalyzer.getDefaultStopSet(), "_italian_")),
			Map.entry("nl", new Language(DutchAnalyzer.getDefaultStopSet(), "_dutch_")),
			Map.entry("no", new Language(NorwegianAnalyzer.getDefaultStopSet(), "_norwegian_")),
			Map.entry("ru", new Language(RussianAnalyzer.getDefaultStopSet(), "_russian_")),
			Map.entry("sv", new Language(SwedishAnalyzer.getDefaultStopSet(), "_swedish_")));

	private StopWords() {
	}

	/** Stop words for the query side; empty when the language has no list. */
	public static CharArraySet of(Locale locale) {
		Language language = BY_LANGUAGE.get(locale.getLanguage());
		if (language == null) {
			log.info("No stop-word list for {} - queries will require every word to occur.",
					locale.toLanguageTag());
			return CharArraySet.EMPTY_SET;
		}
		return language.lucene();
	}

	/** Name of Elasticsearch's predefined list, or {@link #ELASTICSEARCH_NONE}. */
	public static String elasticsearchList(Locale locale) {
		Language language = BY_LANGUAGE.get(locale.getLanguage());
		return language != null ? language.elasticsearchList() : ELASTICSEARCH_NONE;
	}

	/** Languages both engines can drop stop words for. */
	public static boolean isSupported(Locale locale) {
		return BY_LANGUAGE.containsKey(locale.getLanguage());
	}

}
