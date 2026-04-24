package cz.aron.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import cz.aron.api.rest.model.ApuEntitySimplified;
import cz.aron.api.rest.model.SimpleResult;

@Component
public class SimpleResultBuilder {

    public SimpleResult build(SearchHits<IndexedApu> hits, List<ApuEntitySimplified> entities) {
        Map<String, ApuEntitySimplified> byId = entities.stream()
                .collect(Collectors.toMap(ApuEntitySimplified::getId, Function.identity()));

        var result = new SimpleResult();
        result.setCount(hits.getTotalHits());
        result.setItems(hits.getSearchHits().stream()
                .map(hit -> byId.get(hit.getId()))
                .filter(e -> e != null)
                .collect(Collectors.toList()));

        var searchHitList = hits.getSearchHits();
        if (!searchHitList.isEmpty()) {
            var lastSortValues = searchHitList.get(searchHitList.size() - 1).getSortValues();
            if (!lastSortValues.isEmpty()) {
                result.setSearchAfter(new ArrayList<>(lastSortValues));
            }
        }

        result.setAggregations(Aggregations.map(hits.getAggregations()));

        return result;
    }
}
