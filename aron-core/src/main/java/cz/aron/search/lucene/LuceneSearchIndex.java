package cz.aron.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.domain.DataType;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.search.ApuDocument;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.FieldFilter;
import cz.aron.search.RelationDocument;
import cz.aron.search.SearchIndex;
import cz.aron.search.relevance.RelevancePlan;
import jakarta.annotation.PreDestroy;

/**
 * Embedded Lucene adapter of the search port (doc/search-port.md): a supported
 * engine for small/ES-less deployments, and the engine of tests and dev mode.
 * Elasticsearch is Lucene inside, so analysis behavior (tokenization, lowercase,
 * ASCII folding) matches the production ES adapter by construction rather than
 * by imitation. Serves the new API fully; the old API's search endpoints run on
 * this index too, dev/test-grade, via {@link LuceneOldApiSearch}.
 * <p>
 * Storage: {@code search.lucene.path} set = persisted index under that directory
 * ({@code apu/}, {@code rels/} subdirectories); unset = in-memory (tests, dev
 * mode - rebuilt on startup by {@link cz.aron.search.SearchIndexManager}).
 * <p>
 * Layout: mirrors the ES mapping semantics per types.yaml - STRING item fields
 * are analyzed text, ENUM/APU_REF/LINK/INTEGER values are exact terms, UNITDATE
 * {@code ~L}/{@code ~H} bounds become long points (epoch millis) for interval
 * filters. Facet buckets are counted by term enumeration per field - suited to
 * the embedded scale this adapter targets. The indexed-fields CRC lives in the
 * Lucene commit user data (the analog of the ES index {@code _meta}).
 */
@ConditionalOnProperty(name = "search.engine", havingValue = "lucene")
@Component
public class LuceneSearchIndex implements SearchIndex {

	private static final String FIELDS_CRC_KEY = "fieldsCrc";

	private static final String LAYOUT_VERSION_KEY = "layoutVersion";

	/**
	 * Version of the Lucene document layout produced by this code. Bump on any
	 * layout change (new doc-values, changed field types): a persisted index
	 * committed under a different version reports no stored CRC, so the startup
	 * bootstrap rebuilds and reindexes it.
	 */
	private static final String LAYOUT_VERSION = "3";

	private final Analyzer foldingAnalyzer = new FoldingAnalyzer();

	private final TypesHolder typesHolder;

	private final Directory apuDirectory;

	private final Directory relsDirectory;

	private final IndexWriter apuWriter;

	private final IndexWriter relsWriter;

	private final SearcherManager apuSearchers;

	/** Mirrors the ES analysis chain used for analyzed fields: standard tokenizer + lowercase + ASCII folding. */
	private static final class FoldingAnalyzer extends Analyzer {
		@Override
		protected TokenStreamComponents createComponents(String fieldName) {
			var tokenizer = new StandardTokenizer();
			TokenStream stream = new LowerCaseFilter(tokenizer);
			stream = new ASCIIFoldingFilter(stream);
			return new TokenStreamComponents(tokenizer, stream);
		}

		@Override
		public int getPositionIncrementGap(String fieldName) {
			// ES's default gap for multi-valued text: phrases never match across
			// two values of allText (doc/search-relevance.md §4.1)
			return 100;
		}
	}

	public LuceneSearchIndex(TypesHolder typesHolder, @Value("${search.lucene.path:}") String path) {
		this.typesHolder = typesHolder;
		try {
			if (path == null || path.isBlank()) {
				apuDirectory = new ByteBuffersDirectory();
				relsDirectory = new ByteBuffersDirectory();
			} else {
				Path root = Path.of(path);
				Files.createDirectories(root.resolve("apu"));
				Files.createDirectories(root.resolve("rels"));
				apuDirectory = FSDirectory.open(root.resolve("apu"));
				relsDirectory = FSDirectory.open(root.resolve("rels"));
			}
			apuWriter = new IndexWriter(apuDirectory, new IndexWriterConfig(foldingAnalyzer));
			relsWriter = new IndexWriter(relsDirectory, new IndexWriterConfig(foldingAnalyzer));
			apuSearchers = new SearcherManager(apuWriter, null);
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to open Lucene search index", e);
		}
	}

	@PreDestroy
	public void close() throws IOException {
		apuSearchers.close();
		apuWriter.close();
		relsWriter.close();
		apuDirectory.close();
		relsDirectory.close();
	}

	// --- schema lifecycle ---------------------------------------------------

	@Override
	public void createSchema() {
		// the "schema" (analyzers, field handling) lives in code - materialize an
		// initial commit so a persisted index directory is valid after startup
		commitAndRefresh();
	}

	@Override
	public void dropSchema() {
		try {
			apuWriter.deleteAll();
			apuWriter.setLiveCommitData(Set.of());
			relsWriter.deleteAll();
			relsWriter.setLiveCommitData(Set.of());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		commitAndRefresh();
	}

	@Override
	public Long storedFieldsCrc() {
		try (var reader = DirectoryReader.open(apuDirectory)) {
			var userData = reader.getIndexCommit().getUserData();
			if (!LAYOUT_VERSION.equals(userData.get(LAYOUT_VERSION_KEY))) {
				// index written by another layout version = treat as no schema
				return null;
			}
			String crc = userData.get(FIELDS_CRC_KEY);
			return crc != null ? Long.valueOf(crc) : null;
		} catch (IndexNotFoundException e) {
			// no commit yet = schema does not exist
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void storeFieldsCrc(long crc) {
		apuWriter.setLiveCommitData(Set.of(Map.entry(FIELDS_CRC_KEY, Long.toString(crc)),
				Map.entry(LAYOUT_VERSION_KEY, LAYOUT_VERSION)));
		commitAndRefresh();
	}

	// --- write side ---------------------------------------------------------

	@Override
	public void indexApus(Collection<ApuDocument> documents) {
		try {
			for (var document : documents) {
				apuWriter.updateDocument(new Term("uuid", document.getUuid()), toLuceneDocument(document));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		commitAndRefresh();
	}

	@Override
	public void indexRelations(Collection<RelationDocument> relations) {
		try {
			for (var relation : relations) {
				var doc = new Document();
				doc.add(new StringField("source", relation.source(), Field.Store.YES));
				doc.add(new StringField("relation", relation.relation(), Field.Store.YES));
				doc.add(new StringField("target", relation.target(), Field.Store.YES));
				relsWriter.addDocument(doc);
			}
			relsWriter.commit();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void deleteApusBySource(long apuSourceId) {
		try {
			apuWriter.deleteDocuments(LongPoint.newExactQuery("apuSourceId", apuSourceId));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		commitAndRefresh();
	}

	// --- read side ----------------------------------------------------------

	@Override
	public ApuSearchResult search(ApuSearchQuery query) {
		try {
			IndexSearcher searcher = apuSearchers.acquire();
			try {
				Query mainQuery = buildMainQuery(query);
				Query fullQuery = withFacetFilters(mainQuery, query.filters(), null);
				long exactTotal = searcher.count(fullQuery);
				var hits = new ArrayList<ApuSearchResult.Hit>();
				if (exactTotal > query.from() && query.size() > 0) {
					int wanted = query.from() + query.size();
					// doDocScores: the RELEVANCE chain leads with the score field
					TopDocs top = searcher.search(fullQuery, wanted, sortFor(query.sort()), true);
					var storedFields = searcher.storedFields();
					for (int i = query.from(); i < top.scoreDocs.length; i++) {
						var doc = storedFields.document(top.scoreDocs[i].doc);
						hits.add(new ApuSearchResult.Hit(doc.get("uuid"), doc.get("name"), doc.get("description"),
								doc.get("type"), Boolean.parseBoolean(doc.get("containsDigitalObjects"))));
					}
				}
				// the count is exact either way (cheap on an embedded index); the
				// report mirrors the ES adapter's contract: capped = (totalUpTo, GTE)
				boolean capped = query.totalUpTo() != null && exactTotal > query.totalUpTo();
				return new ApuSearchResult(capped ? query.totalUpTo() : exactTotal,
						capped ? ApuSearchResult.TotalRelation.GTE : ApuSearchResult.TotalRelation.EQ,
						hits, countBuckets(searcher, query, mainQuery),
						computeBounds(searcher, query, mainQuery));
			} finally {
				apuSearchers.release(searcher);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Full deterministic sort chain of a mode: primary key, then nameSort, then
	 * the uuid tie-break; documents without the sorted value (no name, no
	 * dating) sort last in either direction (doc/search-relevance.md §4.4).
	 */
	private static Sort sortFor(ApuSearchQuery.SortMode mode) {
		var uuid = new SortField("uuid", SortField.Type.STRING);
		return switch (mode) {
			case RELEVANCE -> new Sort(SortField.FIELD_SCORE, nameSortField(false), uuid);
			case NAME -> new Sort(nameSortField(false), uuid);
			case NAME_DESC -> new Sort(nameSortField(true), uuid);
			case DATE_ASC -> new Sort(dateSortField("dateL", false), nameSortField(false), uuid);
			case DATE_DESC -> new Sort(dateSortField("dateH", true), nameSortField(false), uuid);
		};
	}

	private static SortField nameSortField(boolean reverse) {
		var field = new SortField("nameSort", SortField.Type.STRING, reverse);
		// missing-last must flip with the direction (STRING_LAST = "greater than
		// everything", which a reversed sort would file FIRST)
		field.setMissingValue(reverse ? SortField.STRING_FIRST : SortField.STRING_LAST);
		return field;
	}

	private static SortField dateSortField(String field, boolean reverse) {
		var sortField = new SortedNumericSortField(field, SortField.Type.LONG, reverse);
		sortField.setMissingValue(reverse ? Long.MIN_VALUE : Long.MAX_VALUE);
		return sortField;
	}

	/**
	 * Bucket counting by term enumeration: for each requested bucket field, every
	 * term of the field is counted against the query without the paired filter
	 * field's own facet filters (multi-select semantics). Buckets are ordered by
	 * count descending (ties by value) and capped by the requested size. Cost
	 * grows with term cardinality - fine for the embedded scale this adapter
	 * targets.
	 */
	private Map<String, List<ApuSearchResult.Bucket>> countBuckets(IndexSearcher searcher, ApuSearchQuery query,
			Query mainQuery) throws IOException {
		if (query.buckets().isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, List<ApuSearchResult.Bucket>>();
		for (ApuSearchQuery.BucketRequest bucket : query.buckets()) {
			Query base = withFacetFilters(mainQuery, query.filters(), bucket.filterField());
			var buckets = new ArrayList<ApuSearchResult.Bucket>();
			Terms terms = MultiTerms.getTerms(searcher.getIndexReader(), bucket.bucketField());
			if (terms != null) {
				TermsEnum iterator = terms.iterator();
				BytesRef term;
				while ((term = iterator.next()) != null) {
					String value = term.utf8ToString();
					long count = searcher.count(new BooleanQuery.Builder()
							.add(base, Occur.MUST)
							.add(new TermQuery(new Term(bucket.bucketField(), value)), Occur.FILTER)
							.build());
					if (count > 0) {
						buckets.add(new ApuSearchResult.Bucket(value, count));
					}
				}
			}
			buckets.sort(Comparator.comparingLong(ApuSearchResult.Bucket::count).reversed()
					.thenComparing(ApuSearchResult.Bucket::value));
			result.put(bucket.bucketField(), buckets.size() > bucket.size()
					? new ArrayList<>(buckets.subList(0, bucket.size()))
					: buckets);
		}
		return result;
	}

	/** Dating bounds per requested UNITDATE field: min of {@code ~L}, max of {@code ~H} (doc-values). */
	private Map<String, ApuSearchResult.Bounds> computeBounds(IndexSearcher searcher, ApuSearchQuery query,
			Query mainQuery) throws IOException {
		if (query.boundsFields().isEmpty()) {
			return Map.of();
		}
		var result = new HashMap<String, ApuSearchResult.Bounds>();
		for (String field : query.boundsFields()) {
			Query base = withFacetFilters(mainQuery, query.filters(), field);
			Long min = minMaxMillis(searcher, base, field + "~L", false);
			Long max = minMaxMillis(searcher, base, field + "~H", true);
			if (min != null && max != null) {
				result.put(field, new ApuSearchResult.Bounds(min, max));
			}
		}
		return result;
	}

	/**
	 * Min/max of a date-bound field's doc-values over the matching documents,
	 * resolved as a top-1 search sorted by the field; {@code null} when no
	 * matching document carries the field. Shared with {@link LuceneOldApiSearch}
	 * (the old API's MIN/MAX metric aggregations).
	 */
	Long minMaxMillis(IndexSearcher searcher, Query query, String field, boolean max) throws IOException {
		long missingMarker = max ? Long.MIN_VALUE : Long.MAX_VALUE;
		var sortField = new SortedNumericSortField(field, SortField.Type.LONG, max);
		sortField.setMissingValue(missingMarker);
		var top = searcher.search(query, 1, new Sort(sortField));
		if (top.scoreDocs.length > 0 && top.scoreDocs[0] instanceof FieldDoc fieldDoc) {
			long value = ((Number) fieldDoc.fields[0]).longValue();
			if (value != missingMarker) {
				return value;
			}
		}
		return null;
	}

	private Query buildMainQuery(ApuSearchQuery query) throws IOException {
		var root = new BooleanQuery.Builder();
		if (query.fulltext() != null) {
			RelevancePlan plan = query.fulltext();
			// the gate decides WHAT matches - filter context, no score pollution;
			// scores come exclusively from the weighted tiers (R-9)
			var gate = new BooleanQuery.Builder();
			for (RelevancePlan.Clause clause : plan.gate()) {
				gate.add(clauseQuery(clause, false), Occur.SHOULD);
			}
			gate.setMinimumNumberShouldMatch(plan.minimumShouldMatch());
			root.add(gate.build(), Occur.FILTER);
			for (RelevancePlan.Clause clause : plan.scoring()) {
				root.add(clauseQuery(clause, true), Occur.SHOULD);
			}
		} else {
			root.add(new MatchAllDocsQuery(), Occur.MUST);
		}
		if (query.apuType() != null) {
			root.add(new TermQuery(new Term("type", query.apuType())), Occur.FILTER);
		}
		for (FieldFilter filter : query.filters()) {
			if (filter instanceof FieldFilter.Text text) {
				root.add(textFieldQuery(text), Occur.FILTER);
			}
		}
		return root.build();
	}

	/** Mechanical translation of one planned clause (doc/search-relevance.md §4.7). */
	private Query clauseQuery(RelevancePlan.Clause clause, boolean boosted) throws IOException {
		Query query = switch (clause.kind()) {
			case TERM -> new TermQuery(new Term(clause.field(), clause.text()));
			case PREFIX -> new PrefixQuery(new Term(clause.field(), clause.text()));
			case PHRASE -> phraseQuery(clause.field(), clause.text());
			case ALL_TERMS -> termsQuery(clause.field(), clause.text(), Occur.MUST);
			case ANY_TERM -> termsQuery(clause.field(), clause.text(), Occur.SHOULD);
		};
		return boosted && clause.weight() != 1.0f ? new BoostQuery(query, clause.weight()) : query;
	}

	/** Consecutive analyzed tokens; the analyzer matches the indexed chain. */
	private Query phraseQuery(String field, String text) throws IOException {
		var builder = new PhraseQuery.Builder();
		for (String token : analyze(text)) {
			builder.add(new Term(field, token));
		}
		return builder.build();
	}

	private Query termsQuery(String field, String text, Occur occur) throws IOException {
		var bool = new BooleanQuery.Builder();
		for (String token : analyze(text)) {
			bool.add(new TermQuery(new Term(field, token)), occur);
		}
		return bool.build();
	}

	private Query textFieldQuery(FieldFilter.Text filter) throws IOException {
		return allWordsLastPrefixQuery(filter.field(), filter.text());
	}

	/**
	 * All analyzed words must match, the last one as a prefix (ES matchPhrasePrefix
	 * analog). Shared with {@link LuceneOldApiSearch} (the old API's FTXF filter).
	 */
	Query allWordsLastPrefixQuery(String field, String text) throws IOException {
		List<String> tokens = analyze(text);
		var bool = new BooleanQuery.Builder();
		for (int i = 0; i < tokens.size(); i++) {
			Query tokenQuery = i == tokens.size() - 1
					? new PrefixQuery(new Term(field, tokens.get(i)))
					: new TermQuery(new Term(field, tokens.get(i)));
			bool.add(tokenQuery, Occur.MUST);
		}
		return bool.build();
	}

	/**
	 * Adds the facet filters (Values, Range) except those on the excluded field
	 * (multi-select semantics); a Values filter is an OR over its values, a Range
	 * filter an interval intersection over the {@code ~L}/{@code ~H} bound fields
	 * (engine-shared logic).
	 */
	private static Query withFacetFilters(Query base, List<FieldFilter> filters, String excludedField) {
		var root = new BooleanQuery.Builder().add(base, Occur.MUST);
		boolean any = false;
		for (FieldFilter filter : filters) {
			if (filter instanceof FieldFilter.Values values && !values.field().equals(excludedField)) {
				var or = new BooleanQuery.Builder();
				values.values().forEach(v -> or.add(new TermQuery(new Term(values.field(), v)), Occur.SHOULD));
				or.setMinimumNumberShouldMatch(1);
				root.add(or.build(), Occur.FILTER);
				any = true;
			} else if (filter instanceof FieldFilter.Range range && !range.field().equals(excludedField)) {
				if (range.to() != null) {
					root.add(LongPoint.newRangeQuery(range.field() + "~L", Long.MIN_VALUE, toEpochMillis(range.to())),
							Occur.FILTER);
					any = true;
				}
				if (range.from() != null) {
					root.add(LongPoint.newRangeQuery(range.field() + "~H", toEpochMillis(range.from()),
							Long.MAX_VALUE), Occur.FILTER);
					any = true;
				}
			}
		}
		return any ? root.build() : base;
	}

	/** Runs the folding analysis chain; shared with {@link LuceneOldApiSearch}. */
	List<String> analyze(String text) throws IOException {
		var tokens = new ArrayList<String>();
		try (TokenStream stream = foldingAnalyzer.tokenStream("name", text)) {
			var term = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				tokens.add(term.toString());
			}
			stream.end();
		}
		return tokens;
	}

	private Document toLuceneDocument(ApuDocument apuDocument) {
		var doc = new Document();
		doc.add(new StringField("uuid", apuDocument.getUuid(), Field.Store.YES));
		// sortable uuid - the final tie-break of every sort mode
		doc.add(new SortedDocValuesField("uuid", new BytesRef(apuDocument.getUuid())));
		doc.add(new LongPoint("apuSourceId", apuDocument.getApuSourceId()));
		doc.add(new StringField("containsDigitalObjects", Boolean.toString(apuDocument.isContainsDigitalObjects()),
				Field.Store.YES));
		if (apuDocument.getName() != null) {
			doc.add(new TextField("name", apuDocument.getName(), Field.Store.YES));
		}
		if (apuDocument.getDescription() != null) {
			doc.add(new TextField("description", apuDocument.getDescription(), Field.Store.YES));
		}
		if (apuDocument.getType() != null) {
			doc.add(new StringField("type", apuDocument.getType(), Field.Store.YES));
		}
		if (apuDocument.getNameSort() != null) {
			// index-time Czech collation key computed by ApuDocumentBuilder
			doc.add(new SortedDocValuesField("nameSort", new BytesRef(apuDocument.getNameSort())));
		}
		if (apuDocument.getNameExactCs() != null) {
			doc.add(new StringField("nameExactCs", apuDocument.getNameExactCs(), Field.Store.NO));
		}
		if (apuDocument.getNameExact() != null) {
			doc.add(new StringField("nameExact", apuDocument.getNameExact(), Field.Store.NO));
		}
		// multi-valued: the analyzer's position gap keeps phrases inside one value
		for (String text : apuDocument.getAllText()) {
			doc.add(new TextField("allText", text, Field.Store.NO));
		}
		addDateBound(doc, "dateL", apuDocument.getDateL());
		addDateBound(doc, "dateH", apuDocument.getDateH());
		for (var entry : apuDocument.getValues().entrySet()) {
			for (Object value : entry.getValue()) {
				if (value instanceof String || value instanceof Number) {
					addValueField(doc, entry.getKey(), String.valueOf(value));
				}
				// UNITDATE range maps are represented by their ~L/~H bound entries
			}
		}
		return doc;
	}

	/**
	 * Field typing mirrors the ES mapping built from types.yaml: STRING item
	 * fields and APU_REF {@code ~LABEL} fields are analyzed text, UNITDATE
	 * {@code ~L}/{@code ~H} bounds are long points, everything else is an exact
	 * term (keyword semantics for Values filters and bucket counting).
	 */
	private void addValueField(Document doc, String field, String value) {
		if (field.endsWith("~L") || field.endsWith("~H")) {
			try {
				long millis = toEpochMillis(LocalDateTime.parse(value));
				doc.add(new LongPoint(field, millis));
				// doc-values enable per-document access: min/max metric aggregations
				// of the old API (and future sort/searchAfter slices)
				doc.add(new SortedNumericDocValuesField(field, millis));
				return;
			} catch (DateTimeParseException e) {
				// not a date bound - fall through to the exact term
			}
		}
		ItemType itemType = typesHolder.getItemTypeForCode(field);
		boolean analyzed = (itemType != null && itemType.getType() == DataType.STRING)
				|| (field.endsWith("~LABEL") && !field.endsWith("~ID~LABEL"));
		if (analyzed) {
			doc.add(new TextField(field, value, Field.Store.NO));
		} else {
			doc.add(new StringField(field, value, Field.Store.NO));
		}
	}

	/** Derived global dating bound: range-filterable point + sortable doc-values. */
	private static void addDateBound(Document doc, String field, String bound) {
		if (bound == null) {
			return;
		}
		try {
			long millis = toEpochMillis(LocalDateTime.parse(bound));
			doc.add(new LongPoint(field, millis));
			doc.add(new SortedNumericDocValuesField(field, millis));
		} catch (DateTimeParseException e) {
			// unparseable bound - the document simply has no derived dating
		}
	}

	private static long toEpochMillis(LocalDateTime dateTime) {
		return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	private void commitAndRefresh() {
		try {
			apuWriter.commit();
			apuSearchers.maybeRefreshBlocking();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Searcher pool of the apu index; shared with {@link LuceneOldApiSearch}. */
	SearcherManager apuSearchers() {
		return apuSearchers;
	}

}
