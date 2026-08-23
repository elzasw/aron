package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.ApuType;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.FacetType;

/**
 * Which section a configured facet belongs to - the decision every other part of
 * the new API trusts: the search endpoints validate a request's filters against
 * it and the home page validates a tile's filters against it at startup.
 *
 * <p>Plain unit test on the fixed-list constructor: the conditions are YAML
 * fragments written as a deployment writes them, because the bug this pins came
 * from a form the file uses and the code did not read.
 */
class FacetScopeTest {

	/** A facet whose {@code when} is the given YAML fragment (null = no condition). */
	private static FacetConfigDto facet(String source, String whenYaml) {
		var facet = new FacetConfigDto();
		facet.setSource(source);
		facet.setType(FacetType.ENUM);
		if (whenYaml != null) {
			facet.setWhen(new Yaml().load(whenYaml));
		}
		return facet;
	}

	private static List<String> codesFor(FacetScope scope, ApuType apuType) {
		return scope.facetsFor(apuType).stream().map(scoped -> scoped.facet().getSource()).toList();
	}

	@Test
	void aSectionConditionKeepsTheFacetOutOfEveryOtherSection() {
		var scope = new FacetScope(List.of(
				facet("UNIT~TYPE", "apuType: ARCH_DESC"),
				facet("INST~REF", "apuType: FUND")));

		assertThat(codesFor(scope, ApuType.ARCH_DESC)).containsExactly("UNIT~TYPE");
		assertThat(codesFor(scope, ApuType.FUND)).containsExactly("INST~REF");
		assertThat(codesFor(scope, ApuType.INSTITUTION)).isEmpty();
	}

	@Test
	void aCompoundConditionIsStillBoundToItsSection() {
		// the form the shipped config uses for its register facets: the section
		// plus a dependency on another facet's selection. Reading only the simple
		// form advertised eight ARCH_DESC facets for every section - INSTITUTION,
		// which configures none of its own, got those eight as its whole list
		var scope = new FacetScope(List.of(facet("REG~GEO~REF", """
				all:
				  - apuType: ARCH_DESC
				  - filter: REGISTRY_TYPE
				    value: rejstřík zeměpisný
				""")));

		assertThat(codesFor(scope, ApuType.ARCH_DESC)).containsExactly("REG~GEO~REF");
		for (ApuType other : List.of(ApuType.FUND, ApuType.ENTITY, ApuType.INSTITUTION,
				ApuType.FINDING_AID, ApuType.COLLECTION)) {
			assertThat(codesFor(scope, other)).as("offered for %s", other).isEmpty();
		}
		// the facet must also be unknown to filter validation there, which is what
		// keeps a home-page tile from linking into a search the server rejects
		assertThat(scope.facet(ApuType.INSTITUTION, "REG~GEO~REF")).isNull();
		assertThat(scope.facet(ApuType.ARCH_DESC, "REG~GEO~REF")).isNotNull();
	}

	@Test
	void withoutASectionConditionAFacetBelongsEverywhere() {
		// both spellings of "no section": no when at all, and a conjunction that
		// only constrains another facet's value
		var scope = new FacetScope(List.of(
				facet("TEST~FACET", null),
				facet("EVERYWHERE", """
						all:
						  - filter: UNIT_TYPE
						    value: matrika
						""")));

		for (ApuType apuType : List.of(ApuType.ARCH_DESC, ApuType.FUND, ApuType.INSTITUTION)) {
			assertThat(codesFor(scope, apuType)).containsExactly("TEST~FACET", "EVERYWHERE");
		}
	}

	@Test
	void aFacetTheApiCannotServeIsInvisibleToo() {
		var extended = new FacetConfigDto();
		extended.setSource("OLD~ONLY");
		extended.setType(FacetType.MULTI_REF_EXT);
		extended.setWhen(Map.of("apuType", "ARCH_DESC"));

		assertThat(codesFor(new FacetScope(List.of(extended)), ApuType.ARCH_DESC)).isEmpty();
	}

	/** Every form the grammar does not describe, one case per row. */
	static List<Arguments> unreadableConditions() {
		return List.of(
				Arguments.of("unknown operator", "any:\n  - apuType: FUND", "unknown key 'any'"),
				Arguments.of("unknown apuType", "apuType: FONDS", "unknown apuType 'FONDS'"),
				Arguments.of("apuType with no value", "apuType:", "'apuType' has no value"),
				Arguments.of("apuType beside all", "apuType: FUND\nall:\n  - apuType: FUND",
						"combines 'apuType' with 'all'"),
				Arguments.of("all is a mapping", "all:\n  apuType: FUND", "must be a non-empty list"),
				Arguments.of("empty all", "all: []", "must be a non-empty list"),
				Arguments.of("unknown key in a condition", "all:\n  - apuTypes: FUND", "unknown key 'apuTypes'"),
				Arguments.of("filter without value", "all:\n  - filter: UNIT_TYPE",
						"needs both 'filter' and 'value'"),
				Arguments.of("value without filter", "all:\n  - value: matrika",
						"needs both 'filter' and 'value'"),
				Arguments.of("two apuTypes", "all:\n  - apuType: FUND\n  - apuType: ENTITY",
						"more than one apuType"),
				Arguments.of("condition is not a mapping", "all:\n  - FUND", "must be a mapping"));
	}

	/**
	 * A condition the grammar does not describe stops the startup. It used to mean
	 * "applies to every section", which is invisible until a reader is offered a
	 * facet of somebody else's section - or is not offered one at all.
	 *
	 * <p>One case per row rather than a loop, so a parser regression reports every
	 * form it breaks instead of stopping at the first.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("unreadableConditions")
	void anUnreadableConditionStopsTheStartup(String name, String when, String message) {
		assertThatThrownBy(() -> new FacetScope(List.of(facet("BROKEN", when))).facetsFor(ApuType.FUND))
				.isInstanceOf(IllegalStateException.class)
				// the message names the facet, so an operator knows which entry to fix
				.hasMessageContaining("BROKEN")
				.hasMessageContaining(message);
	}

	@Test
	void aWhenThatIsNotAMappingIsRejected() {
		var facet = new FacetConfigDto();
		facet.setSource("BROKEN");
		facet.setType(FacetType.ENUM);
		facet.setWhen("ARCH_DESC");

		assertThatThrownBy(() -> new FacetScope(List.of(facet)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("'when' must be a mapping");
	}

}
