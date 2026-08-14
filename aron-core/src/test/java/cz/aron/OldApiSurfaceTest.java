package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import cz.aron.domain.ApuEntity;
import cz.aron.repository.ApuEntityRepository;

/**
 * Pins the externally visible URL surface of the old API (everything under
 * {@code /api/aron}, including the SOAP file-transfer endpoint). These URLs are a
 * published contract consumed by the previous-generation UI and by Transfagent
 * deployments; internal refactorings (context-path removal, mapping prefixes) must
 * keep every assertion here green unchanged. See PLAN.md.
 *
 * Runs against a full server on a random port with H2 (real Liquibase changelog)
 * and without Elasticsearch. The "test" profile is an OVERLAY over the real
 * application.yml (application-test.yml), so production configuration such as the
 * servlet context-path is genuinely exercised here.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OldApiSurfaceTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ApuEntityRepository apuEntityRepository;

	// redirects are asserted explicitly, so the client must not follow them
	private final HttpClient client = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static String contentType(HttpResponse<?> response) {
		return response.headers().firstValue("Content-Type").orElse("");
	}

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
		HttpRequest request = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + "/api/aron/pageTemplate/topImage")).GET().build();
		var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
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
	void soapFileTransferWsdl() throws Exception {
		var response = get("/api/aron/cxf/ft?wsdl");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("definitions");
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
	void endpointsOutsideContextPathDoNotExist() throws Exception {
		// the API lives under /api/aron only - the URL root stays free for the future UI
		var response = get("/facets");
		assertThat(response.statusCode()).isEqualTo(404);
	}

}
