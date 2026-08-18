package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientResponseException;

import cz.aron.search.ApuDocument;
import cz.aron.search.SearchIndex;
import cz.aron.search.ContentLocale;
import cz.aron.test.api.v1.ApuApi;
import cz.aron.test.api.v1.SearchApi;
import cz.aron.test.api.v1.SystemApi;
import cz.aron.test.api.v1.UiApi;
import cz.aron.test.api.v1.model.ApuSearchRequest;
import cz.aron.test.api.v1.model.ApuType;
import cz.aron.test.api.v1.model.DetailItem;
import cz.aron.test.api.v1.model.DetailItemKind;
import cz.aron.test.api.v1.model.DetailPart;
import cz.aron.test.api.v1.model.PartViewType;
import cz.aron.test.api.v1.model.QueryMode;
import cz.aron.test.api.v1.model.TreeDirection;
import cz.aron.test.api.v1.model.TreeNode;
import cz.aron.test.api.v1.model.DatingFacetResult;
import cz.aron.test.api.v1.model.EnumFacetResult;
import cz.aron.test.api.v1.model.FacetBucket;
import cz.aron.test.api.v1.model.FacetDef;
import cz.aron.test.api.v1.model.FacetOptionsRequest;
import cz.aron.test.api.v1.model.FacetResult;
import cz.aron.test.api.v1.model.FacetType;
import cz.aron.test.api.v1.model.RefFacetResult;
import cz.aron.test.api.v1.model.MenuItem;
import cz.aron.test.api.v1.model.MenuItemCode;
import cz.aron.test.api.v1.model.SystemInfo;
import cz.aron.test.api.v1.model.TotalRelation;
import cz.aron.test.api.v1.model.TypeCount;
import cz.aron.test.api.v1.model.UiConfig;
import cz.aron.test.api.v1.model.ValuesFilter;

/**
 * Drives the new portal API (/api/v1) through the typed Java client generated
 * from the TypeSpec-emitted contract (csc pattern) - proving the whole pipeline:
 * TypeSpec -> committed OpenAPI -> generated server interface + controller ->
 * generated client -> real HTTP round trip.
 */
class NewApiV1Test extends AbstractTest {

	/** The fixtures mirror ApuDocumentBuilder; the test deployment runs the default search locale. */
	private static final ContentLocale CONTENT_LOCALE = new ContentLocale("cs-CZ");

	@Autowired
	private SearchIndex searchIndex;

	/**
	 * ARCH_DESC fixture for the facet features (idempotent: documents replace by
	 * uuid). Assertions filter on the unique v1-* values, so data of other tests
	 * sharing the context cannot interfere.
	 */
	@BeforeEach
	void seedSearchData() {
		var record1 = doc(uuid(1), "V1 matrika Přerov", Map.of(
				"LANG~CODE", List.of("v1-cze"),
				"REL~ENTITY", List.of("ent-v1-a"),
				"REL~ENTITY~LABEL", List.of("Karel Novák"),
				"REL~ENTITY~ID~LABEL", List.of("ent-v1-a|Karel Novák"),
				"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
				"UNIT~DATE~H", List.of("1850-12-31T23:59:59")));
		var record2 = doc(uuid(2), "V1 sbírka listin", Map.of(
				"LANG~CODE", List.of("v1-cze"),
				"REL~ENTITY", List.of("ent-v1-b"),
				"REL~ENTITY~LABEL", List.of("Jan Dvořák"),
				"REL~ENTITY~ID~LABEL", List.of("ent-v1-b|Jan Dvořák"),
				"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
				"UNIT~DATE~H", List.of("1910-12-31T23:59:59")));
		var record3 = doc(uuid(3), "V1 kronika", Map.of("LANG~CODE", List.of("v1-ger")));
		var fund = doc(uuid(4), "V1 fond města", Map.of(
				"REL~ENTITY", List.of("ent-v1-f"),
				"REL~ENTITY~LABEL", List.of("Okresní archiv"),
				"REL~ENTITY~ID~LABEL", List.of("ent-v1-f|Okresní archiv")));
		fund.setType("FUND");
		searchIndex.indexApus(List.of(record1, record2, record3, fund));
	}

	@Test
	void systemInfoViaGeneratedClient() {
		SystemInfo info = new SystemApi(v1ApiClient()).systemGetInfo();
		assertThat(info.getName()).isEqualTo("aron2");
		assertThat(info.getVersion()).isNotBlank();
	}

	@Test
	void uiConfigViaGeneratedClient() {
		UiConfig config = new UiApi(v1ApiClient()).uiGetConfig();
		// test-config pageTemplate.yaml has no menu/localizations - the defaults apply
		assertThat(config.getName()).isEqualTo("ARON test page template");
		assertThat(config.getLocalizations()).containsExactly("cs_CZ");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.FUND, MenuItemCode.ARCH_DESC, MenuItemCode.ENTITY, MenuItemCode.HELP);
		// the HELP link falls back to the configured help-url
		assertThat(config.getMenuItems().get(3).getUrl()).isEqualTo("http://help.test.example");
	}

	@Test
	void uiLogoIsServedWithImageContentType() throws Exception {
		var response = getBytes("/api/v1/ui/logo");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("image/svg+xml");
		assertThat(response.body()).isNotEmpty();
	}

	@Test
	void facetDefinitionsComeTypedFromDeploymentConfig() {
		var facets = new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC);

		// section facets in configuration order; the when-less TEST~FACET applies everywhere
		assertThat(facets).extracting(FacetDef::getCode)
				.containsExactly("TEST~FACET", "TITLE~MAIN", "LANG~CODE", "UNIT~DATE", "REL~ENTITY");
		var byCode = facets.stream().collect(Collectors.toMap(FacetDef::getCode, Function.identity()));
		// label = explicit title, or the types.yaml item name
		assertThat(byCode.get("TEST~FACET").getLabel()).isEqualTo("Test facet");
		assertThat(byCode.get("LANG~CODE").getLabel()).isEqualTo("Language");
		assertThat(byCode.get("TITLE~MAIN").getType()).isEqualTo(FacetType.FULLTEXT);
		assertThat(byCode.get("LANG~CODE").getType()).isEqualTo(FacetType.ENUM);
		assertThat(byCode.get("UNIT~DATE").getType()).isEqualTo(FacetType.UNITDATE);
	}

	@Test
	void searchFindsTheSeedApu() {
		// general search (no apuType): hits the input-dir seed, returns no facets
		var request = new ApuSearchRequest();
		request.setQuery("Testovací");
		var response = new SearchApi(v1ApiClient()).searchSearch(request);

		assertThat(response.getTotal()).isEqualTo(1);
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getUuid()).isEqualTo("5e8c2b41-93a7-4d1e-8ccc-9ddd0eee1aaa");
		assertThat(response.getItems().get(0).getApuType()).isEqualTo(ApuType.INSTITUTION);
		assertThat(response.getFacets()).isEmpty();

		// section restriction applies
		request.setApuType(ApuType.FUND);
		assertThat(new SearchApi(v1ApiClient()).searchSearch(request).getTotal()).isZero();
	}

	@Test
	void relaxedRetryIsReportedInQueryMode() {
		// strict AND yields nothing for a query with one stray word; the server
		// retries any-word once and labels the result (B7)
		var request = new ApuSearchRequest();
		request.setQuery("Testovací slovonavic");
		var response = new SearchApi(v1ApiClient()).searchSearch(request);
		assertThat(response.getQueryMode()).isEqualTo(QueryMode.RELAXED);
		assertThat(response.getTotal()).isGreaterThanOrEqualTo(1);

		// a fully matching query stays strict
		request.setQuery("Testovací");
		assertThat(new SearchApi(v1ApiClient()).searchSearch(request).getQueryMode()).isEqualTo(QueryMode.STRICT);
	}

	@Test
	void totalsCarryTheirAccuracyRelation() {
		// small result set: total is exact
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.ARCH_DESC);
		var response = new SearchApi(v1ApiClient()).searchSearch(request);
		assertThat(response.getTotalRelation()).isEqualTo(TotalRelation.EQ);

		// a totalUpTo below the hit count caps the total and reports GTE
		request.setTotalUpTo(1);
		response = new SearchApi(v1ApiClient()).searchSearch(request);
		assertThat(response.getTotal()).isEqualTo(1);
		assertThat(response.getTotalRelation()).isEqualTo(TotalRelation.GTE);
		// the page itself is not limited by the accuracy
		assertThat(response.getItems().size()).isGreaterThan(1);
	}

	@Test
	void pagingBeyondTheWindowIsRejected() {
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.ARCH_DESC);
		request.setFrom(9_995);
		request.setSize(10); // from + size > 10 000
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(request))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void searchReturnsRefBucketsAndDatingBounds() {
		// the LANG~CODE filter scopes every OTHER facet's result to the fixture
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.ARCH_DESC);
		request.setFilters(List.of(valuesFilter("LANG~CODE", "v1-cze")));
		var response = new SearchApi(v1ApiClient()).searchSearch(request);

		assertThat(response.getTotal()).isEqualTo(2);
		Map<String, FacetResult> byCode = response.getFacets().stream()
				.collect(Collectors.toMap(FacetResult::getCode, Function.identity()));

		// MULTI_REF facets answer with a REF result: uuid as value plus its label
		assertThat(byCode.get("REL~ENTITY")).isInstanceOfSatisfying(RefFacetResult.class,
				ref -> assertThat(ref.getBuckets())
						.extracting(FacetBucket::getValue, FacetBucket::getLabel, FacetBucket::getCount)
						.containsExactlyInAnyOrder(
								tuple("ent-v1-a", "Karel Novák", 1L),
								tuple("ent-v1-b", "Jan Dvořák", 1L)));

		// UNITDATE facets answer with a DATING result carrying the bounds
		assertThat(byCode.get("UNIT~DATE")).isInstanceOfSatisfying(DatingFacetResult.class, dating -> {
			assertThat(dating.getBounds()).isNotNull();
			assertThat(dating.getBounds().getMinYear()).isEqualTo(1800);
			assertThat(dating.getBounds().getMaxYear()).isEqualTo(1910);
		});

		// the ENUM facet's own buckets ignore its filter (multi-select)
		assertThat(byCode.get("LANG~CODE")).isInstanceOfSatisfying(EnumFacetResult.class,
				enumResult -> assertThat(enumResult.getBuckets())
						.extracting(FacetBucket::getValue, FacetBucket::getCount)
						.contains(tuple("v1-cze", 2L), tuple("v1-ger", 1L)));

		// per-type counts respect the filter (both v1-cze records are ARCH_DESC)
		assertThat(response.getTypeCounts())
				.extracting(TypeCount::getApuType, TypeCount::getCount)
				.contains(tuple(ApuType.ARCH_DESC, 2L));
	}

	@Test
	void enumFacetOverReferenceSourceCarriesLabels() {
		// the FUND section configures an ENUM facet over the APU_REF source
		// REL~ENTITY - buckets must resolve display labels, not raw uuids
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.FUND);
		var response = new SearchApi(v1ApiClient()).searchSearch(request);

		var facet = response.getFacets().stream()
				.filter(f -> "REL~ENTITY".equals(f.getCode()))
				.findFirst().orElseThrow();
		assertThat(facet).isInstanceOfSatisfying(EnumFacetResult.class,
				enumResult -> assertThat(enumResult.getBuckets())
						.extracting(FacetBucket::getValue, FacetBucket::getLabel)
						.contains(tuple("ent-v1-f", "Okresní archiv")));
	}

	@Test
	void facetOptionsProvideFoldedTypeAhead() {
		var request = optionsRequest(ApuType.ARCH_DESC);
		request.setQ("novak"); // folded form of "Novák"
		var options = new SearchApi(v1ApiClient()).searchGetFacetOptions("REL~ENTITY", request).getOptions();
		assertThat(options)
				.extracting(FacetBucket::getValue, FacetBucket::getLabel)
				.containsExactly(tuple("ent-v1-a", "Karel Novák"));

		// prefix on the last word
		request.setQ("dvo");
		assertThat(new SearchApi(v1ApiClient()).searchGetFacetOptions("REL~ENTITY", request).getOptions())
				.extracting(FacetBucket::getValue)
				.containsExactly("ent-v1-b");

		// ENUM options match against the value itself
		var enumRequest = optionsRequest(ApuType.ARCH_DESC);
		enumRequest.setQ("v1-c");
		assertThat(new SearchApi(v1ApiClient()).searchGetFacetOptions("LANG~CODE", enumRequest).getOptions())
				.extracting(FacetBucket::getValue)
				.containsExactly("v1-cze");
	}

	@Test
	void facetOptionsRejectNonEnumerableOrUnknownFacets() {
		var request = optionsRequest(ApuType.ARCH_DESC);

		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchGetFacetOptions("TITLE~MAIN", request))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchGetFacetOptions("NEZNAMA", request))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
	}

	// --- APU detail (render model, D-9) ----------------------------------------

	/** Fixture uuids from test-config/import/detail-transfer/apusrc-detail.xml. */
	private static final String DETAIL_FUND = "9f1d0000-0000-4000-8000-90000000000f";
	private static final String DETAIL_ARCH_DESC = "9f1d0000-0000-4000-8000-90000000000d";
	private static final String DETAIL_SIBLING_B = "9f1d0000-0000-4000-8000-90000000000b";
	private static final String DETAIL_SIBLING_C = "9f1d0000-0000-4000-8000-90000000000c";
	private static final String DETAIL_GRANDCHILD = "9f1d0000-0000-4000-8000-9000000000aa";

	@Test
	void detailServesTheRenderModel() {
		var detail = new ApuApi(v1ApiClient()).apuGetDetail(DETAIL_ARCH_DESC, null, null);

		assertThat(detail.getName()).isEqualTo("V1D Kronika obce Testov");
		assertThat(detail.getDescription()).isEqualTo("1850–1910");
		assertThat(detail.getApuType()).isEqualTo(ApuType.ARCH_DESC);
		assertThat(detail.getChildCount()).isZero();

		// tree path = ancestor chain root-first, the APU itself last
		assertThat(detail.getTreePath()).extracting(TreeNode::getUuid, TreeNode::getName)
				.containsExactly(
						tuple(DETAIL_FUND, "V1D Sbírka kronik"),
						tuple(DETAIL_ARCH_DESC, "V1D Kronika obce Testov"));

		// parts ordered by the display model (PT~TITLE first, XML had PT~BODY first);
		// the part with only invisible items is omitted
		assertThat(detail.getParts()).extracting(DetailPart::getCode).containsExactly("PT~TITLE", "PT~BODY");
		assertThat(detail.getParts().get(0).getItems())
				.extracting(DetailItem::getValue).containsExactly("Kronika obce Testov");

		var body = detail.getParts().get(1);
		assertThat(body.getLabel()).isEqualTo("Body");
		assertThat(body.getValue()).isEqualTo("Obsahová část");
		// view types come from the display model (PT_TITLE standalone, PT_BODY grouped)
		assertThat(detail.getParts().get(0).getViewType()).isEqualTo(PartViewType.STANDALONE);
		assertThat(body.getViewType()).isEqualTo(PartViewType.GROUPED);
		// items in viewOrder (types.yaml declaration order), invisible one filtered;
		// dating formatted, reference resolved to a link, external link typed
		assertThat(body.getItems()).extracting(DetailItem::getCode, DetailItem::getKind, DetailItem::getValue)
				.containsExactly(
						tuple("UNIT~DATE", DetailItemKind.TEXT, "1850–1910"),
						tuple("LANG~CODE", DetailItemKind.TEXT, "cze"),
						tuple("CNT~ITEMS", DetailItemKind.TEXT, "12"),
						tuple("REL~ENTITY", DetailItemKind.REF, "V1D Sbírka kronik"),
						tuple("LINK~SOURCE", DetailItemKind.LINK, "Zdroj digitalizace"));
		assertThat(body.getItems().get(3).getRef().getUuid()).isEqualTo(DETAIL_FUND);
		assertThat(body.getItems().get(4).getHref()).isEqualTo("https://example.org/kronika");

		// metadata-only sections of this fixture are empty (binaries arrive with the tiles slice)
		assertThat(detail.getAttachments()).isEmpty();
		assertThat(detail.getDigitalObjects()).isEmpty();
	}

	@Test
	void detailOfTheFundSeesItsChildren() {
		var detail = new ApuApi(v1ApiClient()).apuGetDetail(DETAIL_FUND, null, null);
		assertThat(detail.getApuType()).isEqualTo(ApuType.FUND);
		assertThat(detail.getChildCount()).isEqualTo(3);
		// a root APU's tree path is just itself
		assertThat(detail.getTreePath()).extracting(TreeNode::getUuid).containsExactly(DETAIL_FUND);
	}

	@Test
	void treeUnderReturnsChildrenInTreeOrder() {
		var api = new ApuApi(v1ApiClient());

		var children = api.apuGetTreeNodes(DETAIL_FUND, TreeDirection.UNDER, null, null);
		assertThat(children).extracting(TreeNode::getUuid, TreeNode::getPos, TreeNode::getChildCount)
				.containsExactly(
						tuple(DETAIL_ARCH_DESC, 1, 0),
						tuple(DETAIL_SIBLING_B, 2, 1),
						tuple(DETAIL_SIBLING_C, 3, 0));

		// the grandchild is one level deeper and shows its description in the tree
		var grandchildren = api.apuGetTreeNodes(DETAIL_SIBLING_B, TreeDirection.UNDER, null, null);
		assertThat(grandchildren).hasSize(1);
		assertThat(grandchildren.get(0).getUuid()).isEqualTo(DETAIL_GRANDCHILD);
		assertThat(grandchildren.get(0).getDescription()).isEqualTo("První svazek");
		assertThat(grandchildren.get(0).getDepth()).isGreaterThan(children.get(0).getDepth());
	}

	@Test
	void treeSiblingWindowsFollowTheDirection() {
		var api = new ApuApi(v1ApiClient());

		assertThat(api.apuGetTreeNodes(DETAIL_SIBLING_B, TreeDirection.BEFORE, null, null))
				.extracting(TreeNode::getUuid).containsExactly(DETAIL_ARCH_DESC);
		assertThat(api.apuGetTreeNodes(DETAIL_SIBLING_B, TreeDirection.AFTER, null, null))
				.extracting(TreeNode::getUuid).containsExactly(DETAIL_SIBLING_C);
		// edges of the level have nothing before/after
		assertThat(api.apuGetTreeNodes(DETAIL_ARCH_DESC, TreeDirection.BEFORE, null, null)).isEmpty();
		assertThat(api.apuGetTreeNodes(DETAIL_SIBLING_C, TreeDirection.AFTER, null, null)).isEmpty();
	}

	@Test
	void treeAnswers404AndSupportsConditionalRequests() throws Exception {
		assertThatThrownBy(() -> new ApuApi(v1ApiClient())
				.apuGetTreeNodes("00000000-0000-4000-8000-000000000000", TreeDirection.UNDER, null, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(404));

		var first = get("/api/v1/apu/" + DETAIL_FUND + "/tree?direction=UNDER");
		assertThat(first.statusCode()).isEqualTo(200);
		String eTag = first.headers().firstValue("ETag").orElseThrow();
		var notModified = get("/api/v1/apu/" + DETAIL_FUND + "/tree?direction=UNDER", "If-None-Match", eTag);
		assertThat(notModified.statusCode()).isEqualTo(304);
	}

	@Test
	void detailAnswers404ForUnknownOrMalformedUuid() {
		assertThatThrownBy(() -> new ApuApi(v1ApiClient())
				.apuGetDetail("00000000-0000-4000-8000-000000000000", null, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
		assertThatThrownBy(() -> new ApuApi(v1ApiClient()).apuGetDetail("neni-uuid", null, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void detailSupportsConditionalRequests() throws Exception {
		var first = get("/api/v1/apu/" + DETAIL_ARCH_DESC);
		assertThat(first.statusCode()).isEqualTo(200);
		String eTag = first.headers().firstValue("ETag").orElseThrow();
		assertThat(first.headers().firstValue("Last-Modified")).isPresent();

		var notModified = get("/api/v1/apu/" + DETAIL_ARCH_DESC, "If-None-Match", eTag);
		assertThat(notModified.statusCode()).isEqualTo(304);
		assertThat(notModified.body()).isEmpty();

		var changed = get("/api/v1/apu/" + DETAIL_ARCH_DESC, "If-None-Match", "\"jiny-etag\"");
		assertThat(changed.statusCode()).isEqualTo(200);
	}

	// --- helpers --------------------------------------------------------------

	private static String uuid(int n) {
		return UUID.nameUUIDFromBytes(("v1-search-" + n).getBytes()).toString();
	}

	private static FacetOptionsRequest optionsRequest(ApuType apuType) {
		var request = new FacetOptionsRequest();
		request.setApuType(apuType);
		return request;
	}

	private static ValuesFilter valuesFilter(String facet, String value) {
		var filter = new ValuesFilter();
		filter.setFacet(facet);
		filter.setValues(List.of(value));
		return filter;
	}

	private static ApuDocument doc(String uuid, String name, Map<String, List<Object>> values) {
		var document = new ApuDocument();
		document.setUuid(uuid);
		document.setName(name);
		document.setNameSort(CONTENT_LOCALE.sortKey(name));
		document.setType("ARCH_DESC");
		document.setApuSourceId(999_200L);
		document.getValues().putAll(values);
		return document;
	}

}
