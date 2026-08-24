package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import cz.aron.api.v1.model.TextRun;

/**
 * The daoFooter configuration - pure logic, no Spring: what a valid file
 * resolves to per license code and language, and the whole family of mistakes
 * that must stop the startup (a footer is reached with every digitized record,
 * so a mistake found later is found by a reader).
 */
class DaoFooterConfigTest {

	private static final UnaryOperator<String> IMAGE_URL = name -> "/api/v1/ui/images/" + name;

	@TempDir
	Path tempDir;

	/** Images resolve against the test-config directory ({@code record.svg} exists there). */
	private DeploymentImages images() {
		return new DeploymentImages(null, "./src/test/resources/test-config/images");
	}

	private DaoFooterConfig load(String yaml) throws IOException {
		Path file = tempDir.resolve("daoFooter.yaml");
		Files.writeString(file, yaml);
		return new DaoFooterConfig(file.toString(), images());
	}

	@Test
	void unconfiguredMeansNoFooterAnywhere() {
		var config = new DaoFooterConfig("", images());
		assertThat(config.footerFor("CC-BY-4.0", Locale.ENGLISH, IMAGE_URL)).isNull();
	}

	@Test
	void licenseIsMatchedByCodeWithDefaultFallback() throws IOException {
		var config = load("""
				dedication:
				  text: { cs: "Podporil {donor}.", en: "Supported by {donor}." }
				  links:
				    donor: { label: NAKI, url: "https://example.org" }
				licenses:
				  - code: default
				    text: "Licence: {owner}"
				    links:
				      owner: { label: { cs: Archiv, en: Archives }, url: "https://example.org/a" }
				  - code: CC-BY-4.0
				    text: "{license}"
				    image: record.svg
				    links:
				      license: { label: CC BY 4.0, url: "https://creativecommons.org/licenses/by/4.0/" }
				""");

		var matched = config.footerFor("CC-BY-4.0", Locale.ENGLISH, IMAGE_URL);
		assertThat(matched.getDedication()).extracting(TextRun::getText)
				.containsExactly("Supported by ", "NAKI", ".");
		assertThat(matched.getLicense()).singleElement().satisfies(run -> {
			assertThat(run.getText()).isEqualTo("CC BY 4.0");
			assertThat(run.getUrl()).isEqualTo("https://creativecommons.org/licenses/by/4.0/");
		});
		assertThat(matched.getLicenseImage()).isEqualTo("/api/v1/ui/images/record.svg");

		// an unknown code and a missing code both land on the default entry
		for (String code : new String[] { "unknown", null }) {
			var fallback = config.footerFor(code, Locale.forLanguageTag("cs"), IMAGE_URL);
			assertThat(fallback.getLicense()).extracting(TextRun::getText)
					.containsExactly("Licence: ", "Archiv");
			assertThat(fallback.getLicenseImage()).isNull();
		}
		// the reader's language picks the variant
		assertThat(config.footerFor(null, Locale.forLanguageTag("cs"), IMAGE_URL).getDedication())
				.extracting(TextRun::getText).containsExactly("Podporil ", "NAKI", ".");
	}

	@Test
	void licensesWithoutDedicationStillResolve() throws IOException {
		var config = load("""
				licenses:
				  - code: default
				    text: "Volne dilo"
				""");
		var footer = config.footerFor(null, Locale.ENGLISH, IMAGE_URL);
		assertThat(footer.getDedication()).isNull();
		assertThat(footer.getLicense()).extracting(TextRun::getText).containsExactly("Volne dilo");
	}

	@Test
	void unknownCodeWithoutDefaultYieldsDedicationAlone() throws IOException {
		var config = load("""
				dedication:
				  text: "Digitized."
				licenses:
				  - code: CC-BY-4.0
				    text: "CC"
				""");
		var footer = config.footerFor("something-else", Locale.ENGLISH, IMAGE_URL);
		assertThat(footer.getDedication()).extracting(TextRun::getText).containsExactly("Digitized.");
		assertThat(footer.getLicense()).isNull();
	}

	@ParameterizedTest
	@MethodSource("brokenConfigurations")
	void mistakesFailTheStartup(String yaml, String messageFragment) {
		assertThatThrownBy(() -> load(yaml))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(messageFragment);
	}

	static Stream<Arguments> brokenConfigurations() {
		return Stream.of(
				Arguments.of("unknown: 1", "daoFooter: unknown key 'unknown'"),
				Arguments.of("dedication: nonsense", "daoFooter dedication: must be a mapping"),
				Arguments.of("""
						licenses:
						  - text: "No code"
						""", "an entry has no 'code'"),
				Arguments.of("""
						licenses:
						  - code: a
						    text: "x"
						  - code: a
						    text: "y"
						""", "duplicate code 'a'"),
				Arguments.of("""
						licenses:
						  - code: a
						    text: "See {nowhere}"
						""", "'{nowhere}' has no link of that name"),
				Arguments.of("""
						licenses:
						  - code: a
						    text: { cs: "Odkaz {a}", en: "No link here" }
						    links:
						      a: { label: x, url: "https://example.org" }
						""", "link 'a' is not used in every language"),
				Arguments.of("""
						licenses:
						  - code: a
						    text: "x"
						    image: does-not-exist.svg
						""", "image 'does-not-exist.svg' is not served"),
				Arguments.of("{}", "declares neither a dedication nor licenses"));
	}

	@Test
	void missingFileFailsTheStartup() {
		assertThatThrownBy(() -> new DaoFooterConfig(tempDir.resolve("absent.yaml").toString(), images()))
				.hasMessageContaining("does not exist");
	}

}
