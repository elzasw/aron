package cz.aron.core.model.types.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 18.11.2020.
 */
@Getter
@Setter
public class ItemTypeGroup {
    private String code;
    private List<String> items = new ArrayList<>();
}
