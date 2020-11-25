package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Filter representing the 'equals' filter condition on given {@link FieldFilter#field}.
 */
@EqualsAndHashCode(callSuper = true)
public class EqFilter extends FieldValueFilter {

    EqFilter() {
        super(FilterOperation.EQ);
    }

    public EqFilter(@NotBlank String field, @NotNull Enum<?> value) {
        this(field, value.name());
    }

    public EqFilter(@NotBlank String field, @NotBlank Enum<?> value, boolean nestedQueryEnabled) {
        super(FilterOperation.EQ, field, value.name(), nestedQueryEnabled);
    }

    public EqFilter(@NotBlank String field, @NotBlank String value) {
        super(FilterOperation.EQ, field, value);
    }

    @Builder
    public EqFilter(@NotBlank String field, @NotBlank String value, boolean nestedQueryEnabled) {
        super(FilterOperation.EQ, field, value, nestedQueryEnabled);
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldLeafNode indexField = getIndexFieldLeafNode(indexedFields);

        QueryBuilder queryBuilder = QueryBuilders.matchQuery(indexField.getElasticSearchPath(), value)
                .operator(Operator.AND);
        return wrapIfNested(indexField, queryBuilder);
    }
}
