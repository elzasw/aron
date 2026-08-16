package cz.aron.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.search.ApuDocument;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.RelationDocument;
import cz.aron.search.SearchIndex;
import jakarta.annotation.PreDestroy;

/**
 * Embedded Lucene adapter of the search port (doc/search-port.md): a supported
 * engine for small/ES-less deployments, and the engine of tests and dev mode.
 * Elasticsearch is Lucene inside, so analysis behavior (tokenization, lowercase,
 * ASCII folding) matches the production ES adapter by construction rather than
 * by imitation. Serves the new API only - the frozen old-API read path talks to
 * Elasticsearch directly.
 * <p>
 * Storage: {@code search.lucene.path} set = persisted index under that directory
 * ({@code apu/}, {@code rels/} subdirectories); unset = in-memory (tests, dev
 * mode - rebuilt on startup by {@link cz.aron.search.SearchIndexManager}).
 * <p>
 * Layout notes: Lucene documents are schemaless, so the dynamic types.yaml
 * values need no pre-declared mapping - every value is indexed as an exact
 * (keyword-like) term, which is what the port's value filters require. UNITDATE
 * range maps and relation payloads are accepted but not yet indexed; they gain a
 * typed representation with the Phase 7 slices that read them. The indexed-fields
 * CRC lives in the Lucene commit user data (the analog of the ES index
 * {@code _meta}).
 */
@ConditionalOnProperty(name = "search.engine", havingValue = "lucene")
@Component
public class LuceneSearchIndex implements SearchIndex {

	private static final String FIELDS_CRC_KEY = "fieldsCrc";

	private final Analyzer foldingAnalyzer = new FoldingAnalyzer();

	private final Directory apuDirectory;

	private final Directory relsDirectory;

	private final IndexWriter apuWriter;

	private final IndexWriter relsWriter;

	private final SearcherManager apuSearchers;

	/** Mirrors the ES analysis chain used for name/description: standard tokenizer + lowercase + ASCII folding. */
	private static final class FoldingAnalyzer extends Analyzer {
		@Override
		protected TokenStreamComponents createComponents(String fieldName) {
			var tokenizer = new StandardTokenizer();
			TokenStream stream = new LowerCaseFilter(tokenizer);
			stream = new ASCIIFoldingFilter(stream);
			return new TokenStreamComponents(tokenizer, stream);
		}
	}

	public LuceneSearchIndex(@Value("${search.lucene.path:}") String path) {
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
			String crc = reader.getIndexCommit().getUserData().get(FIELDS_CRC_KEY);
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
		apuWriter.setLiveCommitData(Set.of(Map.entry(FIELDS_CRC_KEY, Long.toString(crc))));
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
				Query luceneQuery = toLuceneQuery(query);
				long total = searcher.count(luceneQuery);
				var hits = new ArrayList<ApuSearchResult.Hit>();
				if (total > 0 && query.size() > 0) {
					int wanted = (query.page() + 1) * query.size();
					var top = searcher.search(luceneQuery, wanted);
					var storedFields = searcher.storedFields();
					for (int i = query.page() * query.size(); i < top.scoreDocs.length; i++) {
						var doc = storedFields.document(top.scoreDocs[i].doc);
						hits.add(new ApuSearchResult.Hit(doc.get("uuid"), doc.get("name"), doc.get("type")));
					}
				}
				return new ApuSearchResult(total, hits);
			} finally {
				apuSearchers.release(searcher);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Query toLuceneQuery(ApuSearchQuery query) throws IOException {
		var root = new BooleanQuery.Builder();
		if (query.fulltext() != null) {
			// OR semantics across tokens and fields (ES multi_match default)
			var text = new BooleanQuery.Builder();
			for (String token : analyze(query.fulltext())) {
				text.add(new TermQuery(new Term("name", token)), Occur.SHOULD);
				text.add(new TermQuery(new Term("description", token)), Occur.SHOULD);
			}
			root.add(text.build(), Occur.MUST);
		} else {
			root.add(new MatchAllDocsQuery(), Occur.MUST);
		}
		query.valueFilters()
				.forEach((field, value) -> root.add(new TermQuery(new Term(field, value)), Occur.FILTER));
		return root.build();
	}

	private List<String> analyze(String text) throws IOException {
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

	private static Document toLuceneDocument(ApuDocument apuDocument) {
		var doc = new Document();
		doc.add(new StringField("uuid", apuDocument.getUuid(), Field.Store.YES));
		doc.add(new LongPoint("apuSourceId", apuDocument.getApuSourceId()));
		if (apuDocument.getName() != null) {
			doc.add(new TextField("name", apuDocument.getName(), Field.Store.YES));
		}
		if (apuDocument.getDescription() != null) {
			doc.add(new TextField("description", apuDocument.getDescription(), Field.Store.NO));
		}
		if (apuDocument.getType() != null) {
			doc.add(new StringField("type", apuDocument.getType(), Field.Store.YES));
		}
		if (apuDocument.getNameSort() != null) {
			// index-time Czech collation key computed by ApuDocumentBuilder
			doc.add(new SortedDocValuesField("nameSort", new BytesRef(apuDocument.getNameSort())));
		}
		for (var entry : apuDocument.getValues().entrySet()) {
			for (Object value : entry.getValue()) {
				// UNITDATE range maps get a typed representation with a later slice
				if (value instanceof String || value instanceof Number) {
					doc.add(new StringField(entry.getKey(), String.valueOf(value), Field.Store.NO));
				}
			}
		}
		return doc;
	}

	private void commitAndRefresh() {
		try {
			apuWriter.commit();
			apuSearchers.maybeRefreshBlocking();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
