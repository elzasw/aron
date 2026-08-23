package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static cz.aron.web.v1.BuiltInFacets.RELATED_FACET;
import static cz.aron.web.v1.BuiltInFacets.DATE_FACET;
import static cz.aron.web.v1.BuiltInFacets.TYPE_FACET;

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
import cz.aron.search.DocumentFixtures;
import cz.aron.search.SearchIndex;
import cz.aron.test.api.v1.ApuApi;
import cz.aron.test.api.v1.SearchApi;
import cz.aron.test.api.v1.SystemApi;
import cz.aron.test.api.v1.UiApi;
import cz.aron.test.api.v1.model.ApuSearchItem;
import cz.aron.test.api.v1.model.ApuSearchRequest;
import cz.aron.test.api.v1.model.ApuType;
import cz.aron.test.api.v1.model.DetailItem;
import cz.aron.test.api.v1.model.DatingPrecision;
import cz.aron.test.api.v1.model.DetailItemKind;
import cz.aron.test.api.v1.model.LinkItem;
import cz.aron.test.api.v1.model.RefItem;
import cz.aron.test.api.v1.model.TextItem;
import cz.aron.test.api.v1.model.UnitDateItem;
import cz.aron.test.api.v1.model.DetailPart;
import cz.aron.test.api.v1.model.PartViewType;
import cz.aron.test.api.v1.model.QueryMode;
import cz.aron.test.api.v1.model.RangeFilter;
import cz.aron.test.api.v1.model.TreeDirection;
import cz.aron.test.api.v1.model.TreeNode;
import cz.aron.test.api.v1.model.DatingFacetResult;
import cz.aron.test.api.v1.model.EnumFacetResult;
import cz.aron.test.api.v1.model.FacetBucket;
import cz.aron.test.api.v1.model.FooterColumn;
import cz.aron.test.api.v1.model.FooterLink;
import cz.aron.test.api.v1.model.FooterLinkCode;
import cz.aron.test.api.v1.model.FacetDef;
import cz.aron.test.api.v1.model.FacetOptionTooltip;
import cz.aron.test.api.v1.model.FacetOptionsRequest;
import cz.aron.test.api.v1.model.FacetValueCondition;
import cz.aron.test.api.v1.model.FacetResult;
import cz.aron.test.api.v1.model.FacetType;
import cz.aron.test.api.v1.model.RefFacetResult;
import cz.aron.test.api.v1.model.ResultField;
import cz.aron.test.api.v1.model.ResultFieldStyle;
import cz.aron.test.api.v1.model.ResultIcon;
import cz.aron.test.api.v1.model.ResultValue;
import cz.aron.test.api.v1.model.MenuItem;
import cz.aron.test.api.v1.model.MenuItemCode;
import cz.aron.test.api.v1.model.LinkTile;
import cz.aron.test.api.v1.model.SearchTile;
import cz.aron.test.api.v1.model.SystemInfo;
import cz.aron.test.api.v1.model.TileGroup;
import cz.aron.test.api.v1.model.TileKind;
import cz.aron.test.api.v1.model.TileStyle;
import cz.aron.test.api.v1.model.TextRun;
import cz.aron.test.api.v1.model.TotalRelation;
import cz.aron.test.api.v1.model.TypeCount;
import cz.aron.test.api.v1.model.UiConfig;
import cz.aron.test.api.v1.model.RelatedFilter;
import cz.aron.test.api.v1.model.RelationDirection;
import cz.aron.test.api.v1.model.ValuesFilter;

/**
 * Drives the new portal API (/api/v1) through the typed Java client generated
 * from the TypeSpec-emitted contract (csc pattern) - proving the whole pipeline:
 * TypeSpec -> committed OpenAPI -> generated server interface + controller ->
 * generated client -> real HTTP round trip.
 */
class NewApiV1Test extends AbstractTest {

	@Autowired
	private SearchIndex searchIndex;

	/**
	 * ARCH_DESC fixture for the facet features (idempotent: documents replace by
	 * uuid). Assertions filter on the unique v1-* values, so data of other tests
	 * sharing the context cannot interfere.
	 */
	@BeforeEach
	void seedSearchData() {
		var record1 = doc(uuid(1), "V1S matrika Přerov", Map.of(
				"LANG~CODE", List.of("v1-cze"),
				"REL~ENTITY", List.of("ent-v1-a"),
				"REL~ENTITY~LABEL", List.of("Karel Novák"),
				"REL~ENTITY~ID~LABEL", List.of("ent-v1-a|Karel Novák"),
				"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
				"UNIT~DATE~H", List.of("1850-12-31T23:59:59"),
				"ORDERED~CODE", List.of("aaa-most", "zzz-last-by-name")));
		var record2 = doc(uuid(2), "V1S sbírka listin", Map.of(
				"LANG~CODE", List.of("v1-cze"),
				"REL~ENTITY", List.of("ent-v1-b"),
				"REL~ENTITY~LABEL", List.of("Jan Dvořák"),
				"REL~ENTITY~ID~LABEL", List.of("ent-v1-b|Jan Dvořák"),
				"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
				"UNIT~DATE~H", List.of("1910-12-31T23:59:59"),
				"ORDERED~CODE", List.of("aaa-most", "mmm-middle")));
		var record3 = doc(uuid(3), "V1S kronika", Map.of("LANG~CODE", List.of("v1-ger")));
		var fund = doc(uuid(4), "V1S fond města", Map.of(
				"REL~ENTITY", List.of("ent-v1-f"),
				"REL~ENTITY~LABEL", List.of("Okresní archiv"),
				"REL~ENTITY~ID~LABEL", List.of("ent-v1-f|Okresní archiv")));
		fund.setType("FUND");
		searchIndex.indexApus(List.of(record1, record2, record3, fund));
	}

	@Test
	void systemInfoViaGeneratedClient() {
		SystemInfo info = new SystemApi(v1ApiClient()).systemGetInfo();
		assertThat(info.getName()).isEqualTo("aron");
		assertThat(info.getVersion()).isNotBlank();
	}

	@Test
	void uiConfigViaGeneratedClient() {
		UiConfig config = new UiApi(v1ApiClient()).uiGetConfig(null);
		// test-config pageTemplate.yaml declares two localizations and no menu - the default menu applies
		assertThat(config.getName()).isEqualTo("ARON test page template");
		assertThat(config.getLocalizations()).containsExactly("cs_CZ", "en");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.FUND, MenuItemCode.ARCH_DESC, MenuItemCode.ENTITY, MenuItemCode.HELP);
		// the HELP link falls back to the configured help-url
		assertThat(config.getMenuItems().get(3).getUrl()).isEqualTo("http://help.test.example");
		// footer links the deployment publishes: a well-known code the UI labels
		// itself (the accessibility statement) plus a free, localized link
		assertThat(config.getFooterLinks())
				.extracting(FooterLink::getCode, FooterLink::getLabel, FooterLink::getUrl)
				.containsExactly(
						tuple(FooterLinkCode.ACCESSIBILITY, null, "http://accessibility.test.example"),
						tuple(null, "Kontakt", "http://contact.test.example"));

		// the label follows the reader's language
		assertThat(new UiApi(v1ApiClient()).uiGetConfig("en").getFooterLinks())
				.extracting(FooterLink::getLabel).containsExactly(null, "Contact");
	}

	@Test
	void uiConfigCarriesTheDeploymentsHomePage() {
		var page = new UiApi(v1ApiClient()).uiGetConfig(null).getHomePage();

		assertThat(page.getGroups()).extracting(TileGroup::getLabel, TileGroup::getStyle).containsExactly(
				tuple("Mohlo by vás zajímat", TileStyle.GRID),
				// a group with no configured style is a plain row of links
				tuple("Celé sekce", TileStyle.LIST));

		// a tile leading into a section carries typed filters, so the search page
		// receives the constraint as one the reader can see and undo
		var search = (SearchTile) page.getGroups().get(0).getTiles().get(0);
		assertThat(search.getKind()).isEqualTo(TileKind.SEARCH);
		assertThat(search.getApuType()).isEqualTo(ApuType.ARCH_DESC);
		assertThat(search.getNote()).isEqualTo("podle jazyka popisu");
		assertThat(search.getColumnSpan()).isEqualTo(2);
		assertThat(search.getImagePositionY()).isEqualTo("30%");
		// the server builds the image URL from the request's own context path
		assertThat(search.getImageUrl()).isEqualTo("/api/v1/ui/images/record.svg");
		assertThat(search.getFilters()).singleElement().isInstanceOfSatisfying(ValuesFilter.class, filter -> {
			// searchConfig.yaml writes LANG_CODE; the API's facet code is the tilde form
			assertThat(filter.getFacet()).isEqualTo("LANG~CODE");
			assertThat(filter.getValues()).containsExactly("cze");
		});

		var link = (LinkTile) page.getGroups().get(0).getTiles().get(1);
		assertThat(link.getKind()).isEqualTo(TileKind.LINK);
		assertThat(link.getUrl()).isEqualTo("https://www.portafontium.eu");

		// tile labels follow the reader's language like every other configured text
		var english = new UiApi(v1ApiClient()).uiGetConfig("en").getHomePage();
		assertThat(english.getGroups().get(0).getTiles().get(0).getLabel()).isEqualTo("Written in Czech");
	}

	@Test
	void uiConfigCarriesTheHomePagesFooterBand() {
		var footer = new UiApi(v1ApiClient()).uiGetConfig(null).getHomePage().getFooter();

		assertThat(footer.getColumns()).extracting(FooterColumn::getHeading)
				.containsExactly("Základní informace", "Kontakt");

		// prose carries its link inside the sentence: the deployment writes a named
		// placeholder and the server resolves it into runs, so the portal renders
		// its own anchors and never configured markup
		var prose = footer.getColumns().get(0).getParagraphs();
		assertThat(prose.get(0).getRuns()).extracting(TextRun::getText, TextRun::getUrl).containsExactly(
				tuple("Portál je aplikace ", null),
				tuple("Testovacího archivu", "http://archiv.test.example"),
				tuple(" a zpřístupňuje popis archiválií.", null));
		assertThat(prose.get(1).getRuns()).extracting(TextRun::getText).containsExactly("© 2026");

		// a link may carry a deployment-supplied mark, its URL built by the server;
		// the label stays the accessible name
		assertThat(footer.getColumns().get(1).getLinks())
				.extracting(FooterLink::getLabel, FooterLink::getUrl, FooterLink::getImageUrl)
				.containsExactly(tuple("badatelna@test.example", "mailto:badatelna@test.example",
						"/api/v1/ui/images/field.svg"));

		// the whole band follows the reader's language, runs included
		var english = new UiApi(v1ApiClient()).uiGetConfig("en").getHomePage().getFooter();
		assertThat(english.getColumns().get(0).getParagraphs().get(0).getRuns())
				.extracting(TextRun::getText, TextRun::getUrl).containsExactly(
						tuple("The portal is an application of the ", null),
						tuple("Test Archives", "http://archiv.test.example"),
						tuple(" and presents archival descriptions.", null));
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
		var facets = new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC, null);

		// section facets in configuration order; the when-less TEST~FACET applies everywhere
		assertThat(facets).extracting(FacetDef::getCode)
				.containsExactly("TEST~FACET", "TITLE~MAIN", "LANG~CODE", "UNIT~DATE", "REL~ENTITY",
						"DEPENDENT~FACET", "ORDERED~CODE");
		var byCode = facets.stream().collect(Collectors.toMap(FacetDef::getCode, Function.identity()));
		// label = explicit title, or the types.yaml item name
		assertThat(byCode.get("TEST~FACET").getLabel()).isEqualTo("Test facet");
		assertThat(byCode.get("LANG~CODE").getLabel()).isEqualTo("Language");
		assertThat(byCode.get("TITLE~MAIN").getType()).isEqualTo(FacetType.FULLTEXT);
		assertThat(byCode.get("LANG~CODE").getType()).isEqualTo(FacetType.ENUM);
		assertThat(byCode.get("UNIT~DATE").getType()).isEqualTo(FacetType.UNITDATE);
	}

	@Test
	void aFacetSaysWhichSelectionItWaitsFor() {
		// the compound condition's other half: whether it holds depends on the
		// reader's filters, so the definition carries it and the client settles it
		var facets = new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC, "en").stream()
				.collect(Collectors.toMap(FacetDef::getCode, Function.identity()));

		assertThat(facets.get("DEPENDENT~FACET").getOfferedWhen())
				.extracting(FacetValueCondition::getFacet, FacetValueCondition::getValue)
				// the condition names the facet the way the file does (LANG_CODE); the
				// code a client filters on is the tilde form, so it is rewritten here
				.containsExactly(tuple("LANG~CODE", "v1-cze"));
		// a facet that only names a section waits for nothing
		assertThat(facets.get("LANG~CODE").getOfferedWhen()).isEmpty();
	}

	@Test
	void perOptionExplanationsAreServedAndTranslated() {
		// they ride on the definitions, not on the buckets: the search response
		// that carries buckets takes no language
		var czech = new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC, "cs").stream()
				.filter(f -> "LANG~CODE".equals(f.getCode())).findFirst().orElseThrow();
		assertThat(czech.getOptionTooltips())
				.extracting(FacetOptionTooltip::getValue, FacetOptionTooltip::getTooltip)
				.containsExactly(tuple("v1-cze", "čeština včetně starších forem"));

		// the sibling localization file translates them, keyed by the option value
		var english = new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC, "en").stream()
				.filter(f -> "LANG~CODE".equals(f.getCode())).findFirst().orElseThrow();
		assertThat(english.getOptionTooltips())
				.extracting(FacetOptionTooltip::getTooltip)
				.containsExactly("Czech, older forms included");
	}

	@Test
	void aSectionsFacetsStayInThatSection() {
		// the fixture's DEPENDENT~FACET carries the compound condition of the
		// shipped register facets (a section plus a dependency on another facet's
		// value). Reading only the simple form advertised such facets for every
		// section, and INSTITUTION - which configures none of its own - got them
		// as its whole list
		var institution = new SearchApi(v1ApiClient()).searchGetFacets(ApuType.INSTITUTION, "en");
		assertThat(institution).extracting(FacetDef::getCode).containsExactly("TEST~FACET");

		assertThat(new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC, "en"))
				.extracting(FacetDef::getCode).contains("DEPENDENT~FACET");

		// and it is unknown to filter validation elsewhere, which is what keeps a
		// home-page tile from linking into a search the server then rejects
		var foreign = new ApuSearchRequest();
		foreign.setApuType(ApuType.FUND);
		foreign.setFilters(List.of(valuesFilter("DEPENDENT~FACET", "whatever")));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(foreign))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void theConfiguredBucketOrderLeads() {
		// order: names the values an archive wants offered first; frequency alone
		// would bury them (aaa-most is the commonest of the three)
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.ARCH_DESC);
		request.setFilters(List.of(valuesFilter("LANG~CODE", "v1-cze")));
		var facet = new SearchApi(v1ApiClient()).searchSearch(request).getFacets().stream()
				.filter(f -> "ORDERED~CODE".equals(f.getCode())).findFirst().orElseThrow();

		assertThat(facet).isInstanceOfSatisfying(EnumFacetResult.class,
				enumResult -> assertThat(enumResult.getBuckets())
						.extracting(FacetBucket::getValue, FacetBucket::getCount)
						.containsExactly(
								// the two named values, in the order the file gives them
								tuple("zzz-last-by-name", 1L),
								tuple("mmm-middle", 1L),
								// then whatever the file does not mention, by count
								tuple("aaa-most", 2L)));
	}

	@Test
	void searchFindsTheSeedApu() {
		// general search (no apuType): hits the input-dir seed
		var request = new ApuSearchRequest();
		request.setQuery("Testovací");
		var response = new SearchApi(v1ApiClient()).searchSearch(request);

		assertThat(response.getTotal()).isEqualTo(1);
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getUuid()).isEqualTo("5e8c2b41-93a7-4d1e-8ccc-9ddd0eee1aaa");
		assertThat(response.getItems().get(0).getApuType()).isEqualTo(ApuType.INSTITUTION);
		// the general search has no section configuration to apply, so its facets
		// are the built-in ones
		assertThat(response.getFacets()).extracting(FacetResult::getCode)
				.containsExactly(TYPE_FACET, DATE_FACET, RELATED_FACET);

		// section restriction applies
		request.setApuType(ApuType.FUND);
		assertThat(new SearchApi(v1ApiClient()).searchSearch(request).getTotal()).isZero();
	}

	@Test
	void builtInFacetsAreOfferedWhereNoSectionIsChosen() {
		var facets = new SearchApi(v1ApiClient()).searchGetFacets(null, "en");

		assertThat(facets).extracting(FacetDef::getCode, FacetDef::getType).containsExactly(
				tuple(TYPE_FACET, FacetType.ENUM),
				tuple(DATE_FACET, FacetType.UNITDATE),
				tuple(RELATED_FACET, FacetType.REF));
		// their text is the product's own, not a deployment's - and it is served,
		// so an API consumer needs no vocabulary of its own
		assertThat(facets).extracting(FacetDef::getLabel)
				.containsExactly("Record type", "Dating", "Related to");
		assertThat(new SearchApi(v1ApiClient()).searchGetFacets(null, "cs"))
				.extracting(FacetDef::getLabel)
				.containsExactly("Typ záznamu", "Datace", "Souvisí s");

		// a section keeps its configured facets, and the built-in ones stay out of
		// it: a section already is one record type, and its own dating facets say
		// which item type they date
		assertThat(new SearchApi(v1ApiClient()).searchGetFacets(ApuType.ARCH_DESC, "en"))
				.extracting(FacetDef::getCode)
				.doesNotContain(TYPE_FACET, DATE_FACET);
	}

	@Test
	void builtInDatingSpansEveryRecordTypeAndDisclosesTheUndated() {
		// a mixed set on purpose: two dated ARCH_DESC records, one undated, one
		// undated FUND - what the general search normally looks like
		var request = new ApuSearchRequest();
		request.setQuery("V1S");
		var response = new SearchApi(v1ApiClient()).searchSearch(request);
		assertThat(response.getTotal()).isEqualTo(4);

		var dating = (DatingFacetResult) response.getFacets().stream()
				.filter(f -> DATE_FACET.equals(f.getCode())).findFirst().orElseThrow();
		// bounds are the hull of every dating item type, so one facet dates every
		// record type
		assertThat(dating.getBounds().getMinYear()).isEqualTo(1800);
		assertThat(dating.getBounds().getMaxYear()).isEqualTo(1910);
		// and the reader is told what a dating filter would cost them
		assertThat(dating.getUndatedCount()).isEqualTo(2);

		// the filter means what it says: only the record dated 1800-1850 overlaps
		request.setFilters(List.of(rangeFilter(DATE_FACET, "1805", "1852", false)));
		var strict = new SearchApi(v1ApiClient()).searchSearch(request);
		assertThat(strict.getTotal()).isEqualTo(1);
		assertThat(strict.getItems()).extracting(ApuSearchItem::getUuid).containsExactly(uuid(1));
		// bounds ignore the facet's own range filter, so the slider can be widened
		assertThat(((DatingFacetResult) strict.getFacets().stream()
				.filter(f -> DATE_FACET.equals(f.getCode())).findFirst().orElseThrow())
						.getBounds().getMaxYear()).isEqualTo(1910);

		// ... and the reader can put the undated records back
		request.setFilters(List.of(rangeFilter(DATE_FACET, "1805", "1852", true)));
		assertThat(new SearchApi(v1ApiClient()).searchSearch(request).getItems())
				.extracting(ApuSearchItem::getUuid)
				.containsExactlyInAnyOrder(uuid(1), uuid(3), uuid(4));
	}

	@Test
	void builtInTypeFacetNarrowsWithoutLeavingTheGeneralSearch() {
		var request = new ApuSearchRequest();
		request.setQuery("V1S");
		var response = new SearchApi(v1ApiClient()).searchSearch(request);

		assertThat(response.getFacets()).filteredOn(f -> TYPE_FACET.equals(f.getCode()))
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.type(EnumFacetResult.class))
				.satisfies(types -> assertThat(types.getBuckets())
						.extracting(FacetBucket::getValue, FacetBucket::getCount)
						.containsExactlyInAnyOrder(tuple("ARCH_DESC", 3L), tuple("FUND", 1L)));

		// narrowing to one type keeps the reader in the general search
		request.setFilters(List.of(valuesFilter(TYPE_FACET, "FUND")));
		var narrowed = new SearchApi(v1ApiClient()).searchSearch(request);
		assertThat(narrowed.getItems()).extracting(ApuSearchItem::getUuid).containsExactly(uuid(4));
		// the facet's own buckets ignore it (multi-select), so the reader can widen
		assertThat(narrowed.getFacets()).filteredOn(f -> TYPE_FACET.equals(f.getCode()))
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.type(EnumFacetResult.class))
				.satisfies(types -> assertThat(types.getBuckets())
						.extracting(FacetBucket::getValue)
						.containsExactlyInAnyOrder("ARCH_DESC", "FUND"));
		// as do the type-count chips, which lead into a section rather than filter
		assertThat(narrowed.getTypeCounts()).extracting(TypeCount::getApuType)
				.contains(ApuType.ARCH_DESC, ApuType.FUND);
	}

	@Test
	void builtInFacetsAreRejectedInASectionSearch() {
		// a section's facets are its own; offering these there would put a second
		// dating slider next to the ones the deployment configured
		var dated = new ApuSearchRequest();
		dated.setApuType(ApuType.ARCH_DESC);
		dated.setFilters(List.of(rangeFilter(DATE_FACET, "1805", "1852", false)));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(dated))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

		var typed = new ApuSearchRequest();
		typed.setApuType(ApuType.ARCH_DESC);
		typed.setFilters(List.of(valuesFilter(TYPE_FACET, "FUND")));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(typed))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
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
			// both v1-cze records are dated, so there is nothing to offer including
			assertThat(dating.getUndatedCount()).isZero();
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
	void deploymentConfiguredTextIsLocalizedToo() {
		// the deployment's own text lives in the config files, its translations in
		// the *_localization.yaml sibling; the config keeps the source language
		var uiApi = new UiApi(v1ApiClient());
		assertThat(uiApi.uiGetConfig(null).getName()).isEqualTo("ARON test page template");
		assertThat(uiApi.uiGetConfig("en").getName()).isEqualTo("ARON test page template (en)");
		// a language with no translation keeps the source language
		assertThat(uiApi.uiGetConfig("de").getName()).isEqualTo("ARON test page template");

		var searchApi = new SearchApi(v1ApiClient());
		var czech = facetsByCode(searchApi.searchGetFacets(ApuType.ARCH_DESC, null));
		var english = facetsByCode(searchApi.searchGetFacets(ApuType.ARCH_DESC, "en"));

		// an explicit facet title is translated like any other display text
		assertThat(czech.get("TEST~FACET").getLabel()).isEqualTo("Test facet");
		assertThat(english.get("TEST~FACET").getLabel()).isEqualTo("Test facet (en)");
		// so are tooltips; an untranslated one falls back
		assertThat(czech.get("TITLE~MAIN").getTooltip()).isEqualTo("Zadejte část názvu");
		assertThat(english.get("TITLE~MAIN").getTooltip()).isEqualTo("Enter part of the name");
		assertThat(english.get("LANG~CODE").getLabel()).isEqualTo("Language (en)");
	}

	private static java.util.Map<String, FacetDef> facetsByCode(java.util.List<FacetDef> facets) {
		return facets.stream().collect(Collectors.toMap(FacetDef::getCode, Function.identity()));
	}

	@Test
	void presentationLanguageSwitchesTheServerRenderedLabels() {
		var apuApi = new ApuApi(v1ApiClient());

		// no lang - the deployment default (cs_CZ), which has no translations: source names
		var byDefault = apuApi.apuGetDetail(DETAIL_ARCH_DESC, null, null, null);
		assertThat(byDefault.getParts().get(0).getLabel()).isEqualTo("Title");

		// a configured localization with translations in types_localization.yaml
		var inEnglish = apuApi.apuGetDetail(DETAIL_ARCH_DESC, "en", null, null);
		assertThat(inEnglish.getParts().get(0).getLabel()).isEqualTo("Title (en)");

		// an unconfigured language is served in the default rather than refused
		assertThat(apuApi.apuGetDetail(DETAIL_ARCH_DESC, "de", null, null).getParts().get(0).getLabel())
				.isEqualTo("Title");

		// facet labels follow the same language
		var searchApi = new SearchApi(v1ApiClient());
		assertThat(labelOf(searchApi.searchGetFacets(ApuType.ARCH_DESC, null), "LANG~CODE"))
				.isEqualTo("Language");
		assertThat(labelOf(searchApi.searchGetFacets(ApuType.ARCH_DESC, "en"), "LANG~CODE"))
				.isEqualTo("Language (en)");
	}

	@Test
	void conditionalRequestsDoNotServeAnotherLanguagesBody() throws Exception {
		// the ETag covers the language, so a client holding the Czech copy still
		// gets a body when it asks for English
		var czech = get("/api/v1/apu/" + DETAIL_ARCH_DESC);
		String eTag = czech.headers().firstValue("ETag").orElseThrow();
		assertThat(eTag).contains("cs-CZ");

		var revalidated = get("/api/v1/apu/" + DETAIL_ARCH_DESC, "If-None-Match", eTag);
		assertThat(revalidated.statusCode()).isEqualTo(304);

		var english = get("/api/v1/apu/" + DETAIL_ARCH_DESC + "?lang=en", "If-None-Match", eTag);
		assertThat(english.statusCode()).isEqualTo(200);
		assertThat(english.headers().firstValue("ETag").orElseThrow()).contains("en");
	}

	private static String labelOf(java.util.List<FacetDef> facets, String code) {
		return facets.stream().filter(f -> code.equals(f.getCode())).findFirst().orElseThrow().getLabel();
	}

	// --- structured search results ---------------------------------------------

	@Test
	void searchHitsCarryTheirStructuredResultWhenTheDataHasOne() {
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.ARCH_DESC);
		request.setQuery("Kronika");
		request.setSize(20);
		var items = new SearchApi(v1ApiClient()).searchSearch(request).getItems().stream()
				.collect(Collectors.toMap(ApuSearchItem::getUuid, Function.identity(), (a, b) -> a));

		// the fixture APU carries a <result>; its siblings do not, and one response
		// covers both - which is why the feature needs no deployment flag
		var structured = items.get(DETAIL_ARCH_DESC).getStructured();
		assertThat(structured).isNotNull();
		assertThat(items.get(DETAIL_SIBLING_B).getStructured()).isNull();

		assertThat(structured.getCode()).isEqualTo("A_IB");
		// a thumbnail given as a deployment image name is resolved to a usable URL
		assertThat(structured.getThumbnailUrl()).isEqualTo("/api/v1/ui/images/record.svg");
		assertThat(structured.getThumbnailLinkUrl()).isEqualTo("https://example.org/kronika-nahled");

		// rows in delivery order; a row can hold more than one field
		assertThat(structured.getRows()).hasSize(4);
		assertThat(structured.getRows().get(2).getFields()).extracting(ResultField::getCode)
				.containsExactly("J_S", "J_IC");
		assertThat(structured.getRows().get(0).getFields().get(0).getValues())
				.extracting(ResultValue::getText, ResultValue::getRefUuid)
				.containsExactly(tuple("Kronika obce Testov", null));
		// a field can hold several values, and a value can reference another record
		assertThat(structured.getRows().get(2).getFields().get(0).getValues())
				.extracting(ResultValue::getText).containsExactly("K-12", "K-12a");
		assertThat(structured.getRows().get(3).getFields().get(0).getValues())
				.extracting(ResultValue::getText, ResultValue::getRefUuid)
				.containsExactly(tuple("V1D Sbírka kronik", DETAIL_FUND));
	}

	@Test
	void resultLayoutComesTypedFromDeploymentConfigInTheReadersLanguage() {
		var searchApi = new SearchApi(v1ApiClient());

		var czech = searchApi.searchGetResultLayout(null);
		assertThat(czech.getFieldSeparator()).isEqualTo(" | ");
		var byCode = czech.getFields().stream()
				.collect(Collectors.toMap(ResultFieldStyle::getCode, Function.identity()));
		assertThat(byCode.get("N").getHeading()).isTrue();
		assertThat(byCode.get("N").getScale()).isEqualTo(1.2f);
		assertThat(byCode.get("J_S").getPrefix()).isEqualTo("sign.: ");
		assertThat(byCode.get("J_S").getValueSeparator()).isEqualTo(", ");
		// a field image is resolved to a usable URL, like the record icons below
		assertThat(byCode.get("J_S").getIconUrl()).isEqualTo("/api/v1/ui/images/field.svg");
		// the label defaults to the visible prefix; a field styled without either stays unlabeled
		assertThat(byCode.get("J_IC").getLabel()).isEqualTo("Inv. č.: ");
		assertThat(byCode.get("D").getLabel()).isNull();
		// the global iconSize fills in for icons that do not set their own
		assertThat(czech.getIcons()).extracting(ResultIcon::getCode, ResultIcon::getUrl, ResultIcon::getSize)
				.containsExactly(
						tuple("A_IB", "/api/v1/ui/images/record.svg", 35),
						tuple("A_IM", "/api/v1/ui/images/record.svg", 28));

		// prefixes and labels are deployment text, translated in the sibling file
		var english = searchApi.searchGetResultLayout("en").getFields().stream()
				.collect(Collectors.toMap(ResultFieldStyle::getCode, Function.identity()));
		assertThat(english.get("J_S").getPrefix()).isEqualTo("ref.: ");
		assertThat(english.get("J_F").getLabel()).isEqualTo("Archival fonds");
		// an untranslated language keeps the configured source language
		assertThat(searchApi.searchGetResultLayout("de").getFields().stream()
				.filter(field -> "J_S".equals(field.getCode()))
				.findFirst().orElseThrow().getPrefix())
				.isEqualTo("sign.: ");
	}

	@Test
	void deploymentImagesServeOnlyPlainNamesFromTheConfiguredDirectory() throws Exception {
		var image = getBytes("/api/v1/ui/images/record.svg");
		assertThat(image.statusCode()).isEqualTo(200);
		assertThat(contentType(image)).isEqualTo("image/svg+xml");
		assertThat(new String(image.body(), java.nio.charset.StandardCharsets.UTF_8)).contains("<svg");

		// an unknown name, a name outside the directory and an unsupported format
		// are all 404 - the file set is deployment data, not part of the app
		assertThat(get("/api/v1/ui/images/neexistuje.svg").statusCode()).isEqualTo(404);
		assertThat(get("/api/v1/ui/images/types.yaml").statusCode()).isEqualTo(404);
		assertThat(get("/api/v1/ui/images/..%2F..%2Ftypes.yaml").statusCode()).isIn(400, 404);
	}

	@Test
	void detailServesTheRenderModel() {
		var detail = new ApuApi(v1ApiClient()).apuGetDetail(DETAIL_ARCH_DESC, null, null, null);

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
				.extracting(item -> ((TextItem) item).getValue()).containsExactly("Kronika obce Testov");

		var body = detail.getParts().get(1);
		assertThat(body.getLabel()).isEqualTo("Body");
		assertThat(body.getValue()).isEqualTo("Obsahová část");
		// view types come from the display model (PT_TITLE standalone, PT_BODY grouped)
		assertThat(detail.getParts().get(0).getViewType()).isEqualTo(PartViewType.STANDALONE);
		assertThat(body.getViewType()).isEqualTo(PartViewType.GROUPED);
		// items in viewOrder (types.yaml declaration order), invisible one filtered;
		// dating formatted, reference resolved to a link, external link typed
		// each kind deserializes into its own model (the `kind` discriminator drives it)
		assertThat(body.getItems()).extracting(DetailItem::getCode, DetailItem::getKind, Object::getClass)
				.containsExactly(
						tuple("UNIT~DATE", DetailItemKind.UNITDATE, UnitDateItem.class),
						tuple("LANG~CODE", DetailItemKind.TEXT, TextItem.class),
						tuple("CNT~ITEMS", DetailItemKind.TEXT, TextItem.class),
						tuple("REL~ENTITY", DetailItemKind.REF, RefItem.class),
						tuple("LINK~SOURCE", DetailItemKind.LINK, LinkItem.class));
		var unitDate = (UnitDateItem) body.getItems().get(0);
		assertThat(unitDate.getValue()).isEqualTo("1850–1910");
		// the dating's machine-readable bounds are fields of the dating item itself
		assertThat(unitDate.getFrom()).isEqualTo("1850-01-01T00:00:00");
		assertThat(unitDate.getFromPrecision()).isEqualTo(DatingPrecision.YEAR);
		assertThat(((TextItem) body.getItems().get(1)).getValue()).isEqualTo("cze");
		assertThat(((RefItem) body.getItems().get(3)).getRef().getUuid()).isEqualTo(DETAIL_FUND);
		assertThat(((RefItem) body.getItems().get(3)).getRef().getName()).isEqualTo("V1D Sbírka kronik");
		assertThat(((LinkItem) body.getItems().get(4)).getCaption()).isEqualTo("Zdroj digitalizace");
		assertThat(((LinkItem) body.getItems().get(4)).getHref()).isEqualTo("https://example.org/kronika");

		// this fixture carries no attachments or digital objects (DaoServingTest covers those)
		assertThat(detail.getAttachments()).isEmpty();
		assertThat(detail.getDigitalObjects()).isEmpty();
	}

	@Test
	void detailOfTheFundSeesItsChildren() {
		var detail = new ApuApi(v1ApiClient()).apuGetDetail(DETAIL_FUND, null, null, null);
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
				.apuGetDetail("00000000-0000-4000-8000-000000000000", null, null, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
		assertThatThrownBy(() -> new ApuApi(v1ApiClient()).apuGetDetail("neni-uuid", null, null, null))
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


	/**
	 * The relation filter over the built-in {@code ~RELATED} facet - what a
	 * record page's "find related" action sends. INCOMING is the default and
	 * spans every reference item type; OUTGOING follows only the references the
	 * record actually shows, so an index-only one never leads the reader on.
	 */
	@Test
	void relatedFilterSearchesBothEndsOfTheRelation() {
		var searchApi = new SearchApi(v1ApiClient());

		// incoming (the default): whoever references the fund
		var incoming = new ApuSearchRequest();
		incoming.setFilters(List.of(relatedFilter(DETAIL_FUND)));
		assertThat(searchApi.searchSearch(incoming).getItems())
				.extracting(ApuSearchItem::getUuid).contains(DETAIL_ARCH_DESC);

		// ... and the index-only reference makes its own record findable too
		var byIndexOnly = new ApuSearchRequest();
		byIndexOnly.setFilters(List.of(relatedFilter(DETAIL_SIBLING_B)));
		assertThat(searchApi.searchSearch(byIndexOnly).getItems())
				.extracting(ApuSearchItem::getUuid).contains(DETAIL_ARCH_DESC);

		// outgoing: the visible reference only - the index-only one is not a link
		// the reader was offered, so it is not a way out of this record either
		var outgoing = new ApuSearchRequest();
		var outgoingFilter = relatedFilter(DETAIL_ARCH_DESC);
		outgoingFilter.setDirection(RelationDirection.OUTGOING);
		outgoing.setFilters(List.of(outgoingFilter));
		assertThat(searchApi.searchSearch(outgoing).getItems())
				.extracting(ApuSearchItem::getUuid)
				.containsExactly(DETAIL_FUND);

		// a record that shows no reference relates to nothing outwards - the
		// clause must not degenerate into "match everything"
		var nothingToFollow = new ApuSearchRequest();
		var emptyOutgoing = relatedFilter(DETAIL_SIBLING_B);
		emptyOutgoing.setDirection(RelationDirection.OUTGOING);
		nothingToFollow.setFilters(List.of(emptyOutgoing));
		var response = searchApi.searchSearch(nothingToFollow);
		assertThat(response.getTotal()).isZero();
		assertThat(response.getItems()).isEmpty();
	}

	@Test
	void relatedFilterOnAConfiguredFacetStaysWithinItsField() {
		var request = new ApuSearchRequest();
		request.setApuType(ApuType.ARCH_DESC);
		var filter = relatedFilter(DETAIL_FUND);
		filter.setFacet("REL~ENTITY");
		request.setFilters(List.of(filter));
		assertThat(new SearchApi(v1ApiClient()).searchSearch(request).getItems())
				.extracting(ApuSearchItem::getUuid).containsExactly(DETAIL_ARCH_DESC);

		// a facet that is not a relation cannot carry the filter
		var wrongFacet = new ApuSearchRequest();
		wrongFacet.setApuType(ApuType.ARCH_DESC);
		var onEnum = relatedFilter(DETAIL_FUND);
		onEnum.setFacet("LANG~CODE");
		wrongFacet.setFilters(List.of(onEnum));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(wrongFacet))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void relatedFilterRejectsAnUnusableTarget() {
		var malformed = new ApuSearchRequest();
		malformed.setFilters(List.of(relatedFilter("not-a-uuid")));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(malformed))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

		var empty = new ApuSearchRequest();
		var noApus = new RelatedFilter();
		noApus.setFacet(RELATED_FACET);
		empty.setFilters(List.of(noApus));
		assertThatThrownBy(() -> new SearchApi(v1ApiClient()).searchSearch(empty))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
	}

	private static String uuid(int n) {
		return UUID.nameUUIDFromBytes(("v1-search-" + n).getBytes()).toString();
	}

	private static FacetOptionsRequest optionsRequest(ApuType apuType) {
		var request = new FacetOptionsRequest();
		request.setApuType(apuType);
		return request;
	}


	/** RELATED filter on the built-in relation facet (what the "find related" action sends). */
	private static RelatedFilter relatedFilter(String apu) {
		var filter = new RelatedFilter();
		filter.setFacet(RELATED_FACET);
		filter.setApus(List.of(apu));
		return filter;
	}

	/** RANGE filter on a dating facet; {@code includeUndated} adds the undated records. */
	private static RangeFilter rangeFilter(String facet, String from, String to, boolean includeUndated) {
		var filter = new RangeFilter();
		filter.setFacet(facet);
		filter.setFrom(from);
		filter.setTo(to);
		filter.setIncludeUndated(includeUndated);
		return filter;
	}

	private static ValuesFilter valuesFilter(String facet, String value) {
		var filter = new ValuesFilter();
		filter.setFacet(facet);
		filter.setValues(List.of(value));
		return filter;
	}

	private static ApuDocument doc(String uuid, String name, Map<String, List<Object>> values) {
		return DocumentFixtures.apu(uuid, name, "ARCH_DESC", 999_200L, values);
	}

}
