package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.aron.api.v1.model.FooterLink;
import cz.aron.api.v1.model.FooterLinkCode;
import cz.aron.api.v1.model.MenuItem;
import cz.aron.api.v1.model.MenuItemCode;
import cz.aron.api.v1.model.UiConfig;

/** Plain unit test of the pageTemplate.yaml → UiConfig conversion (no Spring). */
class UiConfigLoaderTest {

	private static final String HELP_URL = "http://help.example";

	@TempDir
	Path tempDir;

	private static final Locale CZECH = Locale.forLanguageTag("cs");

	private UiConfigLoader loader(String yaml, String helpUrl) throws IOException {
		Path file = tempDir.resolve("pageTemplate.yaml");
		Files.writeString(file, yaml, StandardCharsets.UTF_8);
		var loader = new UiConfigLoader(file.toString(), helpUrl);
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
		assertThat(config.getLocalizations()).containsExactly("cs_CZ");
		assertThat(config.getMenuItems()).extracting(MenuItem::getCode).containsExactly(
				MenuItemCode.FUND, MenuItemCode.ARCH_DESC, MenuItemCode.ENTITY, MenuItemCode.HELP);
		assertThat(config.getMenuItems().get(3).getUrl()).isEqualTo(HELP_URL);
		// a deployment without a footer section publishes no links
		assertThat(config.getFooterLinks()).isEmpty();
	}

	@Test
	void missingNameFallsBackToPortalDefault() throws IOException {
		assertThat(config("localizations:\n  - cs_CZ\n  - en_US\n").getName()).isEqualTo("Archiv online");
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
	void unknownMenuCodeFailsTheStartup() {
		assertThatThrownBy(() -> loader("menu:\n  - code: TYPO\n", HELP_URL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("TYPO");
	}

}
