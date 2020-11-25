package cz.inqool.eas.common.domain.index.dto.aggregation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Bucket aggregations don’t calculate metrics over fields like the metrics aggregations do, but instead, they create
 * buckets of documents. Each bucket is associated with a criterion (depending on the aggregation type) which determines
 * whether or not a document in the current context "falls" into it. In other words, the buckets effectively define
 * document sets. In addition to the buckets themselves, the {@code bucket} aggregations also compute and return the
 * number of documents that "fell into" each bucket.
 * <p>
 * Bucket aggregations, as opposed to {@code metrics} aggregations, can hold sub-aggregations. These sub-aggregations
 * will be aggregated for the buckets created by their "parent" bucket aggregation.
 * <p>
 * There are different bucket aggregators, each with a different "bucketing" strategy. Some define a single bucket, some
 * define fixed number of multiple buckets, and others dynamically create the buckets during the aggregation process.
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket.html">Bucket
 * Aggregations</a>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "aggregator",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "FILTERS", value = FiltersAggregation.class),
        @JsonSubTypes.Type(name = "TERMS", value = TermsAggregation.class),
})
@Getter
@Setter
abstract public class BucketAggregation extends AbstractAggregation {

    /**
     * Bucket aggregator
     *
     * @see BucketAggregator
     */
    protected BucketAggregator aggregator;

    /**
     * Sub-aggregations
     */
    protected List<Aggregation> aggregations;


    public BucketAggregation(@NotNull BucketAggregator aggregator) {
        super(AggregationFamily.BUCKET);
        this.aggregator = aggregator;
    }

    public BucketAggregation(BucketAggregator aggregator, List<Aggregation> aggregations) {
        super(AggregationFamily.BUCKET);
        this.aggregator = aggregator;
        this.aggregations = aggregations;
    }


    /**
     * Bucket aggregator, each determining a different "bucketing" strategy. Some define a single bucket, some define
     * fixed number of multiple buckets, and others dynamically create the buckets during the aggregation process.
     */
    public enum BucketAggregator {

        /**
         * A bucket aggregation returning a form of <a href="https://en.wikipedia.org/wiki/Adjacency_matrix">adjacency
         * matrix</a>. The request provides a collection of named filter expressions, similar to the {@code filters}
         * aggregation request. Each bucket in the response represents a non-empty cell in the matrix of intersecting
         * filters.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-adjacency-matrix-aggregation.html#search-aggregations-bucket-adjacency-matrix-aggregation">Adjacency
         * Matrix Aggregation</a>
         */
        ADJACENCY_MATRIX,

        /**
         * A special single bucket aggregation that selects child documents that have the specified type, as defined in
         * a <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/parent-join.html">{@code join}
         * field</a>.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-children-aggregation.html#search-aggregations-bucket-children-aggregation">Children
         * Aggregation</a>
         */
        CHILDREN,

        /**
         * A multi-bucket aggregation that creates composite buckets from different sources.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-composite-aggregation.html#search-aggregations-bucket-composite-aggregation">Composite
         * Aggregation</a>
         */
        COMPOSITE,

        /**
         * This multi-bucket aggregation is similar to the normal <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-histogram-aggregation.html">histogram</a>,
         * but it can only be used with date values. Because dates are represented internally in Elasticsearch as long
         * values, it is possible, but not as accurate, to use the normal {@code histogram} on dates as well. The main
         * difference in the two APIs is that here the interval can be specified using date/time expressions. Time-based
         * data requires special support because time-based intervals are not always a fixed length.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-datehistogram-aggregation.html#search-aggregations-bucket-datehistogram-aggregation">Date
         * Histogram Aggregation</a>
         */
        DATE_HISTOGRAM,

        /**
         * A range aggregation that is dedicated for date values. The main difference between this aggregation and the
         * normal <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-range-aggregation.html">range</a>
         * aggregation is that the {@code from} and {@code to} values can be expressed in <a
         * href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/common-options.html#date-math">Date
         * Math</a> expressions, and it is also possible to specify a date format by which the {@code from} and {@code
         * to} response fields will be returned. Note that this aggregation includes the {@code from} value and excludes
         * the {@code to} value for each range.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-daterange-aggregation.html#search-aggregations-bucket-daterange-aggregation">Date
         * Range Aggregation</a>
         */
        DATE_RANGE,

        /**
         * Like the {@code sampler} aggregation this is a filtering aggregation used to limit any sub aggregations'
         * processing to a sample of the top-scoring documents. The {@code diversified_sampler} aggregation adds the
         * ability to limit the number of matches that share a common value such as an "author".
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-diversified-sampler-aggregation.html#search-aggregations-bucket-diversified-sampler-aggregation">Diversified
         * Sampler Aggregation</a>
         */
        DIVERSIFIED_SAMPLER,

        /**
         * Defines a single bucket of all the documents in the current document set context that match a specified
         * filter. Often this will be used to narrow down the current aggregation context to a specific set of
         * documents.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-filter-aggregation.html#search-aggregations-bucket-filter-aggregation">Filter
         * Aggregation</a>
         */
        FILTER,


        /**
         * Defines a multi bucket aggregation where each bucket is associated with a filter. Each bucket will collect
         * all documents that match its associated filter.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-filters-aggregation.html#search-aggregations-bucket-filters-aggregation">Filters
         * Aggregation</a>
         */
        FILTERS,

        /**
         * A multi-bucket aggregation that works on {@code geo_point} fields and conceptually works very similar to the
         * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-range-aggregation.html">range</a>
         * aggregation. The user can define a point of origin and a set of distance range buckets. The aggregation
         * evaluate the distance of each document value from the origin point and determines the buckets it belongs to
         * based on the ranges (a document belongs to a bucket if the distance between the document and the origin falls
         * within the distance range of the bucket).
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-geodistance-aggregation.html#search-aggregations-bucket-geodistance-aggregation">Geo
         * Distance Aggregation</a>
         */
        GEO_DISTANCE,

        /**
         * A multi-bucket aggregation that works on {@code geo_point} fields and groups points into buckets that
         * represent cells in a grid. The resulting grid can be sparse and only contains cells that have matching data.
         * Each cell is labeled using a <a href="http://en.wikipedia.org/wiki/Geohash">geohash</a> which is of
         * user-definable precision.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-geohashgrid-aggregation.html#search-aggregations-bucket-geohashgrid-aggregation">GeoHash
         * grid Aggregation</a>
         */
        GEOHASH_GRID,

        /**
         * Defines a single bucket of all the documents within the search execution context. This context is defined by
         * the indices and the document types you’re searching on, but is <b>not</b> influenced by the search query
         * itself.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-global-aggregation.html#search-aggregations-bucket-global-aggregation">Global
         * Aggregation</a>
         */
        GLOBAL,

        /**
         * A multi-bucket values source based aggregation that can be applied on numeric values extracted from the
         * documents. It dynamically builds fixed size (a.k.a. interval) buckets over the values.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-histogram-aggregation.html#search-aggregations-bucket-histogram-aggregation">Histogram
         * Aggregation</a>
         */
        HISTOGRAM,

        /**
         * Just like the dedicated <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-daterange-aggregation.html">date</a>
         * range aggregation, there is also a dedicated range aggregation for IP typed fields.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-iprange-aggregation.html#search-aggregations-bucket-iprange-aggregation">IP
         * Range Aggregation</a>
         */
        IP_RANGE,

        /**
         * A field data based single bucket aggregation, that creates a bucket of all documents in the current document
         * set context that are missing a field value (effectively, missing a field or having the configured NULL value
         * set). This aggregator will often be used in conjunction with other field data bucket aggregators (such as
         * ranges) to return information for all the documents that could not be placed in any of the other buckets due
         * to missing field data values.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-missing-aggregation.html#search-aggregations-bucket-missing-aggregation">Missing
         * Aggregation</a>
         */
        MISSING,

        /**
         * A special single bucket aggregation that enables aggregating nested documents.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-nested-aggregation.html#search-aggregations-bucket-nested-aggregation">Nested
         * Aggregation</a>
         */
        NESTED,

        /**
         * A multi-bucket value source based aggregation that enables the user to define a set of ranges - each
         * representing a bucket. During the aggregation process, the values extracted from each document will be
         * checked against each bucket range and "bucket" the relevant/matching document. Note that this aggregation
         * includes the {@code from} value and excludes the {@code to} value for each range.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-range-aggregation.html#search-aggregations-bucket-range-aggregation">Range
         * Aggregation</a>
         */
        RANGE,

        /**
         * A special single bucket aggregation that enables aggregating on parent docs from nested documents.
         * Effectively this aggregation can break out of the nested block structure and link to other nested structures
         * or the root document, which allows nesting other aggregations that aren’t part of the nested object in a
         * nested aggregation.
         * <p>
         * The {@code reverse_nested} aggregation must be defined inside a {@code nested} aggregation.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-reverse-nested-aggregation.html#search-aggregations-bucket-reverse-nested-aggregation">Reverse
         * nested Aggregation</a>
         */
        REVERSE_NESTED,

        /**
         * A filtering aggregation used to limit any sub aggregations' processing to a sample of the top-scoring
         * documents.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-sampler-aggregation.html#search-aggregations-bucket-sampler-aggregation">Sampler
         * Aggregation</a>
         */
        SAMPLER,

        /**
         * An aggregation that returns interesting or unusual occurrences of terms in a set.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-significantterms-aggregation.html#search-aggregations-bucket-significantterms-aggregation">Significant
         * Terms Aggregation</a>
         */
        SIGNIFICANT_TERMS,

        /**
         * An aggregation that returns interesting or unusual occurrences of free-text terms in a set. It is like the <a
         * href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-significantterms-aggregation.html">significant
         * terms</a> aggregation but differs in that:
         * <ul>
         *   <li>It is specifically designed for use on type {@code text} fields</li>
         *   <li>It does not require field data or doc-values</li>
         *   <li>It re-analyzes text content on-the-fly meaning it can also filter duplicate sections of noisy text that
         * otherwise tend to skew statistics.</li>
         * </ul>
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-significanttext-aggregation.html#search-aggregations-bucket-significanttext-aggregation">Significant
         * Text Aggregation</a>
         */
        SIGNIFICANT_TEXT,

        /**
         * A multi-bucket value source based aggregation where buckets are dynamically built - one per unique value.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation">Terms
         * Aggregation</a>
         */
        TERMS
    }
}
