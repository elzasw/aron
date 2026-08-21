package cz.aron.web.v1;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.api.v1.SystemApi;
import cz.aron.api.v1.model.SystemInfo;

/**
 * Implements the /api/v1/system endpoints of the new portal API. HTTP mappings
 * live on the generated interface; the URLs carry the full /api/v1 prefix from
 * the TypeSpec contract, so no MVC prefixing applies to new-API controllers
 * (unlike the old API - see WebConfig).
 * <p>
 * The running version is disclosed only where a deployment asks for it
 * ({@code system.expose-version}, off by default): it answers a support
 * question and helps whoever debugs an installation - a test server turns it on
 * - while a reader of a public portal can do nothing with it. Withholding it
 * means not sending it, not sending it and asking the UI to hide it, which
 * would leave it a request away.
 */
@RestController
public class SystemInfoController implements SystemApi {

	private final String version;

	public SystemInfoController(Optional<BuildProperties> buildProperties,
			@Value("${system.expose-version:false}") boolean exposeVersion) {
		this.version = exposeVersion
				? buildProperties.map(BuildProperties::getVersion).orElse("dev")
				: null;
	}

	@Override
	public ResponseEntity<SystemInfo> systemGetInfo() {
		var info = new SystemInfo("aron");
		info.setVersion(version);
		return ResponseEntity.ok(info);
	}

}
