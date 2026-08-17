package cz.aron.search;

import java.util.List;
import java.util.Set;

/**
 * Engine-neutral search request (the port's read side).
 *
 * <p>Multi-select facet semantics: hits and total respect ALL filters; the
 * buckets/bounds of a facet ignore the facet filters (Values, Range) sitting on
 * its own filter field, so an active selection can always be widened.
 *
 * @param apuType      restricts to one APU type ({@code null} = all types)
 * @param fulltext     tokens matched against the indexed name/description
 *                     (engine analysis applies; {@code null} = match all)
 * @param filters      field-level filters, all must match (see {@link FieldFilter})
 * @param buckets      value-bucket count requests (see {@link BucketRequest})
 * @param boundsFields UNITDATE item codes to compute dating bounds for (min of
 *                     {@code field~L}, max of {@code field~H} over the matching
 *                     documents, the field's own Range filter excluded)
 * @param from         zero-based offset of the first hit
 * @param size         page size (bounded by the caller)
 * @param sort         named sort mode
 * @param totalUpTo    accuracy limit of the result's total: matching documents
 *                     are counted exactly up to this value, beyond it the total
 *                     is reported as {@code (totalUpTo, GTE)}; {@code null} =
 *                     count exactly (callers resolve their configured default)
 */
public record ApuSearchQuery(String apuType, String fulltext, List<FieldFilter> filters,
		List<BucketRequest> buckets, Set<String> boundsFields, int from, int size, SortMode sort,
		Integer totalUpTo) {

	/** Exact-total variant - the accuracy limit defaults to unlimited. */
	public ApuSearchQuery(String apuType, String fulltext, List<FieldFilter> filters,
			List<BucketRequest> buckets, Set<String> boundsFields, int from, int size, SortMode sort) {
		this(apuType, fulltext, filters, buckets, boundsFields, from, size, sort, null);
	}

	public enum SortMode {
		RELEVANCE,
		/** Czech-alphabetical by name - engines sort by the index-time collation key (nameSort). */
		NAME
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

	public static ApuSearchQuery fulltext(String fulltext) {
		return new ApuSearchQuery(null, fulltext, List.of(), List.of(), Set.of(), 0, 20, SortMode.RELEVANCE);
	}

	public static ApuSearchQuery matchAll(int from, int size) {
		return new ApuSearchQuery(null, null, List.of(), List.of(), Set.of(), from, size, SortMode.RELEVANCE);
	}

}
