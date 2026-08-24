package cz.aron.search.relevance;

import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import cz.aron.search.Stemmers;
import cz.aron.search.StopWords;

/**
 * The token chains the query planner runs, built once per content locale: the
 * canonical one (stop words removed), the fallback that keeps them for a query
 * made of nothing else, and - where the locale has a stemmer - the stemming
 * chain of the inflection-aware tiers (R-17).
 * <p>
 * Deliberately the same Lucene classes Elasticsearch itself embeds, so query
 * tokens match indexed terms by construction rather than by imitation.
 */
public final class QueryAnalyzers {

	/** For the default {@code search.content-locale} - what plain helpers and tests get. */
	public static final QueryAnalyzers DEFAULT = of(Locale.of("cs", "CZ"));

	private final Analyzer canonical;

	private final Analyzer nonStop;

	private final Analyzer stemming;

	private QueryAnalyzers(CharArraySet stopWords, Analyzer stemming) {
		this.canonical = foldingAnalyzer(stopWords);
		this.nonStop = foldingAnalyzer(CharArraySet.EMPTY_SET);
		this.stemming = stemming;
	}

	public static QueryAnalyzers of(Locale contentLocale) {
		return new QueryAnalyzers(StopWords.of(contentLocale), Stemmers.analyzer(contentLocale));
	}

	/** Stop words removed - the most aggressive chain, so the gate can never require a dropped token. */
	public Analyzer canonical() {
		return canonical;
	}

	/** Stop words kept - answers a query that consists of nothing else (B5). */
	public Analyzer nonStop() {
		return nonStop;
	}

	/**
	 * The stemming chain (stop + stem + fold, {@link Stemmers#analyzer});
	 * {@code null} when the content locale has no stemmer.
	 */
	public Analyzer stemming() {
		return stemming;
	}

	private static Analyzer foldingAnalyzer(CharArraySet stopWords) {
		return new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				var tokenizer = new StandardTokenizer();
				TokenStream stream = new LowerCaseFilter(tokenizer);
				if (!stopWords.isEmpty()) {
					stream = new StopFilter(stream, stopWords);
				}
				stream = new ASCIIFoldingFilter(stream);
				return new TokenStreamComponents(tokenizer, stream);
			}
		};
	}

}
