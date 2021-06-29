package cz.aron.core.facets.dto;

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
    private int displayedItems;
    private int maxDisplayedItems;
    private String tooltip;
    private String description;
    private List<Object> tooltips;
    private String orderBy;
    private List<String> order = new ArrayList<>();
    private String group;
    private List<IntervalSpec> intervals = new ArrayList<>();
    private List<InnerFacet> facets = new ArrayList<>();
}
