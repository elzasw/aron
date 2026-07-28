package cz.aron.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.domain.dto.DaoFileRedirectDto;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.DaoFileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves human-facing permalinks to their canonical application URLs and issues an HTTP 302 redirect.
 * <p>
 * Permalinks may contain slashes, so the endpoints use wildcard mappings and read the tail of the
 * request URI directly rather than a single-segment path variable. For that reason these operations
 * are not part of the code-generated {@code AronApi} interface.
 */
@Tag(name = "Redirect", description = "Permalink resolution and redirection")
@RestController
public class RedirectApi {

	private static final Logger log = LoggerFactory.getLogger(RedirectApi.class);

	private final ApuEntityRepository apuEntityRepository;

	private final DaoFileRepository daoFileRepository;

	public RedirectApi(ApuEntityRepository apuEntityRepository, DaoFileRepository daoFileRepository) {
		this.apuEntityRepository = apuEntityRepository;
		this.daoFileRepository = daoFileRepository;
	}

	@Operation(summary = "Resolve an APU permalink and redirect to the APU detail page.")
	@ApiResponse(responseCode = "302", description = "Redirect to the APU detail page.")
	@ApiResponse(responseCode = "404", description = "No APU found for the given permalink.")
	@GetMapping("/redirect/**")
	public ResponseEntity<Void> redirect(HttpServletRequest request) {
		String permalink = tail(request, "/redirect");
		log.info("Redirecting {}", permalink);
		List<UUID> ids = apuEntityRepository.findUuidsByPermalink(permalink);
		if (ids.isEmpty()) {
			log.warn("Entity not found for permalink: {}", permalink);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
		}
		String redirectUrl = "/apu/" + ids.get(0);
		log.info("Redirecting from /redirect{} to {}", permalink, redirectUrl);
		return redirectTo(redirectUrl);
	}

	@Operation(summary = "Resolve an attachment name and redirect to its file download.")
	@ApiResponse(responseCode = "302", description = "Redirect to the attachment file.")
	@ApiResponse(responseCode = "404", description = "No (or more than one) attachment found for the given name.")
	@GetMapping("/attachment/**")
	public ResponseEntity<Void> redirectAttachment(HttpServletRequest request) {
		String name = tail(request, "/attachment/");
		log.info("Get attachment {}", name);
		List<UUID> ids = daoFileRepository.findUuidsByAttachmentName(name);
		if (ids.isEmpty()) {
			log.warn("Attachment not found for permalink: {}", name);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
		} else if (ids.size() > 1) {
			log.warn("Multiple attachments found for permalink: {}", name);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Multiple attachments found");
		}
		String redirectUrl = "/api/aron/file/" + ids.get(0);
		log.info("Redirecting from /attachment/{} to {}", name, redirectUrl);
		return redirectTo(redirectUrl);
	}

	@Operation(summary = "Resolve a digital object file permalink and redirect to the file within its APU.")
	@ApiResponse(responseCode = "302", description = "Redirect to the digital object file.")
	@ApiResponse(responseCode = "404", description = "No digital object file found for the given permalink.")
	@GetMapping("/redirectimage/**")
	public ResponseEntity<Void> redirectImage(HttpServletRequest request) {
		String permalink = tail(request, "/redirectimage");
		log.info("Redirecting {}", permalink);
		List<DaoFileRedirectDto> ids = daoFileRepository.findRedirectByPermalink(permalink);
		if (ids.isEmpty()) {
			log.warn("Entity not found for permalink: {}", permalink);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
		}
		DaoFileRedirectDto id = ids.get(0);
		String redirectUrl = "/apu/" + id.apuId() + "/dao/" + id.daoId() + "/file/" + id.fileId();
		log.info("Redirecting from /redirectimage{} to {}", permalink, redirectUrl);
		return redirectTo(redirectUrl);
	}

	/**
	 * Returns the request URI tail after {@code prefix}, with the servlet context path removed.
	 * Preserves any leading slash that is part of {@code prefix} (e.g. {@code "/redirect"} keeps the
	 * following slash, {@code "/attachment/"} strips it) to match the stored permalink format.
	 */
	private static String tail(HttpServletRequest request, String prefix) {
		String withoutContext = request.getRequestURI().substring(request.getContextPath().length());
		return withoutContext.substring(prefix.length());
	}

	private static ResponseEntity<Void> redirectTo(String url) {
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
	}

}
