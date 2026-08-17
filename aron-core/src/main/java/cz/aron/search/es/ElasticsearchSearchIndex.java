package cz.aron.search.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery.OpType;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import cz.aron.domain.DataType;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.indexing.IndexConfig;
import cz.aron.indexing.IndexedApu;
import cz.aron.indexing.IndexedRelation;
import cz.aron.search.ApuDocument;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.FieldFilter;
import cz.aron.search.RelationDocument;
import cz.aron.search.SearchIndex;

/**
 * Elasticsearch adapter of the search port - the production engine (see
 * doc/search-port.md and PLAN.md D-1). Owns the historical document layout
 * ({@link IndexedApu} fixed fields + dynamic values from types.yaml), the
 * settings/mappings creation, and the indexed-fields CRC stored in the "apu"
 * index {@code _meta}.
 */
@ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch", matchIfMissing = true)
@Component
public class ElasticsearchSearchIndex implements SearchIndex {

	private static final String FIELDS_CRC_META_KEY = "fieldsCrc";

	/** Upper bound of returned buckets per facet (terms aggregation size). */
	private static final int BUCKET_LIMIT = 1000;

	private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final ElasticsearchOperations operations;

	private final ElasticsearchConverter converter;

	private final TypesHolder typesHolder;

	private final Resource settingsResource;

	public ElasticsearchSearchIndex(ElasticsearchOperations operations, ElasticsearchConverter converter,
			TypesHolder typesHolder,
			@Value("classpath:elasticsearch/es_settings.json") Resource settingsResource) {
		this.operations = operations;
		this.converter = converter;
		this.typesHolder = typesHolder;
		this.settingsResource = settingsResource;
	}

	@Override
	public void createSchema() {
		if (!operations.indexOps(IndexCoordinates.of("apu")).exists()) {
			var mapping = operations.indexOps(IndexCoordinates.of("apu")).createMapping(IndexedApu.class);
			@SuppressWarnings("unchecked")
			var props = (Map<String, Object>) mapping.get("properties");
			props.putAll(createCustomMapping());
			operations.indexOps(IndexCoordinates.of("apu")).create(loadSettings(), mapping);
		}
		if (!operations.indexOps(IndexCoordinates.of("rels")).exists()) {
			var mapping = operations.indexOps(IndexCoordinates.of("rels")).createMapping(IndexedRelation.class);
			operations.indexOps(IndexCoordinates.of("rels")).create(loadSettings(), mapping);
		}
	}

	@Override
	public void dropSchema() {
		operations.indexOps(IndexCoordinates.of("apu")).delete();
		operations.indexOps(IndexCoordinates.of("rels")).delete();
	}

	@Override
	public Long storedFieldsCrc() {
		var indexOps = operations.indexOps(IndexCoordinates.of("apu"));
		if (!indexOps.exists()) {
			return null;
		}
		var mapping = indexOps.getMapping();
		@SuppressWarnings("unchecked")
		var meta = (Map<String, Object>) mapping.get("_meta");
		if (meta == null || meta.get(FIELDS_CRC_META_KEY) == null) {
			return null;
		}
		return Long.valueOf(meta.get(FIELDS_CRC_META_KEY).toString());
	}

	@Override
	public void storeFieldsCrc(long crc) {
		// partial mapping update - merges _meta without touching field mappings
		operations.indexOps(IndexCoordinates.of("apu"))
				.putMapping(Document.parse("{\"_meta\":{\"" + FIELDS_CRC_META_KEY + "\":\"" + crc + "\"}}"));
	}

	@Override
	public void indexApus(Collection<ApuDocument> documents) {
		var indexQueries = new ArrayList<IndexQuery>(documents.size());
		for (var document : documents) {
			var iq = new IndexQuery();
			iq.setId(document.getUuid());
			iq.setObject(toEsDocument(document));
			iq.setOpType(OpType.INDEX);
			iq.setSource("source");
			indexQueries.add(iq);
		}
		if (!indexQueries.isEmpty()) {
			operations.bulkIndex(indexQueries, IndexCoordinates.of("apu"));
		}
	}

	@Override
	public void indexRelations(Collection<RelationDocument> relations) {
		var indexQueries = new ArrayList<IndexQuery>(relations.size());
		for (var rel : relations) {
			var indexedRel = new IndexedRelation(rel.source(), rel.relation(), rel.target());
			var iq = new IndexQuery();
			iq.setId(UUID.randomUUID().toString());
			iq.setObject(indexedRel);
			iq.setOpType(OpType.INDEX);
			indexQueries.add(iq);
		}
		if (!indexQueries.isEmpty()) {
			operations.bulkIndex(indexQueries, IndexCoordinates.of("rels"));
		}
	}

	@Override
	public void deleteApusBySource(long apuSourceId) {
		var criteria = new Criteria("apuSourceId").is(apuSourceId);
		var query = new CriteriaQuery(criteria);
		DeleteQuery deleteQuery = DeleteQuery.builder(query).build();
		operations.delete(deleteQuery, IndexedApu.class);
	}

	@Override
	public ApuSearchResult search(ApuSearchQuery query) {
		var builder = NativeQuery.builder().withQuery(buildMainQuery(query));
		// Values filters go into the post_filter: they restrict hits and total but
		// not aggregations - that is what the multi-select bucket semantics need
		Query valuesFilter = buildValuesFilters(query.filters(), null);
		if (valuesFilter != null) {
			builder.withFilter(valuesFilter);
		}
		for (String bucketField : query.bucketFields()) {
			// buckets of a field ignore that field's own Values filter
			Query otherValues = buildValuesFilters(query.filters(), bucketField);
			Query aggFilter = otherValues != null ? otherValues : Query.of(q -> q.matchAll(m -> m));
			builder.withAggregation(bucketField, Aggregation.of(a -> a
					.filter(aggFilter)
					.aggregations("values", Aggregation.of(sub -> sub
							.terms(t -> t.field(bucketField).size(BUCKET_LIMIT))))));
		}
		if (query.sort() == ApuSearchQuery.SortMode.NAME) {
			builder.withSort(Sort.by(Sort.Direction.ASC, "nameSort"));
		}
		// arbitrary from-offset: over-fetch from+size and slice (size is capped by
		// the caller and ES limits the window to 10k anyway)
		builder.withPageable(PageRequest.of(0, query.from() + query.size()));
		var hits = operations.search(builder.build(), IndexedApu.class, IndexCoordinates.of("apu"));
		var resultHits = hits.getSearchHits().stream()
				.skip(query.from())
				.map(h -> new ApuSearchResult.Hit(h.getId(), h.getContent().getName(),
						h.getContent().getDescription(), h.getContent().getType(),
						h.getContent().isContainsDigitalObjects()))
				.toList();
		return new ApuSearchResult(hits.getTotalHits(), resultHits, extractBuckets(hits, query.bucketFields()));
	}

	private Query buildMainQuery(ApuSearchQuery query) {
		var bool = new BoolQuery.Builder();
		if (query.fulltext() != null) {
			bool.must(Query.of(q -> q.multiMatch(mm -> mm.fields("name", "description").query(query.fulltext()))));
		} else {
			bool.must(Query.of(q -> q.matchAll(m -> m)));
		}
		if (query.apuType() != null) {
			bool.filter(Query.of(q -> q.term(t -> t.field("type").value(query.apuType()))));
		}
		for (FieldFilter filter : query.filters()) {
			if (filter instanceof FieldFilter.Text text) {
				bool.filter(Query.of(q -> q.matchPhrasePrefix(m -> m.field(text.field()).query(text.text()))));
			} else if (filter instanceof FieldFilter.Range range) {
				// interval intersection over the ~L/~H bound fields (engine-shared logic)
				if (range.to() != null) {
					bool.filter(Query.of(q -> q.range(r -> r.term(t -> t.field(range.field() + "~L")
							.lte(ISO_DATE_TIME.format(range.to()))))));
				}
				if (range.from() != null) {
					bool.filter(Query.of(q -> q.range(r -> r.term(t -> t.field(range.field() + "~H")
							.gte(ISO_DATE_TIME.format(range.from()))))));
				}
			}
		}
		return Query.of(q -> q.bool(bool.build()));
	}

	/** AND of all Values filters, each an OR over its values; {@code null} when none apply. */
	private static Query buildValuesFilters(List<FieldFilter> filters, String excludedField) {
		var bool = new BoolQuery.Builder();
		boolean any = false;
		for (FieldFilter filter : filters) {
			if (filter instanceof FieldFilter.Values values && !values.field().equals(excludedField)) {
				var or = new BoolQuery.Builder();
				values.values().forEach(v -> or.should(Query.of(q -> q.term(t -> t.field(values.field()).value(v)))));
				or.minimumShouldMatch("1");
				bool.filter(Query.of(q -> q.bool(or.build())));
				any = true;
			}
		}
		return any ? Query.of(q -> q.bool(bool.build())) : null;
	}

	private static Map<String, List<ApuSearchResult.Bucket>> extractBuckets(SearchHits<IndexedApu> hits,
			Set<String> bucketFields) {
		if (bucketFields.isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, List<ApuSearchResult.Bucket>>();
		var aggregations = (ElasticsearchAggregations) hits.getAggregations();
		for (String field : bucketFields) {
			var aggregation = aggregations.get(field);
			if (aggregation == null) {
				result.put(field, List.of());
				continue;
			}
			var terms = aggregation.aggregation().getAggregate().filter().aggregations().get("values").sterms();
			result.put(field, terms.buckets().array().stream()
					.map(b -> new ApuSearchResult.Bucket(b.key().stringValue(), b.docCount()))
					.toList());
		}
		return result;
	}

	private Settings loadSettings() {
		try {
			return Settings.parse(settingsResource.getContentAsString(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Maps the engine-neutral {@link ApuDocument} onto the historical Elasticsearch
	 * document layout ({@link IndexedApu} fixed fields + dynamic values) - the
	 * output is identical to what the pre-port implementation produced.
	 */
	private Document toEsDocument(ApuDocument apuDocument) {
		var indexedApu = new IndexedApu();
		indexedApu.setContainsDigitalObjects(apuDocument.isContainsDigitalObjects());
		indexedApu.setDescription(apuDocument.getDescription());
		indexedApu.setIncomingRelTypeGroups(null);
		indexedApu.setIncomingRelTypes(null);
		indexedApu.setName(apuDocument.getName());
		indexedApu.setNameSort(apuDocument.getNameSort());
		indexedApu.setType(apuDocument.getType());
		for (var rel : apuDocument.getRels()) {
			indexedApu.getRels().add(new IndexedApu.NestedRelation(rel.targetId(), rel.type(), rel.groups(),
					rel.label(), rel.idLabel()));
		}
		var doc = converter.mapObject(indexedApu);
		doc.putAll(apuDocument.getValues());
		var apuSourceIdArr = new ArrayList<Object>();
		apuSourceIdArr.add(apuDocument.getApuSourceId());
		doc.put("apuSourceId", apuSourceIdArr);
		return doc;
	}

	private Map<String, Object> createCustomMapping() {
		Map<String, Object> customMapping = new HashMap<>();
		for (ItemType allItemType : typesHolder.getAllItemTypes()) {
			if (!allItemType.isIndexed()) {
				continue;
			}
			Map<String, Object> fieldProperties = new HashMap<>();
			String dataType;
			switch (allItemType.getType()) {
				case STRING:
					dataType = "text";
					if (allItemType.getIndexFolding() == null || allItemType.getIndexFolding()) { //defaults to folded
						fieldProperties.put("analyzer", IndexConfig.FOLDING_AND_TOKENIZING_STOP);
					} else if (Boolean.FALSE.equals(allItemType.getIndexFolding())
							&& Boolean.TRUE.equals(allItemType.getCaseInsensitive())) {
						fieldProperties.put("analyzer", IndexConfig.TEXT_LONG_KEYWORD_CI);
						fieldProperties.put("search_analyzer", IndexConfig.TEXT_LONG_KEYWORD_CI);
					} else {  //unfolded also means untokenized for us
						fieldProperties.put("analyzer", IndexConfig.TEXT_LONG_KEYWORD);
					}
					break;
				case ENUM:
					dataType = "keyword";
					break;
				case INTEGER:
					dataType = "integer";
					break;
				case APU_REF:
					dataType = "keyword";
					break;
				case UNITDATE:
					dataType = "date_range";
					break;
				case LINK:
					dataType = "keyword";
					break;
				case ITEM_AGGREG:
					continue;
				default:
					throw new RuntimeException("unknown type");
			}
			fieldProperties.put("type", dataType);
			customMapping.put(allItemType.getCode(), fieldProperties);
			if (allItemType.getType() == DataType.APU_REF) {
				Map<String, Object> labelFieldProperties = new HashMap<>();
				labelFieldProperties.put("analyzer", IndexConfig.FOLDING_AND_TOKENIZING);
				labelFieldProperties.put("type", "text");
				customMapping.put(allItemType.getCode() + "~LABEL", labelFieldProperties);
				customMapping.put(allItemType.getCode() + "~ID~LABEL", fieldProperties);
			} else if (allItemType.getType() == DataType.UNITDATE) {
				var lowProperties = new HashMap<String, Object>();
				lowProperties.put("type", "date");
				customMapping.put(allItemType.getCode() + "~L", lowProperties);
				var highProperties = new HashMap<String, Object>();
				highProperties.put("type", "date");
				customMapping.put(allItemType.getCode() + "~H", highProperties);
			}
		}
		return customMapping;
	}

}
