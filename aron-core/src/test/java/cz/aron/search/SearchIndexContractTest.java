package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.aron.search.ApuSearchQuery.SortMode;

/**
 * Contract of the search port that EVERY adapter must fulfil (doc/search-port.md
 * §3.5): the embedded Lucene adapter runs it in the default suite
 * (LuceneSearchIndexTest), the Elasticsearch adapter under the opt-in es-it
 * profile (*IT). Only behavior both engines share belongs here — engine
 * specifics (stemming, ranking, stop words, word-order semantics of text
 * filters) get engine-only tests.
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
		return doc(uuid, name, "ARCH_DESC", sourceId, values);
	}

	protected static ApuDocument doc(String uuid, String name, String type, long sourceId,
			Map<String, List<Object>> values) {
		var document = new ApuDocument();
		document.setUuid(uuid);
		document.setName(name);
		document.setNameSort(ApuDocumentBuilder.czechSortKey(name));
		document.setType(type);
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

		var all = index.search(ApuSearchQuery.matchAll(0, 10));
		assertThat(all.total()).isEqualTo(1);
		assertThat(all.hits().get(0).name()).isEqualTo("Replaced");
	}

	@Test
	void valuesFilterMatchesAnyOfTheValuesExactly() {
		indexApus(List.of(
				doc(uuid(3), "Czech record", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(4), "German record", 1, Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(5), "Latin record", 1, Map.of("LANG~CODE", List.of("lat")))));

		var czech = index.search(query(null, List.of(new FieldFilter.Values("LANG~CODE", List.of("cze")))));
		assertThat(czech.total()).isEqualTo(1);
		assertThat(czech.hits().get(0).uuid()).isEqualTo(uuid(3));

		// OR across the selected values
		var czechOrGerman = index
				.search(query(null, List.of(new FieldFilter.Values("LANG~CODE", List.of("cze", "ger")))));
		assertThat(czechOrGerman.total()).isEqualTo(2);

		// exact keyword semantics - no prefix matching
		assertThat(index.search(query(null, List.of(new FieldFilter.Values("LANG~CODE", List.of("cz")))))
				.total()).isZero();
	}

	@Test
	void apuTypeRestrictsTheSearch() {
		indexApus(List.of(
				doc(uuid(6), "Fond A", "FUND", 1, Map.of()),
				doc(uuid(7), "Popis B", "ARCH_DESC", 1, Map.of())));

		var funds = index.search(
				new ApuSearchQuery("FUND", null, List.of(), List.of(), Set.of(), 0, 10, SortMode.RELEVANCE));
		assertThat(funds.total()).isEqualTo(1);
		assertThat(funds.hits().get(0).uuid()).isEqualTo(uuid(6));
	}

	@Test
	void textFilterMatchesAnalyzedFieldWithFolding() {
		indexApus(List.of(
				doc(uuid(8), "Zaznam A", 1, Map.of("TITLE~MAIN", List.of("Stavební dokumentace Přerov"))),
				doc(uuid(9), "Zaznam B", 1, Map.of("TITLE~MAIN", List.of("Kronika obce")))));

		var byFolded = index.search(query(null, List.of(new FieldFilter.Text("TITLE~MAIN", "stavebni"))));
		assertThat(byFolded.total()).isEqualTo(1);
		assertThat(byFolded.hits().get(0).uuid()).isEqualTo(uuid(8));

		assertThat(index.search(query(null, List.of(new FieldFilter.Text("TITLE~MAIN", "neexistuje")))).total())
				.isZero();
	}

	@Test
	void rangeFilterMatchesIntersectingIntervals() {
		indexApus(List.of(
				doc(uuid(10), "Kniha 1800-1850", 1, Map.of(
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"))),
				doc(uuid(11), "Kniha 1900-1910", 1, Map.of(
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59")))));

		// [1840, 1899] intersects only the first interval
		var range = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE",
				LocalDateTime.parse("1840-01-01T00:00:00"), LocalDateTime.parse("1899-12-31T23:59:59")))));
		assertThat(range.total()).isEqualTo(1);
		assertThat(range.hits().get(0).uuid()).isEqualTo(uuid(10));

		// open lower bound
		var upTo1905 = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE", null,
				LocalDateTime.parse("1905-01-01T00:00:00")))));
		assertThat(upTo1905.total()).isEqualTo(2);

		// disjoint interval
		assertThat(index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE",
				LocalDateTime.parse("1860-01-01T00:00:00"), LocalDateTime.parse("1890-01-01T00:00:00")))))
				.total()).isZero();
	}

	@Test
	void bucketsFollowMultiSelectSemantics() {
		indexApus(List.of(
				doc(uuid(12), "Zaznam 1", 1, Map.of("LANG~CODE", List.of("cze"), "REL~ENTITY", List.of("apu-a"))),
				doc(uuid(13), "Zaznam 2", 1, Map.of("LANG~CODE", List.of("cze"), "REL~ENTITY", List.of("apu-b"))),
				doc(uuid(14), "Zaznam 3", 1, Map.of("LANG~CODE", List.of("ger"), "REL~ENTITY", List.of("apu-a")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Values("LANG~CODE", List.of("cze"))),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10),
						ApuSearchQuery.BucketRequest.of("REL~ENTITY", 10)),
				Set.of(), 0, 10, SortMode.RELEVANCE));

		// hits and total respect the filter
		assertThat(result.total()).isEqualTo(2);
		// the filtered facet's own buckets ignore its filter (the user can widen the selection)
		assertThat(result.buckets().get("LANG~CODE")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("cze", 2), new ApuSearchResult.Bucket("ger", 1));
		// other facets' buckets respect it
		assertThat(result.buckets().get("REL~ENTITY")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("apu-a", 1), new ApuSearchResult.Bucket("apu-b", 1));
	}

	@Test
	void bucketsAreOrderedByCountThenValueAndCappedBySize() {
		indexApus(List.of(
				doc(uuid(30), "Z1", 1, Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(31), "Z2", 1, Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(32), "Z3", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(33), "Z4", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(34), "Z5", 1, Map.of("LANG~CODE", List.of("lat")))));

		var all = index.search(new ApuSearchQuery(null, null, List.of(),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10)), Set.of(), 0, 0, SortMode.RELEVANCE));
		assertThat(all.buckets().get("LANG~CODE")).containsExactly(
				new ApuSearchResult.Bucket("cze", 2), new ApuSearchResult.Bucket("ger", 2),
				new ApuSearchResult.Bucket("lat", 1));

		var capped = index.search(new ApuSearchQuery(null, null, List.of(),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 2)), Set.of(), 0, 0, SortMode.RELEVANCE));
		assertThat(capped.buckets().get("LANG~CODE")).containsExactly(
				new ApuSearchResult.Bucket("cze", 2), new ApuSearchResult.Bucket("ger", 2));
	}

	@Test
	void refBucketsPairTheCompositeFieldWithTheFilterField() {
		// reference facets enumerate <code>~ID~LABEL while their Values filter
		// sits on <code> - the pairing drives the multi-select exclusion
		indexApus(List.of(
				doc(uuid(35), "R1", 1, Map.of(
						"REL~ENTITY", List.of("apu-a"), "REL~ENTITY~ID~LABEL", List.of("apu-a|Novak"))),
				doc(uuid(36), "R2", 1, Map.of(
						"REL~ENTITY", List.of("apu-b"), "REL~ENTITY~ID~LABEL", List.of("apu-b|Dvorak")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Values("REL~ENTITY", List.of("apu-a"))),
				List.of(new ApuSearchQuery.BucketRequest("REL~ENTITY~ID~LABEL", "REL~ENTITY", 10)),
				Set.of(), 0, 10, SortMode.RELEVANCE));

		// hits respect the filter, the facet's own buckets ignore it
		assertThat(result.total()).isEqualTo(1);
		assertThat(result.buckets().get("REL~ENTITY~ID~LABEL")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("apu-a|Novak", 1), new ApuSearchResult.Bucket("apu-b|Dvorak", 1));
	}

	@Test
	void rangeFiltersOfOtherFacetsApplyToBuckets() {
		indexApus(List.of(
				doc(uuid(37), "Stara", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"))),
				doc(uuid(38), "Nova", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Range("UNIT~DATE", LocalDateTime.parse("1890-01-01T00:00:00"), null)),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10)), Set.of(), 0, 10, SortMode.RELEVANCE));

		// another facet's RANGE filter restricts both hits and buckets
		assertThat(result.total()).isEqualTo(1);
		assertThat(result.buckets().get("LANG~CODE"))
				.containsExactly(new ApuSearchResult.Bucket("cze", 1));
	}

	@Test
	void datingBoundsFollowMultiSelectSemantics() {
		indexApus(List.of(
				doc(uuid(40), "Kniha A", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"))),
				doc(uuid(41), "Kniha B", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59"))),
				doc(uuid(42), "Kniha C bez datace", 1, Map.of("LANG~CODE", List.of("ger")))));

		// the facet's own RANGE filter is excluded from its bounds (the slider can widen)
		var withOwnRange = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Range("UNIT~DATE", LocalDateTime.parse("1890-01-01T00:00:00"), null)),
				List.of(), Set.of("UNIT~DATE"), 0, 0, SortMode.RELEVANCE));
		var bounds = withOwnRange.bounds().get("UNIT~DATE");
		assertThat(bounds).isNotNull();
		assertThat(atUtcYear(bounds.minMillis())).isEqualTo(1800);
		assertThat(atUtcYear(bounds.maxMillis())).isEqualTo(1910);

		// other facets' filters apply to the bounds
		var withOtherFilter = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Values("LANG~CODE", List.of("ger"))),
				List.of(), Set.of("UNIT~DATE"), 0, 0, SortMode.RELEVANCE));
		// no matching document carries the dating - no bounds entry
		assertThat(withOtherFilter.bounds()).doesNotContainKey("UNIT~DATE");
	}

	private static int atUtcYear(long epochMillis) {
		return java.time.Instant.ofEpochMilli(epochMillis).atOffset(java.time.ZoneOffset.UTC).getYear();
	}

	@Test
	void nameSortFollowsCzechCollation() {
		indexApus(List.of(
				doc(uuid(15), "Chalupa", 1, Map.of()),
				doc(uuid(16), "Cibule", 1, Map.of()),
				doc(uuid(17), "Hrad", 1, Map.of())));

		var sorted = index
				.search(new ApuSearchQuery(null, null, List.of(), List.of(), Set.of(), 0, 10, SortMode.NAME));

		// Czech alphabet: c < h < ch
		assertThat(sorted.hits()).extracting(ApuSearchResult.Hit::name)
				.containsExactly("Cibule", "Hrad", "Chalupa");
	}

	@Test
	void deleteBySourceRemovesOnlyThatSource() {
		indexApus(List.of(
				doc(uuid(18), "From source one", 1, Map.of()),
				doc(uuid(19), "From source two", 2, Map.of())));

		deleteApusBySource(1);

		var all = index.search(ApuSearchQuery.matchAll(0, 10));
		assertThat(all.total()).isEqualTo(1);
		assertThat(all.hits().get(0).uuid()).isEqualTo(uuid(19));
	}

	@Test
	void pagingByFromOffsetReportsFullTotal() {
		indexApus(List.of(
				doc(uuid(20), "Record one", 1, Map.of()),
				doc(uuid(21), "Record two", 1, Map.of()),
				doc(uuid(22), "Record three", 1, Map.of())));

		var page = index.search(ApuSearchQuery.matchAll(0, 2));
		assertThat(page.total()).isEqualTo(3);
		assertThat(page.hits()).hasSize(2);

		// arbitrary offset, not page-aligned
		var offset = index.search(ApuSearchQuery.matchAll(2, 2));
		assertThat(offset.total()).isEqualTo(3);
		assertThat(offset.hits()).hasSize(1);
	}

	@Test
	void relationsAreAcceptedForIndexing() {
		// the rels index is write-only within the application (see design Q3)
		assertThatCode(() -> indexRelations(List.of(
				new RelationDocument(uuid(1), "REL~DIRECT", uuid(2)))))
				.doesNotThrowAnyException();
	}

	private static ApuSearchQuery query(String fulltext, List<FieldFilter> filters) {
		return new ApuSearchQuery(null, fulltext, filters, List.of(), Set.of(), 0, 10, SortMode.RELEVANCE);
	}

}
