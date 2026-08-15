package cz.aron.search;

import java.util.List;

/**
 * Minimal engine-neutral search result. Hit order is engine-specific
 * (relevance); callers must not rely on a particular order across engines.
 */
public record ApuSearchResult(long total, List<Hit> hits) {

	public record Hit(String uuid, String name, String type) {
	}

}
