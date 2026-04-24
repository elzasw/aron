package cz.aron.indexing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.client.elc.Aggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.AggregationsContainer;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import cz.aron.api.rest.model.AggregationResult;

class Aggregations {

    static Map<String, List<AggregationResult>> map(AggregationsContainer<?> container) {
        if (container == null) {
            return Map.of();
        }
        var esAggs = (ElasticsearchAggregations) container;
        var result = new HashMap<String, List<AggregationResult>>();
        for (ElasticsearchAggregation agg : esAggs.aggregations()) {
            Aggregation named = agg.aggregation();
            result.put(named.getName(), mapAggregate(named.getAggregate()));
        }
        return result;
    }

    static Map<String, List<AggregationResult>> mapNative(Map<String, Aggregate> aggregations) {
        if (aggregations.isEmpty()) {
            return Map.of();
        }
        var result = new HashMap<String, List<AggregationResult>>();
        aggregations.forEach((name, agg) -> result.put(name, mapAggregate(agg)));
        return result;
    }

    static List<AggregationResult> mapAggregate(Aggregate aggregate) {
        if (aggregate.isSterms()) {
            return aggregate.sterms().buckets().array().stream()
                    .map(b -> new AggregationResult()
                            .key(b.key().stringValue())
                            .value(String.valueOf(b.docCount()))
                            .aggregations(mapNative(b.aggregations())))
                    .collect(Collectors.toList());
        }
        if (aggregate.isLterms()) {
            return aggregate.lterms().buckets().array().stream()
                    .map(b -> new AggregationResult()
                            .key(String.valueOf(b.key()))
                            .value(String.valueOf(b.docCount()))
                            .aggregations(mapNative(b.aggregations())))
                    .collect(Collectors.toList());
        }
        if (aggregate.isNested()) {
            var na = aggregate.nested();
            return List.of(new AggregationResult()
                    .value(String.valueOf(na.docCount()))
                    .aggregations(mapNative(na.aggregations())));
        }
        if (aggregate.isMissing()) {
            return List.of(new AggregationResult()
                    .value(String.valueOf(aggregate.missing().docCount())));
        }
        return List.of();
    }
}
