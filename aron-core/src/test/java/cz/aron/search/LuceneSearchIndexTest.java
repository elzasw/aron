package cz.aron.search;

import org.springframework.test.util.ReflectionTestUtils;

import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.TypesLoader;
import cz.aron.search.lucene.LuceneSearchIndex;

/**
 * Runs the port contract against the embedded Lucene adapter (plain unit test,
 * no Spring; in-memory directory). Lucene commits make writes visible
 * immediately, so no refresh override is needed.
 */
class LuceneSearchIndexTest extends SearchIndexContractTest {

	@Override
	protected SearchIndex createIndex() {
		var typesLoader = new TypesLoader(null, "src/test/resources/test-config/types.yaml");
		var typesHolder = new TypesHolder(typesLoader);
		ReflectionTestUtils.invokeMethod(typesHolder, "loadData");
		return new LuceneSearchIndex(typesHolder, "");
	}

}
