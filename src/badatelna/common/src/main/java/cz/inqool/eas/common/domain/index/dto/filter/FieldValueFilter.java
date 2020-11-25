package cz.inqool.eas.common.domain.index.dto.filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Filter representing a comparison filter condition on given {@link FieldFilter#field}.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
abstract public class FieldValueFilter extends FieldFilter {

    /**
     * Value used in comparison
     */
    @NotBlank
    protected String value;


    protected FieldValueFilter(@NotNull String operation) {
        super(operation);
    }

    protected FieldValueFilter(@NotNull String operation, @NotBlank String field, @NotBlank String value) {
        super(operation, field);
        this.value = value;
    }

    protected FieldValueFilter(@NotNull String operation, @NotBlank String field, @NotBlank String value, boolean nestedQueryEnabled) {
        super(operation, field, nestedQueryEnabled);
        this.value = value;
    }

}
