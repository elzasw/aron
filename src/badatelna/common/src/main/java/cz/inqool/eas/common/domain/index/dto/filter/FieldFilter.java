package cz.inqool.eas.common.domain.index.dto.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexFieldNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import cz.inqool.eas.common.exception.InvalidAttribute;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static cz.inqool.eas.common.exception.InvalidAttribute.ErrorCode.FIELD_NOT_INDEXED;

/**
 * Filter representing a filter condition on given {@link FieldFilter#field}.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
abstract public class FieldFilter<FILTER extends FieldFilter<FILTER>> extends AbstractFilter {

    /**
     * If set to {@code false}, the filter returned by {@link #toQueryBuilder(IndexObjectFields)} will not be an
     * instance of {@link NestedQueryBuilder} (even if the indexField is nested).
     */
    protected boolean nestedQueryEnabled = true;

    /**
     * Attribute name (in case of nested object filtering must always contain dot-separated path)
     */
    @NotBlank
    protected String field;

    /**
     * Caching value for indexed field related to {@link #field}
     */
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private IndexFieldLeafNode indexFieldNode;


    protected FieldFilter(@NotNull String operation) {
        super(operation);
    }

    protected FieldFilter(@NotNull String operation, @NotBlank String field) {
        super(operation);
        this.field = field;
    }


    /**
     * Disables wrapping this filter into a nested query (if the field is in a nested object somewhere). Use when
     * this filter will be used in custom-defined {@link NestedFilter}.
     *
     * @return {@code this} for method chaining
     */
    public FILTER inNestedQuery() {
        nestedQueryEnabled = false;
        //noinspection unchecked
        return (FILTER) this;
    }


    /**
     * Wraps given query builder into a nested query in case the {@code indexField} is nested.
     */
    protected QueryBuilder wrapIfNested(IndexFieldNode indexFieldNode, QueryBuilder queryBuilder) {
        if (nestedQueryEnabled && indexFieldNode.isOfNested()) {
            return QueryBuilders.nestedQuery(indexFieldNode.getNestedPath(), queryBuilder, ScoreMode.None);
        } else {
            return queryBuilder;
        }
    }


    /**
     * Returns adequate index field node to given {@link #field}.
     */
    protected IndexFieldLeafNode getIndexFieldLeafNode(IndexObjectFields indexObjectFields) {
        if (this.indexFieldNode == null) {
            IndexFieldLeafNode leafNode = indexObjectFields.get(field, IndexFieldLeafNode.class);
            if (!leafNode.isIndexed()) {
                throw new InvalidAttribute(this.getClass(), null, field, FIELD_NOT_INDEXED);
            }
            this.indexFieldNode = leafNode;
        }
        return this.indexFieldNode;
    }
}
