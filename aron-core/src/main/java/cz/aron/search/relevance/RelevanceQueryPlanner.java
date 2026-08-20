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
 * tiers. Tokens of at least {@code relevance.prefixMinLength} letters gate as
 * word prefixes ("pardub" finds Pardubice, R-14); shorter tokens must match a
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
 */
public final class RelevanceQueryPlanner {

	/** Gate and baseline field: the multi-valued catch-all of every searchable value. */
	public static final String ALL_TEXT = "allText";

	/** At most this many query tokens are used; extra tokens are ignored (B12). */
	private static final int MAX_TOKENS = 32;

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

		var gate = new ArrayList<Clause>();
		for (String token : tokens) {
			// automatic partial matching (R-14): a long-enough token matches the
			// BEGINNING of a word ("pardub" finds Pardubice); short tokens must
			// match whole, so stop-word-length fragments stay precise. The gate
			// is non-scoring - full-word tiers keep exact matches ranked first.
			gate.add(new Clause(ALL_TEXT,
					token.length() >= config.prefixMinLength() ? MatchKind.PREFIX : MatchKind.TERM, token, 0));
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

		return new RelevancePlan(gate, minimumShouldMatch, scoring(query, phrases, tokens, config));
	}

	/** The weighted tiers of §4.2; a non-positive weight disables its tier. */
	private static List<Clause> scoring(String query, List<String> phrases, List<String> tokens,
			RelevanceConfig config) {
		// operators stripped: the plain text of the query for the analyzed tiers
		String plain = (query.replace("\"", " ").replace("*", " ")).trim().replaceAll("\\s+", " ");
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
		// per-token word prefixes: partially typed words still rank name bearers
		// first ("univ bratisl" - Univerzita Bratislava). Deliberately BELOW the
		// full-word tiers, so an exact match always outranks a partial one; a
		// full word is a prefix of itself, so fully matching documents earn
		// these tiers too and never fall behind
		for (String token : tokens) {
			if (token.length() >= config.prefixMinLength()) {
				add(scoring, "name", MatchKind.PREFIX, token, config.nameWordPrefix());
				add(scoring, "nameVariants", MatchKind.PREFIX, token, config.nameVariantsWordPrefix());
			}
		}
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

}
