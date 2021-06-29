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
public class FacetsConfigDto {
    private List<FacetConfigDto> facets = new ArrayList<>();
}
