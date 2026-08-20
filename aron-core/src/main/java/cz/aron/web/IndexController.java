package cz.aron.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import cz.aron.web.v1.PresentationLocales;
import cz.aron.web.v1.UiConfigLoader;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves the SPA shell for the enumerated SPA route families (Elza/CAM pattern -
 * no blind catch-all; a new top-level UI route requires adding a mapping here).
 * <p>
 * The shell is a lightweight template: the servlet context path - which already
 * includes the reverse proxy's X-Forwarded-Prefix thanks to
 * {@code server.forward-headers-strategy=framework} - is substituted per request
 * into {@code <base href>} and the {@code window.serverContextPath} JavaScript
 * global, so the same artifact works at the URL root and under any subpath
 * without rebuild. The deployment's name, default language and primary colour
 * are substituted with it, so the first paint is truthful before any script
 * runs.
 * <p>
 * Shell resolution: the real UI's template ({@code META-INF/aron-ui/index.html},
 * provided by the aron-ui resource jar - present in the distribution) is
 * preferred; without it the aron-core placeholder ({@code web/index.html}) is
 * served, e.g. in backend-only runs and tests. The UI template deliberately
 * lives outside {@code META-INF/resources/} so it can never be served raw with
 * its tokens unsubstituted.
 */
@Controller
public class IndexController {

	private static final String UI_SHELL = "classpath:META-INF/aron-ui/index.html";

	private static final String PLACEHOLDER_SHELL = "classpath:web/index.html";

	private final String shellTemplate;

	private final UiConfigLoader uiConfigLoader;

	private final PresentationLocales presentationLocales;

	public IndexController(ResourceLoader resourceLoader, UiConfigLoader uiConfigLoader,
			PresentationLocales presentationLocales) throws IOException {
		this.uiConfigLoader = uiConfigLoader;
		this.presentationLocales = presentationLocales;
		Resource shell = resourceLoader.getResource(UI_SHELL);
		if (!shell.exists()) {
			shell = resourceLoader.getResource(PLACEHOLDER_SHELL);
		}
		this.shellTemplate = shell.getContentAsString(StandardCharsets.UTF_8);
	}

	@GetMapping(path = { "/", "/apu/**", "/institution/**", "/fund/**", "/finding-aid/**", "/arch-desc/**",
			"/entity/**", "/originator/**", "/news/**" })
	public ResponseEntity<String> spaShell(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		// the reader's own language is a client-side choice the server cannot know,
		// so the first paint states the deployment's default; the UI corrects both
		// once it has loaded
		Locale defaultLocale = presentationLocales.getDefault();
		String html = shellTemplate
				.replace("__CONTEXT_PATH_HTML__", htmlAttributeEscape(contextPath))
				.replace("__CONTEXT_PATH_JS__", jsStringEscape(contextPath))
				.replace("__PAGE_LANG__", htmlAttributeEscape(defaultLocale.getLanguage()))
				.replace("__PAGE_TITLE__", htmlTextEscape(uiConfigLoader.getConfig(defaultLocale).getName()))
				.replace("__PRIMARY_COLOR_CSS__", primaryColorDeclarations());
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_HTML)
				.cacheControl(CacheControl.noCache())
				.body(html);
	}

	/**
	 * The deployment's primary colour as custom properties the UI's palette reads,
	 * or nothing when it keeps the portal default (the UI's own fallbacks then
	 * apply). The values are validated as CSS colours when the configuration is
	 * loaded, so nothing here can escape the stylesheet.
	 */
	private String primaryColorDeclarations() {
		UiConfigLoader.PrimaryColor color = uiConfigLoader.getPrimaryColor();
		if (color == null) {
			return "";
		}
		return "--aron-primary-dark: " + color.dark() + "; --aron-primary-main: " + color.main() + ";";
	}

	/**
	 * The context path normally contains no markup, but a client talking to the
	 * application directly can inject an arbitrary X-Forwarded-Prefix - escape both
	 * substitution contexts so it cannot break out of them.
	 */
	private static String htmlAttributeEscape(String value) {
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Element text: the portal name comes from deployment configuration, not from the request. */
	private static String htmlTextEscape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String jsStringEscape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003C");
	}

}
