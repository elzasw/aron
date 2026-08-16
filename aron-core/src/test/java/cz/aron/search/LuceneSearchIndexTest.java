package cz.aron.search;

import cz.aron.search.lucene.LuceneSearchIndex;

/**
 * Runs the port contract against the embedded Lucene adapter (plain unit test,
 * no Spring; in-memory directory). Lucene commits make writes visible
 * immediately, so no refresh override is needed.
 */
class LuceneSearchIndexTest extends SearchIndexContractTest {

	@Override
	protected SearchIndex createIndex() {
		return new LuceneSearchIndex("");
	}

}
