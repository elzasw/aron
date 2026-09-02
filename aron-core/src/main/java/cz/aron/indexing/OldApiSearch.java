package cz.aron.indexing;

import cz.aron.api.rest.model.Params;

/**
 * Engine boundary of the old-API search endpoints ({@code /api/aron/apu/list*}).
 * The frozen old-API request model ({@link Params}) is translated by the
 * implementation selected by {@code search.engine}:
 * <ul>
 * <li>{@link EsOldApiSearch} - the production path (verbatim the former direct
 * Elasticsearch code, still frozen; the parity reference),</li>
 * <li>{@code cz.aron.search.lucene.LuceneOldApiSearch} - dev/test-grade support
 * on the embedded engine, so ES-less deployments, dev mode and the default test
 * suite can serve the old UI (approximate relevance/fulltext semantics;
 * {@code searchAfter} not supported yet).</li>
 * </ul>
 * Deliberately separate from the engine-neutral search port
 * ({@code cz.aron.search.SearchIndex}): the port's query model stays minimal and
 * new-API-shaped (doc/search-port.md) and must not grow to carry the whole old
 * API.
 */
public interface OldApiSearch {

	OldApiSearchResult search(Params params);

}
