package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the SPA serving contract at the URL root: the shell is served for "/" and
 * the enumerated SPA route families (IndexController - no blind catch-all), with
 * the effective public prefix (context path / X-Forwarded-Prefix) injected into
 * {@code <base href>} and {@code window.serverContextPath}. API misses, unknown
 * routes and missing assets stay 404. Complemented by {@link SubpathServingTest}
 * for the context-path deployment mode.
 */
class SpaServingTest extends AbstractTest {

	@Test
	void rootServesSpaShell() throws Exception {
		var response = get("/");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentType(response)).startsWith("text/html");
		assertThat(response.body()).contains("<base href=\"/\"");
		assertThat(response.body()).contains("window.serverContextPath = \"\"");
	}

	@Test
	void apuDeepLinkServesSpaShell() throws Exception {
		var response = get("/apu/0f0e0d0c-0b0a-0908-0706-050403020100");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("<base href=\"/\"");
	}

	@Test
	void forwardedPrefixIsInjectedIntoShell() throws Exception {
		// reverse-proxy subpath deployment: proxy strips /aron and announces it
		var response = get("/", "X-Forwarded-Prefix", "/aron");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("<base href=\"/aron/\"");
		assertThat(response.body()).contains("window.serverContextPath = \"/aron\"");
	}

	@Test
	void unregisteredRouteIs404() throws Exception {
		// SPA routes are enumerated (Elza/CAM pattern), not a catch-all
		assertThat(get("/definitely-not-a-route").statusCode()).isEqualTo(404);
	}

	@Test
	void internalSoapEndpointLivesOutsideApiNamespace() throws Exception {
		assertThat(get("/cxf/ft?wsdl").statusCode()).isEqualTo(200);
	}

	@Test
	void unknownOldApiPathIsNotSwallowedByFallback() throws Exception {
		assertThat(get("/api/aron/does-not-exist").statusCode()).isEqualTo(404);
	}

	@Test
	void futureNewApiNamespaceIsNotSwallowedByFallback() throws Exception {
		assertThat(get("/api/v1/does-not-exist").statusCode()).isEqualTo(404);
	}

	@Test
	void missingAssetIs404() throws Exception {
		assertThat(get("/assets/missing.js").statusCode()).isEqualTo(404);
	}

	@Test
	void actuatorLivesAtRoot() throws Exception {
		assertThat(get("/actuator/health").statusCode()).isEqualTo(200);
	}

}
