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
import org.springframework.mock.web.MockHttpServletRequest;

import cz.aron.api.v1.model.ResultFieldStyle;
import cz.aron.api.v1.model.ResultIcon;

/**
 * Loading of the deployment's structured-result layout: no Spring, one temporary
 * config directory per case. Covers the sibling-file overlay and the strictness
 * that turns a configuration mistake into a failed startup rather than a
 * silently ignored key.
 */
class ResultLayoutLoaderTest {

	@TempDir
	Path directory;

	@Test
	void unconfiguredDeploymentGetsAnEmptyLayout() throws IOException {
		var layout = loader("", null).getLayout(Locale.forLanguageTag("cs-CZ"));

		assertThat(layout.getFields()).isEmpty();
		assertThat(layout.getIcons()).isEmpty();
		assertThat(layout.getFieldSeparator()).isNull();
	}

	@Test
	void readsStylingIconsAndTheSeparator() throws IOException {
		var layout = loader("""
				fieldSeparator: " · "
				iconSize: 35
				icons:
				  - code: A_IB
				    image: record.svg
				  - code: A_IM
				    image: record.svg
				    size: 28
				fields:
				  - code: N
				    heading: true
				    scale: 1.2
				    bold: true
				  - code: J_S
				    prefix: "sign.: "
				    valueSeparator: ", "
				    color: "#666666"
				    image: field.svg
				""", null).getLayout(Locale.forLanguageTag("cs-CZ"));

		assertThat(layout.getFieldSeparator()).isEqualTo(" · ");
		assertThat(layout.getFields()).extracting(ResultFieldStyle::getCode, ResultFieldStyle::getHeading,
				ResultFieldStyle::getScale, ResultFieldStyle::getBold)
				.containsExactly(tuple("N", true, 1.2f, true), tuple("J_S", null, null, null));
		var signature = layout.getFields().get(1);
		assertThat(signature.getPrefix()).isEqualTo("sign.: ");
		assertThat(signature.getValueSeparator()).isEqualTo(", ");
		assertThat(signature.getColor()).isEqualTo("#666666");
		assertThat(signature.getIconUrl()).isEqualTo("/aron/api/v1/ui/images/field.svg");
		// the label of a field styled with a prefix is that prefix
		assertThat(signature.getLabel()).isEqualTo("sign.: ");
		assertThat(layout.getFields().get(0).getLabel()).isNull();

		// the global iconSize fills in where an icon does not set its own
		assertThat(layout.getIcons()).extracting(ResultIcon::getCode, ResultIcon::getUrl, ResultIcon::getSize)
				.containsExactly(tuple("A_IB", "/aron/api/v1/ui/images/record.svg", 35),
						tuple("A_IM", "/aron/api/v1/ui/images/record.svg", 28));
	}

	@Test
	void translatesPrefixesAndLabelsFromTheSiblingFile() throws IOException {
		var loader = loader("""
				fields:
				  - code: J_S
				    prefix: "sign.: "
				  - code: J_F
				    label: "Archivní soubor"
				""", """
				fields:
				  en:
				    J_S:
				      prefix: "ref.: "
				    J_F:
				      label: "Archival fonds"
				""");

		assertThat(prefixOf(loader, "en", "J_S")).isEqualTo("ref.: ");
		assertThat(labelOf(loader, "en", "J_F")).isEqualTo("Archival fonds");
		// the configured file keeps the source language and stays the fallback
		assertThat(prefixOf(loader, "cs-CZ", "J_S")).isEqualTo("sign.: ");
		assertThat(labelOf(loader, "de", "J_F")).isEqualTo("Archivní soubor");
	}

	@Test
	void configurationMistakesFailTheStartup() throws IOException {
		assertThatThrownBy(() -> loader("""
				fields:
				  - code: N
				    heading: true
				    colour: red
				""", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("unknown key 'colour'");

		assertThatThrownBy(() -> loader("""
				fields:
				  - prefix: "bez kódu"
				""", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("has no 'code'");

		assertThatThrownBy(() -> loader("""
				icons:
				  - code: A_IB
				""", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("has no 'image'");

		assertThatThrownBy(() -> loader("""
				fields:
				  - code: N
				    scale: velky
				""", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("expected a number");

		// an image reference the deployment cannot serve is a mistake, not a default
		assertThatThrownBy(() -> loader("""
				icons:
				  - code: A_IB
				    image: record.svg
				""", null, false))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("webResources.images is not configured");

		assertThatThrownBy(() -> new ResultLayoutLoader(directory.resolve("chybi.yaml").toString(),
				images(true)).load())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("does not exist");
	}

	private ResultLayoutLoader loader(String layout, String translations) throws IOException {
		return loader(layout, translations, true);
	}

	/** A loaded loader over a temporary resultLayout.yaml (+ optional sibling). */
	private ResultLayoutLoader loader(String layout, String translations, boolean withImages) throws IOException {
		if (layout.isEmpty()) {
			var loader = new ResultLayoutLoader("", images(withImages));
			loader.load();
			return loader;
		}
		Path file = directory.resolve("resultLayout.yaml");
		Files.writeString(file, layout, StandardCharsets.UTF_8);
		if (translations != null) {
			Files.writeString(directory.resolve("resultLayout_localization.yaml"), translations,
					StandardCharsets.UTF_8);
		}
		var loader = new ResultLayoutLoader(file.toString(), images(withImages));
		loader.load();
		return loader;
	}

	/** Image resolution for a deployment served under the /aron subpath. */
	private DeploymentImages images(boolean configured) {
		var request = new MockHttpServletRequest();
		request.setContextPath("/aron");
		return new DeploymentImages(request, configured ? directory.toString() : "");
	}

	private static String prefixOf(ResultLayoutLoader loader, String language, String code) {
		return styleOf(loader, language, code).getPrefix();
	}

	private static String labelOf(ResultLayoutLoader loader, String language, String code) {
		return styleOf(loader, language, code).getLabel();
	}

	private static ResultFieldStyle styleOf(ResultLayoutLoader loader, String language, String code) {
		return loader.getLayout(Locale.forLanguageTag(language)).getFields().stream()
				.filter(field -> code.equals(field.getCode()))
				.findFirst()
				.orElseThrow();
	}

}
