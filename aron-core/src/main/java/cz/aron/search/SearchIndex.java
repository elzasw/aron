package cz.aron.search;

import java.util.Collection;

/**
 * Port of the search engine (see doc/search-port.md). Adapters:
 * {@code cz.aron.search.es} (Elasticsearch - the default production engine,
 * required while the old API lives) and {@code cz.aron.search.lucene} (embedded
 * Lucene - small/ES-less deployments, tests and dev mode; serves the new API
 * only). Selected by the {@code search.engine} property
 * ({@code elasticsearch}, the default, or {@code lucene}).
 * <p>
 * The boundary is domain-shaped: no engine types leak through it. The write
 * side is used by the import pipeline and the startup bootstrap
 * ({@link SearchIndexManager}); the read side is deliberately minimal and grows
 * with the new-API slices (PLAN.md Phase 7).
 */
public interface SearchIndex {

	// --- schema lifecycle -------------------------------------------------

	/** Creates the schema (indexes/mappings) if not present; no-op otherwise. */
	void createSchema();

	/** Drops the schema including all indexed data; no-op when not present. */
	void dropSchema();

	/**
	 * Fingerprint of the configuration the current schema was built for
	 * (see {@link SearchIndexManager#currentSchemaCrc()}), stored in the schema's
	 * own metadata (ES: index {@code _meta}). {@code null} when the schema does
	 * not exist or carries no marker - the caller then rebuilds and reindexes.
	 */
	Long storedSchemaCrc();

	void storeSchemaCrc(long crc);

	// --- write side -------------------------------------------------------

	/** Indexes the documents; an existing document with the same uuid is replaced. */
	void indexApus(Collection<ApuDocument> documents);

	void indexRelations(Collection<RelationDocument> relations);

	/**
	 * Removes the documents with the given ids (APU uuids); an unknown id is a
	 * no-op. Any collection size is accepted - an adapter whose engine caps a
	 * request's id list (Elasticsearch: {@code index.max_terms_count}) chunks
	 * internally.
	 */
	void deleteApus(Collection<String> uuids);

	// --- read side (minimal; grows with the Phase 7 slices) ----------------

	ApuSearchResult search(ApuSearchQuery query);

}
