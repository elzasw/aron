package cz.aron.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.aron.api.rest.ScriptsApi;
import cz.aron.service.CitationService;
import cz.aron.web.v1.PresentationLocales;

/**
 * The frozen old API's script endpoint, which the old portal's UI calls for a
 * record's citation ({@code /script/citation/{id}}). It republishes what the
 * script produced, verbatim - the old UI reads {@code citation} or {@code error}
 * out of it.
 * <p>
 * The script is looked up among the deployment's configured citation scripts
 * ({@code webResources.citation}) by file name, so the same script serves this
 * endpoint and {@code /api/v1/apu/{uuid}/citations} - one file, one wording. A
 * name that matches no configured script is 404: the endpoint used to read
 * {@code <scriptName>.groovy} relative to the working directory, which let a
 * public URL name the file to execute.
 * <p>
 * The old UI shows a single citation and asks for {@code citation}, so this
 * endpoint serves exactly one form - the first configured one whose script has
 * that name, which is what the shipped {@code citation.groovy} is. A second
 * citation norm is therefore offered by the new UI only; the old one keeps the
 * citation it has always shown.
 */
@RestController
public class ScriptController implements ScriptsApi {

	private final CitationService citationService;

	private final PresentationLocales presentationLocales;

	public ScriptController(CitationService citationService, PresentationLocales presentationLocales) {
		this.citationService = citationService;
		this.presentationLocales = presentationLocales;
	}

	@Override
	public ResponseEntity<Resource> runApuScript(String scriptName, UUID id) {
		var form = citationService.formByScript(scriptName + ".groovy")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such script."));
		// this endpoint carries no language; the deployment's default applies
		String rendered = citationService.renderRaw(form, id, presentationLocales.resolve(null));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new ByteArrayResource(rendered.getBytes(StandardCharsets.UTF_8)));
	}

}
