package cz.aron.search;

import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cz.CzechStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stemmer of the described material's language ({@code search.content-locale})
 * - inflection-aware matching (doc/search-relevance.md R-17): "hrad" and
 * "hradu" stem to the same term, so a complete word matches its inflected
 * forms in both directions.
 * <p>
 * Both engines must stem identically, or a query stemmed one way would be
 * matched against terms stemmed another way. This class is the single place
 * where that pairing is decided (the {@link StopWords} pattern): one entry
 * names the Lucene stem filter and the language name under which
 * Elasticsearch's built-in {@code stemmer} token filter instantiates <b>the
 * same class</b> - so adding a language means adding one line, and it is not
 * possible to add it to one side only. Only built-in Elasticsearch filters, no
 * analysis plugins (the standing rule).
 * <p>
 * A language nobody has a stemmer for gets none: matching then requires the
 * exact word forms - stricter, the safe direction. <b>Adding a language later
 * changes what the stemmed fields hold for deployments of that locale while
 * the schema fingerprint stays the same</b> - a new entry must therefore be
 * accompanied by a document-layout version bump in both adapters, or existing
 * indexes would keep serving unstemmed fields.
 */
public final class Stemmers {

	private static final Logger log = LoggerFactory.getLogger(Stemmers.class);

	/** One language's stemmer on both engines. */
	private record Language(UnaryOperator<TokenStream> lucene, String elasticsearchName) {
	}

	private static final Map<String, Language> BY_LANGUAGE = Map.of(
			"cs", new Language(CzechStemFilter::new, "czech"));

	/**
	 * Substituted for locales without a stemmer, so es_settings.json stays a
	 * valid definition: the filter is then referenced by an analyzer no field is
	 * mapped with, so it never runs - it only has to parse.
	 */
	private static final String ELASTICSEARCH_PLACEHOLDER = "minimal_english";

	private Stemmers() {
	}

	/** Languages both engines can stem identically. */
	public static boolean isSupported(Locale locale) {
		return BY_LANGUAGE.containsKey(locale.getLanguage());
	}

	/**
	 * Language name for Elasticsearch's {@code stemmer} token filter; a
	 * valid-but-unused placeholder when the language has no stemmer (see
	 * {@link #ELASTICSEARCH_PLACEHOLDER}).
	 */
	public static String elasticsearchLanguage(Locale locale) {
		Language language = BY_LANGUAGE.get(locale.getLanguage());
		return language != null ? language.elasticsearchName() : ELASTICSEARCH_PLACEHOLDER;
	}

	/**
	 * The full stemming chain - tokenize, lowercase, stop, <b>stem, then
	 * fold</b> (the stemmer's rules work on the language's own letters, so it
	 * must run before diacritics folding). ONE construction point shared by the
	 * embedded engine's index side and the query planner, so the two sides
	 * cannot drift; it mirrors the {@code folding_stop_and_stem} analyzer of
	 * es_settings.json filter for filter. {@code null} when the language has no
	 * stemmer - callers then index no stemmed fields and plan no stemmed
	 * clauses.
	 */
	public static Analyzer analyzer(Locale locale) {
		Language language = BY_LANGUAGE.get(locale.getLanguage());
		if (language == null) {
			log.info("No stemmer for {} - matching requires the exact word forms.", locale.toLanguageTag());
			return null;
		}
		CharArraySet stopWords = StopWords.of(locale);
		return new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				var tokenizer = new StandardTokenizer();
				TokenStream stream = new LowerCaseFilter(tokenizer);
				if (!stopWords.isEmpty()) {
					stream = new StopFilter(stream, stopWords);
				}
				stream = language.lucene().apply(stream);
				stream = new ASCIIFoldingFilter(stream);
				return new TokenStreamComponents(tokenizer, stream);
			}

			@Override
			public int getPositionIncrementGap(String fieldName) {
				// ES's default gap for multi-valued text - phrases never match
				// across two values (the allText rule, §4.1)
				return 100;
			}
		};
	}

}
