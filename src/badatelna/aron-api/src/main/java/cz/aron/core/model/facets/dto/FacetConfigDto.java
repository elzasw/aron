package cz.aron.core.model.facets.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 18.11.2020.
 */
@Getter
@Setter
public class FacetConfigDto {
//    private WhenConfigDto when;
    private Object when;
    private FacetType type;
    private String source;
    private DisplayType display = DisplayType.ALWAYS;
    private int maxItems;
    private List<IntervalSpec> intervals = new ArrayList<>();
    private List<InnerFacet> facets = new ArrayList<>();
}
