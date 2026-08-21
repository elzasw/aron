package cz.aron.web.v1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.aron.api.v1.model.ApuType;
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

	private List<FacetConfigDto> facets;

	@Autowired
	FacetScope(FacetsLoader facetsLoader) {
		this.facetsLoader = facetsLoader;
	}

	/** A fixed facet list - for unit tests, which have no configuration file. */
	FacetScope(List<FacetConfigDto> facets) {
		this.facetsLoader = null;
		this.facets = List.copyOf(facets);
	}

	@PostConstruct
	void load() {
		if (facets != null) {
			return;
		}
		try {
			facets = facetsLoader.loadFacets();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to load facet configuration", e);
		}
	}

	/**
	 * The section's facets, minus the kinds the new API does not serve
	 * ({@code MULTI_REF_EXT} - see {@link FacetType}). Not advertising them also
	 * keeps them out of filter validation: the new API behaves as if a facet it
	 * cannot serve were not configured.
	 */
	public List<FacetConfigDto> facetsFor(ApuType apuType) {
		return facets.stream()
				.filter(f -> f.getType() != FacetType.MULTI_REF_EXT)
				.filter(f -> appliesTo(f, apuType))
				.toList();
	}

	/** The section's facet of this code, or {@code null} when it has none. */
	public FacetConfigDto facet(ApuType apuType, String code) {
		return facetsFor(apuType).stream().filter(f -> Objects.equals(f.getSource(), code)).findFirst().orElse(null);
	}

	/** A facet without a when-condition applies to every APU type. */
	private static boolean appliesTo(FacetConfigDto facet, ApuType apuType) {
		if (facet.getWhen() == null) {
			return true;
		}
		if (facet.getWhen() instanceof Map<?, ?> when) {
			Object condition = when.get("apuType");
			return condition == null || Objects.equals(String.valueOf(condition), apuType.getValue());
		}
		return true;
	}

}
