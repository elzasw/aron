package cz.aron.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Serves the SPA shell for the enumerated SPA route families (Elza/CAM pattern -
 * no blind catch-all; a new top-level UI route requires adding a mapping here).
 * <p>
 * The shell (web/index.html) is a lightweight template: the servlet context path -
 * which already includes the reverse proxy's X-Forwarded-Prefix thanks to
 * {@code server.forward-headers-strategy=framework} - is substituted per request
 * into {@code <base href>} and the {@code window.serverContextPath} JavaScript
 * global, so the same artifact works at the URL root and under any subpath
 * without rebuild.
 */
@Controller
public class IndexController {

	private final String shellTemplate;

	public IndexController(@Value("classpath:web/index.html") Resource shell) throws IOException {
		this.shellTemplate = shell.getContentAsString(StandardCharsets.UTF_8);
	}

	@GetMapping(path = { "/", "/apu/**" })
	public ResponseEntity<String> spaShell(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		String html = shellTemplate
				.replace("__CONTEXT_PATH_HTML__", htmlAttributeEscape(contextPath))
				.replace("__CONTEXT_PATH_JS__", jsStringEscape(contextPath));
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_HTML)
				.cacheControl(CacheControl.noCache())
				.body(html);
	}

	/**
	 * The context path normally contains no markup, but a client talking to the
	 * application directly can inject an arbitrary X-Forwarded-Prefix - escape both
	 * substitution contexts so it cannot break out of them.
	 */
	private static String htmlAttributeEscape(String value) {
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String jsStringEscape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003C");
	}

}
