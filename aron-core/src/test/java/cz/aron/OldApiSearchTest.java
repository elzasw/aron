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

import cz.aron.domain.ApuEntity;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.search.ApuDocument;
import cz.aron.search.ApuDocumentBuilder;
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
		}
		searchIndex.indexApus(List.of(
				doc(uuid(1), "OldApi pořadač", Map.of(
						"LANG~CODE", List.of("oldapi-cze"),
						"UNIT~DATE~L", List.of("1800-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1850-12-31T23:59:59"))),
				doc(uuid(2), "OldApi deník", Map.of(
						"LANG~CODE", List.of("oldapi-ger"),
						"UNIT~DATE~L", List.of("1900-01-01T00:00:00"),
						"UNIT~DATE~H", List.of("1910-12-31T23:59:59")))));
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

	private static ApuDocument doc(String uuid, String name, Map<String, List<Object>> values) {
		var document = new ApuDocument();
		document.setUuid(uuid);
		document.setName(name);
		document.setNameSort(ApuDocumentBuilder.czechSortKey(name));
		document.setType(APU_TYPE);
		document.setApuSourceId(999_100L);
		document.getValues().putAll(values);
		return document;
	}

}
