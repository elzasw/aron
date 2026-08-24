package cz.aron.search.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
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
import co.elastic.clients.elasticsearch._types.query_dsl.RangeRelation;
import cz.aron.domain.DataType;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.indexing.IndexConfig;
import cz.aron.indexing.IndexedApu;
import cz.aron.indexing.IndexedRelation;
import cz.aron.search.ApuDocument;
import cz.aron.search.ContentLocale;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.FieldFilter;
import cz.aron.search.StopWords;
import cz.aron.search.RelationDocument;
import cz.aron.search.SearchIndex;
import cz.aron.search.relevance.RelevancePlan;

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

	/** Placeholder of the content locale's stop-word list in es_settings.json. */
	private static final String STOP_WORDS_TOKEN = "__STOP_WORDS__";

	private static final String SCHEMA_CRC_META_KEY = "schemaCrc";

	private static final String LAYOUT_VERSION_META_KEY = "layoutVersion";

	/**
	 * Version of the fixed-field document layout produced by this adapter (the
	 * types.yaml CRC does not cover fixed fields). Bump on any layout change: an
	 * index written under a different version reports no stored CRC, so the
	 * startup bootstrap rebuilds and reindexes it - the analog of the Lucene
	 * adapter's commit-user-data version.
	 */
	private static final String LAYOUT_VERSION = "5";

	/** Name prefix of dating-bounds aggregations (avoids clashes with bucket aggregations). */
	private static final String BOUNDS_AGG_PREFIX = "bounds~";

	/** Name of the per-type counts aggregation (the tilde keeps it clash-free too). */
	private static final String TYPE_COUNTS_AGG = "types~counts";

	private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private final ElasticsearchOperations operations;

	private final ElasticsearchConverter converter;

	private final TypesHolder typesHolder;

	private final Resource settingsResource;

	private final ContentLocale contentLocale;

	public ElasticsearchSearchIndex(ElasticsearchOperations operations, ElasticsearchConverter converter,
			TypesHolder typesHolder,
			@Value("classpath:elasticsearch/es_settings.json") Resource settingsResource,
			ContentLocale contentLocale) {
		this.operations = operations;
		this.converter = converter;
		this.typesHolder = typesHolder;
		this.settingsResource = settingsResource;
		this.contentLocale = contentLocale;
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
	public Long storedSchemaCrc() {
		var indexOps = operations.indexOps(IndexCoordinates.of("apu"));
		if (!indexOps.exists()) {
			return null;
		}
		var mapping = indexOps.getMapping();
		@SuppressWarnings("unchecked")
		var meta = (Map<String, Object>) mapping.get("_meta");
		if (meta == null || meta.get(SCHEMA_CRC_META_KEY) == null) {
			return null;
		}
		if (!LAYOUT_VERSION.equals(String.valueOf(meta.get(LAYOUT_VERSION_META_KEY)))) {
			// index written by another layout version = treat as no schema
			return null;
		}
		return Long.valueOf(meta.get(SCHEMA_CRC_META_KEY).toString());
	}

	@Override
	public void storeSchemaCrc(long crc) {
		// partial mapping update - merges _meta without touching field mappings
		operations.indexOps(IndexCoordinates.of("apu"))
				.putMapping(Document.parse("{\"_meta\":{\"" + SCHEMA_CRC_META_KEY + "\":\"" + crc + "\",\""
						+ LAYOUT_VERSION_META_KEY + "\":\"" + LAYOUT_VERSION + "\"}}"));
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
		// facet filters (Values, Range) and the apuType restriction go into the
		// post_filter: they restrict hits and total but not aggregations - each
		// aggregation applies the OTHER facets' filters (and the apuType) itself
		// (multi-select semantics); the typeCounts aggregation is the one that
		// deliberately drops the apuType
		Query postFilter = withApuType(buildFacetFilters(query.filters(), List.of()), query.apuType());
		if (postFilter != null) {
			builder.withFilter(postFilter);
		}
		for (ApuSearchQuery.BucketRequest bucket : query.buckets()) {
			builder.withAggregation(bucket.bucketField(), Aggregation.of(a -> a
					.filter(aggregationFilter(query, List.of(bucket.filterField()), true))
					.aggregations("values", Aggregation.of(sub -> sub
							.terms(t -> t.field(bucket.bucketField()).size(bucket.size()))))));
		}
		for (var request : query.bounds()) {
			String field = request.boundsField();
			// the value_counts detect "no dating present": the min/max values alone
			// cannot (the client maps their null to 0.0). The filter's own doc_count
			// minus minCount is then the undated count, needing no aggregation of
			// its own
			builder.withAggregation(BOUNDS_AGG_PREFIX + field, Aggregation.of(a -> a
					.filter(aggregationFilter(query, request.filterFields(), true))
					.aggregations("min", Aggregation.of(sub -> sub.min(m -> m.field(boundField(field, false)))))
					.aggregations("max", Aggregation.of(sub -> sub.max(m -> m.field(boundField(field, true)))))
					.aggregations("minCount",
							Aggregation.of(sub -> sub.valueCount(v -> v.field(boundField(field, false)))))
					.aggregations("maxCount",
							Aggregation.of(sub -> sub.valueCount(v -> v.field(boundField(field, true)))))));
		}
		if (query.typeCounts()) {
			// excluding the type field itself: the built-in type facet follows the
			// same multi-select rule as every other facet, so a selected type never
			// hides the alternatives to it
			builder.withAggregation(TYPE_COUNTS_AGG, Aggregation.of(a -> a
					.filter(aggregationFilter(query, List.of("type"), false))
					.aggregations("values", Aggregation.of(sub -> sub.terms(t -> t.field("type").size(20))))));
		}
		// full deterministic sort chains (uuid mirror field "id" is the final
		// tie-break of every mode); ES's default missing=_last already files
		// unnamed/undated documents last in either direction
		switch (query.sort()) {
			case RELEVANCE -> {
				builder.withSort(so -> so.score(sc -> sc.order(SortOrder.Desc)));
				builder.withSort(so -> so.field(f -> f.field("nameSort").order(SortOrder.Asc)));
				builder.withSort(so -> so.field(f -> f.field("id").order(SortOrder.Asc)));
			}
			case NAME -> {
				builder.withSort(so -> so.field(f -> f.field("nameSort").order(SortOrder.Asc)));
				builder.withSort(so -> so.field(f -> f.field("id").order(SortOrder.Asc)));
			}
			case NAME_DESC -> {
				builder.withSort(so -> so.field(f -> f.field("nameSort").order(SortOrder.Desc)));
				builder.withSort(so -> so.field(f -> f.field("id").order(SortOrder.Asc)));
			}
			case DATE_ASC -> {
				builder.withSort(so -> so.field(f -> f.field("dateL").order(SortOrder.Asc)));
				builder.withSort(so -> so.field(f -> f.field("nameSort").order(SortOrder.Asc)));
				builder.withSort(so -> so.field(f -> f.field("id").order(SortOrder.Asc)));
			}
			case DATE_DESC -> {
				builder.withSort(so -> so.field(f -> f.field("dateH").order(SortOrder.Desc)));
				builder.withSort(so -> so.field(f -> f.field("nameSort").order(SortOrder.Asc)));
				builder.withSort(so -> so.field(f -> f.field("id").order(SortOrder.Asc)));
			}
		}
		// explicit total accuracy: without it ES silently caps totals at 10 000
		// (and Spring Data's total alone does not carry the GTE relation)
		if (query.totalUpTo() != null) {
			builder.withTrackTotalHitsUpTo(query.totalUpTo());
		} else {
			builder.withTrackTotalHits(true);
		}
		// native from/size; zero size = an aggregation/bounds-only query without hits
		if (query.size() > 0) {
			builder.withPageable(new OffsetPageable(query.from(), query.size()));
		} else {
			builder.withMaxResults(0);
		}
		var hits = operations.search(builder.build(), IndexedApu.class, IndexCoordinates.of("apu"));
		var resultHits = hits.getSearchHits().stream()
				.map(h -> new ApuSearchResult.Hit(h.getId(), h.getContent().getName(),
						h.getContent().getDescription(), h.getContent().getType(),
						h.getContent().isContainsDigitalObjects()))
				.toList();
		// deterministic accuracy cap, mirrored by the Lucene adapter: above
		// totalUpTo the result is always (totalUpTo, GTE) - even when ES happens
		// to know the exact count (match-all shortcut), so the API's behavior
		// does not flicker with the query shape
		long rawTotal = hits.getTotalHits();
		long total;
		ApuSearchResult.TotalRelation relation;
		if (query.totalUpTo() != null && rawTotal > query.totalUpTo()) {
			total = query.totalUpTo();
			relation = ApuSearchResult.TotalRelation.GTE;
		} else {
			total = rawTotal;
			relation = hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO
					? ApuSearchResult.TotalRelation.EQ
					: ApuSearchResult.TotalRelation.GTE;
		}
		return new ApuSearchResult(total, relation, resultHits, extractBuckets(hits, query.buckets()),
				extractBounds(hits, query.bounds()), extractTypeCounts(hits, query.typeCounts()));
	}

	/**
	 * Pageable with an arbitrary offset: the ES request converter reads
	 * {@code getOffset()} into the native {@code from}, so non-page-aligned
	 * offsets need no over-fetching.
	 */
	private record OffsetPageable(int offset, int size) implements Pageable {

		@Override
		public int getPageNumber() {
			return offset / size;
		}

		@Override
		public int getPageSize() {
			return size;
		}

		@Override
		public long getOffset() {
			return offset;
		}

		@Override
		public Sort getSort() {
			return Sort.unsorted();
		}

		@Override
		public Pageable next() {
			return new OffsetPageable(offset + size, size);
		}

		@Override
		public Pageable previousOrFirst() {
			return offset >= size ? new OffsetPageable(offset - size, size) : first();
		}

		@Override
		public Pageable first() {
			return new OffsetPageable(0, size);
		}

		@Override
		public Pageable withPage(int pageNumber) {
			return new OffsetPageable(pageNumber * size, size);
		}

		@Override
		public boolean hasPrevious() {
			return offset > 0;
		}
	}

	private Query buildMainQuery(ApuSearchQuery query) {
		var bool = new BoolQuery.Builder();
		if (query.fulltext() != null) {
			RelevancePlan plan = query.fulltext();
			// the gate decides WHAT matches - filter context, no score pollution;
			// scores come exclusively from the weighted tiers (R-9)
			var gate = new BoolQuery.Builder();
			for (RelevancePlan.Clause clause : plan.gate()) {
				gate.should(clauseQuery(clause, false));
			}
			gate.minimumShouldMatch(String.valueOf(plan.minimumShouldMatch()));
			bool.filter(Query.of(q -> q.bool(gate.build())));
			for (RelevancePlan.Clause clause : plan.scoring()) {
				bool.should(clauseQuery(clause, true));
			}
		} else {
			bool.must(Query.of(q -> q.matchAll(m -> m)));
		}
		// the apuType restriction deliberately lives in the post_filter (see
		// search()), so the typeCounts aggregation can ignore it
		for (FieldFilter filter : query.filters()) {
			if (filter instanceof FieldFilter.Text text) {
				bool.filter(Query.of(q -> q.matchPhrasePrefix(m -> m.field(text.field()).query(text.text()))));
			}
			if (filter instanceof FieldFilter.Related related) {
				bool.filter(relatedQuery(related));
			}
		}
		return Query.of(q -> q.bool(bool.build()));
	}

	/**
	 * Relation query: the reference fields matching any named target, ORed with
	 * the document's own id being one of the pre-resolved uuids. Lives in the
	 * query (not the post_filter), so every aggregation respects it - a relation
	 * restriction is not a facet the reader can widen by re-selecting.
	 */
	private static Query relatedQuery(FieldFilter.Related related) {
		var or = new BoolQuery.Builder();
		boolean any = false;
		if (!related.targets().isEmpty()) {
			var targets = related.targets().stream().map(FieldValue::of).toList();
			for (String field : related.refFields()) {
				or.should(Query.of(q -> q.terms(t -> t.field(field).terms(v -> v.value(targets)))));
				any = true;
			}
		}
		if (!related.uuids().isEmpty()) {
			var uuids = related.uuids().stream().map(FieldValue::of).toList();
			or.should(Query.of(q -> q.terms(t -> t.field("id").terms(v -> v.value(uuids)))));
			any = true;
		}
		if (!any) {
			// an empty bool matches everything, so say "nothing" explicitly: a record
			// with no visible reference is related to nothing, not to all
			return Query.of(q -> q.matchNone(m -> m));
		}
		or.minimumShouldMatch("1");
		return Query.of(q -> q.bool(or.build()));
	}

	/** ANDs the apuType restriction onto a (nullable) filter; {@code null} when neither applies. */
	private static Query withApuType(Query filter, String apuType) {
		if (apuType == null) {
			return filter;
		}
		Query typeQuery = Query.of(q -> q.term(t -> t.field("type").value(apuType)));
		if (filter == null) {
			return typeQuery;
		}
		return Query.of(q -> q.bool(b -> b.filter(filter).filter(typeQuery)));
	}

	/**
	 * Filter of one aggregation: the OTHER facets' filters (multi-select) plus -
	 * except for typeCounts - the apuType restriction.
	 */
	private Query aggregationFilter(ApuSearchQuery query, List<String> excludedFields, boolean includeApuType) {
		Query filter = buildFacetFilters(query.filters(), excludedFields);
		if (includeApuType) {
			filter = withApuType(filter, query.apuType());
		}
		return filter != null ? filter : Query.of(q -> q.matchAll(m -> m));
	}

	/** Mechanical translation of one planned clause (doc/search-relevance.md §4.7). */
	private static Query clauseQuery(RelevancePlan.Clause clause, boolean boosted) {
		Float boost = boosted ? clause.weight() : null;
		return switch (clause.kind()) {
			case TERM -> Query.of(q -> q.term(t -> t.field(clause.field()).value(clause.text()).boost(boost)));
			case PREFIX -> Query.of(q -> q.prefix(p -> p.field(clause.field()).value(clause.text()).boost(boost)));
			case PHRASE -> Query.of(q -> q.matchPhrase(m -> m.field(clause.field()).query(clause.text()).boost(boost)));
			case ALL_TERMS -> Query.of(q -> q.match(m -> m.field(clause.field()).query(clause.text())
					.operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And).boost(boost)));
			case ANY_TERM -> Query.of(q -> q.match(m -> m.field(clause.field()).query(clause.text()).boost(boost)));
		};
	}

	/**
	 * AND of the facet filters (Values, Range), skipping those on the excluded
	 * field (multi-select); {@code null} when none apply.
	 */
	private static Query buildFacetFilters(List<FieldFilter> filters, List<String> excludedFields) {
		var bool = new BoolQuery.Builder();
		boolean any = false;
		for (FieldFilter filter : filters) {
			if (filter instanceof FieldFilter.Values values && !excludedFields.contains(values.field())) {
				var or = new BoolQuery.Builder();
				values.values().forEach(v -> or.should(Query.of(q -> q.term(t -> t.field(values.field()).value(v)))));
				or.minimumShouldMatch("1");
				bool.filter(Query.of(q -> q.bool(or.build())));
				any = true;
			} else if (filter instanceof FieldFilter.Range range
					&& excludedFields.stream().noneMatch(range.fields()::contains)) {
				Query intersection = intersectionQuery(range);
				if (intersection != null) {
					bool.filter(range.includeUndated() ? orUndated(range.fields(), intersection) : intersection);
					any = true;
				}
			}
		}
		return any ? Query.of(q -> q.bool(bool.build())) : null;
	}

	/**
	 * Lower/upper bound field of a dating field: the document-level hull for
	 * {@link FieldFilter#ANY_DATING}, the item type's own {@code ~L}/{@code ~H}
	 * otherwise. The Lucene adapter maps the same two names, so a Range filter
	 * and a bounds request mean the same thing on both engines.
	 */
	private static String boundField(String field, boolean upper) {
		if (FieldFilter.ANY_DATING.equals(field)) {
			return upper ? "dateH" : "dateL";
		}
		return field + (upper ? "~H" : "~L");
	}

	/**
	 * A dating of any of the fields overlapping the range (engine-shared logic);
	 * {@code null} when both bounds are open and there is nothing to ask.
	 *
	 * <p>The item field itself is a {@code date_range} carrying every dating of
	 * that item type, so an INTERSECTS range query matches per interval rather
	 * than over their hull - the record dated 1850-1860 and again in 1600 is not
	 * in 1700.
	 */
	private static Query intersectionQuery(FieldFilter.Range range) {
		if (range.from() == null && range.to() == null) {
			return null;
		}
		String from = range.from() != null ? ISO_DATE_TIME.format(range.from()) : null;
		String to = range.to() != null ? ISO_DATE_TIME.format(range.to()) : null;
		return anyOf(range.fields(), field -> Query.of(q -> q.range(r -> r.date(d -> d
				.field(field)
				.gte(from)
				.lte(to)
				.relation(RangeRelation.Intersects)))));
	}

	/**
	 * The intersection, or a document carrying no such dating at all - what
	 * {@code includeUndated} asks for.
	 */
	private static Query orUndated(List<String> fields, Query intersection) {
		Query undated = Query.of(q -> q.bool(b -> b
				.mustNot(anyOf(fields, field -> Query.of(e -> e.exists(x -> x.field(datedField(field))))))));
		return Query.of(q -> q.bool(b -> b.should(intersection).should(undated).minimumShouldMatch("1")));
	}

	/**
	 * The field whose presence means "this record carries a dating of that item
	 * type" - the bound rather than the intervals, so that "undated" means the
	 * same here as in the count the reader is shown beside the slider.
	 */
	private static String datedField(String field) {
		return boundField(field, false);
	}

	/** OR over the fields, so one facet can span several dating item types. */
	private static Query anyOf(List<String> fields, java.util.function.Function<String, Query> of) {
		if (fields.size() == 1) {
			return of.apply(fields.get(0));
		}
		var bool = new BoolQuery.Builder().minimumShouldMatch("1");
		fields.forEach(field -> bool.should(of.apply(field)));
		return Query.of(q -> q.bool(bool.build()));
	}

	private static List<ApuSearchResult.Bucket> extractTypeCounts(SearchHits<IndexedApu> hits, boolean requested) {
		if (!requested) {
			return List.of();
		}
		var aggregations = (ElasticsearchAggregations) hits.getAggregations();
		var aggregation = aggregations.get(TYPE_COUNTS_AGG);
		if (aggregation == null) {
			return List.of();
		}
		var terms = aggregation.aggregation().getAggregate().filter().aggregations().get("values").sterms();
		return terms.buckets().array().stream()
				.map(b -> new ApuSearchResult.Bucket(b.key().stringValue(), b.docCount()))
				.sorted(Comparator.comparingLong(ApuSearchResult.Bucket::count).reversed()
						.thenComparing(ApuSearchResult.Bucket::value))
				.toList();
	}

	private static Map<String, List<ApuSearchResult.Bucket>> extractBuckets(SearchHits<IndexedApu> hits,
			List<ApuSearchQuery.BucketRequest> buckets) {
		if (buckets.isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, List<ApuSearchResult.Bucket>>();
		var aggregations = (ElasticsearchAggregations) hits.getAggregations();
		for (ApuSearchQuery.BucketRequest bucket : buckets) {
			var aggregation = aggregations.get(bucket.bucketField());
			if (aggregation == null) {
				result.put(bucket.bucketField(), List.of());
				continue;
			}
			var terms = aggregation.aggregation().getAggregate().filter().aggregations().get("values").sterms();
			result.put(bucket.bucketField(), terms.buckets().array().stream()
					.map(b -> new ApuSearchResult.Bucket(b.key().stringValue(), b.docCount()))
					.toList());
		}
		return result;
	}

	private static Map<String, ApuSearchResult.Bounds> extractBounds(SearchHits<IndexedApu> hits,
			List<ApuSearchQuery.BoundsRequest> requests) {
		if (requests.isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, ApuSearchResult.Bounds>();
		var aggregations = (ElasticsearchAggregations) hits.getAggregations();
		for (var request : requests) {
			String field = request.boundsField();
			var aggregation = aggregations.get(BOUNDS_AGG_PREFIX + field);
			if (aggregation == null) {
				continue;
			}
			var filterAgg = aggregation.aggregation().getAggregate().filter();
			var subAggs = filterAgg.aggregations();
			double datedCount = subAggs.get("minCount").valueCount().value();
			// no matching document carries the dating - no entry then
			if (datedCount > 0 && subAggs.get("maxCount").valueCount().value() > 0) {
				result.put(field, new ApuSearchResult.Bounds((long) subAggs.get("min").min().value(),
						(long) subAggs.get("max").max().value(),
						filterAgg.docCount() - (long) datedCount));
			}
		}
		return result;
	}

	private Settings loadSettings() {
		try {
			// the stop-word list follows the described material's language; the same
			// list the query planner uses, so both sides drop the same words
			String settings = settingsResource.getContentAsString(StandardCharsets.UTF_8)
					.replace(STOP_WORDS_TOKEN, StopWords.elasticsearchList(contentLocale.getLocale()));
			return Settings.parse(settings);
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
		// the id field mirrors the document _id as a sortable keyword (tie-breaks)
		indexedApu.setId(apuDocument.getUuid());
		indexedApu.setContainsDigitalObjects(apuDocument.isContainsDigitalObjects());
		indexedApu.setDescription(apuDocument.getDescription());
		indexedApu.setIncomingRelTypeGroups(null);
		indexedApu.setIncomingRelTypes(null);
		indexedApu.setName(apuDocument.getName());
		indexedApu.setNameSort(apuDocument.getNameSort());
		indexedApu.setNameExact(apuDocument.getNameExact());
		indexedApu.setNameExactFolded(apuDocument.getNameExactFolded());
		indexedApu.setAllText(apuDocument.getAllText());
		indexedApu.setNameVariants(apuDocument.getNameVariants());
		indexedApu.setNameVariantsExact(apuDocument.getNameVariantsExact());
		indexedApu.setNameVariantsExactFolded(apuDocument.getNameVariantsExactFolded());
		indexedApu.setRefLabels(apuDocument.getRefLabels());
		// substring-match companions: same sources, the trigram analyzer (R-15)
		indexedApu.setAllTextGrams(apuDocument.getAllText());
		indexedApu.setNameGrams(apuDocument.getName());
		indexedApu.setNameVariantsGrams(apuDocument.getNameVariants());
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
		// derived global dating bounds (dating sort); mapped in createCustomMapping
		if (apuDocument.getDateL() != null) {
			doc.put("dateL", List.of(apuDocument.getDateL()));
		}
		if (apuDocument.getDateH() != null) {
			doc.put("dateH", List.of(apuDocument.getDateH()));
		}
		return doc;
	}

	private Map<String, Object> createCustomMapping() {
		Map<String, Object> customMapping = new HashMap<>();
		// derived global dating bounds (min ~L / max ~H per document) - dating sort
		customMapping.put("dateL", Map.of("type", "date"));
		customMapping.put("dateH", Map.of("type", "date"));
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
