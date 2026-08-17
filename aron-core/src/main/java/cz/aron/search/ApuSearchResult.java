package cz.aron.search;

import java.util.List;
import java.util.Map;

/**
 * Engine-neutral search result. Hit order follows the requested sort mode; for
 * {@code RELEVANCE} the exact ranking is engine-specific and callers must not
 * rely on a particular order across engines.
 *
 * @param total         total number of matching documents (independent of
 *                      paging), counted exactly up to the query's
 *                      {@code totalUpTo} and capped by it
 * @param totalRelation {@code EQ} = {@code total} is exact, {@code GTE} = at
 *                      least {@code total} documents match (counting stopped
 *                      at the query's accuracy limit)
 * @param hits          the requested page of hits
 * @param buckets       value buckets per requested bucket field, ordered by
 *                      count descending (ties by value ascending) and capped
 *                      by the request's size
 * @param bounds        dating bounds per requested bounds field; a field has
 *                      no entry when no matching document carries its dating
 */
public record ApuSearchResult(long total, TotalRelation totalRelation, List<Hit> hits,
		Map<String, List<Bucket>> buckets, Map<String, Bounds> bounds) {

	/** Accuracy of {@link #total()}. */
	public enum TotalRelation {
		EQ,
		GTE
	}

	public record Hit(String uuid, String name, String description, String type, boolean containsDigitalObjects) {
	}

	public record Bucket(String value, long count) {
	}

	/** Dating bounds in epoch millis (UTC) - min of the {@code ~L}, max of the {@code ~H} bound field. */
	public record Bounds(long minMillis, long maxMillis) {
	}

}
