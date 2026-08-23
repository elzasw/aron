package cz.aron;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import cz.aron.test.api.v1.invoker.ApiClient;

/**
 * Base of all Spring integration tests - carries the ONLY {@code @SpringBootTest}
 * declaration of the suite, so every subclass shares one cached application
 * context (the in-house pattern of Elza/CAM/csc; see CLAUDE.md, Testing).
 * <p>
 * Rules: never add {@code @MockBean}, {@code @DirtiesContext}, or new
 * {@code @ActiveProfiles}/{@code @TestPropertySource} combinations - each unique
 * configuration forks another context, which is the main cost driver of a Spring
 * suite. A subclass may re-declare {@code @SpringBootTest} to get a deliberately
 * different context - currently SubpathServingTest and SoapPortServingTest, both
 * because the setting under test is fixed when the server starts and cannot be
 * varied per request; justify any new one in its javadoc.
 * <p>
 * Keep this base THIN: transport helpers only. Feature-specific fixtures belong
 * to the feature's test class.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = AbstractTest.CLASSPATH_CONFIG_ONLY)
public abstract class AbstractTest {

	/**
	 * Restricts config-data discovery to the classpath, so the suite sees exactly
	 * application.yml + application-test.yml and nothing else. Without it a
	 * deployment configuration in the working directory ({@code ./config/}) would
	 * WIN over the test overlay - an external plain application.yml outranks a
	 * profile-specific one from the jar - and a developer's own database would
	 * silently replace H2.
	 */
	static final String CLASSPATH_CONFIG_ONLY = "spring.config.location=optional:classpath:/";

	@LocalServerPort
	protected int port;

	// redirects are asserted explicitly in tests, so the client must not follow them
	private final HttpClient client = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();

	/** Plain GET against the running server; header name/value pairs optional. */
	protected HttpResponse<String> get(String path, String... headers) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
		for (int i = 0; i < headers.length; i += 2) {
			builder.header(headers[i], headers[i + 1]);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** JSON POST against the running server. */
	protected HttpResponse<String> post(String path, String jsonBody) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	/** GET returning raw bytes (binary endpoints). */
	protected HttpResponse<byte[]> getBytes(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	protected static String contentType(HttpResponse<?> response) {
		return response.headers().firstValue("Content-Type").orElse("");
	}

	/** Typed client of the new API (/api/v1) pointed at the running server. */
	protected ApiClient v1ApiClient() {
		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath("http://localhost:" + port);
		return apiClient;
	}

}
