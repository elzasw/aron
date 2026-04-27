package cz.aron.indexing;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

//import cz.aron.api.rest.model.Relation;
//import cz.aron.api.rest.model.Result;

@Component
public class ResultBuilder {

	/*
    public Result build(SearchHits<IndexedApu> hits) {
        var result = new Result();
        result.setCount(hits.getTotalHits());
        result.setItems(hits.getSearchHits().stream()
                .map(hit -> new Relation(hit.getId()))
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
    */
}
