package cz.aron.search.relevance;

import java.util.List;

/**
 * Engine-neutral plan of one fulltext query, produced by
 * {@link RelevanceQueryPlanner} and translated mechanically by every search
 * adapter (doc/search-relevance.md §4.7).
 *
 * <p>The {@code gate} decides WHAT matches: its clauses (one per query token or
 * quoted phrase, all against the allText catch-all) run in filter context -
 * non-scoring - combined with {@code minimumShouldMatch}. The {@code scoring}
 * clauses decide the ORDER: weighted should-clauses that never affect recall.
 *
 * @param gate               non-scoring match clauses (weight is meaningless here)
 * @param minimumShouldMatch how many gate clauses must match (1..gate.size())
 * @param scoring            weighted ranking clauses
 */
public record RelevancePlan(List<Clause> gate, int minimumShouldMatch, List<Clause> scoring) {

	/** How a clause matches its field. */
	public enum MatchKind {
		/** Exact term (already normalized/analyzed by the planner). */
		TERM,
		/** Term prefix (begins-with; already normalized). */
		PREFIX,
		/** Exact phrase; engines analyze {@code text} with the field's analyzer. */
		PHRASE,
		/** All analyzed words must match the field. */
		ALL_TERMS,
		/** Any analyzed word matches the field (BM25 scores more matches higher). */
		ANY_TERM,
	}

	/** One match clause; {@code weight} applies to scoring clauses only. */
	public record Clause(String field, MatchKind kind, String text, float weight) {
	}

	/** The zero-hit retry: same clauses, any single one suffices (B7). */
	public RelevancePlan relaxed() {
		return new RelevancePlan(gate, 1, scoring);
	}

	public boolean relaxable() {
		return minimumShouldMatch > 1;
	}

}
