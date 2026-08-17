package cz.aron.search.relevance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import cz.aron.search.ApuDocumentBuilder;
import cz.aron.search.relevance.RelevancePlan.Clause;
import cz.aron.search.relevance.RelevancePlan.MatchKind;

/**
 * Plans one fulltext query (doc/search-relevance.md §4.2): parses the minimal
 * user syntax (quoted phrases, trailing {@code word*} prefix, everything else
 * literal), tokenizes with ONE canonical analyzer, and emits the non-scoring
 * gate plus the weighted scoring tiers. Pure logic - unit-tested without an
 * engine or Spring.
 *
 * <p>Tokenization deliberately uses the Lucene analysis library (standard
 * tokenizer + lowercase + ASCII folding + the {@code _czech_} stop set): it is
 * the exact chain both engines run at index time - Elasticsearch embeds these
 * very classes - so gate tokens match indexed terms by construction, not by
 * imitation. The stop-filtered chain is canonical (the most aggressive - no
 * scoring field can drop a token the gate requires); a query whose tokens are
 * all stop words falls back to the non-stop chain, so it stays answerable
 * against allText, which keeps stop words (B5).
 */
public final class RelevanceQueryPlanner {

	/** Gate and baseline field: the multi-valued catch-all of every searchable value. */
	public static final String ALL_TEXT = "allText";

	/** At most this many query tokens are used; extra tokens are ignored (B12). */
	private static final int MAX_TOKENS = 32;

	private static final Pattern PHRASE = Pattern.compile("\"([^\"]*)\"");

	private static final CharArraySet CZECH_STOP_SET = CzechAnalyzer.getDefaultStopSet();

	private static final Analyzer CANONICAL = foldingAnalyzer(true);

	private static final Analyzer NON_STOP = foldingAnalyzer(false);

	private RelevanceQueryPlanner() {
	}

	/**
	 * Plans the query; {@code null} when it contains nothing searchable (the
	 * caller treats that as match-all).
	 */
	public static RelevancePlan plan(String query, RelevanceConfig config) {
		if (query == null || query.isBlank()) {
			return null;
		}

		// balanced "..." pairs are phrases; an unbalanced quote is literal (the
		// analyzers drop it anyway) - B4
		var phrases = new ArrayList<String>();
		Matcher matcher = PHRASE.matcher(query);
		var remainder = new StringBuilder();
		int consumed = 0;
		while (matcher.find()) {
			remainder.append(query, consumed, matcher.start());
			if (!matcher.group(1).isBlank()) {
				phrases.add(matcher.group(1));
			}
			consumed = matcher.end();
		}
		remainder.append(query.substring(consumed));

		// a trailing star marks a begins-with word; stars elsewhere are literal
		// (the analyzers drop them) - B6
		var prefixTokens = new ArrayList<String>();
		var plainWords = new StringBuilder();
		for (String word : remainder.toString().split("\\s+")) {
			if (word.length() > 1 && word.endsWith("*") && word.indexOf('*') == word.length() - 1) {
				String normalized = ApuDocumentBuilder.normalize(word.substring(0, word.length() - 1));
				if (normalized != null && !normalized.isBlank()) {
					prefixTokens.add(normalized);
				}
			} else {
				plainWords.append(word).append(' ');
			}
		}

		// canonical tokens; stop-word-only queries fall back to the non-stop chain (B5)
		List<String> tokens = analyze(CANONICAL, plainWords.toString());
		if (tokens.isEmpty() && phrases.isEmpty() && prefixTokens.isEmpty()) {
			tokens = analyze(NON_STOP, plainWords.toString());
		}

		var gate = new ArrayList<Clause>();
		for (String token : tokens) {
			if (gate.size() >= MAX_TOKENS) {
				break;
			}
			gate.add(new Clause(ALL_TEXT, MatchKind.TERM, token, 0));
		}
		for (String prefix : prefixTokens) {
			if (gate.size() >= MAX_TOKENS) {
				break;
			}
			gate.add(new Clause(ALL_TEXT, MatchKind.PREFIX, prefix, 0));
		}
		for (String phrase : phrases) {
			if (gate.size() >= MAX_TOKENS) {
				break;
			}
			gate.add(new Clause(ALL_TEXT, MatchKind.PHRASE, phrase, 0));
		}
		if (gate.isEmpty()) {
			return null;
		}

		int minimumShouldMatch = Math.clamp(
				Math.round(gate.size() * config.minimumShouldMatchPercent() / 100.0f), 1, gate.size());

		return new RelevancePlan(gate, minimumShouldMatch, scoring(query, phrases, config));
	}

	/** The weighted tiers of §4.2; a non-positive weight disables its tier. */
	private static List<Clause> scoring(String query, List<String> phrases, RelevanceConfig config) {
		// operators stripped: the plain text of the query for the analyzed tiers
		String plain = (query.replace("\"", " ").replace("*", " ")).trim().replaceAll("\\s+", " ");
		String normalizedCs = ApuDocumentBuilder.normalizeCs(plain);
		String normalized = ApuDocumentBuilder.normalize(plain);

		var scoring = new ArrayList<Clause>();
		add(scoring, "nameExactCs", MatchKind.TERM, normalizedCs, config.nameExactCs());
		add(scoring, "nameExact", MatchKind.TERM, normalized, config.nameExact());
		add(scoring, "nameExact", MatchKind.PREFIX, normalized, config.namePrefix());
		add(scoring, "name", MatchKind.PHRASE, plain, config.namePhrase());
		add(scoring, "name", MatchKind.ALL_TERMS, plain, config.nameTerms());
		for (String refLabelField : config.refLabelFields()) {
			add(scoring, refLabelField, MatchKind.PHRASE, plain, config.refLabelsPhrase());
			add(scoring, refLabelField, MatchKind.ANY_TERM, plain, config.refLabelsTerms());
		}
		add(scoring, "description", MatchKind.PHRASE, plain, config.descriptionPhrase());
		add(scoring, "description", MatchKind.ANY_TERM, plain, config.descriptionTerms());
		add(scoring, ALL_TEXT, MatchKind.ANY_TERM, plain, config.allTextTerms());
		for (RelevanceConfig.PromotedField promoted : config.promotedFields()) {
			add(scoring, promoted.field(), MatchKind.PHRASE, plain, promoted.phrase());
			add(scoring, promoted.field(), MatchKind.ANY_TERM, plain, promoted.terms());
		}
		// a quoted phrase additionally scores as a phrase in the name even when
		// the free text around it differs
		for (String phrase : phrases) {
			add(scoring, "name", MatchKind.PHRASE, phrase, config.namePhrase());
		}
		return scoring;
	}

	private static void add(List<Clause> scoring, String field, MatchKind kind, String text, float weight) {
		if (weight > 0 && text != null && !text.isBlank()) {
			scoring.add(new Clause(field, kind, text, weight));
		}
	}

	private static List<String> analyze(Analyzer analyzer, String text) {
		var tokens = new ArrayList<String>();
		if (text.isBlank()) {
			return tokens;
		}
		try (TokenStream stream = analyzer.tokenStream(ALL_TEXT, text)) {
			var term = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				tokens.add(term.toString());
			}
			stream.end();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return tokens;
	}

	private static Analyzer foldingAnalyzer(boolean stopWords) {
		return new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				var tokenizer = new StandardTokenizer();
				TokenStream stream = new LowerCaseFilter(tokenizer);
				if (stopWords) {
					stream = new StopFilter(stream, CZECH_STOP_SET);
				}
				stream = new ASCIIFoldingFilter(stream);
				return new TokenStreamComponents(tokenizer, stream);
			}
		};
	}

}
