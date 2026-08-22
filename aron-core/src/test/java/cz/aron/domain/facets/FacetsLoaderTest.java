package cz.aron.domain.facets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.TooltipSpec;

/**
 * Reading searchConfig.yaml. The bean binding already refuses an unknown key, so
 * what is worth pinning here is the values it cannot check for itself - and the
 * rewrite that makes a facet's source match the index.
 */
class FacetsLoaderTest {

	private static List<FacetConfigDto> load(Path directory, String yaml) throws IOException {
		Path file = directory.resolve("searchConfig.yaml");
		Files.writeString(file, yaml, StandardCharsets.UTF_8);
		var loader = new FacetsLoader(null);
		ReflectionTestUtils.setField(loader, "facetConfig", file.toString());
		return loader.loadFacets();
	}

	@Test
	void underscoresBecomeTildes(@TempDir Path directory) throws Exception {
		// indexing would turn an underscore into a dot, so the source the file
		// writes is not the code the rest of the API uses
		var facets = load(directory, """
				facets:
				  - when:
				      apuType: ARCH_DESC
				    type: ENUM
				    source: UNIT_TYPE
				    orderBy: ASC
				""");

		assertThat(facets).singleElement()
				.extracting(FacetConfigDto::getSource).isEqualTo("UNIT~TYPE");
	}

	@Test
	void anOrderByItCannotActOnStopsTheStartup(@TempDir Path directory) {
		// anything but ASC means "by frequency", so a typo is silently the opposite
		// of the alphabetical order that was asked for
		assertThatThrownBy(() -> load(directory, """
				facets:
				  - when:
				      apuType: ARCH_DESC
				    type: ENUM
				    source: UNIT_TYPE
				    orderBy: ASCENDING
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("UNIT_TYPE")
				.hasMessageContaining("orderBy must be FREQ or ASC");
	}

	@Test
	void anUnreadableConditionStopsTheStartupWithTheFilesOwnSpelling(@TempDir Path directory) {
		// the message has to name something the operator can search the file for,
		// which is why the condition is checked before the rewrite above
		assertThatThrownBy(() -> load(directory, """
				facets:
				  - when:
				      any:
				        - apuType: ARCH_DESC
				    type: ENUM
				    source: UNIT_TYPE
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("'UNIT_TYPE'")
				.hasMessageContaining("unknown key 'any'");
	}

	@Test
	void twoTooltipsForOneOptionKeepTheFirst(@TempDir Path directory) throws Exception {
		// harmless, and warned about at startup: one of the two texts is never shown
		var facets = load(directory, """
				facets:
				  - when:
				      apuType: ARCH_DESC
				    type: ENUM
				    source: UNIT_TYPE
				    tooltips:
				      - value: matrika
				        tooltip: první
				      - value: matrika
				        tooltip: druhý
				""");

		assertThat(facets.get(0).getTooltips()).extracting(TooltipSpec::getTooltip)
				.containsExactly("první", "druhý");
	}

}
