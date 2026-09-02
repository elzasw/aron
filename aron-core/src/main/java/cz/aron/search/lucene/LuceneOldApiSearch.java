package cz.aron.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.api.rest.model.AggregationResult;
import cz.aron.api.rest.model.AndFilter;
import cz.aron.api.rest.model.AnyKeywordFieldFilter;
import cz.aron.api.rest.model.BucketAggregation;
import cz.aron.api.rest.model.ContainsFilter;
import cz.aron.api.rest.model.EqFilter;
import cz.aron.api.rest.model.FilterAggregation;
import cz.aron.api.rest.model.NestedAggregation;
import cz.aron.api.rest.model.FieldSort;
import cz.aron.api.rest.model.Filter;
import cz.aron.api.rest.model.FullTextFieldFilter;
import cz.aron.api.rest.model.FullTextFilter;
import cz.aron.api.rest.model.MaxAggregation;
import cz.aron.api.rest.model.MinAggregation;
import cz.aron.api.rest.model.NotFilter;
import cz.aron.api.rest.model.OrFilter;
import cz.aron.api.rest.model.Params;
import cz.aron.api.rest.model.RangeFilter;
import cz.aron.api.rest.model.ScoreSort;
import cz.aron.api.rest.model.TermsAggregation;
import cz.aron.domain.DataType;
import cz.aron.domain.types.TypesHolder;
import cz.aron.indexing.OldApiSearch;
import cz.aron.indexing.OldApiSearchResult;

/**
 * Old-API search on the embedded Lucene engine - the dev/test-grade
 * implementation of {@link OldApiSearch} that lets ES-less deployments, dev mode
 * and the default test suite serve the old UI. The index content is identical to
 * the ES adapter's by construction ({@code ApuDocumentBuilder}), so this class is
 * pure query translation of the frozen {@link Params} model.
 * <p>
 * Known divergences from the production ES path (acceptable for dev/test, do not
 * rely on them for old-API parity claims):
 * <ul>
 * <li>FTX searches name, description and the analyzed item fields instead of
 * ES {@code query_string} over all fields, without operator syntax;</li>
 * <li>relevance ranking and bucket tie order are engine-specific;</li>
 * <li>CONTAINS matches within single analyzed tokens (mirrors ES wildcard
 * behavior on analyzed fields; multi-word values match on neither engine);</li>
 * <li>the nested {@code rels} aggregations are answered from the flat document
 * (see {@link #relScope}) rather than from nested documents, which costs two
 * things: the label a condition matches is the target's displayed label, where
 * ES matches the indexed one (they differ only under an
 * {@code INT~NAME~INDEX} override), and a NESTED bucket's own count is the
 * records in scope instead of their relations - counting those would mean
 * enumerating every relation value in the index, and the old UI reads only the
 * buckets below;</li>
 * <li>{@code searchAfter} pagination is rejected (the old UI pages by
 * offset).</li>
 * </ul>
 */
@ConditionalOnProperty(name = "search.engine", havingValue = "lucene")
@Component
public class LuceneOldApiSearch implements OldApiSearch {

	private static final Logger log = LoggerFactory.getLogger(LuceneOldApiSearch.class);

	/** ES returns 10 buckets when a terms aggregation does not state a size. */
	private static final int DEFAULT_TERMS_SIZE = 10;

	/** The only nested path the document has ({@code IndexedApu.rels}). */
	private static final String RELS_PATH = "rels";

	/** The {@code rels.*} fields the relation's item type alone decides. */
	private static final Set<String> TYPE_DERIVED_FIELDS = Set.of("rels.type", "rels.groups");

	private final LuceneSearchIndex index;

	private final TypesHolder typesHolder;

	public LuceneOldApiSearch(LuceneSearchIndex index, TypesHolder typesHolder) {
		this.index = index;
		this.typesHolder = typesHolder;
	}

	@Override
	public OldApiSearchResult search(Params params) {
		if (params.getSearchAfter() != null) {
			throw new UnsupportedOperationException(
					"searchAfter pagination is not supported by the lucene engine (use offset paging)");
		}
		try {
			IndexSearcher searcher = index.apuSearchers().acquire();
			try {
				Query query = toQuery(params.getFilters());
				long total = searcher.count(query);

				int size = params.getSize() != null ? params.getSize() : 20;
				int offset = params.getOffset() != null ? params.getOffset() : 0;
				var uuids = new ArrayList<String>();
				if (size > 0 && total > offset) {
					var top = searcher.search(query, offset + size,
							toSort(params, Boolean.TRUE.equals(params.getFlipDirection())));
					var storedFields = searcher.storedFields();
					for (int i = offset; i < top.scoreDocs.length; i++) {
						uuids.add(storedFields.document(top.scoreDocs[i].doc).get("uuid"));
					}
				}

				return new OldApiSearchResult(total, uuids, aggregate(searcher, query, params.getAggregations()),
						null);
			} finally {
				index.apuSearchers().release(searcher);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// -------------------------------------------------------------------------
	// Filters
	// -------------------------------------------------------------------------

	private Query toQuery(List<Filter> filters) throws IOException {
		if (filters == null || filters.isEmpty()) {
			return new MatchAllDocsQuery();
		}
		if (filters.size() == 1) {
			return toQuery(filters.get(0));
		}
		// multiple top-level filters combine with an implicit AND (as in QueryBuilder)
		var bool = new BooleanQuery.Builder();
		for (Filter filter : filters) {
			bool.add(toQuery(filter), Occur.MUST);
		}
		return bool.build();
	}

	private Query toQuery(Filter filter) throws IOException {
		if (filter instanceof AndFilter f) {
			if (f.getFilters() == null || f.getFilters().isEmpty()) {
				return new MatchAllDocsQuery(); // an empty ES bool matches everything
			}
			var bool = new BooleanQuery.Builder();
			for (Filter sub : f.getFilters()) {
				bool.add(toQuery(sub), Occur.MUST);
			}
			return bool.build();
		}
		if (filter instanceof OrFilter f) {
			if (f.getFilters() == null || f.getFilters().isEmpty()) {
				return new MatchAllDocsQuery();
			}
			var bool = new BooleanQuery.Builder();
			for (Filter sub : f.getFilters()) {
				bool.add(toQuery(sub), Occur.SHOULD);
			}
			bool.setMinimumNumberShouldMatch(1);
			return bool.build();
		}
		if (filter instanceof NotFilter f) {
			// a Lucene bool of only MUST_NOT clauses matches nothing - anchor on match-all
			var bool = new BooleanQuery.Builder().add(new MatchAllDocsQuery(), Occur.MUST);
			if (f.getFilters() != null) {
				for (Filter sub : f.getFilters()) {
					bool.add(toQuery(sub), Occur.MUST_NOT);
				}
			}
			return bool.build();
		}
		if (filter instanceof EqFilter f) {
			return new TermQuery(new Term(f.getField(), f.getValue()));
		}
		if (filter instanceof FullTextFieldFilter f) {
			return index.allWordsLastPrefixQuery(f.getField(), f.getValue());
		}
		if (filter instanceof FullTextFilter f) {
			return fullTextQuery(f.getValue());
		}
		if (filter instanceof RangeFilter f) {
			return rangeQuery(f);
		}
		if (filter instanceof ContainsFilter f) {
			return containsQuery(f);
		}
		if (filter instanceof AnyKeywordFieldFilter f) {
			// exact match on any APU_REF item field (mirrors QueryBuilder's multi_match)
			var bool = new BooleanQuery.Builder();
			for (var itemType : typesHolder.getAllItemTypes()) {
				if (DataType.APU_REF.equals(itemType.getType())) {
					bool.add(new TermQuery(new Term(itemType.getCode(), f.getValue())), Occur.SHOULD);
				}
			}
			bool.setMinimumNumberShouldMatch(1);
			return bool.build();
		}
		throw new IllegalArgumentException("Unsupported filter type: " + filter.getClass().getSimpleName());
	}

	/**
	 * FTX approximation of ES {@code query_string} over all fields: any analyzed
	 * token matches in name, description or any analyzed item field (STRING items
	 * and APU_REF {@code ~LABEL} companions).
	 */
	private Query fullTextQuery(String value) throws IOException {
		var fields = new ArrayList<String>();
		fields.add("name");
		fields.add("description");
		for (var itemType : typesHolder.getAllItemTypes()) {
			if (DataType.STRING.equals(itemType.getType())) {
				fields.add(itemType.getCode());
			} else if (DataType.APU_REF.equals(itemType.getType())) {
				fields.add(itemType.getCode() + "~LABEL");
			}
		}
		var bool = new BooleanQuery.Builder();
		for (String token : index.analyze(value)) {
			for (String field : fields) {
				bool.add(new TermQuery(new Term(field, token)), Occur.SHOULD);
			}
		}
		bool.setMinimumNumberShouldMatch(1);
		return bool.build();
	}

	/**
	 * RANGE on a UNITDATE item matches its datings themselves, one interval at a
	 * time - the same INTERSECTS the Elasticsearch adapter gets from the
	 * {@code date_range} field the old portal has always queried, so the two
	 * engines answer this alike. RANGE directly on a bound field is a plain long
	 * range; anything else falls back to a lexicographic term range.
	 */
	private Query rangeQuery(RangeFilter f) {
		String field = f.getField();
		String lower = f.getGte() != null ? f.getGte() : f.getGt();
		String upper = f.getLte() != null ? f.getLte() : f.getLt();

		var itemType = typesHolder.getItemTypeForCode(field);
		if (itemType != null && DataType.UNITDATE.equals(itemType.getType())) {
			Long lowerMillis = parseMillis(lower, f.getGt() != null ? 1 : 0);
			Long upperMillis = parseMillis(upper, f.getLt() != null ? -1 : 0);
			return LuceneSearchIndex.intervalQuery(field,
					lowerMillis != null ? lowerMillis : Long.MIN_VALUE,
					upperMillis != null ? upperMillis : Long.MAX_VALUE);
		}
		if (field.endsWith("~L") || field.endsWith("~H")) {
			Long lowerMillis = parseMillis(lower, f.getGt() != null ? 1 : 0);
			Long upperMillis = parseMillis(upper, f.getLt() != null ? -1 : 0);
			return LongPoint.newRangeQuery(field,
					lowerMillis != null ? lowerMillis : Long.MIN_VALUE,
					upperMillis != null ? upperMillis : Long.MAX_VALUE);
		}
		return new TermRangeQuery(field,
				lower != null ? new BytesRef(lower) : null,
				upper != null ? new BytesRef(upper) : null,
				f.getGt() == null, f.getLt() == null);
	}

	/**
	 * Parses an ISO date-time bound - local ({@code 1850-01-01T00:00:00}) or with
	 * an offset as the old UI really sends it ({@code 0001-01-01T00:00:00.000Z});
	 * {@code exclusiveShift} turns gt/lt into inclusive millis.
	 */
	private static Long parseMillis(String value, int exclusiveShift) {
		if (value == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli() + exclusiveShift;
		} catch (DateTimeParseException e) {
			try {
				return OffsetDateTime.parse(value).toInstant().toEpochMilli() + exclusiveShift;
			} catch (DateTimeParseException e2) {
				throw new IllegalArgumentException("Range bound is not an ISO date-time: " + value, e2);
			}
		}
	}

	/**
	 * CONTAINS as a wildcard over analyzed tokens: the value is folded by the same
	 * analysis chain, so matching is diacritics- and case-insensitive like the ES
	 * {@code case_insensitive} wildcard. Values analyzing into several tokens can
	 * never match a single indexed token - the same holds for the ES wildcard on
	 * an analyzed field.
	 */
	private Query containsQuery(ContainsFilter f) throws IOException {
		List<String> tokens = index.analyze(f.getValue());
		String needle = tokens.size() == 1 ? tokens.get(0) : String.join(" ", tokens);
		return new WildcardQuery(new Term(f.getField(), "*" + needle + "*"));
	}

	// -------------------------------------------------------------------------
	// Sort
	// -------------------------------------------------------------------------

	private Sort toSort(Params params, boolean flip) {
		var sorts = params.getSort();
		if (sorts == null || sorts.isEmpty()) {
			return flip ? new Sort(new SortField(null, SortField.Type.SCORE, true)) : Sort.RELEVANCE;
		}
		var sortFields = new ArrayList<SortField>();
		for (cz.aron.api.rest.model.Sort sort : sorts) {
			if (sort instanceof FieldSort f) {
				// "name" sorts by the index-time Czech collation key (as in QueryBuilder)
				String field = "name".equals(f.getField()) ? "nameSort" : f.getField();
				var sortField = new SortField(field, SortField.Type.STRING, flip);
				sortField.setMissingValue(flip ? SortField.STRING_FIRST : SortField.STRING_LAST);
				sortFields.add(sortField);
			} else if (sort instanceof ScoreSort) {
				// relevance sorts descending by default; flip inverts it
				sortFields.add(new SortField(null, SortField.Type.SCORE, flip));
			}
		}
		return sortFields.isEmpty() ? Sort.RELEVANCE : new Sort(sortFields.toArray(SortField[]::new));
	}

	// -------------------------------------------------------------------------
	// Aggregations
	// -------------------------------------------------------------------------

	private Map<String, List<AggregationResult>> aggregate(IndexSearcher searcher, Query query,
			List<cz.aron.api.rest.model.Aggregation> aggregations) throws IOException {
		if (aggregations == null || aggregations.isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, List<AggregationResult>>();
		for (var agg : aggregations) {
			result.put(aggregationName(agg), aggregation(searcher, query, agg));
		}
		return result;
	}

	/**
	 * One aggregation in document scope: a FILTER narrows the query its
	 * sub-aggregations see, a NESTED on the {@code rels} path moves them into
	 * relation scope.
	 */
	private List<AggregationResult> aggregation(IndexSearcher searcher, Query query,
			cz.aron.api.rest.model.Aggregation agg) throws IOException {
		String name = aggregationName(agg);
		if (agg instanceof TermsAggregation terms && terms.getField() != null) {
			return termsBuckets(searcher, query, terms);
		}
		if (agg instanceof MaxAggregation max && max.getField() != null) {
			return List.of(minMax(searcher, query, max.getField().toString(),
					max.getFormat() != null ? max.getFormat().toString() : null, name, true));
		}
		if (agg instanceof MinAggregation min && min.getField() != null) {
			return List.of(minMax(searcher, query, min.getField().toString(),
					min.getFormat() != null ? min.getFormat().toString() : null, name, false));
		}
		if (agg instanceof FilterAggregation filter && filter.getFilter() != null) {
			Query narrowed = new BooleanQuery.Builder().add(query, Occur.MUST)
					.add(toQuery(filter.getFilter()), Occur.FILTER).build();
			return List.of(new AggregationResult().key(name).value(String.valueOf(searcher.count(narrowed)))
					.aggregations(aggregate(searcher, narrowed, filter.getAggregations())));
		}
		if (agg instanceof NestedAggregation nested && RELS_PATH.equals(nested.getPath())) {
			// the records in scope, not their relations - see the class javadoc
			return List.of(new AggregationResult().key(name).value(String.valueOf(searcher.count(query)))
					.aggregations(relAggregations(searcher, query, RelCondition.ANY, null, nested.getAggregations())));
		}
		log.debug("Answering unsupported aggregation with an empty result on the lucene engine: {}",
				agg.getClass().getSimpleName());
		return emptyShape(agg);
	}

	private static String aggregationName(cz.aron.api.rest.model.Aggregation agg) {
		if (agg instanceof TermsAggregation a && a.getName() != null) {
			return a.getName();
		}
		if (agg instanceof NestedAggregation a && a.getName() != null) {
			return a.getName();
		}
		if (agg instanceof FilterAggregation a && a.getName() != null) {
			return a.getName();
		}
		if (agg instanceof MaxAggregation a && a.getName() != null) {
			return a.getName();
		}
		if (agg instanceof MinAggregation a && a.getName() != null) {
			return a.getName();
		}
		return agg.getType();
	}

	/**
	 * Empty result of an unsupported aggregation, recursively shaped like the ES
	 * response would be: a zero-count bucket wrapping its sub-aggregations, empty
	 * terms lists, valueless metrics.
	 */
	private static List<AggregationResult> emptyShape(cz.aron.api.rest.model.Aggregation aggregation) {
		if (aggregation instanceof TermsAggregation) {
			return List.of();
		}
		if (aggregation instanceof MaxAggregation || aggregation instanceof MinAggregation) {
			return List.of(new AggregationResult().key(aggregationName(aggregation)));
		}
		var subAggs = new HashMap<String, List<AggregationResult>>();
		if (aggregation instanceof BucketAggregation bucket && bucket.getAggregations() != null) {
			for (var sub : bucket.getAggregations()) {
				subAggs.put(aggregationName(sub), emptyShape(sub));
			}
		}
		return List.of(new AggregationResult().key(aggregationName(aggregation)).value("0").aggregations(subAggs));
	}

	/**
	 * Terms buckets by term enumeration, counted against the full query. Ordered
	 * like the ES default: document count descending, ties by key ascending;
	 * truncated to the requested size (ES default {@value #DEFAULT_TERMS_SIZE}).
	 */
	private List<AggregationResult> termsBuckets(IndexSearcher searcher, Query query, TermsAggregation aggregation)
			throws IOException {
		var buckets = new ArrayList<AggregationResult>();
		var terms = MultiTerms.getTerms(searcher.getIndexReader(), aggregation.getField());
		if (terms == null) {
			return buckets; // unknown field aggregates to no buckets, as on ES
		}
		var iterator = terms.iterator();
		BytesRef term;
		while ((term = iterator.next()) != null) {
			String value = term.utf8ToString();
			long count = searcher.count(new BooleanQuery.Builder()
					.add(query, Occur.MUST)
					.add(new TermQuery(new Term(aggregation.getField(), value)), Occur.FILTER)
					.build());
			if (count > 0) {
				buckets.add(new AggregationResult().key(value).value(String.valueOf(count)));
			}
		}
		return orderAndTruncate(buckets, aggregation);
	}

	/** The ES bucket order - count descending, ties by key ascending - then cut to size. */
	private static List<AggregationResult> orderAndTruncate(List<AggregationResult> buckets,
			TermsAggregation aggregation) {
		buckets.sort(Comparator.comparingLong((AggregationResult b) -> Long.parseLong(b.getValue())).reversed()
				.thenComparing(AggregationResult::getKey));
		int size = aggregation.getSize() != null ? aggregation.getSize() : DEFAULT_TERMS_SIZE;
		return buckets.size() > size ? new ArrayList<>(buckets.subList(0, size)) : buckets;
	}

	/**
	 * Min/max of a date-bound field ({@code ~L}/{@code ~H} doc-values) over the
	 * matching documents (the engine brick shared with the port's dating bounds).
	 * Mirrors the ES metric result mapping: {@code value} is the epoch-millis
	 * double, {@code asString} its {@code format}-formatted form; both
	 * {@code null} when nothing matches.
	 */
	private AggregationResult minMax(IndexSearcher searcher, Query query, String field, String format, String key,
			boolean max) throws IOException {
		Long millis = index.minMaxMillis(searcher, query, field, max);
		var result = new AggregationResult().key(key);
		if (millis != null) {
			result.value(String.valueOf((double) millis));
			if (format != null) {
				result.asString(DateTimeFormatter.ofPattern(format).withZone(ZoneOffset.UTC)
						.format(Instant.ofEpochMilli(millis)));
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Relation scope - the nested rels aggregations
	// -------------------------------------------------------------------------

	private Map<String, List<AggregationResult>> relAggregations(IndexSearcher searcher, Query query,
			RelCondition condition, List<RelBucket> scope, List<cz.aron.api.rest.model.Aggregation> aggregations)
			throws IOException {
		if (aggregations == null || aggregations.isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, List<AggregationResult>>();
		for (var agg : aggregations) {
			result.put(aggregationName(agg), relAggregation(searcher, query, condition, scope, agg));
		}
		return result;
	}

	/**
	 * One aggregation in relation scope. {@code scope} is what the enclosing
	 * filters have already enumerated, or {@code null} while nothing has needed
	 * the relations yet - a relation's count depends on its value alone, so a
	 * further condition narrows an enumerated scope in place instead of
	 * enumerating a second time.
	 */
	private List<AggregationResult> relAggregation(IndexSearcher searcher, Query query, RelCondition condition,
			List<RelBucket> scope, cz.aron.api.rest.model.Aggregation agg) throws IOException {
		String name = aggregationName(agg);
		if (agg instanceof FilterAggregation filter && filter.getFilter() != null) {
			RelCondition narrowedCondition = condition.and(relCondition(filter.getFilter()));
			List<RelBucket> narrowedScope = scope != null ? retain(scope, narrowedCondition)
					: relScope(searcher, query, narrowedCondition);
			long relations = narrowedScope.stream().mapToLong(RelBucket::count).sum();
			return List.of(new AggregationResult().key(name).value(String.valueOf(relations)).aggregations(
					relAggregations(searcher, query, narrowedCondition, narrowedScope, filter.getAggregations())));
		}
		if (agg instanceof TermsAggregation terms && terms.getField() != null) {
			return relTermsBuckets(scope != null ? scope : relScope(searcher, query, condition), terms);
		}
		log.debug("Answering unsupported relation aggregation with an empty result on the lucene engine: {}",
				agg.getClass().getSimpleName());
		return emptyShape(agg);
	}

	/**
	 * The relations the condition admits, each with the number of matching
	 * records carrying it. A reference item type is a relation type and its
	 * {@code ~ID~LABEL} terms are that type's relation values - which is what
	 * makes the nested aggregations answerable from the flat document, with
	 * neither a join nor an index of their own.
	 *
	 * <p>The work is proportional to the distinct relation values of the admitted
	 * types, which is why the condition's type gate matters: the old UI's
	 * autocomplete names one reference type and enumerates that one field.
	 */
	private List<RelBucket> relScope(IndexSearcher searcher, Query query, RelCondition condition) throws IOException {
		var scope = new ArrayList<RelBucket>();
		for (var itemType : typesHolder.getAllItemTypes()) {
			if (!DataType.APU_REF.equals(itemType.getType()) || !condition.possibleForType(itemType.getCode())) {
				continue;
			}
			String field = itemType.getCode() + "~ID~LABEL";
			var terms = MultiTerms.getTerms(searcher.getIndexReader(), field);
			if (terms == null) {
				continue;
			}
			var groups = typesHolder.getItemGroupsForItemType(itemType.getCode());
			var iterator = terms.iterator();
			BytesRef term;
			while ((term = iterator.next()) != null) {
				Rel rel = Rel.of(itemType.getCode(), groups, term.utf8ToString());
				if (!condition.matches(rel)) {
					continue;
				}
				long count = searcher.count(new BooleanQuery.Builder()
						.add(query, Occur.MUST)
						.add(new TermQuery(new Term(field, rel.idLabel())), Occur.FILTER)
						.build());
				if (count > 0) {
					scope.add(new RelBucket(rel, count));
				}
			}
		}
		return scope;
	}

	private static List<RelBucket> retain(List<RelBucket> scope, RelCondition condition) throws IOException {
		var narrowed = new ArrayList<RelBucket>();
		for (RelBucket bucket : scope) {
			if (condition.matches(bucket.rel())) {
				narrowed.add(bucket);
			}
		}
		return narrowed;
	}

	/**
	 * Terms buckets of a {@code rels.*} field: the relations in scope grouped by
	 * that field's values and counted as ES counts nested documents - the records
	 * carrying each relation, summed. A field that is not one of the relation's
	 * own aggregates to no buckets, as an unmapped field does on ES.
	 */
	private static List<AggregationResult> relTermsBuckets(List<RelBucket> scope, TermsAggregation aggregation) {
		var counts = new HashMap<String, Long>();
		for (RelBucket bucket : scope) {
			List<String> values = bucket.rel().values(aggregation.getField());
			if (values != null) {
				values.forEach(value -> counts.merge(value, bucket.count(), Long::sum));
			}
		}
		var buckets = new ArrayList<AggregationResult>();
		counts.forEach((key, count) -> buckets.add(new AggregationResult().key(key).value(String.valueOf(count))));
		return orderAndTruncate(buckets, aggregation);
	}

	/**
	 * The nested filter's conditions, translated onto the reconstructed relation.
	 * The document-scope translation cannot serve here: these conditions select
	 * relations, and a document-level clause would match a record through one
	 * relation while the bucket belongs to another.
	 */
	private RelCondition relCondition(Filter filter) throws IOException {
		if (filter instanceof AndFilter f) {
			RelCondition condition = RelCondition.ANY;
			for (RelCondition sub : relConditions(f.getFilters())) {
				condition = condition.and(sub);
			}
			return condition;
		}
		if (filter instanceof OrFilter f) {
			List<RelCondition> alternatives = relConditions(f.getFilters());
			return new RelCondition() {

				@Override
				public boolean possibleForType(String type) {
					return alternatives.isEmpty() || alternatives.stream().anyMatch(c -> c.possibleForType(type));
				}

				@Override
				public boolean matches(Rel rel) throws IOException {
					if (alternatives.isEmpty()) {
						return true; // an empty ES bool matches everything
					}
					for (RelCondition alternative : alternatives) {
						if (alternative.matches(rel)) {
							return true;
						}
					}
					return false;
				}
			};
		}
		if (filter instanceof NotFilter f) {
			List<RelCondition> excluded = relConditions(f.getFilters());
			return new RelCondition() {

				@Override
				public boolean possibleForType(String type) {
					return true; // a negation rules out relations, not whole item types
				}

				@Override
				public boolean matches(Rel rel) throws IOException {
					for (RelCondition condition : excluded) {
						if (condition.matches(rel)) {
							return false;
						}
					}
					return true;
				}
			};
		}
		if (filter instanceof EqFilter f) {
			return fieldCondition(f.getField(), values -> values.contains(f.getValue()));
		}
		if (filter instanceof FullTextFieldFilter f) {
			List<String> tokens = index.analyze(f.getValue());
			return fieldCondition(f.getField(), values -> {
				for (String value : values) {
					if (matchesAllWordsLastPrefix(tokens, value)) {
						return true;
					}
				}
				return false;
			});
		}
		if (filter instanceof ContainsFilter f) {
			List<String> valueTokens = index.analyze(f.getValue());
			String needle = valueTokens.size() == 1 ? valueTokens.get(0) : String.join(" ", valueTokens);
			return fieldCondition(f.getField(), values -> {
				for (String value : values) {
					for (String token : index.analyze(value)) {
						if (token.contains(needle)) {
							return true;
						}
					}
				}
				return false;
			});
		}
		throw new IllegalArgumentException(
				"Unsupported filter type in a relation aggregation: " + filter.getClass().getSimpleName());
	}

	private List<RelCondition> relConditions(List<Filter> filters) throws IOException {
		var conditions = new ArrayList<RelCondition>();
		if (filters != null) {
			for (Filter filter : filters) {
				conditions.add(relCondition(filter));
			}
		}
		return conditions;
	}

	/**
	 * A condition on one {@code rels.*} field. Only the fields the item type
	 * itself decides - the relation's type and its groups - can close the type
	 * gate; the others differ from relation to relation.
	 */
	private RelCondition fieldCondition(String field, RelValuesPredicate predicate) {
		if (Rel.of("", List.of(), "").values(field) == null) {
			throw new IllegalArgumentException("Unsupported relation field: " + field);
		}
		return new RelCondition() {

			@Override
			public boolean possibleForType(String type) {
				if (!TYPE_DERIVED_FIELDS.contains(field)) {
					return true;
				}
				var probe = new Rel(type, typesHolder.getItemGroupsForItemType(type), "", "", "");
				try {
					return predicate.test(probe.values(field));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			@Override
			public boolean matches(Rel rel) throws IOException {
				return predicate.test(rel.values(field));
			}
		};
	}

	/**
	 * The Java side of {@link LuceneSearchIndex#allWordsLastPrefixQuery}: every
	 * analyzed word present, the last one as a prefix. No word matches nothing,
	 * as an empty query does on either engine - which is why a reference facet's
	 * autocomplete offers its options only once the reader types.
	 */
	private boolean matchesAllWordsLastPrefix(List<String> tokens, String value) throws IOException {
		if (tokens.isEmpty()) {
			return false;
		}
		List<String> valueTokens = index.analyze(value);
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			boolean found = i == tokens.size() - 1
					? valueTokens.stream().anyMatch(valueToken -> valueToken.startsWith(token))
					: valueTokens.contains(token);
			if (!found) {
				return false;
			}
		}
		return true;
	}

	/**
	 * One relation as the flat document carries it: the reference item type is
	 * the relation's type and carries its groups, and one {@code ~ID~LABEL} value
	 * pairs the target's uuid with the target's label.
	 */
	private record Rel(String type, List<String> groups, String targetId, String label, String idLabel) {

		static Rel of(String type, List<String> groups, String idLabel) {
			int separator = idLabel.indexOf('|');
			return separator < 0
					? new Rel(type, groups, idLabel, "", idLabel)
					: new Rel(type, groups, idLabel.substring(0, separator), idLabel.substring(separator + 1),
							idLabel);
		}

		/** Values of one {@code rels.*} field; {@code null} when the field is not one. */
		List<String> values(String field) {
			return switch (field) {
			case "rels.type" -> List.of(type);
			case "rels.groups" -> groups;
			case "rels.targetId" -> List.of(targetId);
			case "rels.label" -> List.of(label);
			case "rels.idLabel" -> List.of(idLabel);
			default -> null;
			};
		}
	}

	/** A relation in scope, with the number of matching records carrying it. */
	private record RelBucket(Rel rel, long count) {
	}

	/**
	 * A condition on a relation, in two parts: which item types can carry a
	 * matching relation at all - so a type-scoped filter enumerates one reference
	 * field instead of every one of them - and whether a concrete relation
	 * matches.
	 */
	private interface RelCondition {

		RelCondition ANY = new RelCondition() {

			@Override
			public boolean possibleForType(String type) {
				return true;
			}

			@Override
			public boolean matches(Rel rel) {
				return true;
			}
		};

		boolean possibleForType(String type);

		boolean matches(Rel rel) throws IOException;

		default RelCondition and(RelCondition other) {
			RelCondition self = this;
			return new RelCondition() {

				@Override
				public boolean possibleForType(String type) {
					return self.possibleForType(type) && other.possibleForType(type);
				}

				@Override
				public boolean matches(Rel rel) throws IOException {
					return self.matches(rel) && other.matches(rel);
				}
			};
		}
	}

	/** Whether the values of one {@code rels.*} field satisfy a condition. */
	@FunctionalInterface
	private interface RelValuesPredicate {

		boolean test(List<String> values) throws IOException;
	}

}
