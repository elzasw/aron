package cz.aron.search;

import cz.aron.search.memory.InMemorySearchIndex;

/** Runs the port contract against the in-memory adapter (plain unit test, no Spring). */
class InMemorySearchIndexTest extends SearchIndexContractTest {

	@Override
	protected SearchIndex createIndex() {
		return new InMemorySearchIndex();
	}

}
