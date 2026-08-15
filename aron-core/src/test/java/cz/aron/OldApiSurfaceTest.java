package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
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
	void contextLoadsWithoutElasticsearchAndPostgres() {
		// @SpringBootTest booting the full server is the assertion
	}

	@Test
	void facets() throws Exception {
		var response = get("/api/aron/facets");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("application/json");
		// underscores in facet sources are published as tildes
		assertThat(response.body()).contains("TEST~FACET");
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

	@Test
	void soapFileTransferWsdlIsInternalOutsideApiPrefix() throws Exception {
		// internal service-to-service interface (Transfagent ingest): served at
		// /cxf/*, deliberately outside the publicly proxied /api namespace
		var response = get("/cxf/ft?wsdl");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("definitions");
		assertThat(get("/api/aron/cxf/ft?wsdl").statusCode()).isEqualTo(404);
	}

	@Test
	void redirectUnknownPermalinkIs404() throws Exception {
		var response = get("/api/aron/redirect/does-not-exist");
		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void attachmentUnknownNameIs404() throws Exception {
		var response = get("/api/aron/attachment/does-not-exist");
		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void redirectImageUnknownPermalinkIs404() throws Exception {
		var response = get("/api/aron/redirectimage/does-not-exist");
		assertThat(response.statusCode()).isEqualTo(404);
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
