package cz.aron.search;

import java.util.List;
import java.util.Set;

/**
 * Engine-neutral search request (the port's read side).
 *
 * @param apuType      restricts to one APU type ({@code null} = all types)
 * @param fulltext     tokens matched against the indexed name/description
 *                     (engine analysis applies; {@code null} = match all)
 * @param filters      field-level filters, all must match (see {@link FieldFilter})
 * @param bucketFields fields to compute value-bucket counts for. Multi-select
 *                     semantics: buckets of a field ignore that field's own
 *                     {@link FieldFilter.Values} filter (all other filters apply)
 * @param from         zero-based offset of the first hit
 * @param size         page size (bounded by the caller)
 * @param sort         named sort mode
 */
public record ApuSearchQuery(String apuType, String fulltext, List<FieldFilter> filters, Set<String> bucketFields,
		int from, int size, SortMode sort) {

	public enum SortMode {
		RELEVANCE,
		/** Czech-alphabetical by name - engines sort by the index-time collation key (nameSort). */
		NAME
	}

	public static ApuSearchQuery fulltext(String fulltext) {
		return new ApuSearchQuery(null, fulltext, List.of(), Set.of(), 0, 20, SortMode.RELEVANCE);
	}

	public static ApuSearchQuery matchAll(int from, int size) {
		return new ApuSearchQuery(null, null, List.of(), Set.of(), from, size, SortMode.RELEVANCE);
	}

}
