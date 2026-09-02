package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ResultRowItem;
import cz.aron.api.rest.model.ResultRowItemValue;
import cz.aron.api.rest.model.StructuredResult;
import cz.aron.domain.ApuEntity;
import cz.aron.mapper.StructuredResultSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.search.ApuDocument;
import cz.aron.search.DocumentFixtures;
import cz.aron.search.SearchIndex;

/**
 * Old-API search endpoints ({@code /api/aron/apu/list*}) end to end on the
 * embedded Lucene engine - the request JSON is what the old UI really sends
 * (including its extra properties like {@code family} or
 * {@code nestedQueryEnabled}). Runs in the default suite without Elasticsearch;
 * ES-specific behavior of the same requests belongs to the es-it profile.
 * <p>
 * Fixture data is additive with unique ids (999_1xx) and its own apu type, so it
 * cannot interfere with the assertions of other tests sharing the context.
 */
class OldApiSearchTest extends AbstractTest {

	private static final String APU_TYPE = "OLDAPI~KIND";

	/** Own type for the structured-result fixture, so it cannot skew the counts above. */
	private static final String APU_TYPE_STRUCTURED = "OLDAPI~STRUCTURED";

	@Autowired
	private SearchIndex searchIndex;

	@Autowired
	private ApuEntityRepository apuEntityRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void seed() {
		// the fixture is immutable - skip the DB inserts once present (an assigned
		// id makes save() INSERT, so re-saving would violate the primary key);
		// indexing below is idempotent by itself (documents replace by uuid)
		if (!apuEntityRepository.existsById(999_101L)) {
			saveEntity(999_101L, uuid(1), "OldApi pořadač", "První testovací záznam");
			saveEntity(999_102L, uuid(2), "OldApi deník", "Druhý testovací záznam");
			saveStructured(999_103L, uuid(3), "OldApi mapa");
		}
		searchIndex.indexApus(List.of(
				doc(uuid(1), "OldApi pořadač", Map.of(
						"LANG~CODE", List.of("oldapi-cze"),
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"),
						"FUND~REF", List.of("oldapi-fund-1"),
						"FUND~REF~LABEL", List.of("OldApi Sbírka matrik"),
						"FUND~REF~ID~LABEL", List.of("oldapi-fund-1|OldApi Sbírka matrik"))),
				doc(uuid(2), "OldApi deník", Map.of(
						"LANG~CODE", List.of("oldapi-ger"),
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59"),
						"FUND~REF", List.of("oldapi-fund-2"),
						"FUND~REF~LABEL", List.of("OldApi Archiv města"),
						"FUND~REF~ID~LABEL", List.of("oldapi-fund-2|OldApi Archiv města")))));
		searchIndex.indexApus(List.of(doc(uuid(3), "OldApi mapa", APU_TYPE_STRUCTURED, Map.of())));
	}

	@Test
	void listviewFiltersSortsAggregatesAndHydratesFromDatabase() throws Exception {
		var response = post("/api/aron/apu/listview?listType=TEST", """
				{"size":10,
				 "sort":[{"type":"FIELD","field":"name","order":"ASC","sortMode":"MIN"}],
				 "filters":[{"field":"type","operation":"EQ","value":"%s"}],
				 "aggregations":[{"family":"BUCKET","aggregator":"TERMS","name":"langs",
				                  "field":"LANG~CODE","size":9999}]}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("count").asLong()).isEqualTo(2);
		// items come from the database, in index sort order (Czech collation)
		assertThat(body.get("items")).hasSize(2);
		assertThat(body.get("items").get(0).get("name").asText()).isEqualTo("OldApi deník");
		assertThat(body.get("items").get(0).get("description").asText()).isEqualTo("Druhý testovací záznam");
		assertThat(body.get("items").get(1).get("name").asText()).isEqualTo("OldApi pořadač");
		// requested terms buckets are served (size 9999 honored, both buckets back)
		var langs = body.get("aggregations").get("langs");
		assertThat(langs).hasSize(2);
		assertThat(langs.get(0).get("key").asText()).startsWith("oldapi-");
	}

	@Test
	void listviewAcceptsTheOldUiDropdownRequestVerbatim() throws Exception {
		// captured from the old UI (finding-aid page): unknown index field plus
		// extra JSON properties must be tolerated, result is an empty aggregation
		var response = post("/api/aron/apu/listview?listType=FUND~INST~REF", """
				{"size":0,
				 "aggregations":[{"family":"BUCKET","aggregator":"TERMS","name":"FUND~INST~REF",
				                  "field":"FUND~INST~REF~ID~LABEL","size":9999}],
				 "filters":[{"field":"type","operation":"EQ","value":"FINDING_AID","nestedQueryEnabled":false}]}
				""");

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		// the field is unmapped in the test configuration - no buckets, no error
		assertThat(body.get("aggregations").get("FUND~INST~REF")).isEmpty();
	}

	@Test
	void dateRangeLimitsViaMinMaxMetrics() throws Exception {
		// the old UI's year-slider bounds request (METRIC MAX/MIN with format)
		var response = post("/api/aron/apu/listview?listType=GET-DATE-RANGE", """
				{"size":0,
				 "aggregations":[
				   {"family":"METRIC","aggregator":"MAX","name":"maxH","field":"UNIT~DATE~H","format":"yyyy"},
				   {"family":"METRIC","aggregator":"MIN","name":"minL","field":"UNIT~DATE~L","format":"yyyy"}],
				 "filters":[{"field":"type","operation":"EQ","value":"%s"}]}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("aggregations").get("maxH").get(0).get("asString").asText()).isEqualTo("1910");
		assertThat(body.get("aggregations").get("minL").get(0).get("asString").asText()).isEqualTo("1800");
	}

	@Test
	void listResultsAnswersWithCountAndAggregations() throws Exception {
		// this fixture stores no Kryo result blobs, so the service serves its
		// placeholder ("Prazdny vysledek") item for each matching uuid
		var response = post("/api/aron/apu/listresults?listType=TEST", """
				{"size":5,"filters":[{"field":"type","operation":"EQ","value":"%s"}]}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("count").asLong()).isEqualTo(2);
		assertThat(body.get("items")).hasSize(2);
		assertThat(body.get("items").get(0).get("id").asText()).isIn(uuid(1), uuid(2));
	}

	@Test
	void listResultsServesTheStoredBlobInTheWireFormat() throws Exception {
		var response = post("/api/aron/apu/listresults?listType=TEST", """
				{"size":5,"filters":[{"field":"type","operation":"EQ","value":"%s"}]}
				""".formatted(APU_TYPE_STRUCTURED));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("items")).hasSize(1);
		JsonNode item = body.get("items").get(0);
		assertThat(item.get("id").asText()).isEqualTo(uuid(3));
		assertThat(item.get("t").asText()).isEqualTo("A_D");
		assertThat(item.get("tn").asText()).isEqualTo("mapa.svg");
		// snake_case is the wire format the old UI reads - camel-casing it here
		// silently hid every thumbnail link
		assertThat(item.has("tnLink")).isFalse();
		assertThat(item.get("tn_link").asText()).isEqualTo("https://example.org/mapa");
		var value = item.get("l").get(0).get(0);
		assertThat(value.get("t").asText()).isEqualTo("N");
		assertThat(value.get("v").get(0).get("v").asText()).isEqualTo("Mapa katastru");
	}

	@Test
	void unitdateRangeFilterNarrowsTheResult() throws Exception {
		var response = post("/api/aron/apu/list?listType=TEST", """
				{"size":10,"filters":[
				  {"field":"type","operation":"EQ","value":"%s"},
				  {"field":"UNIT~DATE","operation":"RANGE",
				   "gte":"1840-01-01T00:00:00","lte":"1899-12-31T23:59:59"}]}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("count").asLong()).isEqualTo(1);
		assertThat(body.get("items").get(0).get("name").asText()).isEqualTo("OldApi pořadač");
	}

	@Test
	void getCountRangeRequestVerbatim() throws Exception {
		// captured from the old UI's /fund page (year-range count preview);
		// bounds come with millis + Z, empty aggregations/sort arrays
		var response = post("/api/aron/apu/listview?listType=GET-COUNT-RANGE", """
				{"aggregations":[],
				 "filters":[{"field":"type","operation":"EQ","value":"%s"},
				            {"field":"UNIT~DATE","operation":"RANGE",
				             "gte":"0001-01-01T00:00:00.000Z","lte":"2026-12-31T23:59:59.999Z"}],
				 "sort":[],"flipDirection":false,"size":0}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(objectMapper.readTree(response.body()).get("count").asLong()).isEqualTo(2);
	}

	@Test
	void getOptionsRelRequestOffersTheMatchingReferences() throws Exception {
		// captured from the old UI's autocomplete filter of a reference facet: the
		// options are the relations whose own label matches what the reader typed
		var response = post("/api/aron/apu/list?listType=GET-OPTIONSREL-BY_SOURCE_FUND~REF", """
				{"size":0,
				 "aggregations":[{"name":"items","family":"BUCKET","aggregator":"NESTED","path":"rels",
				   "aggregations":[{"family":"BUCKET","aggregator":"FILTER","name":"relsFilterAgg",
				     "filter":{"operation":"AND","filters":[
				       {"field":"rels.type","operation":"EQ","value":"FUND~REF","nestedQueryEnabled":false},
				       {"operation":"FTXF","field":"rels.label","value":"Sbí","nestedQueryEnabled":false}]},
				     "aggregations":[{"family":"BUCKET","aggregator":"TERMS","name":"idLabel","field":"rels.idLabel"}]}]}],
				 "filters":[{"field":"type","operation":"EQ","value":"%s"},
				            {"field":"FUND~REF~LABEL","operation":"FTXF","value":"Sbí"}],
				 "source":"get-options-by-source"}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		JsonNode idLabel = body.get("aggregations").get("items").get(0)
				.get("aggregations").get("relsFilterAgg").get(0)
				.get("aggregations").get("idLabel");
		assertThat(idLabel).hasSize(1);
		// the key is what the old UI splits into the option's id and its name
		assertThat(idLabel.get(0).get("key").asText()).isEqualTo("oldapi-fund-1|OldApi Sbírka matrik");
		assertThat(idLabel.get(0).get("value").asText()).isEqualTo("1");
	}

	@Test
	void getEntityRelationshipsRequestCountsTheRelationTypes() throws Exception {
		// captured from the old UI's entity detail: a document-scope filter around
		// the nested relations of one target
		var response = post("/api/aron/apu/list?listType=GET-ENTITY-RELATIONSHIPS_GRP", """
				{"size":0,
				 "aggregations":[{"family":"BUCKET","aggregator":"FILTER","name":"apuFilterAgg",
				   "filter":{"operation":"OR","filters":[{"field":"type","operation":"EQ","value":"%s"}]},
				   "aggregations":[{"name":"nestedAgg","family":"BUCKET","aggregator":"NESTED","path":"rels",
				     "aggregations":[{"family":"BUCKET","aggregator":"FILTER","name":"relsFilterAgg",
				       "filter":{"field":"rels.targetId","operation":"EQ","value":"oldapi-fund-1",
				                 "nestedQueryEnabled":false},
				       "aggregations":[{"family":"BUCKET","aggregator":"TERMS","name":"relsTypeAgg",
				                        "field":"rels.type"}]}]}]}]}
				""".formatted(APU_TYPE));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		JsonNode relsType = body.get("aggregations").get("apuFilterAgg").get(0)
				.get("aggregations").get("nestedAgg").get(0)
				.get("aggregations").get("relsFilterAgg").get(0)
				.get("aggregations").get("relsTypeAgg");
		assertThat(relsType).hasSize(1);
		assertThat(relsType.get(0).get("key").asText()).isEqualTo("FUND~REF");
		assertThat(relsType.get(0).get("value").asText()).isEqualTo("1");
	}

	@Test
	void invalidRangeBoundIsABadRequestWithAMessage() throws Exception {
		var response = post("/api/aron/apu/list?listType=TEST", """
				{"size":0,"filters":[{"field":"UNIT~DATE","operation":"RANGE","gte":"not-a-date"}]}
				""");

		assertThat(response.statusCode()).isEqualTo(400);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("message").asText()).contains("not-a-date");
	}

	// --- helpers --------------------------------------------------------------

	private static String uuid(int n) {
		return UUID.nameUUIDFromBytes(("oldapi-it-" + n).getBytes()).toString();
	}

	private void saveEntity(long id, String uuid, String name, String description) {
		ApuEntity apu = new ApuEntity();
		apu.setId(id);
		apu.setUuid(UUID.fromString(uuid));
		apu.setName(name);
		apu.setDescription(description);
		apuEntityRepository.save(apu);
	}

	/** An APU carrying a stored structured result, as the import writes it. */
	private void saveStructured(long id, String uuid, String name) {
		var value = new ResultRowItemValue();
		value.setV("Mapa katastru");
		var field = new ResultRowItem();
		field.setT("N");
		field.addVItem(value);
		var result = new StructuredResult();
		result.setId(uuid);
		result.setT("A_D");
		result.setTn("mapa.svg");
		result.setTnLink("https://example.org/mapa");
		result.addLItem(List.of(field));

		ApuEntity apu = new ApuEntity();
		apu.setId(id);
		apu.setUuid(UUID.fromString(uuid));
		apu.setName(name);
		apu.setResult(StructuredResultSerializer.serialize(result));
		apuEntityRepository.save(apu);
	}

	private static ApuDocument doc(String uuid, String name, Map<String, List<Object>> values) {
		return doc(uuid, name, APU_TYPE, values);
	}

	private static ApuDocument doc(String uuid, String name, String type, Map<String, List<Object>> values) {
		return DocumentFixtures.apu(uuid, name, type, 999_100L, values);
	}

}
