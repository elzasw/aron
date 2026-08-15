package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import cz.aron.test.api.v1.SystemApi;
import cz.aron.test.api.v1.invoker.ApiClient;
import cz.aron.test.api.v1.model.SystemInfo;

/**
 * Drives the new portal API (/api/v1) through the typed Java client generated
 * from the TypeSpec-emitted contract (csc pattern) - proving the whole pipeline:
 * TypeSpec -> committed OpenAPI -> generated server interface + controller ->
 * generated client -> real HTTP round trip.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NewApiV1Test {

	@LocalServerPort
	private int port;

	private SystemApi systemApi() {
		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath("http://localhost:" + port);
		return new SystemApi(apiClient);
	}

	@Test
	void systemInfoViaGeneratedClient() {
		SystemInfo info = systemApi().systemGetInfo();
		assertThat(info.getName()).isEqualTo("aron2");
		assertThat(info.getVersion()).isNotBlank();
	}

}
