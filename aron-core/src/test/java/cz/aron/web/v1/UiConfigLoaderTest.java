package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.aron.api.v1.model.MenuItem;
import cz.aron.api.v1.model.MenuItemCode;

/** Plain unit test of the pageTemplate.yaml → UiConfig conversion (no Spring). */
class UiConfigLoaderTest {

	private static final String HELP_URL = "http://help.example";

	@TempDir
	Path tempDir;

	private UiConfigLoader loader(String yaml, String helpUrl) throws IOException {
		Path file = tempDir.resolve("pageTemplate.yaml");
		Files.writeString(file, yaml, StandardCharsets.UTF_8);
		var loader = new UiConfigLoader(file.toString(), helpUrl);
		loader.load();
		return loader;
	}

	@Test
	void minimalTemplateGetsDefaults() throws IOException {
		var config = loader("name: Testovací portál\n", HELP_URL).getConfig();

		assertThat(config.getName()).isEqualTo("Testovací portál");
		assertThat(config.getLocalizations()).containsExactly("cs_CZ");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.FUND, MenuItemCode.ARCH_DESC, MenuItemCode.ENTITY, MenuItemCode.HELP);
		assertThat(config.getMenuItems().get(3).getUrl()).isEqualTo(HELP_URL);
	}

	@Test
	void missingNameFallsBackToPortalDefault() throws IOException {
		assertThat(loader("localizations:\n  - cs_CZ\n  - en_US\n", HELP_URL).getConfig().getName())
				.isEqualTo("Archiv online");
	}

	@Test
	void explicitMenuIsPassedThroughTyped() throws IOException {
		var config = loader("""
				name: Portál
				localizations:
				  - cs_CZ
				  - en_US
				menu:
				  - code: INSTITUTION
				  - code: ARCH_DESC
				    color: "#79a7d1"
				  - code: NEWS
				    url: https://archiv.example/aktuality
				""", HELP_URL).getConfig();

		assertThat(config.getLocalizations()).containsExactly("cs_CZ", "en_US");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.INSTITUTION, MenuItemCode.ARCH_DESC, MenuItemCode.NEWS);
		assertThat(config.getMenuItems().get(0).getColor()).isNull();
		assertThat(config.getMenuItems().get(1).getColor()).isEqualTo("#79a7d1");
		assertThat(config.getMenuItems().get(2).getUrl()).isEqualTo("https://archiv.example/aktuality");
	}

	@Test
	void helpWithoutAnyUrlIsDropped() throws IOException {
		var config = loader("menu:\n  - code: FUND\n  - code: HELP\n", "").getConfig();

		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(MenuItemCode.FUND);
	}

	@Test
	void unknownMenuCodeFailsTheStartup() {
		assertThatThrownBy(() -> loader("menu:\n  - code: TYPO\n", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("TYPO");
	}

}
