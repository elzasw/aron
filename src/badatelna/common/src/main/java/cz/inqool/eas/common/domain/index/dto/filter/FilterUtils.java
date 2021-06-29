package cz.inqool.eas.common.domain.index.dto.filter;

public class FilterUtils {
    public static <T extends FieldFilter<?>> T deNest(T filter) {
        filter.setNestedQueryEnabled(false);
        return filter;
    }
}
