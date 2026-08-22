package cz.aron.web.v1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.aron.api.v1.model.ApuType;
import cz.aron.domain.facets.FacetCondition;
import cz.aron.domain.facets.FacetsLoader;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.FacetType;
import jakarta.annotation.PostConstruct;

/**
 * The facets the new API serves, scoped to one APU type - one definition shared
 * by everything that has to agree on it: the search endpoints validate a
 * request's filters against it, and the home-page configuration validates a
 * tile's filters against it at startup. A facet the API does not serve has to be
 * invisible to both, or a tile would link to a search the server then rejects.
 * <p>
 * Facet codes are the tilde form ({@code UNIT~TYPE}): {@link FacetsLoader}
 * rewrites the underscores of searchConfig.yaml, because indexing would
 * otherwise turn them into dots.
 */
@Component
public class FacetScope {

	private final FacetsLoader facetsLoader;

	private List<ScopedFacet> facets;

	/**
	 * One configured facet with its {@code when} condition parsed. Parsed once at
	 * startup rather than per request, and parsed at all because the condition's
	 * grammar has two forms - reading only the simpler one made a facet of one
	 * section apply to every section.
	 *
	 * <p>The two travel together because everything that has to agree about a
	 * facet needs both halves: the section decides where it is served, the value
	 * conditions what else has to be true before it is offered - and a home-page
	 * tile is validated against both.
	 */
	public record ScopedFacet(FacetConfigDto facet, FacetCondition condition) {
	}

	@Autowired
	FacetScope(FacetsLoader facetsLoader) {
		this.facetsLoader = facetsLoader;
	}

	/** A fixed facet list - for unit tests, which have no configuration file. */
	FacetScope(List<FacetConfigDto> facets) {
		this.facetsLoader = null;
		this.facets = scope(facets);
	}

	@PostConstruct
	void load() {
		if (facets != null) {
			return;
		}
		try {
			facets = scope(facetsLoader.loadFacets());
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to load facet configuration", e);
		}
	}

	/** Pairs every facet with its parsed condition; an unreadable one fails here. */
	private static List<ScopedFacet> scope(List<FacetConfigDto> facets) {
		return facets.stream()
				.map(facet -> new ScopedFacet(facet, FacetCondition.parse(facet.getWhen(), facet.getSource())))
				.toList();
	}

	/**
	 * The section's facets, minus the kinds the new API does not serve
	 * ({@code MULTI_REF_EXT} - see {@link FacetType}). Not advertising them also
	 * keeps them out of filter validation: the new API behaves as if a facet it
	 * cannot serve were not configured.
	 */
	public List<ScopedFacet> facetsFor(ApuType apuType) {
		return facets.stream()
				.filter(scoped -> scoped.facet().getType() != FacetType.MULTI_REF_EXT)
				.filter(scoped -> scoped.condition().appliesTo(apuType.getValue()))
				.toList();
	}

	/** The section's facet of this code, or {@code null} when it has none. */
	public ScopedFacet facet(ApuType apuType, String code) {
		return facetsFor(apuType).stream()
				.filter(scoped -> Objects.equals(scoped.facet().getSource(), code))
				.findFirst().orElse(null);
	}

}
