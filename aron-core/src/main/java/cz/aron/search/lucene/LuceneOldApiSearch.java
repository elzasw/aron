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

import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
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
 * <li>NESTED/FILTER aggregations (the rels shapes of detail pages) answer with
 * an empty result of the correct recursive shape - a later slice will serve
 * them from the rels index;</li>
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
	 * RANGE on a UNITDATE item is an interval intersection over its {@code ~L}/
	 * {@code ~H} bound fields (the base field itself is not indexed here - the ES
	 * date_range field is represented by the bounds, see the adapter layout).
	 * RANGE directly on a bound field is a plain long range; anything else falls
	 * back to a lexicographic term range.
	 */
	private Query rangeQuery(RangeFilter f) {
		String field = f.getField();
		String lower = f.getGte() != null ? f.getGte() : f.getGt();
		String upper = f.getLte() != null ? f.getLte() : f.getLt();

		var itemType = typesHolder.getItemTypeForCode(field);
		if (itemType != null && DataType.UNITDATE.equals(itemType.getType())) {
			var bool = new BooleanQuery.Builder();
			Long lowerMillis = parseMillis(lower, f.getGt() != null ? 1 : 0);
			Long upperMillis = parseMillis(upper, f.getLt() != null ? -1 : 0);
			if (upperMillis != null) {
				bool.add(LongPoint.newRangeQuery(field + "~L", Long.MIN_VALUE, upperMillis), Occur.FILTER);
			}
			if (lowerMillis != null) {
				bool.add(LongPoint.newRangeQuery(field + "~H", lowerMillis, Long.MAX_VALUE), Occur.FILTER);
			}
			bool.add(new MatchAllDocsQuery(), Occur.MUST);
			return bool.build();
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
			String name = aggregationName(agg);
			if (agg instanceof TermsAggregation terms && terms.getField() != null) {
				result.put(name, termsBuckets(searcher, query, terms));
			} else if (agg instanceof MaxAggregation max && max.getField() != null) {
				result.put(name, List.of(minMax(searcher, query, max.getField().toString(),
						max.getFormat() != null ? max.getFormat().toString() : null, name, true)));
			} else if (agg instanceof MinAggregation min && min.getField() != null) {
				result.put(name, List.of(minMax(searcher, query, min.getField().toString(),
						min.getFormat() != null ? min.getFormat().toString() : null, name, false)));
			} else {
				// NESTED/FILTER rels shapes are not computed yet (a later slice will
				// serve them from the rels index) - answer with an EMPTY result of
				// the same recursive shape: the old UI navigates the structure
				// without guards, a missing key would crash it
				log.debug("Answering unsupported aggregation with an empty result on the lucene engine: {}",
						agg.getClass().getSimpleName());
				result.put(name, emptyShape(agg));
			}
		}
		return result;
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
		buckets.sort(Comparator.comparingLong((AggregationResult b) -> Long.parseLong(b.getValue())).reversed()
				.thenComparing(AggregationResult::getKey));
		int size = aggregation.getSize() != null ? aggregation.getSize() : DEFAULT_TERMS_SIZE;
		return buckets.size() > size ? new ArrayList<>(buckets.subList(0, size)) : buckets;
	}

	/**
	 * Min/max of a date-bound field ({@code ~L}/{@code ~H} doc-values) over the
	 * matching documents: top-1 search sorted by the field. Mirrors the ES metric
	 * result mapping: {@code value} is the epoch-millis double, {@code asString}
	 * its {@code format}-formatted form; both {@code null} when nothing matches.
	 */
	private AggregationResult minMax(IndexSearcher searcher, Query query, String field, String format, String key,
			boolean max) throws IOException {
		long missingMarker = max ? Long.MIN_VALUE : Long.MAX_VALUE;
		var sortField = new SortedNumericSortField(field, SortField.Type.LONG, max);
		sortField.setMissingValue(missingMarker);
		var top = searcher.search(query, 1, new Sort(sortField));

		Long millis = null;
		if (top.scoreDocs.length > 0 && top.scoreDocs[0] instanceof FieldDoc fieldDoc) {
			long value = ((Number) fieldDoc.fields[0]).longValue();
			if (value != missingMarker) {
				millis = value;
			}
		}
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

}
