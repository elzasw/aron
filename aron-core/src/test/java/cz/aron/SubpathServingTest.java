package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.domain.ApuEntity;
import cz.aron.repository.ApuEntityRepository;

/**
 * Verifies the standalone subpath deployment mode: with
 * {@code server.servlet.context-path=/aron} the SAME artifact serves the SPA shell
 * and the complete old API (REST, SOAP, permalink redirects) under /aron - no
 * rebuild, everything shifts uniformly. Together with {@link SpaServingTest} (root
 * + X-Forwarded-Prefix) this covers the deployment matrix from PLAN.md.
 * <p>
 * DELIBERATE SECOND CONTEXT (the only one besides the shared AbstractTest
 * context): the context path cannot be changed per request. It also needs its own
 * in-memory database - the named {@code mem:testdb} of the default context stays
 * alive JVM-wide (DB_CLOSE_DELAY=-1), and a second Liquibase run against it would
 * collide on the changelog table.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"server.servlet.context-path=/aron",
		"spring.datasource.url=jdbc:h2:mem:testdb-subpath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=VALUE,ORDER;DB_CLOSE_DELAY=-1",
		"spring.liquibase.url=jdbc:h2:mem:testdb-subpath;MODE=PostgreSQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1" })
class SubpathServingTest extends AbstractTest {

	@Autowired
	private ApuEntityRepository apuEntityRepository;

	@Test
	void spaShellCarriesContextPath() throws Exception {
		var response = get("/aron/");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("<base href=\"/aron/\"");
		assertThat(response.body()).contains("window.serverContextPath = \"/aron\"");
	}

	@Test
	void oldApiIsServedUnderContextPath() throws Exception {
		var response = get("/aron/api/aron/facets");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("TEST~FACET");
	}

	@Test
	void soapWsdlIsServedUnderContextPath() throws Exception {
		var response = get("/aron/cxf/ft?wsdl");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("definitions");
	}

	@Test
	void nothingIsServedOutsideTheContextPath() throws Exception {
		assertThat(get("/api/aron/facets").statusCode()).isEqualTo(404);
	}

	@Test
	void permalinkRedirectHonorsContextPath() throws Exception {
		UUID uuid = UUID.randomUUID();
		ApuEntity apu = new ApuEntity();
		apu.setId(999_002L);
		apu.setUuid(uuid);
		apu.setPermalink("/subpath-permalink");
		apuEntityRepository.save(apu);

		var response = get("/aron/api/aron/redirect/subpath-permalink");
		assertThat(response.statusCode()).isEqualTo(302);
		assertThat(response.headers().firstValue("Location")).contains("/aron/apu/" + uuid);
	}

}
