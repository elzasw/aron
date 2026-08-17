package cz.aron.search;

import java.util.List;
import java.util.Map;

/**
 * Engine-neutral search result. Hit order follows the requested sort mode; for
 * {@code RELEVANCE} the exact ranking is engine-specific and callers must not
 * rely on a particular order across engines.
 *
 * @param total   total number of matching documents (independent of paging)
 * @param hits    the requested page of hits
 * @param buckets value-bucket counts per requested bucket field; bucket order
 *                is unspecified (the caller orders per facet configuration)
 */
public record ApuSearchResult(long total, List<Hit> hits, Map<String, List<Bucket>> buckets) {

	public record Hit(String uuid, String name, String description, String type, boolean containsDigitalObjects) {
	}

	public record Bucket(String value, long count) {
	}

}
