package cz.aron.web.v1;

import java.util.Optional;

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
 */
@RestController
public class SystemInfoController implements SystemApi {

	private final String version;

	public SystemInfoController(Optional<BuildProperties> buildProperties) {
		this.version = buildProperties.map(BuildProperties::getVersion).orElse("dev");
	}

	@Override
	public ResponseEntity<SystemInfo> systemGetInfo() {
		return ResponseEntity.ok(new SystemInfo("aron2", version));
	}

}
