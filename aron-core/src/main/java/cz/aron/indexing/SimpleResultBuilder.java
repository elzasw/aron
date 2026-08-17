package cz.aron.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cz.aron.api.rest.model.ApuEntitySimplified;
import cz.aron.api.rest.model.SimpleResult;

@Component
public class SimpleResultBuilder {

    public SimpleResult build(OldApiSearchResult searchResult, List<ApuEntitySimplified> entities) {
        Map<String, ApuEntitySimplified> byId = entities.stream()
                .collect(Collectors.toMap(ApuEntitySimplified::getId, Function.identity()));

        var result = new SimpleResult();
        result.setCount(searchResult.total());
        result.setItems(searchResult.uuids().stream()
                .map(byId::get)
                .filter(e -> e != null)
                .collect(Collectors.toList()));

        if (searchResult.searchAfter() != null && !searchResult.searchAfter().isEmpty()) {
            result.setSearchAfter(new ArrayList<>(searchResult.searchAfter()));
        }

        result.setAggregations(searchResult.aggregations());

        return result;
    }
}
