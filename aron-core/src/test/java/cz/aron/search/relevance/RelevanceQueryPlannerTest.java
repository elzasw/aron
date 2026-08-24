package cz.aron.search.relevance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import cz.aron.domain.facets.dto.RelevanceFieldWeightsDto;
import cz.aron.domain.facets.dto.RelevanceSettingsDto;
import cz.aron.search.relevance.RelevancePlan.Clause;
import cz.aron.search.relevance.RelevancePlan.GateClause;
import cz.aron.search.relevance.RelevancePlan.MatchKind;

/**
 * Unit tests of the query planner (no Spring, no engine): the user syntax,
 * canonical tokenization with the stop-word fallback, the token cap, gate
 * composition and weight resolution (doc/search-relevance.md §4.2, pinning the
 * planner half of the behavior specification §5).
 */
class RelevanceQueryPlannerTest {

	private static final RelevanceConfig DEFAULTS = RelevanceConfig.defaults();

	private static RelevancePlan plan(String query) {
		return RelevanceQueryPlanner.plan(query, DEFAULTS);
	}

	/** First alternative of every gate slot - the trigram/term half of each token. */
	private static List<Clause> gateFirst(RelevancePlan plan) {
		return plan.gate().stream().map(slot -> slot.anyOf().get(0)).toList();
	}

	private static List<String> gateTexts(RelevancePlan plan) {
		return gateFirst(plan).stream().map(Clause::text).toList();
	}

	@Test
	void nothingSearchableMeansNoPlan() {
		assertThat(plan(null)).isNull();
		assertThat(plan("   ")).isNull();
	}

	@Test
	void tokensAreFoldedAndGateAsSubstrings() {
		// R-15: long-enough tokens gate as all-trigrams substring matches on the
		// allTextGrams companion ("ardub" - Pardubice); with the Czech stemmer
		// each token alternatively matches as a stem-equal whole word (R-17)
		var plan = plan("Václav NOVÁK");

		// pre-split trigrams: what the *Grams fields hold, all-of-them semantics
		assertThat(gateFirst(plan)).containsExactly(
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "vac acl cla lav", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "nov ova vak", 0));
		assertThat(plan.gate()).allSatisfy(slot -> {
			assertThat(slot.anyOf()).hasSize(2);
			assertThat(slot.anyOf().get(1).field()).isEqualTo("allTextStemmed");
			assertThat(slot.anyOf().get(1).kind()).isEqualTo(MatchKind.TERM);
		});
		// strict default: every token must match
		assertThat(plan.minimumShouldMatch()).isEqualTo(2);
	}

	@Test
	void shortTokensMustMatchWholeWords() {
		// below relevance.partialMinLength (default 3) a token stays exact - a
		// two-letter fragment must not match inside every longer word
		var plan = plan("sv Praze");
		assertThat(plan.gate().get(0).anyOf())
				.containsExactly(new Clause("allText", MatchKind.TERM, "sv", 0));
		assertThat(plan.gate().get(1).anyOf().get(0))
				.isEqualTo(new Clause("allTextGrams", MatchKind.ALL_TERMS, "pra raz aze", 0));
	}

	@Test
	void tokensAlternativelyMatchStemEqualWords() {
		// R-17: "hradu" must find "hrad", which contains none of its trigrams -
		// the token's second gate alternative is its stemmed form as a whole word
		var plan = plan("hradu");
		assertThat(plan.gate().get(0).anyOf()).containsExactly(
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "hra rad adu", 0),
				new Clause("allTextStemmed", MatchKind.TERM, "hrad", 0));
		// the inflection-aware scoring tiers, engine-analyzed with the stemmed chain
		assertThat(plan.scoring()).contains(
				new Clause("nameStemmed", MatchKind.ALL_TERMS, "hradu", 40),
				new Clause("nameVariantsStemmed", MatchKind.ALL_TERMS, "hradu", 8),
				new Clause("allTextStemmed", MatchKind.ANY_TERM, "hradu", 1));
	}

	@Test
	void stemmingCanBeDisabledAndNeedsAStemmer() {
		// query-side off-switch: no stemmed alternatives, no stemmed tiers
		var settings = new RelevanceSettingsDto();
		settings.setStemming(false);
		var disabled = RelevanceConfig.withSettings(settings, List.of(), QueryAnalyzers.DEFAULT);
		var plan = RelevanceQueryPlanner.plan("hradu", disabled);
		assertThat(plan.gate().get(0).anyOf()).hasSize(1);
		assertThat(plan.scoring()).extracting(Clause::field)
				.doesNotContain("allTextStemmed", "nameStemmed", "nameVariantsStemmed");

		// a language without a stemmer (Slovak) never emits stemmed clauses
		var slovak = RelevanceConfig.withSettings(null, List.of(),
				QueryAnalyzers.of(java.util.Locale.forLanguageTag("sk")));
		assertThat(RelevanceQueryPlanner.plan("hradu", slovak).gate().get(0).anyOf()).hasSize(1);
	}

	@Test
	void stopWordsAreDroppedButNeverCauseEmptyGates() {
		// B5: "v" disappears from a mixed query, the required count follows
		var mixed = plan("kostel v Praze");
		assertThat(gateTexts(mixed)).containsExactly("kos ost ste tel", "pra raz aze");
		assertThat(mixed.minimumShouldMatch()).isEqualTo(2);

		// a stop-word-only query falls back to the non-stop chain; single
		// letters match whole words, never as prefixes - and fallback tokens
		// are stop words the stemmed field drops, so they gate unstemmed
		var stopOnly = plan("v");
		assertThat(stopOnly.gate())
				.containsExactly(GateClause.of(new Clause("allText", MatchKind.TERM, "v", 0)));
	}

	@Test
	void quotedPhrasesBecomePhraseClauses() {
		// B4: balanced quotes = phrase (exact words, never stemmed); the words
		// around it stay tokens
		var plan = plan("zápis \"kronika města\"");
		assertThat(plan.gate().get(0).anyOf().get(0))
				.isEqualTo(new Clause("allTextGrams", MatchKind.ALL_TERMS, "zap api pis", 0));
		assertThat(plan.gate().get(1).anyOf())
				.containsExactly(new Clause("allText", MatchKind.PHRASE, "kronika města", 0));

		// an unbalanced quote is literal (the analyzers drop it)
		var unbalanced = plan("kronika \"města");
		assertThat(gateFirst(unbalanced)).extracting(Clause::kind)
				.containsOnly(MatchKind.ALL_TERMS);
		assertThat(gateTexts(unbalanced)).containsExactly("kro ron oni nik ika", "mes est sta");
	}

	@Test
	void starsHaveNoMeaning() {
		// B6 (R-14/R-15): the former word* operator is gone - partial matching
		// is automatic; stars anywhere are dropped by the analyzers
		var plan = plan("kron* *ika ko*stel");
		assertThat(gateFirst(plan)).containsExactly(
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "kro ron", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "ika", 0),
				new Clause("allText", MatchKind.TERM, "ko", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "ste tel", 0));
	}

	@Test
	void tokensBeyondTheCapAreIgnored() {
		// B12: at most 32 tokens - in the gate AND in the scoring tiers' text
		var words = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			words.append("slovo").append(i).append(' ');
		}
		var plan = plan(words.toString());
		assertThat(plan.gate()).hasSize(32);
		assertThat(plan.minimumShouldMatch()).isEqualTo(32);
		var baseline = plan.scoring().stream()
				.filter(clause -> clause.field().equals("allText")).findFirst().orElseThrow();
		assertThat(baseline.text().split(" ")).hasSize(32);
	}

	@Test
	void aPastedSentenceGatesOnWholeWords() {
		// R-16/B6: partial matching is a typing pattern - above six tokens the
		// query is pasted text of complete words, gated whole-word so the clause
		// count cannot grow with word length; the per-token partial tiers stay
		// out for the same reason. With a stemmer, whole-word means any
		// inflected form (R-17): the gate carries the stemmed terms
		var plan = plan("Král Vladislav povoluje na žádost Viléma z Pernštejna vklad");
		assertThat(plan.gate()).allSatisfy(slot -> assertThat(slot.anyOf()).hasSize(1));
		assertThat(gateFirst(plan)).extracting(Clause::field).containsOnly("allTextStemmed");
		assertThat(gateFirst(plan)).extracting(Clause::kind).containsOnly(MatchKind.TERM);
		assertThat(gateTexts(plan))
				.containsExactly("kral", "vladislav", "povoluj", "zadost", "vilem", "pernstejn", "vklad");
		assertThat(plan.scoring()).extracting(Clause::field)
				.doesNotContain("nameGrams", "nameVariantsGrams");
	}

	@Test
	void aPastedSentenceGatesOnExactWordsWithoutAStemmer() {
		// the same paste under a locale with no stemmer: plain whole-word terms
		var slovak = RelevanceConfig.withSettings(null, List.of(),
				QueryAnalyzers.of(java.util.Locale.forLanguageTag("sk")));
		var plan = RelevanceQueryPlanner.plan(
				"Král Vladislav povoluje na žádost Viléma z Pernštejna vklad", slovak);
		assertThat(gateFirst(plan)).extracting(Clause::field).containsOnly("allText");
		assertThat(gateTexts(plan)).contains("kral", "vladislav", "povoluje", "pernstejna");
	}

	@Test
	void partialMatchingStillCoversSixTokens() {
		// the same citation one word shorter sits at the threshold: still the
		// type-ahead handling, trigram gates and partial tiers included
		var plan = plan("Král Vladislav povoluje na žádost Viléma z Pernštejna");
		assertThat(gateFirst(plan)).extracting(Clause::field).containsOnly("allTextGrams");
		assertThat(plan.scoring()).extracting(Clause::field).contains("nameGrams");
	}

	@Test
	void aVeryLongTokenContributesBoundedTrigrams() {
		// R-16: only the first 20 characters decompose - 18 trigrams at most,
		// however long the pasted token is
		assertThat(RelevanceQueryPlanner.trigrams("abcdefghijklmnopqrstuvwxyz"))
				.isEqualTo("abc bcd cde def efg fgh ghi hij ijk jkl klm lmn mno nop opq pqr qrs rst");
	}

	@Test
	void theClauseBudgetIsBoundedForAnyInput() {
		// R-16: engines cap a query's nested clauses (Lucene's IndexSearcher
		// default: 1024) and a pasted citation is an ordinary query, so the plan
		// must stay under the cap for ANY input. Leaves are counted
		// pessimistically: analyzed kinds one per word, TERM/PREFIX one each.
		var config = RelevanceConfig.withSettings(null, List.of(
				new RelevanceConfig.PromotedField("TITLE~MAIN", 60, 30),
				new RelevanceConfig.PromotedField("UNIT~CONTENT", 20, 10),
				new RelevanceConfig.PromotedField("REL~ENTITY~LABEL", 15, 12)), QueryAnalyzers.DEFAULT);

		// many long words - the worst pasted-text shape
		var pasted = new StringBuilder();
		for (int i = 0; i < 60; i++) {
			pasted.append("nejneobhospodarovavatelnejsi").append(i).append(' ');
		}
		// few very long words - the worst type-ahead shape (trigram fan-out)
		var fragments = "abcdefghijklmnopqrstuvwxyz1 abcdefghijklmnopqrstuvwxyz2 abcdefghijklmnopqrstuvwxyz3"
				+ " abcdefghijklmnopqrstuvwxyz4 abcdefghijklmnopqrstuvwxyz5 abcdefghijklmnopqrstuvwxyz6";

		assertThat(leafClauses(RelevanceQueryPlanner.plan(pasted.toString(), config))).isLessThan(1024);
		assertThat(leafClauses(RelevanceQueryPlanner.plan(fragments, config))).isLessThan(1024);
	}

	private static int leafClauses(RelevancePlan plan) {
		return Stream.concat(plan.gate().stream().flatMap(slot -> slot.anyOf().stream()),
						plan.scoring().stream())
				.mapToInt(clause -> switch (clause.kind()) {
					case TERM, PREFIX -> 1;
					case PHRASE, ALL_TERMS, ANY_TERM -> clause.text().split(" ").length;
				})
				.sum();
	}

	@Test
	void minimumShouldMatchFollowsTheConfiguredPercent() {
		var settings = new RelevanceSettingsDto();
		settings.setMinimumShouldMatch("50%");
		var config = RelevanceConfig.withSettings(settings, List.of(), QueryAnalyzers.DEFAULT);

		var plan = RelevanceQueryPlanner.plan("jedna dva tri ctyri", config);
		assertThat(plan.minimumShouldMatch()).isEqualTo(2);
		// relaxation drops to any-word
		assertThat(plan.relaxed().minimumShouldMatch()).isEqualTo(1);
	}

	@Test
	void scoringCarriesTheDefaultTiers() {
		var plan = plan("Řehoř");

		assertThat(plan.scoring()).contains(
				new Clause("nameExact", MatchKind.TERM, "řehoř", 1000),
				new Clause("nameExactFolded", MatchKind.TERM, "rehor", 800),
				new Clause("nameExactFolded", MatchKind.PREFIX, "rehor", 200),
				new Clause("name", MatchKind.PHRASE, "Řehoř", 100),
				new Clause("name", MatchKind.ALL_TERMS, "Řehoř", 50),
				// variant name forms: the same ladder, preferred ~ 5x a variant
				new Clause("nameVariantsExact", MatchKind.TERM, "řehoř", 200),
				new Clause("nameVariantsExactFolded", MatchKind.TERM, "rehor", 160),
				new Clause("nameVariantsExactFolded", MatchKind.PREFIX, "rehor", 40),
				new Clause("nameVariants", MatchKind.PHRASE, "Řehoř", 20),
				new Clause("nameVariants", MatchKind.ALL_TERMS, "Řehoř", 10),
				// per-token partial tiers (R-14/R-15): below every full-word tier,
				// word starts above mid-word containment
				new Clause("name", MatchKind.PREFIX, "rehor", 30),
				new Clause("nameGrams", MatchKind.ALL_TERMS, "reh eho hor", 15),
				new Clause("nameVariants", MatchKind.PREFIX, "rehor", 8),
				new Clause("nameVariantsGrams", MatchKind.ALL_TERMS, "reh eho hor", 4),
				// inflection-aware tiers (R-17): between full-form and partial
				new Clause("nameStemmed", MatchKind.ALL_TERMS, "Řehoř", 40),
				new Clause("nameVariantsStemmed", MatchKind.ALL_TERMS, "Řehoř", 8),
				new Clause("allTextStemmed", MatchKind.ANY_TERM, "Řehoř", 1),
				// the combined reference-labels field - never a clause pair per
				// ~LABEL field (R-16)
				new Clause("refLabels", MatchKind.PHRASE, "Řehoř", 12),
				new Clause("refLabels", MatchKind.ANY_TERM, "Řehoř", 10),
				new Clause("description", MatchKind.PHRASE, "Řehoř", 8),
				new Clause("description", MatchKind.ANY_TERM, "Řehoř", 2),
				new Clause("allText", MatchKind.ANY_TERM, "Řehoř", 1));
	}

	@Test
	void configuredWeightsAndPromotedFieldsOverrideTheDefaults() {
		var settings = new RelevanceSettingsDto();
		var name = new RelevanceFieldWeightsDto();
		name.setExact(500f);
		name.setTerms(0f); // zero disables the tier
		settings.setName(name);
		var config = RelevanceConfig.withSettings(settings,
				List.of(new RelevanceConfig.PromotedField("TITLE~MAIN", 60, 30)), QueryAnalyzers.DEFAULT);

		var plan = RelevanceQueryPlanner.plan("kronika", config);
		assertThat(plan.scoring()).contains(
				new Clause("nameExact", MatchKind.TERM, "kronika", 500),
				new Clause("refLabels", MatchKind.PHRASE, "kronika", 12),
				new Clause("refLabels", MatchKind.ANY_TERM, "kronika", 10),
				new Clause("TITLE~MAIN", MatchKind.PHRASE, "kronika", 60),
				new Clause("TITLE~MAIN", MatchKind.ANY_TERM, "kronika", 30));
		assertThat(plan.scoring()).extracting(Clause::field, Clause::kind)
				.doesNotContain(org.assertj.core.groups.Tuple.tuple("name", MatchKind.ALL_TERMS));
	}

	@Test
	void stopWordsFollowTheLanguageOfTheDescribedMaterial() {
		// the gate drops the language's own stop words: Czech "v", German "und"
		var czech = RelevanceConfig.withSettings(null, List.of(),
				QueryAnalyzers.of(java.util.Locale.of("cs", "CZ")));
		var german = RelevanceConfig.withSettings(null, List.of(),
				QueryAnalyzers.of(java.util.Locale.GERMAN));

		assertThat(gateTexts(RelevanceQueryPlanner.plan("kostel v praze", czech)))
				.containsExactly("kos ost ste tel", "pra raz aze");
		// a German corpus keeps "v" (not a German stop word) and drops "und" instead
		assertThat(gateTexts(RelevanceQueryPlanner.plan("kostel v praze", german)))
				.containsExactly("kos ost ste tel", "v", "pra raz aze");
		assertThat(gateTexts(RelevanceQueryPlanner.plan("kirche und turm", german)))
				.containsExactly("kir irc rch che", "tur urm");
	}

	@Test
	void aLanguageWithoutAListKeepsEveryWord() {
		var slovak = RelevanceConfig.withSettings(null, List.of(),
				QueryAnalyzers.of(java.util.Locale.forLanguageTag("sk")));

		assertThat(gateTexts(RelevanceQueryPlanner.plan("kostol v prahe", slovak)))
				.containsExactly("kos ost sto tol", "v", "pra rah ahe");
	}

}
