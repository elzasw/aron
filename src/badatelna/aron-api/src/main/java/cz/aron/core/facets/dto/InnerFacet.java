package cz.aron.core.facets.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Lukas Jane (inQool) 01.12.2020.
 */
@Getter
@Setter
public class InnerFacet {
    private FacetType type;
    private String source;
    private int maxItems;
}
