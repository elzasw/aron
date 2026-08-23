package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.domain.ApuEntity;
import cz.aron.repository.ApuEntityRepository;

/**
 * Pins the externally visible URL surface of the old REST API (everything under
 * {@code /api/aron} - a published contract consumed by the previous-generation UI)
 * plus the location of the internal SOAP file-transfer endpoint ({@code /cxf/*},
 * configured per deployment in Transfagent, never exposed by the public reverse
 * proxy). Internal refactorings must keep every assertion here green unchanged.
 * See PLAN.md.
 *
 * Runs against a full server on a random port with H2 (real Liquibase changelog)
 * and without Elasticsearch. The "test" profile is an OVERLAY over the real
 * application.yml (application-test.yml), so production configuration such as the
 * URL layout is genuinely exercised here.
 */
class OldApiSurfaceTest extends AbstractTest {

	@Autowired
	private ApuEntityRepository apuEntityRepository;

	@Autowired
	private jakarta.persistence.EntityManager entityManager;

	@Test
	void facets() throws Exception {
		var response = get("/api/aron/facets");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/json");
		// underscores in facet sources are published as tildes
		assertThat(response.body()).contains("TEST~FACET");
	}

	/**
	 * The published shape of one facet, not only the endpoint's existence: this
	 * response is the searchConfig.yaml the old portal reads, so the DTO's fields
	 * are external contract and dropping one is a deliberate act, not a
	 * refactoring. {@code intervals} was dropped on purpose - a feature neither
	 * portal ever implemented, so the key was an empty array in every facet.
	 */
	@Test
	void facetsPublishTheConfigurationsOwnFields() throws Exception {
		var body = get("/api/aron/facets").body();

		assertThat(body).contains("\"source\":\"TEST~FACET\"", "\"type\":\"ENUM\"", "\"display\":\"ALWAYS\"",
				"\"maxItems\":10", "\"displayedItems\":5", "\"maxDisplayedItems\":10");
		assertThat(body).doesNotContain("\"intervals\"");
	}

	@Test
	void pageTemplate() throws Exception {
		var response = get("/api/aron/pageTemplate");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/x-yaml");
		assertThat(response.body()).contains("ARON test page template");
	}

	@Test
	void pageTemplateLogo() throws Exception {
		var response = get("/api/aron/pageTemplate/logo");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("image/svg+xml");
		assertThat(response.body()).contains("<svg");
	}

	@Test
	void pageTemplateTopImage() throws Exception {
		var response = getBytes("/api/aron/pageTemplate/topImage");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("image/png");
		assertThat(response.body()).isNotEmpty();
	}

	@Test
	void news() throws Exception {
		var response = get("/api/aron/news");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/x-yaml");
		assertThat(response.body()).contains("Test news item");
	}

	@Test
	void favoriteQuery() throws Exception {
		var response = get("/api/aron/favoriteQuery");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/x-yaml");
		assertThat(response.body()).contains("Test query");
	}

	@Test
	void help() throws Exception {
		var response = get("/api/aron/help");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("http://help.test.example");
	}

	@Test
	void apuPartTypes() throws Exception {
		var response = get("/api/aron/apuPartType");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/json");
		// underscores in type codes are published as tildes
		assertThat(response.body()).contains("PT~TITLE").contains("PT~BODY");
	}

	@Test
	void apuPartItemTypes() throws Exception {
		var response = get("/api/aron/apuPartItemType");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/json");
		assertThat(response.body()).contains("TITLE~MAIN").contains("UNIT~DATE");
	}

	/**
	 * The four frozen search endpoints of the old API; in the default suite they
	 * run on the embedded Lucene engine (OldApiSearch seam), so they are part of
	 * the pinned, callable surface without Elasticsearch. One case per endpoint,
	 * so a broken one does not hide the state of the other three.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "/api/aron/apu/listview", "/api/aron/apu/list", "/api/aron/apu/listsimple",
			"/api/aron/apu/listresults" })
	void searchEndpointAnswersOnTheEmbeddedEngine(String endpoint) throws Exception {
		var response = post(endpoint + "?listType=SURFACE-TEST", "{\"size\":1}");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/json");
		assertThat(response.body()).contains("\"count\"");
	}

	@Test
	void soapFileTransferWsdlIsInternalOutsideApiPrefix() throws Exception {
		// internal service-to-service interface (Transfagent ingest): served at
		// /cxf/*, deliberately outside the publicly proxied /api namespace
		var response = get("/cxf/ft?wsdl");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("definitions");
		assertThat(get("/api/aron/cxf/ft?wsdl").statusCode()).isEqualTo(404);
	}

	/** The lookup endpoints answer 404 for a name they do not know, never a 500 or a redirect. */
	@ParameterizedTest
	@ValueSource(strings = { "/api/aron/redirect/does-not-exist", "/api/aron/attachment/does-not-exist",
			"/api/aron/redirectimage/does-not-exist",
			"/api/aron/tile/00000000-0000-4000-8000-000000000000/image.dzi",
			"/api/aron/tile/00000000-0000-4000-8000-000000000000/image_files/0/0_0.jpg" })
	void unknownNameIs404(String path) throws Exception {
		assertThat(get(path).statusCode()).isEqualTo(404);
	}

	@Test
	void redirectKnownPermalinkRedirectsToApuDetail() throws Exception {
		// exercises the real Liquibase-created schema on H2 (insert + JPQL query)
		UUID uuid = UUID.randomUUID();
		ApuEntity apu = new ApuEntity();
		apu.setId(999_001L);
		apu.setUuid(uuid);
		// stored permalinks keep the leading slash of the /redirect/** tail
		apu.setPermalink("/test-permalink");
		apuEntityRepository.save(apu);

		var response = get("/api/aron/redirect/test-permalink");
		assertThat(response.statusCode()).isEqualTo(302);
		assertThat(response.headers().firstValue("Location")).contains("/apu/" + uuid);
	}

	@Test
	@org.springframework.transaction.annotation.Transactional
	void entitiesWithReservedWordColumnsAreLoadableOnH2() {
		// digital_object & co. have a column literally named "order"; Hibernate
		// auto-quotes it while Liquibase created it on its own H2 connection -
		// this pins that both sides agree (caught by the dev-mode seed, not by
		// uuid-projection queries which never touch the column)
		ApuEntity apu = new ApuEntity();
		apu.setId(999_003L);
		apu.setUuid(UUID.randomUUID());
		apuEntityRepository.saveAndFlush(apu);
		entityManager.clear();

		ApuEntity loaded = apuEntityRepository.findById(999_003L).orElseThrow();
		assertThat(loaded.getDigitalObjects()).isEmpty();
	}

	@Test
	void apiIsNotServedOutsideItsPrefix() throws Exception {
		// the API lives under /api/aron only; SPA routes are enumerated in
		// IndexController and /facets is not one of them
		assertThat(get("/facets").statusCode()).isEqualTo(404);
	}

}
