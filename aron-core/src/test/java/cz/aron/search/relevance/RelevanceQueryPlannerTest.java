package cz.aron.search.relevance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.aron.domain.facets.dto.RelevanceFieldWeightsDto;
import cz.aron.domain.facets.dto.RelevanceSettingsDto;
import cz.aron.search.relevance.RelevancePlan.Clause;
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

	@Test
	void nothingSearchableMeansNoPlan() {
		assertThat(plan(null)).isNull();
		assertThat(plan("   ")).isNull();
	}

	@Test
	void tokensAreFoldedAndGateAsSubstrings() {
		// R-15: long-enough tokens gate as all-trigrams substring matches on the
		// allTextGrams companion ("ardub" - Pardubice)
		var plan = plan("Václav NOVÁK");

		// pre-split trigrams: what the *Grams fields hold, all-of-them semantics
		assertThat(plan.gate()).containsExactly(
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "vac acl cla lav", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "nov ova vak", 0));
		// strict default: every token must match
		assertThat(plan.minimumShouldMatch()).isEqualTo(2);
	}

	@Test
	void shortTokensMustMatchWholeWords() {
		// below relevance.partialMinLength (default 3) a token stays exact - a
		// two-letter fragment must not match inside every longer word
		var plan = plan("sv Praze");
		assertThat(plan.gate()).containsExactly(
				new Clause("allText", MatchKind.TERM, "sv", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "pra raz aze", 0));
	}

	@Test
	void stopWordsAreDroppedButNeverCauseEmptyGates() {
		// B5: "v" disappears from a mixed query, the required count follows
		var mixed = plan("kostel v Praze");
		assertThat(mixed.gate()).extracting(Clause::text)
				.containsExactly("kos ost ste tel", "pra raz aze");
		assertThat(mixed.minimumShouldMatch()).isEqualTo(2);

		// a stop-word-only query falls back to the non-stop chain; single
		// letters match whole words, never as prefixes
		var stopOnly = plan("v");
		assertThat(stopOnly.gate()).containsExactly(new Clause("allText", MatchKind.TERM, "v", 0));
	}

	@Test
	void quotedPhrasesBecomePhraseClauses() {
		// B4: balanced quotes = phrase (exact words); the words around it stay tokens
		var plan = plan("zápis \"kronika města\"");
		assertThat(plan.gate()).containsExactly(
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "zap api pis", 0),
				new Clause("allText", MatchKind.PHRASE, "kronika města", 0));

		// an unbalanced quote is literal (the analyzers drop it)
		var unbalanced = plan("kronika \"města");
		assertThat(unbalanced.gate()).extracting(Clause::kind)
				.containsOnly(MatchKind.ALL_TERMS);
		assertThat(unbalanced.gate()).extracting(Clause::text)
				.containsExactly("kro ron oni nik ika", "mes est sta");
	}

	@Test
	void starsHaveNoMeaning() {
		// B6 (R-14/R-15): the former word* operator is gone - partial matching
		// is automatic; stars anywhere are dropped by the analyzers
		var plan = plan("kron* *ika ko*stel");
		assertThat(plan.gate()).containsExactly(
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "kro ron", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "ika", 0),
				new Clause("allText", MatchKind.TERM, "ko", 0),
				new Clause("allTextGrams", MatchKind.ALL_TERMS, "ste tel", 0));
	}

	@Test
	void tokensBeyondTheCapAreIgnored() {
		// B12: at most 32 tokens
		var words = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			words.append("slovo").append(i).append(' ');
		}
		var plan = plan(words.toString());
		assertThat(plan.gate()).hasSize(32);
		assertThat(plan.minimumShouldMatch()).isEqualTo(32);
	}

	@Test
	void minimumShouldMatchFollowsTheConfiguredPercent() {
		var settings = new RelevanceSettingsDto();
		settings.setMinimumShouldMatch("50%");
		var config = RelevanceConfig.withSettings(settings, List.of(), List.of(), QueryAnalyzers.DEFAULT);

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
		var config = RelevanceConfig.withSettings(settings, List.of("REL~ENTITY~LABEL"),
				List.of(new RelevanceConfig.PromotedField("TITLE~MAIN", 60, 30)), QueryAnalyzers.DEFAULT);

		var plan = RelevanceQueryPlanner.plan("kronika", config);
		assertThat(plan.scoring()).contains(
				new Clause("nameExact", MatchKind.TERM, "kronika", 500),
				new Clause("REL~ENTITY~LABEL", MatchKind.PHRASE, "kronika", 12),
				new Clause("REL~ENTITY~LABEL", MatchKind.ANY_TERM, "kronika", 10),
				new Clause("TITLE~MAIN", MatchKind.PHRASE, "kronika", 60),
				new Clause("TITLE~MAIN", MatchKind.ANY_TERM, "kronika", 30));
		assertThat(plan.scoring()).extracting(Clause::field, Clause::kind)
				.doesNotContain(org.assertj.core.groups.Tuple.tuple("name", MatchKind.ALL_TERMS));
	}

	@Test
	void stopWordsFollowTheLanguageOfTheDescribedMaterial() {
		// the gate drops the language's own stop words: Czech "v", German "und"
		var czech = RelevanceConfig.withSettings(null, List.of(), List.of(),
				QueryAnalyzers.of(java.util.Locale.of("cs", "CZ")));
		var german = RelevanceConfig.withSettings(null, List.of(), List.of(),
				QueryAnalyzers.of(java.util.Locale.GERMAN));

		assertThat(gateTerms(RelevanceQueryPlanner.plan("kostel v praze", czech)))
				.containsExactly("kos ost ste tel", "pra raz aze");
		// a German corpus keeps "v" (not a German stop word) and drops "und" instead
		assertThat(gateTerms(RelevanceQueryPlanner.plan("kostel v praze", german)))
				.containsExactly("kos ost ste tel", "v", "pra raz aze");
		assertThat(gateTerms(RelevanceQueryPlanner.plan("kirche und turm", german)))
				.containsExactly("kir irc rch che", "tur urm");
	}

	@Test
	void aLanguageWithoutAListKeepsEveryWord() {
		var slovak = RelevanceConfig.withSettings(null, List.of(), List.of(),
				QueryAnalyzers.of(java.util.Locale.forLanguageTag("sk")));

		assertThat(gateTerms(RelevanceQueryPlanner.plan("kostol v prahe", slovak)))
				.containsExactly("kos ost sto tol", "v", "pra rah ahe");
	}

	private static List<String> gateTerms(RelevancePlan plan) {
		return plan.gate().stream().map(Clause::text).toList();
	}
}
