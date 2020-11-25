package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotBlank;

/**
 * Filter representing the 'is null' filter condition on given {@link FieldFilter#field}.
 */
@EqualsAndHashCode(callSuper = true)
public class NullFilter extends FieldFilter {

    NullFilter() {
        super(FilterOperation.IS_NULL);
    }

    public NullFilter(@NotBlank String field) {
        super(FilterOperation.IS_NULL, field);
    }

    @Builder
    public NullFilter(@NotBlank String field, boolean nestedQueryEnabled) {
        super(FilterOperation.IS_NULL, field, nestedQueryEnabled);
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldLeafNode indexField = getIndexFieldLeafNode(indexedFields);

        QueryBuilder queryBuilder = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(indexField.getElasticSearchPath()));
        return wrapIfNested(indexField, queryBuilder);
    }
}
