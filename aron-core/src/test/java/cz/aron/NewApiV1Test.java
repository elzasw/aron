package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import cz.aron.test.api.v1.SearchApi;
import cz.aron.test.api.v1.SystemApi;
import cz.aron.test.api.v1.UiApi;
import cz.aron.test.api.v1.model.ApuSearchRequest;
import cz.aron.test.api.v1.model.ApuType;
import cz.aron.test.api.v1.model.FacetDef;
import cz.aron.test.api.v1.model.FacetType;
import cz.aron.test.api.v1.model.MenuItem;
import cz.aron.test.api.v1.model.MenuItemCode;
import cz.aron.test.api.v1.model.SystemInfo;
import cz.aron.test.api.v1.model.UiConfig;

/**
 * Drives the new portal API (/api/v1) through the typed Java client generated
 * from the TypeSpec-emitted contract (csc pattern) - proving the whole pipeline:
 * TypeSpec -> committed OpenAPI -> generated server interface + controller ->
 * generated client -> real HTTP round trip.
 */
class NewApiV1Test extends AbstractTest {

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

}
