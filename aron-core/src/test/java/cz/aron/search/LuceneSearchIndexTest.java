package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
		return newIndex("");
	}

	private static LuceneSearchIndex newIndex(String path) {
		var typesLoader = new TypesLoader(null, "src/test/resources/test-config/types.yaml");
		var typesHolder = new TypesHolder(typesLoader);
		ReflectionTestUtils.invokeMethod(typesHolder, "loadData");
		return new LuceneSearchIndex(typesHolder, path);
	}

	/**
	 * A persisted index written by a different document layout has to be rebuilt,
	 * not reused: its documents lack whatever the new layout indexes. The startup
	 * bootstrap decides that from the stored schema CRC, so reporting none is how
	 * the layout version asks for the rebuild - and bumping that version is the
	 * only thing that protects an existing embedded index from a change like the
	 * dating intervals (doc/search-port.md §11).
	 */
	@Test
	void aPersistedIndexOfAnotherLayoutReportsNoSchema(@TempDir Path directory) throws Exception {
		var persisted = newIndex(directory.toString());
		persisted.createSchema();
		persisted.storeSchemaCrc(4242L);
		assertThat(persisted.storedSchemaCrc()).isEqualTo(4242L);
		persisted.close();

		// the same index, its layout stamp replaced by an older one
		try (var directoryHandle = FSDirectory.open(directory.resolve("apu"));
				var writer = new IndexWriter(directoryHandle, new IndexWriterConfig())) {
			writer.setLiveCommitData(Set.of(Map.entry("schemaCrc", "4242"), Map.entry("layoutVersion", "1")));
			writer.commit();
		}

		var reopened = newIndex(directory.toString());
		assertThat(reopened.storedSchemaCrc()).isNull();
		reopened.close();
	}

}
