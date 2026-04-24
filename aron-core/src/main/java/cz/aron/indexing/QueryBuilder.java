package cz.aron.indexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import cz.aron.api.rest.model.AndFilter;
import cz.aron.api.rest.model.BucketAggregation;
import cz.aron.api.rest.model.EqFilter;
import cz.aron.api.rest.model.FieldSort;
import cz.aron.api.rest.model.Filter;
import cz.aron.api.rest.model.FullTextFieldFilter;
import cz.aron.api.rest.model.FullTextFilter;
import cz.aron.api.rest.model.MaxAggregator;
import cz.aron.api.rest.model.MinAggregator;
import cz.aron.api.rest.model.OrFilter;
import cz.aron.api.rest.model.Params;
import cz.aron.api.rest.model.RangeFilter;
import cz.aron.api.rest.model.ScoreSort;
import cz.aron.api.rest.model.Sort;

@Component
public class QueryBuilder {

    /**
     * Converts a {@link Params} request into a Spring Data Elasticsearch {@link NativeQuery}.
     *
     * <p>{@link cz.aron.api.rest.model.Aggregation#getType()} is used as both the ES aggregation
     * name and the target field/path. {@link BucketAggregation#getAggregator()} is the discriminator
     * that determines the ES aggregation type (TERMS, NESTED, …).
     */
    public NativeQuery build(Params params) {
        NativeQueryBuilder builder = NativeQuery.builder();

        // Filters → ES query
        List<Filter> filters = params.getFilters();
        if (filters != null && !filters.isEmpty()) {
            builder.withQuery(filtersToQuery(filters));
        } else {
            builder.withQuery(q -> q.matchAll(m -> m));
        }

        // Sort
        List<Sort> sorts = params.getSort();
        if (sorts != null && !sorts.isEmpty()) {
            List<SortOptions> sortOptions = toSortOptions(sorts, Boolean.TRUE.equals(params.getFlipDirection()));
            sortOptions.forEach(builder::withSort);
        }

        // Offset/size pagination — offset must be a multiple of size for correct page calculation
        int size = params.getSize() != null ? params.getSize() : 20;
        int offset = params.getOffset() != null ? params.getOffset() : 0;
        if (size==0) {
        	builder.withMaxResults(0);
        } else {
        	builder.withPageable(PageRequest.of(offset > 0 ? offset / size : 0, size));	
        }
        

        // Keyset pagination (takes precedence over offset when both are present)
        if (params.getSearchAfter() != null) {
            List<Object> searchAfter = new ArrayList<>();
            searchAfter.add(params.getSearchAfter());
            builder.withSearchAfter(searchAfter);
        }

        // Aggregations
        List<cz.aron.api.rest.model.Aggregation> aggregations = params.getAggregations();
        if (aggregations != null) {
            for (cz.aron.api.rest.model.Aggregation agg : aggregations) {
                Aggregation esAgg = toAggregation(agg);
                String name = aggregationName(agg);
                if (esAgg != null && name != null) {
                    builder.withAggregation(name, esAgg);
                }
            }
        }

        // Source field filtering
        List<String> fields = params.getFields();
        if (fields != null && !fields.isEmpty()) {
            builder.withSourceFilter(new FetchSourceFilter(true, fields.toArray(String[]::new), null));
        }

        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Filters
    // -------------------------------------------------------------------------

    private Query filtersToQuery(List<Filter> filters) {
        if (filters.size() == 1) {
            return toQuery(filters.get(0));
        }
        // Multiple top-level filters are combined with an implicit AND
        var bool = new BoolQuery.Builder();
        filters.forEach(f -> bool.must(toQuery(f)));
        return Query.of(q -> q.bool(bool.build()));
    }

    private Query toQuery(Filter filter) {
        if (filter instanceof AndFilter f) {
            var bool = new BoolQuery.Builder();
            if (f.getFilters() != null) {
                f.getFilters().forEach(sub -> bool.must(toQuery(sub)));
            }
            return Query.of(q -> q.bool(bool.build()));
        }
        if (filter instanceof OrFilter f) {
            var bool = new BoolQuery.Builder();
            if (f.getFilters() != null) {
                f.getFilters().forEach(sub -> bool.should(toQuery(sub)));
                bool.minimumShouldMatch("1");
            }
            return Query.of(q -> q.bool(bool.build()));
        }
        if (filter instanceof EqFilter f) {
            return Query.of(q -> q.term(t -> t.field(f.getField()).value(f.getValue())));
        }
        if (filter instanceof FullTextFieldFilter f) {
            // match query on a specific field (uses the field's configured analyzer)
            return Query.of(q -> q.match(m -> m.field(f.getField()).query(f.getValue())));
        }
        if (filter instanceof FullTextFilter f) {
            // query_string searches across all indexed fields by default
            return Query.of(q -> q.queryString(qs -> qs.query(f.getValue())));
        }
        if (filter instanceof RangeFilter f) {
            return Query.of(q -> q.range(r -> r.term(t -> {
                t.field(f.getField());
                if (f.getGt() != null) t.gt(f.getGt());
                if (f.getGte() != null) t.gte(f.getGte());
                if (f.getLt() != null) t.lt(f.getLt());
                if (f.getLte() != null) t.lte(f.getLte());
                return t;
            })));
        }
        throw new IllegalArgumentException("Unsupported filter type: " + filter.getClass().getSimpleName());
    }

    // -------------------------------------------------------------------------
    // Sort
    // -------------------------------------------------------------------------

    private List<SortOptions> toSortOptions(List<Sort> sorts, boolean flip) {
        List<SortOptions> result = new ArrayList<>();
        for (Sort sort : sorts) {
            if (sort instanceof FieldSort f) {
                SortOrder order = flip ? SortOrder.Desc : SortOrder.Asc;
                String field = resolveSortField(f.getField());
                result.add(SortOptions.of(s -> s.field(fs -> fs.field(field).order(order))));
            } else if (sort instanceof ScoreSort) {
                // relevance score sorts descending by default; flip inverts it
                SortOrder order = flip ? SortOrder.Asc : SortOrder.Desc;
                result.add(SortOptions.of(s -> s.score(sc -> sc.order(order))));
            }
        }
        return result;
    }

    private String resolveSortField(String field) {
        // "name" has a dedicated sort subfield backed by the ICU collation analyzer
        if ("name".equals(field)) {
            return "name." + IndexConfig.SUFFIX_SORT;
        }
        return field;
    }

    // -------------------------------------------------------------------------
    // Aggregations
    // -------------------------------------------------------------------------

    private Aggregation toAggregation(cz.aron.api.rest.model.Aggregation agg) {
        if (agg instanceof MaxAggregator max && max.getField() != null) {
            String field = max.getField().toString();
            return Aggregation.of(a -> a.max(m -> m.field(field)));
        }
        if (agg instanceof MinAggregator min && min.getField() != null) {
            String field = min.getField().toString();
            return Aggregation.of(a -> a.min(m -> m.field(field)));
        }
        if (!(agg instanceof BucketAggregation bucket) || bucket.getAggregator() == null) {
            return null;
        }
        String field = agg.getType();
        Map<String, Aggregation> subAggs = collectSubAggregations(bucket.getAggregations());

        return switch (bucket.getAggregator()) {
            case TERMS -> Aggregation.of(a -> a.terms(t -> t.field(field)).aggregations(subAggs));
            case NESTED -> Aggregation.of(a -> a.nested(n -> n.path(field)).aggregations(subAggs));
            case FILTER -> Aggregation.of(a -> a.filter(f -> f.matchAll(m -> m)).aggregations(subAggs));
            case MISSING -> Aggregation.of(a -> a.missing(m -> m.field(field)).aggregations(subAggs));
            case RANGE -> Aggregation.of(a -> a.range(r -> r.field(field)).aggregations(subAggs));
            case DATE_RANGE -> Aggregation.of(a -> a.dateRange(dr -> dr.field(field)).aggregations(subAggs));
            default -> null;
        };
    }

    private String aggregationName(cz.aron.api.rest.model.Aggregation agg) {
        if (agg instanceof MaxAggregator max && max.getName() != null) return max.getName();
        if (agg instanceof MinAggregator min && min.getName() != null) return min.getName();
        return agg.getType();
    }

    private Map<String, Aggregation> collectSubAggregations(List<cz.aron.api.rest.model.Aggregation> aggregations) {
        Map<String, Aggregation> subAggs = new HashMap<>();
        if (aggregations != null) {
            for (cz.aron.api.rest.model.Aggregation subAgg : aggregations) {
                Aggregation esSubAgg = toAggregation(subAgg);
                String name = aggregationName(subAgg);
                if (esSubAgg != null && name != null) {
                    subAggs.put(name, esSubAgg);
                }
            }
        }
        return subAggs;
    }
}
