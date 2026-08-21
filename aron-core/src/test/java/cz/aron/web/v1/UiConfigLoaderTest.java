package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

import cz.aron.api.v1.model.FooterLink;
import cz.aron.api.v1.model.FooterLinkCode;
import cz.aron.api.v1.model.MenuItem;
import cz.aron.api.v1.model.MenuItemCode;
import cz.aron.api.v1.model.UiConfig;

/**
 * Plain unit test of the pageTemplate.yaml → UiConfig conversion (no Spring). The
 * {@code homepage:} section has its own test ({@link HomePageConfigTest}), so the
 * fixtures here need no facets and no image directory.
 */
class UiConfigLoaderTest {

	private static final String HELP_URL = "http://help.example";

	@TempDir
	Path tempDir;

	private static final Locale CZECH = Locale.forLanguageTag("cs");

	private UiConfigLoader loader(String yaml, String helpUrl) throws IOException {
		Path file = tempDir.resolve("pageTemplate.yaml");
		Files.writeString(file, yaml, StandardCharsets.UTF_8);
		var loader = new UiConfigLoader(file.toString(), helpUrl, new FacetScope(List.of()),
				new ResultImages(new MockHttpServletRequest(), ""));
		loader.load();
		return loader;
	}

	private UiConfig config(String yaml) throws IOException {
		return loader(yaml, HELP_URL).getConfig(CZECH);
	}

	@Test
	void minimalTemplateGetsDefaults() throws IOException {
		var config = config("name: Testovací portál\n");

		assertThat(config.getName()).isEqualTo("Testovací portál");
		// English is the source language: an unconfigured deployment gets it, a
		// Czech one declares cs_CZ first
		assertThat(config.getLocalizations()).containsExactly("en");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.FUND, MenuItemCode.ARCH_DESC, MenuItemCode.ENTITY, MenuItemCode.HELP);
		assertThat(config.getMenuItems().get(3).getUrl()).isEqualTo(HELP_URL);
		// a deployment without a footer section publishes no links
		assertThat(config.getFooterLinks()).isEmpty();
	}

	@Test
	void missingNameFallsBackToPortalDefault() throws IOException {
		assertThat(config("localizations:\n  - cs_CZ\n  - en_US\n").getName()).isEqualTo("Archives online");
	}

	@Test
	void explicitMenuIsPassedThroughTyped() throws IOException {
		var config = config("""
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
				""");

		assertThat(config.getLocalizations()).containsExactly("cs_CZ", "en_US");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.INSTITUTION, MenuItemCode.ARCH_DESC, MenuItemCode.NEWS);
		assertThat(config.getMenuItems().get(0).getColor()).isNull();
		assertThat(config.getMenuItems().get(1).getColor()).isEqualTo("#79a7d1");
		assertThat(config.getMenuItems().get(2).getUrl()).isEqualTo("https://archiv.example/aktuality");
	}

	@Test
	void helpWithoutAnyUrlIsDropped() throws IOException {
		var config = loader("menu:\n  - code: FUND\n  - code: HELP\n", "").getConfig(CZECH);

		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(MenuItemCode.FUND);
	}

	@Test
	void footerLinksCarryCodesAndLocalizedLabels() throws IOException {
		// the accessibility statement a public-sector deployment must publish is
		// a well-known code the UI labels itself; free links carry their label
		String yaml = """
				localizations:
				  - cs_CZ
				  - en
				footer:
				  links:
				    - code: ACCESSIBILITY
				      url: https://archiv.example/pristupnost
				    - url: https://archiv.example/kontakt
				      label:
				        cs: Kontakt
				        en: Contact
				    - url: https://archiv.example/provozovatel
				      label: Provozovatel
				""";

		var czech = loader(yaml, HELP_URL).getConfig(CZECH).getFooterLinks();
		assertThat(czech).extracting(FooterLink::getCode, FooterLink::getLabel, FooterLink::getUrl).containsExactly(
				tuple(FooterLinkCode.ACCESSIBILITY, null, "https://archiv.example/pristupnost"),
				tuple(null, "Kontakt", "https://archiv.example/kontakt"),
				tuple(null, "Provozovatel", "https://archiv.example/provozovatel"));

		// labels follow the reader's language; a single-string label is the source
		// language and stays as it is
		var english = loader(yaml, HELP_URL).getConfig(Locale.ENGLISH).getFooterLinks();
		assertThat(english).extracting(FooterLink::getLabel)
				.containsExactly(null, "Contact", "Provozovatel");
	}

	@Test
	void footerLinkWithoutCodeOrLabelFailsTheStartup() {
		assertThatThrownBy(() -> loader("footer:\n  links:\n    - url: https://archiv.example/x\n", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("https://archiv.example/x");
	}

	@Test
	void unknownFooterLinkCodeFailsTheStartup() {
		assertThatThrownBy(() -> loader(
				"footer:\n  links:\n    - code: TYPO\n      url: https://archiv.example/x\n", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("TYPO");
	}

	@Test
	void primaryColorIsOptionalAndReadAsAPair() throws IOException {
		// a deployment that configures none keeps the portal default, which lives
		// in the UI's palette - not here
		assertThat(loader("name: Portál", HELP_URL).getPrimaryColor()).isNull();

		var color = loader("""
				primaryColor:
				  dark: hsl(272, 14%, 21%)
				  main: "#5b4a63"
				""", HELP_URL).getPrimaryColor();
		assertThat(color.dark()).isEqualTo("hsl(272, 14%, 21%)");
		assertThat(color.main()).isEqualTo("#5b4a63");
	}

	@Test
	void halfConfiguredOrUnusablePrimaryColorFailsTheStartup() {
		// one shade alone would put the deployment's own header above tiles in the
		// portal's colour
		assertThatThrownBy(() -> loader("""
				primaryColor:
				  dark: black
				""", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("main");
		// the value ends up in the page's stylesheet, so anything that is not a
		// colour stops the startup instead of being written there
		assertThatThrownBy(() -> loader("""
				primaryColor:
				  dark: "red; } body { display: none"
				  main: red
				""", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is not a CSS color");
	}

	@Test
	void unknownMenuCodeFailsTheStartup() {
		assertThatThrownBy(() -> loader("menu:\n  - code: TYPO\n", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("TYPO");
	}

}
