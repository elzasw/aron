package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contract of the search port that EVERY adapter must fulfil (doc/search-port.md
 * §3.5): the embedded Lucene adapter runs it in the default suite
 * (LuceneSearchIndexTest), the Elasticsearch adapter under the opt-in es-it
 * profile (*IT). Only behavior both engines share belongs here — engine
 * specifics (stemming, ranking, stop words) get engine-only tests.
 */
public abstract class SearchIndexContractTest {

	protected SearchIndex index;

	/** Fresh, empty index per test. */
	protected abstract SearchIndex createIndex();

	/**
	 * Called after every mutating operation. Engines with near-real-time semantics
	 * (Elasticsearch) override this with an index refresh so subsequent
	 * searches/deletes see the writes; the contract itself makes no visibility
	 * promises between an unrefreshed write and a read.
	 */
	protected void refreshAfterWrite() {
	}

	@BeforeEach
	void setUpIndex() {
		index = createIndex();
		index.createSchema();
	}

	protected void indexApus(List<ApuDocument> documents) {
		index.indexApus(documents);
		refreshAfterWrite();
	}

	protected void indexRelations(List<RelationDocument> relations) {
		index.indexRelations(relations);
		refreshAfterWrite();
	}

	protected void deleteApusBySource(long apuSourceId) {
		index.deleteApusBySource(apuSourceId);
		refreshAfterWrite();
	}

	protected static ApuDocument doc(String uuid, String name, long sourceId, Map<String, List<Object>> values) {
		var document = new ApuDocument();
		document.setUuid(uuid);
		document.setName(name);
		document.setType("ARCH_DESC");
		document.setApuSourceId(sourceId);
		document.getValues().putAll(values);
		return document;
	}

	private static String uuid(int n) {
		return UUID.nameUUIDFromBytes(("contract-" + n).getBytes()).toString();
	}

	@Test
	void fieldsCrcLivesAndDiesWithTheSchema() {
		assertThat(index.storedFieldsCrc()).isNull();
		index.storeFieldsCrc(123L);
		assertThat(index.storedFieldsCrc()).isEqualTo(123L);
		index.dropSchema();
		assertThat(index.storedFieldsCrc()).isNull();
	}

	@Test
	void fulltextSearchFoldsDiacritics() {
		indexApus(List.of(doc(uuid(1), "Václav Novák", 1, Map.of())));

		var byFolded = index.search(ApuSearchQuery.fulltext("vaclav"));
		assertThat(byFolded.total()).isEqualTo(1);
		assertThat(byFolded.hits()).hasSize(1);
		assertThat(byFolded.hits().get(0).uuid()).isEqualTo(uuid(1));
		assertThat(byFolded.hits().get(0).name()).isEqualTo("Václav Novák");

		assertThat(index.search(ApuSearchQuery.fulltext("novák")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("neexistuje")).total()).isZero();
	}

	@Test
	void indexingSameUuidReplacesTheDocument() {
		indexApus(List.of(doc(uuid(2), "Original", 1, Map.of())));
		indexApus(List.of(doc(uuid(2), "Replaced", 1, Map.of())));

		var all = index.search(new ApuSearchQuery(null, Map.of(), 0, 10));
		assertThat(all.total()).isEqualTo(1);
		assertThat(all.hits().get(0).name()).isEqualTo("Replaced");
	}

	@Test
	void valueFilterMatchesKeywordValuesExactly() {
		indexApus(List.of(
				doc(uuid(3), "Czech record", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(4), "German record", 1, Map.of("LANG~CODE", List.of("ger")))));

		var czech = index.search(new ApuSearchQuery(null, Map.of("LANG~CODE", "cze"), 0, 10));
		assertThat(czech.total()).isEqualTo(1);
		assertThat(czech.hits().get(0).uuid()).isEqualTo(uuid(3));

		assertThat(index.search(new ApuSearchQuery(null, Map.of("LANG~CODE", "cz"), 0, 10)).total()).isZero();
	}

	@Test
	void deleteBySourceRemovesOnlyThatSource() {
		indexApus(List.of(
				doc(uuid(5), "From source one", 1, Map.of()),
				doc(uuid(6), "From source two", 2, Map.of())));

		deleteApusBySource(1);

		var all = index.search(new ApuSearchQuery(null, Map.of(), 0, 10));
		assertThat(all.total()).isEqualTo(1);
		assertThat(all.hits().get(0).uuid()).isEqualTo(uuid(6));
	}

	@Test
	void pagingReportsFullTotal() {
		indexApus(List.of(
				doc(uuid(7), "Record one", 1, Map.of()),
				doc(uuid(8), "Record two", 1, Map.of()),
				doc(uuid(9), "Record three", 1, Map.of())));

		var page = index.search(new ApuSearchQuery(null, Map.of(), 0, 2));
		assertThat(page.total()).isEqualTo(3);
		assertThat(page.hits()).hasSize(2);
	}

	@Test
	void relationsAreAcceptedForIndexing() {
		// the rels index is write-only within the application (see design Q3)
		assertThatCode(() -> indexRelations(List.of(
				new RelationDocument(uuid(1), "REL~DIRECT", uuid(2)))))
				.doesNotThrowAnyException();
	}

}
