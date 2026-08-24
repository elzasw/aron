package cz.aron.service;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cz.aron.domain.ApuType;

/**
 * Loading of the deployment's citation configuration: no Spring, one temporary
 * config directory per case. A citation is reached by a reader's click, so every
 * mistake the file can carry has to fail the startup instead - that strictness
 * is what these cases pin.
 */
class CitationServiceTest {

	@TempDir
	Path directory;

	@Test
	void readsTheFormsWithTheirScriptsAndTranslatedNames() throws IOException {
		script("citation.groovy");
		script("citation-iso.groovy");
		Files.writeString(directory.resolve("citation_localization.yaml"), """
				forms:
				  en:
				    DEFAULT: Citation
				""", StandardCharsets.UTF_8);

		var forms = CitationService.parse(config("""
				forms:
				  - code: DEFAULT
				    label: Citace
				    script: citation.groovy
				    apuTypes: [ARCH_DESC, FUND]
				  - code: CSN_ISO_690
				    label: Citace dle ČSN ISO 690
				    script: citation-iso.groovy
				    apuTypes: [ARCH_DESC]
				"""));

		assertThat(forms).extracting(CitationService.Form::code, CitationService.Form::apuTypes)
				.containsExactly(
						tuple("DEFAULT", List.of(ApuType.ARCH_DESC, ApuType.FUND)),
						tuple("CSN_ISO_690", List.of(ApuType.ARCH_DESC)));
		// the script is read from beside the configuration file, not from the working directory
		assertThat(forms.get(0).script()).contains("citation");
	}

	@Test
	void aFormsNameIsTranslatedInTheSiblingFileAndFallsBackToTheConfiguredOne() throws IOException {
		script("citation.groovy");
		Files.writeString(directory.resolve("citation_localization.yaml"), """
				forms:
				  en:
				    DEFAULT: Citation
				""", StandardCharsets.UTF_8);
		var forms = CitationService.parse(config("""
				forms:
				  - code: DEFAULT
				    label: Citace
				    script: citation.groovy
				    apuTypes: [FUND]
				"""));

		assertThat(CitationService.label(forms.get(0), Locale.ENGLISH)).isEqualTo("Citation");
		assertThat(CitationService.label(forms.get(0), Locale.forLanguageTag("cs-CZ"))).isEqualTo("Citace");
		assertThat(CitationService.label(forms.get(0), Locale.GERMAN)).isEqualTo("Citace");
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
			an unknown root key            | citations:\\n  - code: X         | unknown key 'citations'
			no forms at all                | forms:                           | must be a non-empty list
			a form without a code          | forms:\\n  - label: Citace       | has no 'code'
			a form without a label         | forms:\\n  - code: A             | has no 'label'
			a form without a script        | forms:\\n  - code: A\\n    label: L | has no 'script'
			an unknown form key            | forms:\\n  - code: A\\n    apuType: FUND | unknown key 'apuType'
			""")
	void aMistakeInTheConfigurationFailsTheStartup(String name, String yaml, String message) throws IOException {
		script("citation.groovy");
		Path file = config(yaml.replace("\\n", "\n"));

		assertThatThrownBy(() -> CitationService.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(message);
	}

	@Test
	void aFormNamingAnUnknownRecordTypeFailsTheStartup() throws IOException {
		script("citation.groovy");
		Path file = config("""
				forms:
				  - code: DEFAULT
				    label: Citace
				    script: citation.groovy
				    apuTypes: [ARCH_DESC, NEEXISTUJE]
				""");

		assertThatThrownBy(() -> CitationService.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("unknown record type 'NEEXISTUJE'");
	}

	@Test
	void aFormMustSayWhichRecordTypesItCovers() throws IOException {
		script("citation.groovy");
		Path file = config("""
				forms:
				  - code: DEFAULT
				    label: Citace
				    script: citation.groovy
				""");

		assertThatThrownBy(() -> CitationService.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("must list the record types it covers");
	}

	@Test
	void twoFormsCannotShareACode() throws IOException {
		script("citation.groovy");
		Path file = config("""
				forms:
				  - code: DEFAULT
				    label: Citace
				    script: citation.groovy
				    apuTypes: [FUND]
				  - code: DEFAULT
				    label: Jiná citace
				    script: citation.groovy
				    apuTypes: [ARCH_DESC]
				""");

		assertThatThrownBy(() -> CitationService.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("duplicate code 'DEFAULT'");
	}

	@Test
	void aMissingScriptFailsTheStartup() throws IOException {
		Path file = config("""
				forms:
				  - code: DEFAULT
				    label: Citace
				    script: neexistuje.groovy
				    apuTypes: [FUND]
				""");

		assertThatThrownBy(() -> CitationService.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("names a script that does not exist");
	}

	@Test
	void aMissingConfigurationFileFailsTheStartup() {
		assertThatThrownBy(() -> CitationService.parse(directory.resolve("neexistuje.yaml")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("does not exist");
	}

	private Path config(String yaml) throws IOException {
		Path file = directory.resolve("citation.yaml");
		Files.writeString(file, yaml, StandardCharsets.UTF_8);
		return file;
	}

	private void script(String name) throws IOException {
		Files.writeString(directory.resolve(name), "objectMapper.writeValueAsString([citation: 'x'])",
				StandardCharsets.UTF_8);
	}

}
