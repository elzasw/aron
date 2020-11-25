package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotBlank;

/**
 * Filter representing the 'not-null' filter condition on given {@link FieldFilter#field}.
 */
@EqualsAndHashCode(callSuper = true)
public class NotNullFilter extends FieldFilter {

    NotNullFilter() {
        super(FilterOperation.NOT_NULL);
    }

    public NotNullFilter(@NotBlank String field) {
        super(FilterOperation.NOT_NULL, field);
    }

    @Builder
    public NotNullFilter(@NotBlank String field, boolean nestedQueryEnabled) {
        super(FilterOperation.NOT_NULL, field, nestedQueryEnabled);
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldLeafNode indexField = getIndexFieldLeafNode(indexedFields);

        QueryBuilder queryBuilder = QueryBuilders.existsQuery(indexField.getElasticSearchPath());
        return wrapIfNested(indexField, queryBuilder);
    }
}
