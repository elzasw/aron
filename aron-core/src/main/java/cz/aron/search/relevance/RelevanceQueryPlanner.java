package cz.aron.search.relevance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import cz.aron.search.ApuDocumentBuilder;
import cz.aron.search.relevance.RelevancePlan.Clause;
import cz.aron.search.relevance.RelevancePlan.MatchKind;

/**
 * Plans one fulltext query (doc/search-relevance.md §4.2): parses the minimal
 * user syntax (quoted phrases; everything else literal), tokenizes with ONE
 * canonical analyzer, and emits the non-scoring gate plus the weighted scoring
 * tiers. Tokens of at least {@code relevance.partialMinLength} letters match
 * as substrings of a word ("ardub" finds Pardubice, R-15) - expressed as an
 * all-trigrams match on the {@code *Grams} companion fields, which both
 * engines' ngram analyzers decompose identically; shorter tokens must match a
 * whole word. Pure logic - unit-tested without an engine or Spring.
 *
 * <p>Tokenization deliberately uses the Lucene analysis library (standard
 * tokenizer + lowercase + ASCII folding + the {@code _czech_} stop set): it is
 * the exact chain both engines run at index time - Elasticsearch embeds these
 * very classes - so gate tokens match indexed terms by construction, not by
 * imitation. The stop-filtered chain is canonical (the most aggressive - no
 * scoring field can drop a token the gate requires); a query whose tokens are
 * all stop words falls back to the non-stop chain, so it stays answerable
 * against allText, which keeps stop words (B5).
 *
 * <p>The plan's clause count is bounded for ANY input (R-16): both engines cap
 * a query's nested clauses (Lucene's {@code IndexSearcher} default: 1024) and a
 * pasted citation is an ordinary query - so the scoring tiers read the same
 * token-capped text as the gate, partial matching stops at
 * {@link #PARTIAL_MAX_TOKENS} tokens, and a token contributes at most the
 * trigrams of its first {@link #TRIGRAM_MAX_CHARS} characters. Pinned by the
 * budget test in {@code RelevanceQueryPlannerTest}.
 */
public final class RelevanceQueryPlanner {

	/** Gate and baseline field: the multi-valued catch-all of every searchable value. */
	public static final String ALL_TEXT = "allText";

	/** Trigram companion of allText - the substring gate field (R-15). */
	public static final String ALL_TEXT_GRAMS = "allTextGrams";

	/** Combined reference-labels field: the display labels of every resolved APU_REF item. */
	public static final String REF_LABELS = "refLabels";

	/** At most this many query tokens are used; extra tokens are ignored (B12). */
	private static final int MAX_TOKENS = 32;

	/**
	 * Partial matching (trigram gates, per-token partial tiers) applies only to
	 * queries of at most this many tokens (B6): a fragment is a typing pattern,
	 * and a longer query is pasted text made of complete words - matched
	 * whole-word exactly, and the one query shape whose per-character trigram
	 * fan-out could otherwise grow without bound. A near-miss paste (an
	 * inflected word or two) is still caught by the zero-hit relaxation.
	 */
	private static final int PARTIAL_MAX_TOKENS = 6;

	/** Longest token prefix decomposed into trigrams - bounds the clauses one token can cost. */
	private static final int TRIGRAM_MAX_CHARS = 20;

	private static final Pattern PHRASE = Pattern.compile("\"([^\"]*)\"");

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

		// no other operators: a star has no meaning (the analyzers drop it) - B6

		// canonical tokens; stop-word-only queries fall back to the non-stop chain (B5)
		List<String> tokens = analyze(config.analyzers().canonical(), remainder.toString());
		if (tokens.isEmpty() && phrases.isEmpty()) {
			tokens = analyze(config.analyzers().nonStop(), remainder.toString());
		}
		if (tokens.size() > MAX_TOKENS) {
			tokens = tokens.subList(0, MAX_TOKENS);
		}

		// partial matching applies to short queries only (see PARTIAL_MAX_TOKENS)
		boolean partial = tokens.size() <= PARTIAL_MAX_TOKENS;

		var gate = new ArrayList<Clause>();
		for (String token : tokens) {
			// automatic partial matching (R-15): a long-enough token matches
			// ANYWHERE inside a word ("ardub" finds Pardubice) - all of its
			// trigrams must be present; short tokens must match whole, so
			// stop-word-length fragments stay precise. The gate is non-scoring -
			// full-word tiers keep exact matches ranked first. The planner
			// decomposes the trigrams ITSELF: the engines' ngram analyzers emit
			// grams at one position, which ES's match query treats as synonyms
			// (OR) - pre-split grams keep the all-of-them semantics on both.
			gate.add(partial && token.length() >= config.partialMinLength()
					? new Clause(ALL_TEXT_GRAMS, MatchKind.ALL_TERMS, trigrams(token), 0)
					: new Clause(ALL_TEXT, MatchKind.TERM, token, 0));
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

		return new RelevancePlan(gate, minimumShouldMatch, scoring(query, phrases, tokens, partial, config));
	}

	/** The weighted tiers of §4.2; a non-positive weight disables its tier. */
	private static List<Clause> scoring(String query, List<String> phrases, List<String> tokens,
			boolean partial, RelevanceConfig config) {
		// operators stripped: the plain text of the query for the analyzed tiers,
		// capped at the gate's token budget so the tier cost is bounded too (R-16)
		String plain = capWords((query.replace("\"", " ").replace("*", " ")).trim().replaceAll("\\s+", " "),
				MAX_TOKENS);
		String normalized = ApuDocumentBuilder.normalize(plain);
		String normalizedFolded = ApuDocumentBuilder.normalizeFolded(plain);

		var scoring = new ArrayList<Clause>();
		add(scoring, "nameExact", MatchKind.TERM, normalized, config.nameExact());
		add(scoring, "nameExactFolded", MatchKind.TERM, normalizedFolded, config.nameExactFolded());
		add(scoring, "nameExactFolded", MatchKind.PREFIX, normalizedFolded, config.namePrefix());
		add(scoring, "name", MatchKind.PHRASE, plain, config.namePhrase());
		add(scoring, "name", MatchKind.ALL_TERMS, plain, config.nameTerms());
		// variant name forms (item types marked nameVariant): the same ladder one
		// level below the primary name - preferred ~ 5x a variant (§2)
		add(scoring, "nameVariantsExact", MatchKind.TERM, normalized, config.nameVariantsExact());
		add(scoring, "nameVariantsExactFolded", MatchKind.TERM, normalizedFolded, config.nameVariantsExactFolded());
		add(scoring, "nameVariantsExactFolded", MatchKind.PREFIX, normalizedFolded, config.nameVariantsPrefix());
		add(scoring, "nameVariants", MatchKind.PHRASE, plain, config.nameVariantsPhrase());
		add(scoring, "nameVariants", MatchKind.ALL_TERMS, plain, config.nameVariantsTerms());
		// per-token partial tiers: partially typed words still rank name bearers
		// first ("univ bratisl" - Univerzita Bratislava). Deliberately BELOW the
		// full-word tiers, so an exact match always outranks a partial one, and
		// a word-start match (PREFIX on the analyzed terms) above a mid-word one
		// (all-trigrams on the *Grams companion); a full word satisfies both, so
		// fully matching documents never fall behind. Short queries only - the
		// same rule as the gate (see PARTIAL_MAX_TOKENS)
		if (partial) {
			for (String token : tokens) {
				if (token.length() >= config.partialMinLength()) {
					add(scoring, "name", MatchKind.PREFIX, token, config.nameWordPrefix());
					add(scoring, "nameGrams", MatchKind.ALL_TERMS, trigrams(token), config.nameContains());
					add(scoring, "nameVariants", MatchKind.PREFIX, token, config.nameVariantsWordPrefix());
					add(scoring, "nameVariantsGrams", MatchKind.ALL_TERMS, trigrams(token),
							config.nameVariantsContains());
				}
			}
		}
		add(scoring, REF_LABELS, MatchKind.PHRASE, plain, config.refLabelsPhrase());
		add(scoring, REF_LABELS, MatchKind.ANY_TERM, plain, config.refLabelsTerms());
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

	/**
	 * The token's overlapping trigrams, space-separated ("pardub" - "par ard
	 * rdu dub"): what the {@code *Grams} fields hold per word, matched with
	 * all-of-them semantics. Callers guarantee length >= 3. A very long token
	 * contributes only its first {@link #TRIGRAM_MAX_CHARS} characters - a
	 * prefix-substring match, slightly wider recall for a bounded clause count
	 * (R-16); the gate is non-scoring, so ranking is unaffected.
	 */
	static String trigrams(String token) {
		if (token.length() > TRIGRAM_MAX_CHARS) {
			token = token.substring(0, TRIGRAM_MAX_CHARS);
		}
		if (token.length() <= 3) {
			return token;
		}
		var grams = new StringBuilder();
		for (int i = 0; i + 3 <= token.length(); i++) {
			if (i > 0) {
				grams.append(' ');
			}
			grams.append(token, i, i + 3);
		}
		return grams.toString();
	}

	/** First {@code limit} words of a whitespace-collapsed text (R-16). */
	private static String capWords(String text, int limit) {
		int spaces = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == ' ' && ++spaces == limit) {
				return text.substring(0, i);
			}
		}
		return text;
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

}
