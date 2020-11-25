package cz.inqool.eas.common.domain.index.dto.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import org.elasticsearch.search.aggregations.AggregationBuilder;

/**
 * Represents an ElasticSearch search aggregation.
 * <p>
 * The aggregations framework helps provide aggregated data based on a search query. It is based on simple building
 * blocks called aggregations, that can be composed in order to build complex summaries of the data.
 * <p>
 * An aggregation can be seen as a unit-of-work that builds analytic information over a set of documents. The context of
 * the execution defines what this document set is (e.g. a top-level aggregation executes within the context of the
 * executed query/filters of the search request).
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations.html">Aggregations</a>
 */
@JsonDeserialize(using = AggregationDeserializer.class)
@JsonIgnoreProperties // to disable ignore_unknown deserialization feature
public interface Aggregation {

    /**
     * Returns an elastic search aggregation builder representing aggregation definition.
     *
     * @param indexObjectFields supported indexed fields
     * @return created aggregation builder
     */
    AggregationBuilder toAggregationBuilder(IndexObjectFields indexObjectFields);
}
