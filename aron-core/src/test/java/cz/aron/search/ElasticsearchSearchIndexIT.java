package cz.aron.search;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.TypesLoader;
import cz.aron.search.es.ElasticsearchSearchIndex;

/**
 * Runs the port contract against a REAL Elasticsearch - the fidelity anchor of
 * the in-memory adapter (doc/search-port.md §3.7). Opt-in: activated by the
 * {@code es-it} Maven profile, which starts the official ES distribution as a
 * local process on port 19200 (no local installation, no Docker - see
 * README.md, "Optional Elasticsearch integration tests").
 */
class ElasticsearchSearchIndexIT extends SearchIndexContractTest {

	private static ElasticsearchTemplate operations;

	private static ElasticsearchSearchIndex elasticsearchIndex;

	@BeforeAll
	static void connect() throws Exception {
		int port = Integer.getInteger("es.it.port", 19200);
		var clientConfiguration = ClientConfiguration.builder().connectedTo("localhost:" + port).build();
		var client = ElasticsearchClients.createImperative(clientConfiguration);
		var converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
		converter.afterPropertiesSet();
		operations = new ElasticsearchTemplate(client, converter);

		var typesLoader = new TypesLoader(null, "src/test/resources/test-config/types.yaml");
		var typesHolder = new TypesHolder(typesLoader);
		ReflectionTestUtils.invokeMethod(typesHolder, "loadData");

		elasticsearchIndex = new ElasticsearchSearchIndex(operations, converter, typesHolder,
				new ClassPathResource("elasticsearch/es_settings.json"), new ContentLocale("cs-CZ"));
	}

	@Override
	protected SearchIndex createIndex() {
		// fresh schema per test - drop leftovers from the previous test
		elasticsearchIndex.dropSchema();
		return elasticsearchIndex;
	}

	@Override
	protected void refreshAfterWrite() {
		// ES is near-real-time: make writes visible to searches and delete-by-query
		operations.indexOps(IndexCoordinates.of("apu")).refresh();
		operations.indexOps(IndexCoordinates.of("rels")).refresh();
	}

}
