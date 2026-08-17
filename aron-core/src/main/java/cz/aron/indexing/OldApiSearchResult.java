package cz.aron.indexing;

import java.util.List;
import java.util.Map;

import cz.aron.api.rest.model.AggregationResult;

/**
 * Engine-neutral result of an old-API search ({@link OldApiSearch}). Carries the
 * search-layer outcome only - the endpoint hydrates the actual items from the
 * database by uuid.
 *
 * @param total        total number of matching documents (independent of paging)
 * @param uuids        uuids of the requested page, in result order
 * @param aggregations aggregation results in the old-API wire shape, keyed by
 *                     aggregation name
 * @param searchAfter  keyset-pagination cursor of the last hit ({@code null}
 *                     when the query was not sorted or the engine does not
 *                     provide one)
 */
public record OldApiSearchResult(long total, List<String> uuids,
		Map<String, List<AggregationResult>> aggregations, List<Object> searchAfter) {
}
