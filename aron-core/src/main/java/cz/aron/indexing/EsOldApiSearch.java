package cz.aron.indexing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import cz.aron.api.rest.model.Params;

/**
 * Production implementation of {@link OldApiSearch}: the frozen direct
 * Elasticsearch path ({@link QueryBuilder} + {@link ElasticsearchOperations}),
 * moved verbatim out of the controller. This is the parity reference for the
 * old API - behavior changes here are old-API contract changes.
 */
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch", matchIfMissing = true)
@Component
public class EsOldApiSearch implements OldApiSearch {

	private final ElasticsearchOperations elasticsearchOperations;

	private final QueryBuilder queryBuilder;

	public EsOldApiSearch(ElasticsearchOperations elasticsearchOperations, QueryBuilder queryBuilder) {
		this.elasticsearchOperations = elasticsearchOperations;
		this.queryBuilder = queryBuilder;
	}

	@Override
	public OldApiSearchResult search(Params params) {
		var query = queryBuilder.build(params);
		var hits = elasticsearchOperations.search(query, IndexedApu.class, IndexCoordinates.of("apu"));
		var searchHitList = hits.getSearchHits();
		var uuids = searchHitList.stream().map(SearchHit::getId).toList();

		List<Object> searchAfter = null;
		if (!searchHitList.isEmpty()) {
			var lastSortValues = searchHitList.get(searchHitList.size() - 1).getSortValues();
			if (!lastSortValues.isEmpty()) {
				searchAfter = new ArrayList<>(lastSortValues);
			}
		}

		return new OldApiSearchResult(hits.getTotalHits(), uuids, Aggregations.map(hits.getAggregations()),
				searchAfter);
	}

}
