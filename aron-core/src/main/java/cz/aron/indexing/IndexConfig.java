package cz.aron.indexing;

/**
 * Names of the Elasticsearch analyzers defined in es_settings.json, shared by
 * the index mappings ({@link IndexedApu}, the dynamic types.yaml mapping) and
 * the old-API query builder. Connection configuration is Spring Boot's standard
 * {@code spring.elasticsearch.*}.
 */
public final class IndexConfig {

	public static final String FOLDING_AND_TOKENIZING = "folding_and_tokenizing";

	/** Trigram chain of the substring-match companions (doc/search-relevance.md R-15). */
	public static final String FOLDING_AND_NGRAM = "folding_and_ngram";

	public static final String FOLDING_AND_TOKENIZING_STOP = "folding_and_tokenizing_stop";

	public static final String TEXT_LONG_KEYWORD = "text_long_keyword";

	public static final String TEXT_LONG_KEYWORD_CI = "text_long_keyword_ci";

	private IndexConfig() {
	}

}
