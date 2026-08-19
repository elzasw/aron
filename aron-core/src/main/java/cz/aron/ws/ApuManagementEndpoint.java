package cz.aron.ws;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cz.aron.apux._2020.UuidList;
import cz.aron.integration.ImportDataProcessingService;
import cz.aron.management.v1.ApuManagementPort;
import jakarta.xml.ws.WebServiceException;

/**
 * APU source management over SOAP - the {@code ApuManagementPort} of
 * {@code wsdl/aron_core.wsdl}, published at {@code /cxf/management} by
 * {@link ApuManagementWsConfig}.
 * <p>
 * Internal service-to-service interface: Transfagent withdraws data it has
 * published through it, so it lives outside the public {@code /api} namespace and
 * deployments block {@code /cxf/**} at the reverse proxy.
 */
@Component
public class ApuManagementEndpoint implements ApuManagementPort {

	private static final Logger log = LoggerFactory.getLogger(ApuManagementEndpoint.class);

	private final ImportDataProcessingService importDataProcessingService;

	public ApuManagementEndpoint(ImportDataProcessingService importDataProcessingService) {
		this.importDataProcessingService = importDataProcessingService;
	}

	/**
	 * Deletes the listed ApuSources with all data derived from them. Uuids of
	 * sources that are not present are ignored, so repeating the request is
	 * harmless; a malformed uuid rejects the whole request instead of deleting part
	 * of it.
	 */
	@Override
	public void deleteApuSources(UuidList deleteApuSrcs) {
		if (deleteApuSrcs == null || deleteApuSrcs.getUuid().isEmpty()) {
			log.info("Delete apu sources: nothing requested");
			return;
		}
		List<UUID> uuids = deleteApuSrcs.getUuid().stream().map(ApuManagementEndpoint::parseUuid).toList();
		int deleted = importDataProcessingService.deleteApuSources(uuids);
		log.info("Delete apu sources: {} of {} requested sources deleted", deleted, uuids.size());
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw new WebServiceException("Not a valid apu source uuid: " + value);
		}
	}

}
