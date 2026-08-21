package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.ApuType;
import cz.aron.api.v1.model.HomePage;
import cz.aron.api.v1.model.LinkTile;
import cz.aron.api.v1.model.RangeFilter;
import cz.aron.api.v1.model.SearchTile;
import cz.aron.api.v1.model.TextFilter;
import cz.aron.api.v1.model.TileKind;
import cz.aron.api.v1.model.TileStyle;
import cz.aron.api.v1.model.ValuesFilter;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.FacetType;

/**
 * Plain unit test of the {@code homepage:} configuration (no Spring): the shape
 * a deployment writes, the typed home page a reader gets, and the validation
 * that has to stop the startup - a tile the search endpoint would answer 400 to
 * must never reach a reader.
 */
class HomePageConfigTest {

	private static final Locale CZECH = Locale.forLanguageTag("cs");

	/** Facets of the fixture deployment: ARCH_DESC has all three filterable kinds. */
	private static final FacetScope FACETS = new FacetScope(List.of(
			facet(FacetType.ENUM, "UNIT~TYPE", ApuType.ARCH_DESC),
			facet(FacetType.FULLTEXT, "TITLE", ApuType.ARCH_DESC),
			facet(FacetType.UNITDATE, "UNIT~DATE", ApuType.ARCH_DESC),
			facet(FacetType.MULTI_REF_EXT, "OLD~ONLY", ApuType.ARCH_DESC),
			facet(FacetType.ENUM, "INST~REF", ApuType.FUND)));

	private static FacetConfigDto facet(FacetType type, String source, ApuType apuType) {
		var facet = new FacetConfigDto();
		facet.setType(type);
		facet.setSource(source);
		facet.setWhen(Map.of("apuType", apuType.getValue()));
		return facet;
	}

	private static HomePageConfig parse(String yaml) {
		Map<String, Object> template = new Yaml().load(yaml);
		return HomePageConfig.parse(template.get("homepage"), FACETS, name -> name.endsWith(".jpg"));
	}

	private static HomePage render(String yaml) {
		return parse(yaml).render(CZECH, name -> "/aron/api/v1/ui/result-images/" + name);
	}

	@Test
	void noHomepageSectionMeansTheSearchBoxAlone() {
		assertThat(HomePageConfig.parse(null, FACETS, name -> true)).isNull();
		// an empty groups list is a configuration, not an error
		assertThat(parse("homepage:\n  groups: []\n")).isNull();
	}

	@Test
	void aTileLeadingIntoASectionCarriesTypedFilters() {
		var page = render("""
				homepage:
				  groups:
				    - label:
				        cs: Mohlo by vás zajímat
				        en: You might be interested in
				      style: GRID
				      tiles:
				        - label: { cs: Matriky, en: Parish registers }
				          note: { cs: církevní i civilní, en: church and civil }
				          image: { name: matriky.jpg, positionY: 30% }
				          columnSpan: 2
				          rowSpan: 2
				          apuType: ARCH_DESC
				          query: Zámrsk
				          filters:
				            - facet: UNIT_TYPE
				              values: [matrika, kronika]
				""");

		var group = page.getGroups().get(0);
		assertThat(group.getLabel()).isEqualTo("Mohlo by vás zajímat");
		assertThat(group.getStyle()).isEqualTo(TileStyle.GRID);

		var tile = (SearchTile) group.getTiles().get(0);
		assertThat(tile.getKind()).isEqualTo(TileKind.SEARCH);
		assertThat(tile.getLabel()).isEqualTo("Matriky");
		assertThat(tile.getNote()).isEqualTo("církevní i civilní");
		// the server builds the URL, so nothing absolute is baked into a client
		assertThat(tile.getImageUrl()).isEqualTo("/aron/api/v1/ui/result-images/matriky.jpg");
		assertThat(tile.getImagePositionY()).isEqualTo("30%");
		assertThat(tile.getColumnSpan()).isEqualTo(2);
		assertThat(tile.getApuType()).isEqualTo(ApuType.ARCH_DESC);
		assertThat(tile.getQuery()).isEqualTo("Zámrsk");
		// the facet's configured type decides the filter kind; the code reaches the
		// API in the tilde form the search endpoint matches on
		assertThat(tile.getFilters()).singleElement().isInstanceOfSatisfying(ValuesFilter.class, filter -> {
			assertThat(filter.getFacet()).isEqualTo("UNIT~TYPE");
			assertThat(filter.getValues()).containsExactly("matrika", "kronika");
		});
	}

	@Test
	void textAndDatingFacetsGetTheirOwnFilterKinds() {
		var tiles = render("""
				homepage:
				  groups:
				    - label: Filtry
				      tiles:
				        - label: Podle názvu
				          apuType: ARCH_DESC
				          filters:
				            - facet: TITLE
				              q: pozemková kniha
				        - label: Podle datace
				          apuType: ARCH_DESC
				          filters:
				            - facet: UNIT_DATE
				              from: 1800
				              to: 1850
				""").getGroups().get(0).getTiles();

		assertThat(((SearchTile) tiles.get(0)).getFilters()).singleElement()
				.isInstanceOfSatisfying(TextFilter.class, filter -> {
					assertThat(filter.getFacet()).isEqualTo("TITLE");
					assertThat(filter.getQ()).isEqualTo("pozemková kniha");
				});
		assertThat(((SearchTile) tiles.get(1)).getFilters()).singleElement()
				.isInstanceOfSatisfying(RangeFilter.class, filter -> {
					assertThat(filter.getFrom()).isEqualTo("1800");
					assertThat(filter.getTo()).isEqualTo("1850");
				});
	}

	@Test
	void aTileLeadingOutOfThePortalIsALink() {
		var page = render("""
				homepage:
				  groups:
				    - label: Jiné zdroje
				      tiles:
				        - label: Porta fontium
				          url: https://www.portafontium.eu
				        - label: badatelna@example.org
				          url: "mailto:badatelna@example.org"
				""");

		// no style configured: a plain row of links needs no picture grid
		assertThat(page.getGroups().get(0).getStyle()).isEqualTo(TileStyle.LIST);
		assertThat(page.getGroups().get(0).getTiles()).extracting(tile -> ((LinkTile) tile).getUrl())
				.containsExactly("https://www.portafontium.eu", "mailto:badatelna@example.org");
	}

	@Test
	void aSectionTileNeedsNoFilterAtAll() {
		var tile = (SearchTile) render("""
				homepage:
				  groups:
				    - label: Sekce
				      tiles:
				        - label: Archivní soubory
				          apuType: FUND
				""").getGroups().get(0).getTiles().get(0);

		assertThat(tile.getApuType()).isEqualTo(ApuType.FUND);
		assertThat(tile.getFilters()).isEmpty();
	}

	@Test
	void labelsFollowTheReadersLanguageAndFallBackToTheFirstOne() {
		String yaml = """
				homepage:
				  groups:
				    - label: { cs: Skupina, en: Group }
				      tiles:
				        - label: { cs: Matriky, en: Parish registers }
				          url: https://example.org
				""";

		assertThat(render(yaml).getGroups().get(0).getTiles().get(0).getLabel()).isEqualTo("Matriky");
		var english = parse(yaml).render(Locale.ENGLISH, name -> name);
		assertThat(english.getGroups().get(0).getLabel()).isEqualTo("Group");
		assertThat(english.getGroups().get(0).getTiles().get(0).getLabel()).isEqualTo("Parish registers");
		// a language the deployment did not translate still gets text, not nothing
		var german = parse(yaml).render(Locale.GERMAN, name -> name);
		assertThat(german.getGroups().get(0).getTiles().get(0).getLabel()).isEqualTo("Matriky");
	}

	@Test
	void aFilterOnAFacetTheSectionDoesNotHaveFailsTheStartup() {
		// the search endpoint answers 400 for such a filter, so the reader would
		// follow the tile straight into an error page
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          apuType: FUND
				          filters:
				            - facet: UNIT_TYPE
				              values: [matrika]
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("UNIT_TYPE")
				.hasMessageContaining("FUND");
	}

	@Test
	void aFacetTheNewApiDoesNotServeCountsAsAbsent() {
		// MULTI_REF_EXT is served by the old API only - a tile cannot use it
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Staré
				          apuType: ARCH_DESC
				          filters:
				            - facet: OLD_ONLY
				              values: [x]
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("OLD_ONLY");
	}

	@Test
	void aFilterShapeThatDoesNotFitTheFacetTypeFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          apuType: ARCH_DESC
				          filters:
				            - facet: UNIT_TYPE
				              q: matrika
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("'values' list");
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Datace
				          apuType: ARCH_DESC
				          filters:
				            - facet: UNIT_DATE
				              values: [1800]
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("'from' or a 'to'");
	}

	@Test
	void aTileMustLeadEitherOutOfThePortalOrIntoASection() {
		String both = """
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          url: https://example.org
				          apuType: ARCH_DESC
				""";
		String neither = """
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				""";
		assertThatThrownBy(() -> parse(both)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not both");
		assertThatThrownBy(() -> parse(neither)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not both");
	}

	@Test
	void anInAppOrSchemelessUrlFailsTheStartup() {
		// the old portal guessed in-app vs. external from the string; here an
		// in-portal destination is a SEARCH tile and a link always carries a scheme
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Archivalie
				          url: /arch-desc?f=[]
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("absolute http(s) or mailto");
	}

	@Test
	void collectionHasNoSectionToSearchIn() {
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Sbírky
				          apuType: COLLECTION
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("COLLECTION");
	}

	@Test
	void unservableImageSpanOrPositionFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          url: https://example.org
				          image: { name: ../secrets.txt }
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("resultImages");
		// a wider tile would break the responsive grid
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          url: https://example.org
				          columnSpan: 6
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("columnSpan");
		// the value ends up in the page's style, so anything else stops the startup
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          url: https://example.org
				          image: { name: matriky.jpg, positionX: "50%; background: url(evil)" }
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CSS position keyword");
	}

	@Test
	void aTypoInAKeyOrAMissingLabelFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      tiles:
				        - label: Matriky
				          adress: https://example.org
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("adress");
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - style: LIST
				      tiles:
				        - label: Matriky
				          url: https://example.org
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("label");
		assertThatThrownBy(() -> parse("homepage:\n  groups:\n    - label: Skupina\n"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("no tiles");
	}

	@Test
	void unknownStyleFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				homepage:
				  groups:
				    - label: Skupina
				      style: FILLED
				      tiles:
				        - label: Matriky
				          url: https://example.org
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("FILLED");
	}

}
