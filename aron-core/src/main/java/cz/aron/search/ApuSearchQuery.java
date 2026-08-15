package cz.aron.search;

import java.util.Map;

/**
 * Minimal engine-neutral search request (grows with the Phase 7 slices - D-9).
 *
 * @param fulltext     tokens matched against the indexed name/description
 *                     (engine analysis applies; {@code null} = match all)
 * @param valueFilters exact-match filters on keyword-typed item-type codes
 *                     (tilde form, e.g. {@code LANG~CODE}); all must match
 * @param page         zero-based page index
 * @param size         page size (bounded by the caller)
 */
public record ApuSearchQuery(String fulltext, Map<String, String> valueFilters, int page, int size) {

	public static ApuSearchQuery fulltext(String fulltext) {
		return new ApuSearchQuery(fulltext, Map.of(), 0, 20);
	}

}
