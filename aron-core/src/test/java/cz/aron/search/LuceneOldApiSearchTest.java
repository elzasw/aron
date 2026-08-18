package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cz.aron.api.rest.model.AggregationResult;
import cz.aron.api.rest.model.AndFilter;
import cz.aron.api.rest.model.AnyKeywordFieldFilter;
import cz.aron.api.rest.model.BucketAggregator;
import cz.aron.api.rest.model.ContainsFilter;
import cz.aron.api.rest.model.EqFilter;
import cz.aron.api.rest.model.FieldSort;
import cz.aron.api.rest.model.Filter;
import cz.aron.api.rest.model.FullTextFieldFilter;
import cz.aron.api.rest.model.FullTextFilter;
import cz.aron.api.rest.model.MaxAggregation;
import cz.aron.api.rest.model.MinAggregation;
import cz.aron.api.rest.model.NotFilter;
import cz.aron.api.rest.model.OrFilter;
import cz.aron.api.rest.model.Params;
import cz.aron.api.rest.model.RangeFilter;
import cz.aron.api.rest.model.TermsAggregation;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.TypesLoader;
import cz.aron.search.lucene.LuceneOldApiSearch;
import cz.aron.search.lucene.LuceneSearchIndex;

/**
 * Old-API query translation on the embedded Lucene engine (plain unit test, no
 * Spring; in-memory index). Covers the request surface the old UI actually
 * sends: boolean filter trees, EQ/FTXF/FTX/RANGE/CONTAINS/AKF, offset paging,
 * name/score sort, and the TERMS/MAX/MIN aggregations. The production ES
 * behavior of the same requests is pinned by the frozen EsOldApiSearch path -
 * engine-shared parity beyond this dev/test grade is not claimed.
 */
class LuceneOldApiSearchTest {

	/** The fixtures mirror ApuDocumentBuilder; the test deployment runs the default search locale. */
	private static final ContentLocale CONTENT_LOCALE = new ContentLocale("cs-CZ");

	private LuceneOldApiSearch search;

	private LuceneSearchIndex index;

	@BeforeEach
	void setUp() {
		var typesLoader = new TypesLoader(null, "src/test/resources/test-config/types.yaml");
		var typesHolder = new TypesHolder(typesLoader);
		ReflectionTestUtils.invokeMethod(typesHolder, "loadData");
		index = new LuceneSearchIndex(typesHolder, "");
		index.createSchema();
		search = new LuceneOldApiSearch(index, typesHolder);
		indexFixture();
	}

	/**
	 * Fixture: two FUND records with dating, refs and languages, three ARCH_DESC
	 * records for sorting/paging (Czech collation: Cibule < Hrad < Chalupa).
	 */
	private void indexFixture() {
		var fund1 = doc(uuid(1), "Matriční kniha Přerov", "FUND", Map.of(
				"TITLE~MAIN", List.of("Stavební dokumentace Přerov"),
				"LANG~CODE", List.of("cze"),
				"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
				"UNIT~DATE~H", List.of("1850-12-31T23:59:59"),
				"REL~ENTITY", List.of("ent-1"),
				"REL~ENTITY~LABEL", List.of("Karel Novák"),
				"REL~ENTITY~ID~LABEL", List.of("ent-1|Karel Novák")));
		fund1.setContainsDigitalObjects(true);
		var fund2 = doc(uuid(2), "Sbírka fotografií", "FUND", Map.of(
				"LANG~CODE", List.of("cze"),
				"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
				"UNIT~DATE~H", List.of("1910-12-31T23:59:59"),
				"REL~ENTITY", List.of("ent-2"),
				"REL~ENTITY~LABEL", List.of("Jan Dvořák"),
				"REL~ENTITY~ID~LABEL", List.of("ent-2|Jan Dvořák")));
		index.indexApus(List.of(fund1, fund2,
				doc(uuid(3), "Cibule", "ARCH_DESC", Map.of("LANG~CODE", List.of("lat"))),
				doc(uuid(4), "Hrad", "ARCH_DESC", Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(5), "Chalupa", "ARCH_DESC", Map.of("LANG~CODE", List.of("ger")))));
	}

	@Test
	void eqFilterWithNameSortAndOffsetPaging() {
		var params = params(eq("type", "ARCH_DESC"));
		params.setSort(List.of(fieldSort("name")));
		params.setSize(2);

		var page1 = search.search(params);
		assertThat(page1.total()).isEqualTo(3);
		assertThat(page1.uuids()).containsExactly(uuid(3), uuid(4)); // Cibule, Hrad

		params.setOffset(2);
		var page2 = search.search(params);
		assertThat(page2.uuids()).containsExactly(uuid(5)); // Chalupa (Czech: ch after h)
	}

	@Test
	void flipDirectionReversesTheSort() {
		var params = params(eq("type", "ARCH_DESC"));
		params.setSort(List.of(fieldSort("name")));
		params.setSize(3);
		params.setFlipDirection(true);

		assertThat(search.search(params).uuids()).containsExactly(uuid(5), uuid(4), uuid(3));
	}

	@Test
	void booleanFilterTrees() {
		var not = new NotFilter();
		not.setFilters(List.of(eq("LANG~CODE", "ger")));
		var and = new AndFilter();
		and.setFilters(List.of(eq("type", "ARCH_DESC"), not));
		assertThat(search.search(params(and)).uuids()).containsExactly(uuid(3));

		var or = new OrFilter();
		or.setFilters(List.of(eq("LANG~CODE", "lat"), eq("LANG~CODE", "ger")));
		assertThat(search.search(params(or)).total()).isEqualTo(3);
	}

	@Test
	void fullTextFieldMatchesAllWordsWithLastAsFoldedPrefix() {
		var ftxf = new FullTextFieldFilter();
		ftxf.setField("TITLE~MAIN");
		ftxf.setValue("stavebni dok");
		assertThat(search.search(params(ftxf)).uuids()).containsExactly(uuid(1));

		ftxf.setValue("dokumentace neexistujici");
		assertThat(search.search(params(ftxf)).total()).isZero();
	}

	@Test
	void fullTextSearchesNameAndAnalyzedItemFields() {
		var byName = new FullTextFilter();
		byName.setValue("přerov");
		assertThat(search.search(params(byName)).uuids()).containsExactly(uuid(1));

		var byRefLabel = new FullTextFilter();
		byRefLabel.setValue("dvorak");
		assertThat(search.search(params(byRefLabel)).uuids()).containsExactly(uuid(2));
	}

	@Test
	void unitdateRangeFiltersByIntervalIntersection() {
		// [1840, 1899] intersects only the first fund's dating
		assertThat(search.search(params(range("UNIT~DATE", "1840-01-01T00:00:00", "1899-12-31T23:59:59")))
				.uuids()).containsExactly(uuid(1));
		// open lower bound reaches both
		assertThat(search.search(params(range("UNIT~DATE", null, "1905-01-01T00:00:00"))).total()).isEqualTo(2);
		// disjoint interval
		assertThat(search.search(params(range("UNIT~DATE", "1860-01-01T00:00:00", "1890-01-01T00:00:00")))
				.total()).isZero();
	}

	@Test
	void rangeAcceptsTheOldUiZuluBounds() {
		// the old UI's yearInISO sends bounds with millis and a Z zone designator
		assertThat(search.search(params(range("UNIT~DATE", "1840-01-01T00:00:00.000Z", "1899-12-31T23:59:59.999Z")))
				.uuids()).containsExactly(uuid(1));
		assertThat(search.search(params(range("UNIT~DATE", "0001-01-01T00:00:00.000Z", "2026-12-31T23:59:59.999Z")))
				.total()).isEqualTo(2);
	}

	@Test
	void unsupportedAggregationsAnswerWithAnEmptyShape() {
		// GET-OPTIONSREL-BY_SOURCE shape: NESTED(items) > FILTER(relsFilterAgg) > TERMS(idLabel);
		// the old UI navigates the structure without guards - the shape must exist
		var idLabel = terms("idLabel", "rels.idLabel", null);
		var relsFilter = new cz.aron.api.rest.model.FilterAggregation();
		relsFilter.setName("relsFilterAgg");
		relsFilter.setAggregations(List.of(idLabel));
		var nested = new cz.aron.api.rest.model.NestedAggregation();
		nested.setName("items");
		nested.setPath("rels");
		nested.setAggregations(List.of(relsFilter));

		var params = new Params();
		params.setSize(0);
		params.setAggregations(List.of(nested));

		var items = search.search(params).aggregations().get("items");
		assertThat(items).hasSize(1);
		assertThat(items.get(0).getValue()).isEqualTo("0");
		var relsFilterResult = items.get(0).getAggregations().get("relsFilterAgg");
		assertThat(relsFilterResult).hasSize(1);
		assertThat(relsFilterResult.get(0).getAggregations().get("idLabel")).isEmpty();
	}

	@Test
	void containsMatchesFoldedSubstringOfAnalyzedTokens() {
		var contains = new ContainsFilter();
		contains.setField("REL~ENTITY~LABEL");
		contains.setValue("Nov");
		assertThat(search.search(params(contains)).uuids()).containsExactly(uuid(1));
	}

	@Test
	void anyKeywordFieldMatchesReferenceFields() {
		var akf = new AnyKeywordFieldFilter();
		akf.setValue("ent-2");
		assertThat(search.search(params(akf)).uuids()).containsExactly(uuid(2));
	}

	@Test
	void eqOnBooleanDocumentField() {
		assertThat(search.search(params(eq("containsDigitalObjects", "true"))).uuids())
				.containsExactly(uuid(1));
	}

	@Test
	void termsBucketsOrderByCountThenKeyAndHonorSize() {
		var params = new Params();
		params.setSize(0);
		params.setAggregations(List.of(terms("langs", "LANG~CODE", null)));

		var result = search.search(params);
		assertThat(result.uuids()).isEmpty();
		assertThat(result.aggregations().get("langs"))
				.extracting(AggregationResult::getKey, AggregationResult::getValue)
				.containsExactly(tuple("cze", "2"), tuple("ger", "2"), tuple("lat", "1"));

		params.setAggregations(List.of(terms("langs", "LANG~CODE", 2)));
		assertThat(search.search(params).aggregations().get("langs")).hasSize(2);
	}

	@Test
	void termsBucketsRespectFiltersAndIdLabelFields() {
		var params = params(eq("type", "FUND"));
		params.setSize(0);
		params.setAggregations(List.of(terms("items", "REL~ENTITY~ID~LABEL", 9999)));

		assertThat(search.search(params).aggregations().get("items"))
				.extracting(AggregationResult::getKey)
				.containsExactly("ent-1|Karel Novák", "ent-2|Jan Dvořák");
	}

	@Test
	void termsOnUnknownFieldAggregatesToNoBuckets() {
		var params = new Params();
		params.setSize(0);
		params.setAggregations(List.of(terms("FUND~INST~REF", "FUND~INST~REF~ID~LABEL", 9999)));

		assertThat(search.search(params).aggregations().get("FUND~INST~REF")).isEmpty();
	}

	@Test
	void minMaxAggregationsFormatDateBounds() {
		var max = new MaxAggregation();
		max.setName("maxH");
		max.setField("UNIT~DATE~H");
		max.setFormat("yyyy");
		var min = new MinAggregation();
		min.setName("minL");
		min.setField("UNIT~DATE~L");
		min.setFormat("yyyy");

		var params = params(eq("type", "FUND"));
		params.setSize(0);
		params.setAggregations(List.of(max, min));

		var aggregations = search.search(params).aggregations();
		assertThat(aggregations.get("maxH").get(0).getAsString()).isEqualTo("1910");
		assertThat(aggregations.get("minL").get(0).getAsString()).isEqualTo("1800");
	}

	@Test
	void minMaxWithoutMatchingDocumentsIsNull() {
		var max = new MaxAggregation();
		max.setName("maxH");
		max.setField("UNIT~DATE~H");
		max.setFormat("yyyy");

		var params = params(eq("LANG~CODE", "lat")); // no dating on that record
		params.setSize(0);
		params.setAggregations(List.of(max));

		var result = search.search(params).aggregations().get("maxH").get(0);
		assertThat(result.getValue()).isNull();
		assertThat(result.getAsString()).isNull();
	}

	@Test
	void searchAfterIsRejected() {
		var params = new Params();
		params.setSearchAfter("opaque-cursor");
		assertThatThrownBy(() -> search.search(params)).isInstanceOf(UnsupportedOperationException.class);
	}

	// --- helpers --------------------------------------------------------------

	private static String uuid(int n) {
		return UUID.nameUUIDFromBytes(("oldapi-lucene-" + n).getBytes()).toString();
	}

	private static ApuDocument doc(String uuid, String name, String type, Map<String, List<Object>> values) {
		var document = new ApuDocument();
		document.setUuid(uuid);
		document.setName(name);
		document.setNameSort(CONTENT_LOCALE.sortKey(name));
		document.setType(type);
		document.setApuSourceId(1);
		document.getValues().putAll(values);
		return document;
	}

	private static Params params(Filter filter) {
		var params = new Params();
		params.setFilters(List.of(filter));
		return params;
	}

	private static EqFilter eq(String field, String value) {
		var filter = new EqFilter();
		filter.setField(field);
		filter.setValue(value);
		return filter;
	}

	private static RangeFilter range(String field, String gte, String lte) {
		var filter = new RangeFilter();
		filter.setField(field);
		filter.setGte(gte);
		filter.setLte(lte);
		return filter;
	}

	private static FieldSort fieldSort(String field) {
		var sort = new FieldSort();
		sort.setField(field);
		return sort;
	}

	private static TermsAggregation terms(String name, String field, Integer size) {
		var aggregation = new TermsAggregation();
		aggregation.setAggregator(BucketAggregator.TERMS);
		aggregation.setName(name);
		aggregation.setField(field);
		aggregation.setSize(size);
		return aggregation;
	}

	@AfterEach
	void tearDown() throws Exception {
		index.close();
	}

}
