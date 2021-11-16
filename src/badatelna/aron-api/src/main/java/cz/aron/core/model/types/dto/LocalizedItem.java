package cz.aron.core.model.types.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LocalizedItem {
    private String lang;
    private String text;
}