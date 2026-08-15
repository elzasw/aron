package cz.aron.search.memory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.aron.search.ApuDocument;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.RelationDocument;
import cz.aron.search.SearchIndex;

/**
 * Minimal in-memory adapter of the search port for tests and dev mode
 * (doc/search-port.md §3.3). It mirrors only the essence of the production
 * analysis: lowercase + diacritics folding + tokenization, exact matching on
 * keyword values, and OR semantics across fulltext tokens (Elasticsearch
 * multi_match default).
 * <p>
 * Deliberately NOT emulated: stop words, stemming, phrase/prefix semantics,
 * scoring/ranking, ICU collation. Behavior depending on those is tested only
 * against real Elasticsearch (the es-it profile).
 */
@ConditionalOnProperty(name = "search.engine", havingValue = "memory")
@Component
public class InMemorySearchIndex implements SearchIndex {

	private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}\\p{Nd}]+");

	private final Map<String, ApuDocument> apus = new ConcurrentHashMap<>();

	private final List<RelationDocument> relations = new CopyOnWriteArrayList<>();

	private volatile Long fieldsCrc;

	@Override
	public void createSchema() {
		// nothing to create - documents live in the maps
	}

	@Override
	public void dropSchema() {
		apus.clear();
		relations.clear();
		fieldsCrc = null;
	}

	@Override
	public Long storedFieldsCrc() {
		return fieldsCrc;
	}

	@Override
	public void storeFieldsCrc(long crc) {
		this.fieldsCrc = crc;
	}

	@Override
	public void indexApus(Collection<ApuDocument> documents) {
		for (var document : documents) {
			apus.put(document.getUuid(), document);
		}
	}

	@Override
	public void indexRelations(Collection<RelationDocument> rels) {
		relations.addAll(rels);
	}

	@Override
	public void deleteApusBySource(long apuSourceId) {
		apus.values().removeIf(d -> d.getApuSourceId() == apuSourceId);
	}

	@Override
	public ApuSearchResult search(ApuSearchQuery query) {
		var matching = apus.values().stream()
				.filter(d -> matchesFulltext(d, query.fulltext()))
				.filter(d -> matchesValueFilters(d, query.valueFilters()))
				// stable order so paging is deterministic (order is engine-specific by contract)
				.sorted(Comparator.comparing(ApuDocument::getUuid))
				.toList();
		var hits = matching.stream()
				.skip((long) query.page() * query.size())
				.limit(query.size())
				.map(d -> new ApuSearchResult.Hit(d.getUuid(), d.getName(), d.getType()))
				.toList();
		return new ApuSearchResult(matching.size(), hits);
	}

	private static boolean matchesFulltext(ApuDocument document, String fulltext) {
		if (fulltext == null) {
			return true;
		}
		Set<String> documentTokens = tokenize(document.getName());
		documentTokens.addAll(tokenize(document.getDescription()));
		// OR semantics across query tokens (multi_match default)
		return tokenize(fulltext).stream().anyMatch(documentTokens::contains);
	}

	private static boolean matchesValueFilters(ApuDocument document, Map<String, String> valueFilters) {
		for (var filter : valueFilters.entrySet()) {
			List<Object> values = document.getValues().get(filter.getKey());
			if (values == null || values.stream().noneMatch(v -> String.valueOf(v).equals(filter.getValue()))) {
				return false;
			}
		}
		return true;
	}

	/** Lowercase + diacritics folding + split on non-alphanumeric - the essence of the folding analyzer. */
	private static Set<String> tokenize(String text) {
		if (text == null) {
			return new java.util.HashSet<>();
		}
		String folded = Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
		return TOKEN_SPLIT.splitAsStream(folded)
				.filter(t -> !t.isEmpty())
				.collect(Collectors.toCollection(java.util.HashSet::new));
	}

	/** Test/dev visibility: number of indexed relations (the rels index is write-only in the app). */
	public int relationCount() {
		return relations.size();
	}

	/** Test/dev visibility: all indexed APU documents. */
	public Collection<ApuDocument> allApus() {
		return new ArrayList<>(apus.values());
	}

}
