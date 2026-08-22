package cz.aron.search;

import java.util.List;

import cz.aron.search.relevance.RelevanceConfig;
import cz.aron.search.relevance.RelevancePlan;
import cz.aron.search.relevance.RelevanceQueryPlanner;

/**
 * Engine-neutral search request (the port's read side).
 *
 * <p>Multi-select facet semantics: hits and total respect ALL filters; the
 * buckets/bounds of a facet ignore the facet filters (Values, Range) sitting on
 * its own filter field, so an active selection can always be widened.
 *
 * @param apuType      restricts to one APU type ({@code null} = all types)
 * @param fulltext     planned fulltext query - a non-scoring gate plus weighted
 *                     scoring tiers (see {@link cz.aron.search.relevance.RelevanceQueryPlanner});
 *                     {@code null} = match all
 * @param filters      field-level filters, all must match (see {@link FieldFilter})
 * @param buckets      value-bucket count requests (see {@link BucketRequest})
 * @param bounds       dating bounds to compute, one per UNITDATE facet (see
 *                     {@link BoundsRequest})
 * @param from         zero-based offset of the first hit
 * @param size         page size (bounded by the caller)
 * @param sort         named sort mode
 * @param totalUpTo    accuracy limit of the result's total: matching documents
 *                     are counted exactly up to this value, beyond it the total
 *                     is reported as {@code (totalUpTo, GTE)}; {@code null} =
 *                     count exactly (callers resolve their configured default)
 * @param typeCounts   also count matching documents per APU type; the counts
 *                     respect the fulltext and all filters but IGNORE the
 *                     query's own {@code apuType} restriction (the user can
 *                     switch sections)
 */
public record ApuSearchQuery(String apuType, RelevancePlan fulltext, List<FieldFilter> filters,
		List<BucketRequest> buckets, List<BoundsRequest> bounds, int from, int size, SortMode sort,
		Integer totalUpTo, boolean typeCounts) {

	/** Variant without type counts. */
	public ApuSearchQuery(String apuType, RelevancePlan fulltext, List<FieldFilter> filters,
			List<BucketRequest> buckets, List<BoundsRequest> bounds, int from, int size, SortMode sort,
			Integer totalUpTo) {
		this(apuType, fulltext, filters, buckets, bounds, from, size, sort, totalUpTo, false);
	}

	/** Exact-total variant - the accuracy limit defaults to unlimited. */
	public ApuSearchQuery(String apuType, RelevancePlan fulltext, List<FieldFilter> filters,
			List<BucketRequest> buckets, List<BoundsRequest> bounds, int from, int size, SortMode sort) {
		this(apuType, fulltext, filters, buckets, bounds, from, size, sort, null, false);
	}

	/**
	 * Named sort modes (AUTO is resolved by the API layer before the port).
	 * Every mode ends in the uuid tie-break, so paging is stable; documents
	 * without the sorted value (no name, no dating) sort last in either
	 * direction.
	 */
	public enum SortMode {
		/** Score descending; ties by nameSort, then uuid. */
		RELEVANCE,
		/** Czech-alphabetical by name - engines sort by the index-time collation key (nameSort). */
		NAME,
		NAME_DESC,
		/** Earliest dating ({@code dateL}) ascending; undated last. */
		DATE_ASC,
		/** Latest dating ({@code dateH}) descending; undated last. */
		DATE_DESC
	}

	/**
	 * Value buckets of one field. {@code filterField} names the field whose own
	 * facet filters the counts ignore (multi-select) - usually the bucket field
	 * itself; reference facets enumerate the composite {@code <code>~ID~LABEL}
	 * field while their Values filter sits on {@code <code>}. {@code size} caps
	 * the buckets (ordered by count descending, ties by value ascending).
	 */
	public record BucketRequest(String bucketField, String filterField, int size) {

		public static BucketRequest of(String field, int size) {
			return new BucketRequest(field, field, size);
		}
	}

	/**
	 * Dating bounds of one facet: min of {@code boundsField~L} and max of
	 * {@code boundsField~H} over the matching documents, plus how many of them
	 * carry no such dating.
	 *
	 * <p>{@code filterFields} names the dating fields whose own Range filters the
	 * bounds ignore (multi-select, so a selected range can always be widened) -
	 * the same separation {@link BucketRequest} makes, and needed for the same
	 * reason: a facet may bound on one field while filtering on others.
	 * {@link FieldFilter#ANY_DATING} bounds on the record-level hull while
	 * filtering on every dating item type.
	 */
	public record BoundsRequest(String boundsField, List<String> filterFields) {

		public static BoundsRequest of(String field) {
			return new BoundsRequest(field, List.of(field));
		}
	}

	/** Fulltext planned with the built-in default weights (tests, simple callers). */
	public static ApuSearchQuery fulltext(String fulltext) {
		return new ApuSearchQuery(null, RelevanceQueryPlanner.plan(fulltext, RelevanceConfig.defaults()),
				List.of(), List.of(), List.of(), 0, 20, SortMode.RELEVANCE);
	}

	public static ApuSearchQuery matchAll(int from, int size) {
		return new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), from, size, SortMode.RELEVANCE);
	}

}
