package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.aron.search.ApuSearchQuery.SortMode;
import cz.aron.search.relevance.RelevanceConfig;
import cz.aron.search.relevance.RelevanceQueryPlanner;

/**
 * Contract of the search port that EVERY adapter must fulfil (doc/search-port.md
 * §3.5): the embedded Lucene adapter runs it in the default suite
 * (LuceneSearchIndexTest), the Elasticsearch adapter under the opt-in es-it
 * profile (*IT). Only behavior both engines share belongs here — engine
 * specifics (stemming, ranking, stop words, word-order semantics of text
 * filters) get engine-only tests.
 */
public abstract class SearchIndexContractTest {

	/** The test deployment's search locale (fixtures come from {@link DocumentFixtures}). */
	protected static final ContentLocale CONTENT_LOCALE = DocumentFixtures.CONTENT_LOCALE;

	protected SearchIndex index;

	/** Fresh, empty index per test. */
	protected abstract SearchIndex createIndex();

	/**
	 * Called after every mutating operation. Engines with near-real-time semantics
	 * (Elasticsearch) override this with an index refresh so subsequent
	 * searches/deletes see the writes; the contract itself makes no visibility
	 * promises between an unrefreshed write and a read.
	 */
	protected void refreshAfterWrite() {
	}

	@BeforeEach
	void setUpIndex() {
		index = createIndex();
		index.createSchema();
	}

	protected void indexApus(List<ApuDocument> documents) {
		index.indexApus(documents);
		refreshAfterWrite();
	}

	protected void indexRelations(List<RelationDocument> relations) {
		index.indexRelations(relations);
		refreshAfterWrite();
	}

	protected void deleteApus(String... uuids) {
		index.deleteApus(List.of(uuids));
		refreshAfterWrite();
	}

	protected static ApuDocument doc(String uuid, String name, long sourceId, Map<String, List<Object>> values) {
		return doc(uuid, name, "ARCH_DESC", sourceId, values);
	}

	protected static ApuDocument doc(String uuid, String name, String type, long sourceId,
			Map<String, List<Object>> values) {
		return DocumentFixtures.apu(uuid, name, type, sourceId, values);
	}

	/** Fixture with explicit allText entries (description-like searchable values). */
	protected static ApuDocument docWithAllText(String uuid, String name, String... allTextEntries) {
		var document = doc(uuid, name, 1, Map.of());
		document.getAllText().addAll(List.of(allTextEntries));
		return document;
	}

	/** Fixture with variant name forms - what the builder computes for {@code nameVariant} items. */
	protected static ApuDocument docWithNameVariants(String uuid, String name, String... variants) {
		var document = doc(uuid, name, 1, Map.of());
		DocumentFixtures.addNameVariants(document, variants);
		return document;
	}

	/**
	 * A test's fixture ids. A number belongs to one test, or to one shared corpus
	 * (the datings use 100-102) - never to two tests by accident. The index is
	 * fresh per test, so reuse costs nothing today and is invisible; it is what
	 * stops two tests from ever sharing an index, which on Elasticsearch is a
	 * schema drop and create apiece.
	 */
	private static String uuid(int n) {
		return UUID.nameUUIDFromBytes(("contract-" + n).getBytes()).toString();
	}

	@Test
	void schemaCrcLivesAndDiesWithTheSchema() {
		assertThat(index.storedSchemaCrc()).isNull();
		index.storeSchemaCrc(123L);
		assertThat(index.storedSchemaCrc()).isEqualTo(123L);
		index.dropSchema();
		assertThat(index.storedSchemaCrc()).isNull();
	}

	@Test
	void fulltextSearchFoldsDiacritics() {
		indexApus(List.of(doc(uuid(1), "Václav Novák", 1, Map.of())));

		var byFolded = index.search(ApuSearchQuery.fulltext("vaclav"));
		assertThat(byFolded.total()).isEqualTo(1);
		assertThat(byFolded.hits()).hasSize(1);
		assertThat(byFolded.hits().get(0).uuid()).isEqualTo(uuid(1));
		assertThat(byFolded.hits().get(0).name()).isEqualTo("Václav Novák");

		assertThat(index.search(ApuSearchQuery.fulltext("novák")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("neexistuje")).total()).isZero();
	}

	// --- behavior specification (doc/search-relevance.md §5) -----------------

	@Test
	void everyQueryWordMustMatchRegardlessOfOrder() {
		// B1: AND across words; word order never decides matching
		indexApus(List.of(
				doc(uuid(60), "Václav Novák", 1, Map.of()),
				doc(uuid(61), "Václav Dvořák", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("vaclav novak")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(60));
		assertThat(index.search(ApuSearchQuery.fulltext("novak vaclav")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(60));
		assertThat(index.search(ApuSearchQuery.fulltext("vaclav")).total()).isEqualTo(2);
	}

	@Test
	void wordsMatchAcrossDifferentValues() {
		// B1: a word may match in ANY searchable value (cross-field AND)
		indexApus(List.of(docWithAllText(uuid(62), "Kronika obce", "1850")));

		assertThat(index.search(ApuSearchQuery.fulltext("kronika 1850")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("kronika 1999")).total()).isZero();
	}

	@Test
	void matchingIgnoresCaseAndDiacritics() {
		// B2 + B3 (recall half)
		indexApus(List.of(doc(uuid(63), "Řehoř Mrázek", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("REHOR")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("mrázek")).total()).isEqualTo(1);
	}

	@Test
	void typedDiacriticsRankTheExactNameFirst() {
		// B3 (ranking half): the diacritics-preserving exact tier outranks the folded one
		indexApus(List.of(
				doc(uuid(64), "Řehoř", 1, Map.of()),
				doc(uuid(65), "Rehor", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("Řehoř")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(64), uuid(65));
		assertThat(index.search(ApuSearchQuery.fulltext("Rehor")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(65), uuid(64));
	}

	@Test
	void quotedPhrasesMatchExactlyAndNeverAcrossValues() {
		// B4: phrase order matters; values are position-gapped
		indexApus(List.of(docWithAllText(uuid(66), "Zápis", "Kronika města Přerova", "kostel svatého Jana")));

		assertThat(index.search(ApuSearchQuery.fulltext("\"kronika města\"")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("\"města kronika\"")).total()).isZero();
		// the phrase must not bridge two different values
		assertThat(index.search(ApuSearchQuery.fulltext("\"Přerova kostel\"")).total()).isZero();
	}

	@Test
	void stopWordOnlyQueriesStillSearch() {
		// B5: the canonical analyzer drops "v"; the fallback chain keeps it
		indexApus(List.of(doc(uuid(67), "Kostel v Praze", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("v")).total()).isEqualTo(1);
		// and stop words never cause empty results for mixed queries
		assertThat(index.search(ApuSearchQuery.fulltext("kostel v praze")).total()).isEqualTo(1);
	}

	@Test
	void partialWordsMatchAutomatically() {
		// B6 (R-15): every long-enough fragment matches ANYWHERE inside a word;
		// a star has no meaning (the former operator is gone)
		indexApus(List.of(doc(uuid(68), "Kronika obce", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("kron")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("kron*")).total()).isEqualTo(1);
		// mid-word fragments match too ("ardub" finds Pardubice)
		assertThat(index.search(ApuSearchQuery.fulltext("ronika")).total()).isEqualTo(1);
		// partial words combine like whole ones: AND, any order
		assertThat(index.search(ApuSearchQuery.fulltext("obc kron")).total()).isEqualTo(1);
		assertThat(index.search(ApuSearchQuery.fulltext("nika bce")).total()).isEqualTo(1);
		// short fragments must match a whole word - "ob" matches nothing here
		assertThat(index.search(ApuSearchQuery.fulltext("ob kron")).total()).isZero();
		// fragments never match across word boundaries
		assertThat(index.search(ApuSearchQuery.fulltext("kaobce")).total()).isZero();
	}

	@Test
	void wordStartMatchesOutrankMidWordMatches() {
		// B6 (R-15 ranking): exact > word start > contains
		indexApus(List.of(
				doc(uuid(97), "Nekronika", 1, Map.of()),
				doc(uuid(98), "Kronika obce", 1, Map.of()),
				doc(uuid(99), "Kron", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("kron")).hits())
				.extracting(ApuSearchResult.Hit::uuid)
				.containsExactly(uuid(99), uuid(98), uuid(97));
	}

	@Test
	void partialWordsRankNameBearersFirstAndExactAbovePartial() {
		// R-14 ("univ bratisl"): both word orders are found; records carrying
		// the words in the NAME precede content mentions; and an exact name
		// still outranks a partial match of the same words
		indexApus(List.of(
				doc(uuid(93), "Univerzita Bratislava", 1, Map.of()),
				doc(uuid(94), "Bratislavská univerzita", 1, Map.of()),
				docWithAllText(uuid(95), "Sbírka spisů", "korespondence s Univerzitou Bratislavskou"),
				doc(uuid(96), "Univ Bratisl", 1, Map.of())));

		var hits = index.search(ApuSearchQuery.fulltext("univ bratisl")).hits();
		assertThat(hits).extracting(ApuSearchResult.Hit::uuid)
				.containsExactlyInAnyOrder(uuid(93), uuid(94), uuid(95), uuid(96));
		// the exactly matching name wins over partial name matches...
		assertThat(hits.get(0).uuid()).isEqualTo(uuid(96));
		// ...and the content-only mention comes last
		assertThat(hits.get(3).uuid()).isEqualTo(uuid(95));
	}

	@Test
	void nameMatchesOutrankDeepMatches() {
		// B8 (ordering only): a hit in the name precedes an allText-only hit
		indexApus(List.of(
				docWithAllText(uuid(70), "Zápisy města", "kronika zmíněná v obsahu"),
				doc(uuid(71), "Kronika obce", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("kronika")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(71), uuid(70));
	}

	@Test
	void variantNamesRankAboveContentBelowThePrimaryName() {
		// B14: an entity is findable by its variant forms; a variant match beats
		// a record merely containing the words, and loses to a primary-name match
		indexApus(List.of(
				docWithNameVariants(uuid(75), "Česko", "Czech Republic", "Czechia", "Česká republika"),
				docWithAllText(uuid(76), "Studie", "text zmiňující Czech Republic v obsahu"),
				doc(uuid(77), "Czechia", 1, Map.of())));

		// exact variant beats the content mention
		assertThat(index.search(ApuSearchQuery.fulltext("Czech Republic")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(75), uuid(76));
		// the primary name beats the same value as a variant
		assertThat(index.search(ApuSearchQuery.fulltext("Czechia")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(77), uuid(75));
	}

	@Test
	void variantNamesFollowTheDiacriticsRankingRule() {
		// B14 + B3: typed diacritics prefer the exact variant form
		indexApus(List.of(
				docWithNameVariants(uuid(78), "Entita A", "Řehoř"),
				docWithNameVariants(uuid(79), "Entita B", "Rehor")));

		assertThat(index.search(ApuSearchQuery.fulltext("Řehoř")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(78), uuid(79));
		assertThat(index.search(ApuSearchQuery.fulltext("Rehor")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(79), uuid(78));
	}

	@Test
	void pastedTextMatchesWholeWordsOnly() {
		// B6/R-16: above six words the query is pasted text - a fragment no
		// longer matches inside a word, and complete words keep matching
		indexApus(List.of(doc(uuid(133), "Pardubice kostel kronika mesto zamek hrad vez", 1, Map.of())));

		// six words: "pardub" still matches inside "Pardubice"
		assertThat(index.search(ApuSearchQuery.fulltext("pardub kostel kronika mesto zamek hrad"))
				.total()).isEqualTo(1);
		// seven words: the fragment must match a whole word - it does not
		assertThat(index.search(ApuSearchQuery.fulltext("pardub kostel kronika mesto zamek hrad vez"))
				.total()).isZero();
		// ...while the same seven complete words do
		assertThat(index.search(ApuSearchQuery.fulltext("pardubice kostel kronika mesto zamek hrad vez"))
				.total()).isEqualTo(1);
	}

	@Test
	void inflectedFormsMatchAndTheNameBearerRanksFirst() {
		// B15 (R-17): with a Czech content locale, a word matches its inflected
		// forms in both directions - "hradu" finds "hrad" (which contains none
		// of the query's trigrams) as well as "hrady"; the name bearer first
		indexApus(List.of(
				doc(uuid(134), "Hrad Pernštejn", 1, Map.of()),
				docWithAllText(uuid(135), "Listina", "prodej a hrady roku 1588")));

		assertThat(index.search(ApuSearchQuery.fulltext("hradu")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(134), uuid(135));
	}

	@Test
	void exactFormRanksAboveInflectedMatch() {
		// B15 half two: stemming adds recall, never reorders - the record
		// carrying the query's exact form outranks the stem-equal one
		indexApus(List.of(
				doc(uuid(136), "Hradu kronika", 1, Map.of()),
				doc(uuid(137), "Hrad kronika", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("hradu kronika")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(136), uuid(137));
	}

	@Test
	void referenceLabelMatchesRankAboveContentMentions() {
		// B8: a query matching a referenced record's name ranks above one merely
		// mentioning the words - the combined refLabels field, one however many
		// reference item types the display model declares (R-16)
		var referring = doc(uuid(130), "Listina o prodeji", 1, Map.of());
		DocumentFixtures.addRefLabel(referring, "REL~ENTITY", uuid(131), "Karel Novák");
		indexApus(List.of(referring,
				docWithAllText(uuid(132), "Zápisy města", "sepsal jistý Karel Novák")));

		assertThat(index.search(ApuSearchQuery.fulltext("Karel Novák")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(130), uuid(132));
	}

	@Test
	void relaxedPlanMatchesAnyWord() {
		// B7 (the port half; the automatic retry itself lives in the API layer)
		indexApus(List.of(doc(uuid(72), "Kronika obce", 1, Map.of())));

		var strict = ApuSearchQuery.fulltext("kronika neexistujici");
		assertThat(index.search(strict).total()).isZero();
		var relaxed = new ApuSearchQuery(null, strict.fulltext().relaxed(), List.of(), List.of(), List.of(),
				0, 10, SortMode.RELEVANCE);
		assertThat(index.search(relaxed).total()).isEqualTo(1);
	}

	@Test
	void indexingSameUuidReplacesTheDocument() {
		indexApus(List.of(doc(uuid(2), "Original", 1, Map.of())));
		indexApus(List.of(doc(uuid(2), "Replaced", 1, Map.of())));

		var all = index.search(ApuSearchQuery.matchAll(0, 10));
		assertThat(all.total()).isEqualTo(1);
		assertThat(all.hits().get(0).name()).isEqualTo("Replaced");
	}

	@Test
	void valuesFilterMatchesAnyOfTheValuesExactly() {
		indexApus(List.of(
				doc(uuid(3), "Czech record", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(4), "German record", 1, Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(5), "Latin record", 1, Map.of("LANG~CODE", List.of("lat")))));

		var czech = index.search(query(null, List.of(new FieldFilter.Values("LANG~CODE", List.of("cze")))));
		assertThat(czech.total()).isEqualTo(1);
		assertThat(czech.hits().get(0).uuid()).isEqualTo(uuid(3));

		// OR across the selected values
		var czechOrGerman = index
				.search(query(null, List.of(new FieldFilter.Values("LANG~CODE", List.of("cze", "ger")))));
		assertThat(czechOrGerman.total()).isEqualTo(2);

		// exact keyword semantics - no prefix matching
		assertThat(index.search(query(null, List.of(new FieldFilter.Values("LANG~CODE", List.of("cz")))))
				.total()).isZero();
	}

	@Test
	void apuTypeRestrictsTheSearch() {
		indexApus(List.of(
				doc(uuid(6), "Fond A", "FUND", 1, Map.of()),
				doc(uuid(7), "Popis B", "ARCH_DESC", 1, Map.of())));

		var funds = index.search(
				new ApuSearchQuery("FUND", null, List.of(), List.of(), List.of(), 0, 10, SortMode.RELEVANCE));
		assertThat(funds.total()).isEqualTo(1);
		assertThat(funds.hits().get(0).uuid()).isEqualTo(uuid(6));
	}

	@Test
	void textFilterMatchesAnalyzedFieldWithFolding() {
		indexApus(List.of(
				doc(uuid(8), "Zaznam A", 1, Map.of("TITLE~MAIN", List.of("Stavební dokumentace Přerov"))),
				doc(uuid(9), "Zaznam B", 1, Map.of("TITLE~MAIN", List.of("Kronika obce")))));

		var byFolded = index.search(query(null, List.of(new FieldFilter.Text("TITLE~MAIN", "stavebni"))));
		assertThat(byFolded.total()).isEqualTo(1);
		assertThat(byFolded.hits().get(0).uuid()).isEqualTo(uuid(8));

		assertThat(index.search(query(null, List.of(new FieldFilter.Text("TITLE~MAIN", "neexistuje")))).total())
				.isZero();
	}

	/**
	 * The corpus every dating rule is told apart on: two records dated far apart,
	 * one undated, and a language on each so another facet's filter has something
	 * to act with. One corpus rather than five near-copies - the rules differ in
	 * what they ask of it, not in the data, and on Elasticsearch each fixture
	 * costs a schema of its own.
	 */
	private void indexDatingCorpus() {
		indexApus(List.of(
				doc(uuid(100), "Kniha 1800-1850", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"))),
				doc(uuid(101), "Kniha 1900-1910", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59"))),
				doc(uuid(102), "Kniha bez datace", 1, Map.of("LANG~CODE", List.of("ger")))));
	}

	@Test
	void rangeFilterMatchesIntersectingIntervals() {
		indexDatingCorpus();

		// [1840, 1899] intersects only the first interval
		var range = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE",
				LocalDateTime.parse("1840-01-01T00:00:00"), LocalDateTime.parse("1899-12-31T23:59:59")))));
		assertThat(range.total()).isEqualTo(1);
		assertThat(range.hits().get(0).uuid()).isEqualTo(uuid(100));

		// open lower bound - the undated record is still not in a dated range
		var upTo1905 = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE", null,
				LocalDateTime.parse("1905-01-01T00:00:00")))));
		assertThat(upTo1905.total()).isEqualTo(2);

		// disjoint interval
		assertThat(index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE",
				LocalDateTime.parse("1860-01-01T00:00:00"), LocalDateTime.parse("1890-01-01T00:00:00")))))
				.total()).isZero();
	}

	@Test
	void bucketsFollowMultiSelectSemantics() {
		indexApus(List.of(
				doc(uuid(12), "Zaznam 1", 1, Map.of("LANG~CODE", List.of("cze"), "REL~ENTITY", List.of("apu-a"))),
				doc(uuid(13), "Zaznam 2", 1, Map.of("LANG~CODE", List.of("cze"), "REL~ENTITY", List.of("apu-b"))),
				doc(uuid(14), "Zaznam 3", 1, Map.of("LANG~CODE", List.of("ger"), "REL~ENTITY", List.of("apu-a")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Values("LANG~CODE", List.of("cze"))),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10),
						ApuSearchQuery.BucketRequest.of("REL~ENTITY", 10)),
				List.of(), 0, 10, SortMode.RELEVANCE));

		// hits and total respect the filter
		assertThat(result.total()).isEqualTo(2);
		// the filtered facet's own buckets ignore its filter (the user can widen the selection)
		assertThat(result.buckets().get("LANG~CODE")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("cze", 2), new ApuSearchResult.Bucket("ger", 1));
		// other facets' buckets respect it
		assertThat(result.buckets().get("REL~ENTITY")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("apu-a", 1), new ApuSearchResult.Bucket("apu-b", 1));
	}

	@Test
	void bucketsAreOrderedByCountThenValueAndCappedBySize() {
		indexApus(List.of(
				doc(uuid(30), "Z1", 1, Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(31), "Z2", 1, Map.of("LANG~CODE", List.of("ger"))),
				doc(uuid(32), "Z3", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(33), "Z4", 1, Map.of("LANG~CODE", List.of("cze"))),
				doc(uuid(34), "Z5", 1, Map.of("LANG~CODE", List.of("lat")))));

		var all = index.search(new ApuSearchQuery(null, null, List.of(),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10)), List.of(), 0, 0, SortMode.RELEVANCE));
		assertThat(all.buckets().get("LANG~CODE")).containsExactly(
				new ApuSearchResult.Bucket("cze", 2), new ApuSearchResult.Bucket("ger", 2),
				new ApuSearchResult.Bucket("lat", 1));

		var capped = index.search(new ApuSearchQuery(null, null, List.of(),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 2)), List.of(), 0, 0, SortMode.RELEVANCE));
		assertThat(capped.buckets().get("LANG~CODE")).containsExactly(
				new ApuSearchResult.Bucket("cze", 2), new ApuSearchResult.Bucket("ger", 2));
	}

	@Test
	void refBucketsPairTheCompositeFieldWithTheFilterField() {
		// reference facets enumerate <code>~ID~LABEL while their Values filter
		// sits on <code> - the pairing drives the multi-select exclusion
		indexApus(List.of(
				doc(uuid(35), "R1", 1, Map.of(
						"REL~ENTITY", List.of("apu-a"), "REL~ENTITY~ID~LABEL", List.of("apu-a|Novak"))),
				doc(uuid(36), "R2", 1, Map.of(
						"REL~ENTITY", List.of("apu-b"), "REL~ENTITY~ID~LABEL", List.of("apu-b|Dvorak")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Values("REL~ENTITY", List.of("apu-a"))),
				List.of(new ApuSearchQuery.BucketRequest("REL~ENTITY~ID~LABEL", "REL~ENTITY", 10)),
				List.of(), 0, 10, SortMode.RELEVANCE));

		// hits respect the filter, the facet's own buckets ignore it
		assertThat(result.total()).isEqualTo(1);
		assertThat(result.buckets().get("REL~ENTITY~ID~LABEL")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("apu-a|Novak", 1), new ApuSearchResult.Bucket("apu-b|Dvorak", 1));
	}

	@Test
	void rangeFiltersOfOtherFacetsApplyToBuckets() {
		indexApus(List.of(
				doc(uuid(37), "Stara", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"))),
				doc(uuid(38), "Nova", 1, Map.of(
						"LANG~CODE", List.of("cze"),
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Range("UNIT~DATE", LocalDateTime.parse("1890-01-01T00:00:00"), null)),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10)), List.of(), 0, 10, SortMode.RELEVANCE));

		// another facet's RANGE filter restricts both hits and buckets
		assertThat(result.total()).isEqualTo(1);
		assertThat(result.buckets().get("LANG~CODE"))
				.containsExactly(new ApuSearchResult.Bucket("cze", 1));
	}

	@Test
	void datingBoundsFollowMultiSelectSemanticsAndCountTheUndated() {
		indexDatingCorpus();

		// the facet's own RANGE filter is excluded from its bounds (the slider can
		// widen), and so is it from the undated count - neither may move as the
		// reader drags the slider
		var withOwnRange = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Range("UNIT~DATE", LocalDateTime.parse("1890-01-01T00:00:00"), null)),
				List.of(), List.of(ApuSearchQuery.BoundsRequest.of("UNIT~DATE")), 0, 0, SortMode.RELEVANCE));
		var bounds = withOwnRange.bounds().get("UNIT~DATE");
		assertThat(bounds).isNotNull();
		assertThat(atUtcYear(bounds.minMillis())).isEqualTo(1800);
		assertThat(atUtcYear(bounds.maxMillis())).isEqualTo(1910);
		assertThat(bounds.undatedCount()).isEqualTo(1);

		// unfiltered says the same, which is what "excluded" means
		var unfiltered = index.search(new ApuSearchQuery(null, null, List.of(), List.of(),
				List.of(ApuSearchQuery.BoundsRequest.of("UNIT~DATE")), 0, 0, SortMode.RELEVANCE))
						.bounds().get("UNIT~DATE");
		assertThat(atUtcYear(unfiltered.minMillis())).isEqualTo(1800);
		assertThat(unfiltered.undatedCount()).isEqualTo(1);

		// other facets' filters do apply: the only German record is the undated
		// one, so nothing matching carries the dating and there are no bounds
		var withOtherFilter = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Values("LANG~CODE", List.of("ger"))),
				List.of(), List.of(ApuSearchQuery.BoundsRequest.of("UNIT~DATE")), 0, 0, SortMode.RELEVANCE));
		assertThat(withOtherFilter.bounds()).doesNotContainKey("UNIT~DATE");
	}

	@Test
	void rangeFilterCanIncludeTheUndatedDocuments() {
		indexDatingCorpus();

		var from = LocalDateTime.parse("1805-01-01T00:00:00");
		var to = LocalDateTime.parse("1852-12-31T23:59:59");

		// strict by default: an undated record is not in 1805-1852
		var strict = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE", from, to))));
		assertThat(strict.hits()).extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(100));

		// including the undated adds exactly those, not the ones out of range
		var lenient = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE", from, to, true))));
		assertThat(lenient.hits()).extracting(ApuSearchResult.Hit::uuid)
				.containsExactlyInAnyOrder(uuid(100), uuid(102));
	}

	@Test
	void aShortNameIsFoundByItself() {
		// a fragment shorter than relevance.partialMinLength matches a whole word
		// only, which is what lets a two-letter place name be searched for at all -
		// Aš is a town, and there is nothing longer to type
		indexApus(List.of(
				doc(uuid(54), "Aš", 1, Map.of()),
				doc(uuid(55), "Ašmakov", 1, Map.of())));

		assertThat(index.search(ApuSearchQuery.fulltext("Aš")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(54));
		// and does not leak into every longer word containing it
		assertThat(index.search(ApuSearchQuery.fulltext("aš")).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(54));
	}

	@Test
	void aRangeMatchesTheDatingsThemselvesNotTheirHull() {
		// two datings of one item type, far apart. Written as intervals because
		// that is the only way to say "two datings" - bounds can only say one
		indexApus(List.of(
				doc(uuid(52), "Kniha 1800-1810 a 1900-1910", 1, Map.of("UNIT~DATE", List.of(
						Map.of("gte", "1800-01-01T00:00:00", "lte", "1810-12-31T23:59:59"),
						Map.of("gte", "1900-01-01T00:00:00", "lte", "1910-12-31T23:59:59"))))));

		// the gap between them is not a period this record was ever assigned;
		// matching its 1800-1910 hull would invent one
		assertThat(index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE",
				LocalDateTime.parse("1850-01-01T00:00:00"), LocalDateTime.parse("1860-12-31T23:59:59")))))
				.total()).isZero();

		// either dating itself matches
		for (String year : List.of("1805", "1905")) {
			var hit = index.search(query(null, List.of(new FieldFilter.Range("UNIT~DATE",
					LocalDateTime.parse(year + "-01-01T00:00:00"),
					LocalDateTime.parse(year + "-12-31T23:59:59")))));
			assertThat(hit.hits()).as("dating covering %s", year)
					.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(52));
		}

		// and the bounds a slider offers are still the hull - the span the
		// matching records cover is what its two ends mean
		var bounds = index.search(new ApuSearchQuery(null, null, List.of(), List.of(),
				List.of(ApuSearchQuery.BoundsRequest.of("UNIT~DATE")), 0, 0, SortMode.RELEVANCE))
						.bounds().get("UNIT~DATE");
		assertThat(atUtcYear(bounds.minMillis())).isEqualTo(1800);
		assertThat(atUtcYear(bounds.maxMillis())).isEqualTo(1910);
	}

	@Test
	void aRangeSpanningItemTypesKeepsThemApart() {
		// the built-in dating facet's case: one record dated 1850-1860 as a unit and
		// 1600 for the original it copies. The record's own hull spans 1600-1860, so
		// filtering on that would put this record in 1700; each item type's datings
		// are matched on their own instead
		indexApus(List.of(
				doc(uuid(53), "Kniha s datací předlohy", 1, Map.of(
						"UNIT~DATE", List.of(Map.of("gte", "1850-01-01T00:00:00", "lte", "1860-12-31T23:59:59")),
						"DATE~OTHER", List.of(Map.of("gte", "1600-01-01T00:00:00", "lte", "1600-12-31T23:59:59"))))));

		var everyDating = List.of("UNIT~DATE", "DATE~OTHER");
		assertThat(index.search(query(null, List.of(new FieldFilter.Range(everyDating,
				LocalDateTime.parse("1700-01-01T00:00:00"), LocalDateTime.parse("1700-12-31T23:59:59"), false))))
				.total()).isZero();
		// each of the two datings is found by the facet that spans both
		for (String year : List.of("1600", "1855")) {
			assertThat(index.search(query(null, List.of(new FieldFilter.Range(everyDating,
					LocalDateTime.parse(year + "-01-01T00:00:00"),
					LocalDateTime.parse(year + "-12-31T23:59:59"), false)))).hits())
					.as("dating covering %s", year)
					.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(53));
		}
	}

	@Test
	void anyDatingSpansEveryDatingItemType() {
		indexApus(List.of(
				doc(uuid(49), "Datovano jako UNIT~DATE", 1, Map.of(
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1810-12-31T23:59:59"))),
				doc(uuid(50), "Datovano jako DATE~OTHER", 1, Map.of(
						"DATE~OTHER~L", List.of("1900-01-01T00:00:00"),
						"DATE~OTHER~H", List.of("1910-12-31T23:59:59"))),
				doc(uuid(51), "Bez datace", 1, Map.of())));

		// one facet dates records whose datings live in different item types - which
		// is what a search spanning every record type needs. The fields are named
		// here because they are resolved above the port, from the display model;
		// ANY_DATING is the record-level hull and asks only for bounds
		var range = index.search(query(null, List.of(new FieldFilter.Range(
				List.of("UNIT~DATE", "DATE~OTHER"),
				LocalDateTime.parse("1905-01-01T00:00:00"), LocalDateTime.parse("1906-01-01T00:00:00"), false))));
		assertThat(range.hits()).extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(50));

		var bounds = index.search(new ApuSearchQuery(null, null, List.of(), List.of(),
				List.of(ApuSearchQuery.BoundsRequest.of(FieldFilter.ANY_DATING)), 0, 0, SortMode.RELEVANCE)).bounds()
						.get(FieldFilter.ANY_DATING);
		assertThat(bounds).isNotNull();
		assertThat(atUtcYear(bounds.minMillis())).isEqualTo(1800);
		assertThat(atUtcYear(bounds.maxMillis())).isEqualTo(1910);
		assertThat(bounds.undatedCount()).isEqualTo(1);
	}

	private static int atUtcYear(long epochMillis) {
		return java.time.Instant.ofEpochMilli(epochMillis).atOffset(java.time.ZoneOffset.UTC).getYear();
	}

	/** Fixture with explicit derived dating bounds (what ApuDocumentBuilder computes). */
	protected static ApuDocument docWithDates(String uuid, String name, String dateL, String dateH) {
		var document = doc(uuid, name, 1, Map.of());
		document.setDateL(dateL);
		document.setDateH(dateH);
		return document;
	}

	@Test
	void dateSortsUseTheDerivedBoundsAndFileUndatedLast() {
		// B10: earliest bound ascending / latest bound descending; undated always last
		indexApus(List.of(
				docWithDates(uuid(80), "Střední", "1850-01-01T00:00:00", "1900-12-31T23:59:59"),
				docWithDates(uuid(81), "Stará", "1800-01-01T00:00:00", "1850-12-31T23:59:59"),
				docWithDates(uuid(82), "Nová", "1900-01-01T00:00:00", "1950-12-31T23:59:59"),
				doc(uuid(83), "Bez datace", 1, Map.of())));

		assertThat(index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.DATE_ASC)).hits()).extracting(ApuSearchResult.Hit::uuid)
				.containsExactly(uuid(81), uuid(80), uuid(82), uuid(83));
		assertThat(index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.DATE_DESC)).hits()).extracting(ApuSearchResult.Hit::uuid)
				.containsExactly(uuid(82), uuid(80), uuid(81), uuid(83));
	}

	@Test
	void equalSortValuesOrderStablyByUuid() {
		// B10 (stability): identical names page deterministically - the uuid tie-break
		var uuids = new java.util.ArrayList<>(List.of(uuid(87), uuid(88), uuid(89)));
		indexApus(uuids.stream().map(u -> doc(u, "Stejné jméno", 1, Map.<String, List<Object>>of())).toList());
		java.util.Collections.sort(uuids);

		assertThat(index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.NAME)).hits()).extracting(ApuSearchResult.Hit::uuid)
				.containsExactlyElementsOf(uuids);
	}

	@Test
	void nameSortFollowsContentLocaleAlphabetInBothDirections() {
		indexApus(List.of(
				doc(uuid(15), "Chalupa", 1, Map.of()),
				doc(uuid(16), "Cibule", 1, Map.of()),
				doc(uuid(17), "Hrad", 1, Map.of())));

		// the test deployment runs cs-CZ: c < h < ch
		assertThat(index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.NAME)).hits()).extracting(ApuSearchResult.Hit::name)
				.containsExactly("Cibule", "Hrad", "Chalupa");
		// descending is that same alphabet read backwards, not a byte-order reversal
		assertThat(index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.NAME_DESC)).hits()).extracting(ApuSearchResult.Hit::name)
				.containsExactly("Chalupa", "Hrad", "Cibule");
	}

	@Test
	void typeCountsIgnoreTheTypeRestrictionButRespectFilters() {
		indexApus(List.of(
				doc(uuid(90), "Fond kronik", "FUND", 1, Map.of("LANG~CODE", List.of("tc-cze"))),
				doc(uuid(91), "Kronika A", "ARCH_DESC", 1, Map.of("LANG~CODE", List.of("tc-cze"))),
				doc(uuid(92), "Kronika B", "ARCH_DESC", 1, Map.of("LANG~CODE", List.of("tc-ger")))));

		var result = index.search(new ApuSearchQuery("ARCH_DESC", null,
				List.of(new FieldFilter.Values("LANG~CODE", List.of("tc-cze"))),
				List.of(), List.of(), 0, 10, SortMode.RELEVANCE, null, true));

		// hits and total respect the type restriction
		assertThat(result.total()).isEqualTo(1);
		assertThat(result.hits().get(0).uuid()).isEqualTo(uuid(91));
		// the per-type counts respect the filter but ignore the restriction
		// (count descending, ties by type)
		assertThat(result.typeCounts()).containsExactly(
				new ApuSearchResult.Bucket("ARCH_DESC", 1), new ApuSearchResult.Bucket("FUND", 1));

		// not requested = not computed
		assertThat(index.search(new ApuSearchQuery("ARCH_DESC", null, List.of(), List.of(), List.of(),
				0, 10, SortMode.RELEVANCE)).typeCounts()).isEmpty();
	}

	/** Documents go by their ids; an unknown id is a no-op, other documents stay. */
	@Test
	void deleteByUuidRemovesExactlyTheNamedDocuments() {
		indexApus(List.of(
				doc(uuid(940), "To be removed", 9, Map.of()),
				doc(uuid(941), "To be kept", 9, Map.of())));

		deleteApus(uuid(940), uuid(999));

		var all = index.search(ApuSearchQuery.matchAll(0, 10));
		assertThat(all.hits()).extracting(ApuSearchResult.Hit::uuid).contains(uuid(941)).doesNotContain(uuid(940));
	}

	@Test
	void pagingByFromOffsetReportsFullTotal() {
		indexApus(List.of(
				doc(uuid(20), "Record one", 1, Map.of()),
				doc(uuid(21), "Record two", 1, Map.of()),
				doc(uuid(22), "Record three", 1, Map.of())));

		var page = index.search(ApuSearchQuery.matchAll(0, 2));
		assertThat(page.total()).isEqualTo(3);
		assertThat(page.hits()).hasSize(2);

		// arbitrary offset, not page-aligned
		var offset = index.search(ApuSearchQuery.matchAll(2, 2));
		assertThat(offset.total()).isEqualTo(3);
		assertThat(offset.hits()).hasSize(1);
	}

	@Test
	void totalsAreExactUpToTheAccuracyLimitAndCappedAboveIt() {
		indexApus(List.of(
				doc(uuid(120), "Total one", 1, Map.of()),
				doc(uuid(121), "Total two", 1, Map.of()),
				doc(uuid(122), "Total three", 1, Map.of())));

		// no limit = exact
		var exact = index.search(ApuSearchQuery.matchAll(0, 10));
		assertThat(exact.total()).isEqualTo(3);
		assertThat(exact.totalRelation()).isEqualTo(ApuSearchResult.TotalRelation.EQ);

		// limit above the hit count = still exact
		var above = index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.RELEVANCE, 10));
		assertThat(above.total()).isEqualTo(3);
		assertThat(above.totalRelation()).isEqualTo(ApuSearchResult.TotalRelation.EQ);

		// limit below the hit count = deterministically (limit, GTE) on every
		// engine, even when the engine happens to know the exact count
		var capped = index.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(), 0, 10,
				SortMode.RELEVANCE, 2));
		assertThat(capped.total()).isEqualTo(2);
		assertThat(capped.totalRelation()).isEqualTo(ApuSearchResult.TotalRelation.GTE);
		// the accuracy limit affects counting only, never the returned page
		assertThat(capped.hits()).hasSize(3);
	}

	@Test
	void relatedFilterMatchesEitherEndOfTheRelation() {
		indexApus(List.of(
				doc(uuid(110), "Referencing A", 1, Map.of("REL~ENTITY", List.of(uuid(112)))),
				doc(uuid(111), "Referencing B", 1, Map.of("REL~ENTITY", List.of(uuid(112)))),
				doc(uuid(112), "The entity", 1, Map.of("REL~ENTITY", List.of(uuid(114)))),
				doc(uuid(113), "Points elsewhere", 1, Map.of("REL~ENTITY", List.of(uuid(114)))),
				doc(uuid(114), "What the entity points at", 1, Map.of())));

		// the rels index is write-only within the application (see design Q3), so
		// accepting the write is the whole of its contract
		assertThatCode(() -> indexRelations(List.of(
				new RelationDocument(uuid(110), "REL~DIRECT", uuid(112)))))
				.doesNotThrowAnyException();

		// incoming: whoever references the entity through a field in scope
		assertThat(index.search(query(null, List.of(
				new FieldFilter.Related(List.of("REL~ENTITY"), List.of(uuid(112)), List.of())))).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactlyInAnyOrder(uuid(110), uuid(111));

		// outgoing: the pre-resolved targets, matched on the document's own uuid
		assertThat(index.search(query(null, List.of(
				new FieldFilter.Related(List.of("REL~ENTITY"), List.of(), List.of(uuid(114)))))).hits())
				.extracting(ApuSearchResult.Hit::uuid).containsExactly(uuid(114));

		// both halves at once - one relation, either end of it
		assertThat(index.search(query(null, List.of(
				new FieldFilter.Related(List.of("REL~ENTITY"), List.of(uuid(112)), List.of(uuid(114)))))).hits())
				.extracting(ApuSearchResult.Hit::uuid)
				.containsExactlyInAnyOrder(uuid(110), uuid(111), uuid(114));

		// a field outside the scope does not relate
		assertThat(index.search(query(null, List.of(
				new FieldFilter.Related(List.of("LANG~CODE"), List.of(uuid(112)), List.of()))))
				.total()).isZero();

		// nothing to relate to matches nothing (never everything)
		assertThat(index.search(query(null, List.of(
				new FieldFilter.Related(List.of("REL~ENTITY"), List.of(), List.of())))).total()).isZero();
	}

	@Test
	void relatedFilterNarrowsFacetBucketsToo() {
		indexApus(List.of(
				doc(uuid(115), "Czech, related", 1,
						Map.of("LANG~CODE", List.of("cze"), "REL~ENTITY", List.of(uuid(118)))),
				doc(uuid(116), "German, related", 1,
						Map.of("LANG~CODE", List.of("ger"), "REL~ENTITY", List.of(uuid(118)))),
				doc(uuid(117), "Latin, unrelated", 1, Map.of("LANG~CODE", List.of("lat")))));

		var result = index.search(new ApuSearchQuery(null, null,
				List.of(new FieldFilter.Related(List.of("REL~ENTITY"), List.of(uuid(118)), List.of())),
				List.of(ApuSearchQuery.BucketRequest.of("LANG~CODE", 10)),
				List.of(), 0, 10, SortMode.RELEVANCE));

		assertThat(result.total()).isEqualTo(2);
		// a relation restriction is not a facet the reader can widen, so unlike a
		// Values filter it also constrains the buckets
		assertThat(result.buckets().get("LANG~CODE")).containsExactlyInAnyOrder(
				new ApuSearchResult.Bucket("cze", 1), new ApuSearchResult.Bucket("ger", 1));
	}

	private static ApuSearchQuery query(String fulltext, List<FieldFilter> filters) {
		return new ApuSearchQuery(null,
				RelevanceQueryPlanner.plan(fulltext, RelevanceConfig.defaults()),
				filters, List.of(), List.of(), 0, 10, SortMode.RELEVANCE);
	}

}
