package cz.aron.core.model.types.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Lukas Jane (inQool) 18.11.2020.
 */
@Getter
@Setter
public class ApuPartType {
    private String code;
    private String name;
    private ViewType viewType;
    private List<LocalizedItem> lang = new ArrayList<>();
}
