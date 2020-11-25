package cz.inqool.eas.common.domain.index.dto.sort;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import org.elasticsearch.search.sort.SortBuilder;

/**
 * Represents a sorting specification for ElasticSearch queries.
 *
 * @param <SB> type of supported sort builder
 */
@JsonDeserialize(using = SortDeserializer.class)
@JsonIgnoreProperties // to disable ignore_unknown deserialization feature
public interface Sort<SB extends SortBuilder<SB>> {

    /**
     * Returns an elastic search sort builder representing this sorting specification.
     *
     * @param indexedFields supported indexed fields
     * @return created sort builder
     */
    SB toSortBuilder(IndexObjectFields indexedFields);

    /**
     * Returns a clone of this sorting, only with reversed order.
     */
    Sort<SB> withReversedOrder();


    /**
     * Sort type
     */
    class Type {

        /**
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#sort-search-results">Sort
         * search results</a>
         */
        public static final String FIELD = "FIELD";

        /**
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#geo-sorting">Geo
         * Distance Sorting</a>
         */
        public static final String GEO_DISTANCE = "GEO_DISTANCE";

        /**
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#_script_based_sorting">Script
         * Based Sorting</a>
         */
        public static final String SCRIPT = "SCRIPT";

        /**
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#_track_scores">Track
         * Scores</a>
         */
        public static final String SCORE = "SCORE";
    }
}
