package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cz.aron.test.api.v1.SystemApi;
import cz.aron.test.api.v1.UiApi;
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

}
