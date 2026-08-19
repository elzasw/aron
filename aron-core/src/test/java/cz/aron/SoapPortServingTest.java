package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cz.aron.apux._2020.UuidList;
import cz.aron.management.v1.ApuManagementPort;
import cz.aron.management.v1.AronManagementService;
import cz.aron.ws.SoapListener;
import jakarta.xml.ws.BindingProvider;

/**
 * Verifies the optional separate listener of the internal SOAP interfaces
 * ({@code soap.port}): with it configured the split is strict - {@code /cxf/**}
 * answers only there, and that listener answers nothing else. The default
 * behaviour (no such port, SOAP on the main port) is covered by the shared context
 * of {@link cz.aron.ws.ApuManagementTest}.
 * <p>
 * DELIBERATE SECOND CONTEXT (as {@link SubpathServingTest}, the same reasoning):
 * the extra connector is created when the server starts and cannot be added per
 * test. It needs its own in-memory database too - the named {@code mem:testdb} of
 * the default context stays alive JVM-wide (DB_CLOSE_DELAY=-1) and a second
 * Liquibase run against it would collide on the changelog table.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		AbstractTest.CLASSPATH_CONFIG_ONLY,
		// 0 = ephemeral port; the test asks the listener which one it got
		"soap.port=0",
		"spring.datasource.url=jdbc:h2:mem:testdb-soap;MODE=PostgreSQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=VALUE,ORDER;DB_CLOSE_DELAY=-1",
		"spring.liquibase.url=jdbc:h2:mem:testdb-soap;MODE=PostgreSQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1" })
class SoapPortServingTest extends AbstractTest {

	@Autowired
	private SoapListener soapListener;

	private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

	@Test
	void bothSoapEndpointsAnswerOnTheSoapPort() throws Exception {
		assertThat(onSoapPort("/cxf/management?wsdl").body()).contains("AronManagementService");
		assertThat(onSoapPort("/cxf/ft?wsdl").body()).contains("definitions");
	}

	@Test
	void managementCallIsServedThroughTheSoapPort() {
		// unknown uuid = accepted, so this asserts reachability, not deletion
		management().deleteApuSources(uuidList(UUID.randomUUID().toString()));
	}

	@Test
	void mainPortNoLongerServesTheSoapInterfaces() throws Exception {
		assertThat(get("/cxf/management?wsdl").statusCode()).isEqualTo(404);
		assertThat(get("/cxf/ft?wsdl").statusCode()).isEqualTo(404);
	}

	@Test
	void soapPortServesNothingButTheSoapInterfaces() throws Exception {
		assertThat(onSoapPort("/").statusCode()).isEqualTo(404);
		assertThat(onSoapPort("/api/v1/system/info").statusCode()).isEqualTo(404);
		assertThat(onSoapPort("/api/aron/facets").statusCode()).isEqualTo(404);
		assertThat(onSoapPort("/actuator/health").statusCode()).isEqualTo(404);
	}

	@Test
	void portalKeepsAnsweringOnTheMainPort() throws Exception {
		assertThat(get("/").statusCode()).isEqualTo(200);
		assertThat(get("/api/aron/facets").statusCode()).isEqualTo(200);
	}

	private HttpResponse<String> onSoapPort(String path) throws Exception {
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + soapListener.getPort() + path)).GET()
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private ApuManagementPort management() {
		var wsdl = SoapPortServingTest.class.getResource("/wsdl/aron_core.wsdl");
		var managementPort = new AronManagementService(wsdl).getApuManagementPort();
		((BindingProvider) managementPort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				"http://localhost:" + soapListener.getPort() + "/cxf/management");
		return managementPort;
	}

	private static UuidList uuidList(String... uuids) {
		var list = new UuidList();
		list.getUuid().addAll(List.of(uuids));
		return list;
	}

}
