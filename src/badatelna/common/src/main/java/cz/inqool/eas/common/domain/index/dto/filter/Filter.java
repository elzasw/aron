package cz.inqool.eas.common.domain.index.dto.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Represents a filter condition for searching.
 */
@JsonDeserialize(using = FilterDeserializer.class)
@JsonIgnoreProperties // to disable ignore_unknown deserialization feature
public interface Filter {

    /**
     * Returns an elastic search query builder representing the filter condition.
     *
     * @param indexedFields supported indexed fields
     * @return created query builder
     */
    QueryBuilder toQueryBuilder(IndexObjectFields indexedFields);
}
