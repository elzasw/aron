package cz.aron.core.model.types.dto;

import cz.aron.core.model.DataType;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Lukas Jane (inQool) 18.11.2020.
 */
@Getter
@Setter
public class ItemType {
    private String code;
    private String name;
    private DataType type;
    private boolean indexed = true;
    private Boolean indexFolding;
    private Boolean indexBoost;
}
