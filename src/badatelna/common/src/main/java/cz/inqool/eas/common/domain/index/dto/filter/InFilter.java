package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.index.query.QueryBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter representing the 'IN' filter condition on given {@link FieldFilter#field}.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class InFilter extends FieldFilter<InFilter> {

    /**
     * Allowed values
     */
    @NotNull
    private Set<String> values;


    InFilter() {
        super(FilterOperation.IN);
    }

    public InFilter(@NotBlank String field, @NotNull String... values) {
        this(field, Set.of(values));
    }

    public InFilter(@NotBlank String field, @NotNull Set<String> values) {
        super(FilterOperation.IN, field);
        this.values = values;
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        List<Filter> filters = values.stream()
                .map(value -> new EqFilter(field, value))
                .peek(filter -> filter.setNestedQueryEnabled(isNestedQueryEnabled()))
                .collect(Collectors.toList());

        return new OrFilter(filters).toQueryBuilder(indexedFields);
    }
}
