package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import cz.inqool.eas.common.exception.InvalidAttribute;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotBlank;

import static cz.inqool.eas.common.exception.InvalidAttribute.ErrorCode.FIELD_NOT_FULLTEXT;

/**
 * Filter representing a filter condition on all fields using standard or biased search.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FulltextFieldFilter extends TextFilter {

    FulltextFieldFilter() {
        super(FilterOperation.FTXF);
    }

    public FulltextFieldFilter(@NotBlank String field, @NotBlank String value) {
        this(field, value, null);
    }

    public FulltextFieldFilter(@NotBlank String field, @NotBlank String value, boolean nestedQueryEnabled) {
        this(field, value, null, nestedQueryEnabled);
    }

    public FulltextFieldFilter(@NotBlank String field, @NotBlank String value, Modifier modifier) {
        super(FilterOperation.FTXF, field, value, modifier);
    }

    @Builder
    public FulltextFieldFilter(@NotBlank String field, @NotBlank String value, Modifier modifier, boolean nestedQueryEnabled) {
        super(FilterOperation.FTXF, field, value, modifier, nestedQueryEnabled);
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldLeafNode indexField = getIndexFieldLeafNode(indexedFields);

        if (!indexField.isFulltext()) {
            throw new InvalidAttribute(this.getClass(), null, field, FIELD_NOT_FULLTEXT);
        }

        return QueryBuilders.multiMatchQuery(value)
                .field(indexField.getSearchable(), indexField.getBoost())
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
                .slop(2);
    }
}
