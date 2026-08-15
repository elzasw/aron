package cz.aron.search.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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
		var bool = new BoolQuery.Builder();
		if (query.fulltext() != null) {
			bool.must(Query.of(q -> q.multiMatch(mm -> mm.fields("name", "description").query(query.fulltext()))));
		} else {
			bool.must(Query.of(q -> q.matchAll(m -> m)));
		}
		query.valueFilters().forEach(
				(field, value) -> bool.filter(Query.of(q -> q.term(t -> t.field(field).value(value)))));
		var nativeQuery = NativeQuery.builder()
				.withQuery(Query.of(q -> q.bool(bool.build())))
				.withPageable(PageRequest.of(query.page(), query.size()))
				.build();
		var hits = operations.search(nativeQuery, IndexedApu.class, IndexCoordinates.of("apu"));
		var resultHits = hits.getSearchHits().stream()
				.map(h -> new ApuSearchResult.Hit(h.getId(), h.getContent().getName(), h.getContent().getType()))
				.toList();
		return new ApuSearchResult(hits.getTotalHits(), resultHits);
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
