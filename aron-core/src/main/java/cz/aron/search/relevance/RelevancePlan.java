package cz.aron.search.relevance;

import java.util.List;

/**
 * Engine-neutral plan of one fulltext query, produced by
 * {@link RelevanceQueryPlanner} and translated mechanically by every search
 * adapter (doc/search-relevance.md §4.7).
 *
 * <p>The {@code gate} decides WHAT matches: one slot per query token or quoted
 * phrase, run in filter context - non-scoring - combined with
 * {@code minimumShouldMatch}. The {@code scoring} clauses decide the ORDER:
 * weighted should-clauses that never affect recall.
 *
 * @param gate               non-scoring match slots (weights are meaningless here)
 * @param minimumShouldMatch how many gate slots must match (1..gate.size())
 * @param scoring            weighted ranking clauses
 */
public record RelevancePlan(List<GateClause> gate, int minimumShouldMatch, List<Clause> scoring) {

	/**
	 * One gate slot - a query token or quoted phrase. It matches when ANY of
	 * its alternatives does (a token may match as a substring or as a
	 * stem-equal word, R-17); {@code minimumShouldMatch} counts slots, never
	 * alternatives, so adapters wrap a multi-alternative slot in its own
	 * any-of query.
	 */
	public record GateClause(List<Clause> anyOf) {

		public static GateClause of(Clause... alternatives) {
			return new GateClause(List.of(alternatives));
		}
	}

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
