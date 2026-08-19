package cz.aron.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.AbstractTest;
import cz.aron.apux._2020.UuidList;
import cz.aron.ft.handling.TransferType;
import cz.aron.integration.ImportDataProcessingService;
import cz.aron.management.v1.ApuManagementPort;
import cz.aron.management.v1.AronManagementService;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.ApuSourceRepository;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.SearchIndex;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

/**
 * Drives the internal SOAP management interface ({@code /cxf/management}) through
 * a client generated from the same WSDL: contract -&gt; generated port -&gt; real
 * HTTP round trip, the way NewApiV1Test covers /api/v1. Every test imports its own
 * transfer with random uuids, so the fixtures of the shared context stay intact.
 */
class ApuManagementTest extends AbstractTest {

	@Autowired
	private ImportDataProcessingService importDataProcessingService;

	@Autowired
	private ApuSourceRepository apuSourceRepository;

	@Autowired
	private ApuEntityRepository apuEntityRepository;

	@Autowired
	private SearchIndex searchIndex;

	@Test
	void deletedApuSourceDisappearsFromDatabaseAndIndex(@TempDir Path transfer) throws IOException {
		var apuSourceUuid = UUID.randomUUID();
		var apuUuid = UUID.randomUUID();
		importTransfer(transfer, apuSourceUuid, apuUuid);
		assertThat(apuSourceRepository.findByUuid(apuSourceUuid)).isNotNull();
		assertThat(indexedUuids()).contains(apuUuid.toString());

		management().deleteApuSources(uuidList(apuSourceUuid.toString()));

		assertThat(apuSourceRepository.findByUuid(apuSourceUuid)).isNull();
		assertThat(apuEntityRepository.findByUuid(apuUuid)).isNull();
		assertThat(indexedUuids()).doesNotContain(apuUuid.toString());
	}

	@Test
	void deleteOfUnknownApuSourceIsAccepted() {
		// withdrawing data twice must not fail the caller
		management().deleteApuSources(uuidList(UUID.randomUUID().toString()));
	}

	@Test
	void malformedUuidRejectsTheWholeRequest(@TempDir Path transfer) throws IOException {
		var apuSourceUuid = UUID.randomUUID();
		importTransfer(transfer, apuSourceUuid, UUID.randomUUID());

		assertThatThrownBy(() -> management().deleteApuSources(uuidList(apuSourceUuid.toString(), "not-a-uuid")))
				.isInstanceOf(WebServiceException.class);

		assertThat(apuSourceRepository.findByUuid(apuSourceUuid)).isNotNull();
	}

	@Test
	void contractWsdlIsServed() throws Exception {
		var response = get("/cxf/management?wsdl");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("AronManagementService").contains("DeleteApuSources");
	}

	/** Client built from the committed WSDL, pointed at the running test server. */
	private ApuManagementPort management() {
		var wsdl = ApuManagementTest.class.getResource("/wsdl/aron_core.wsdl");
		var managementPort = new AronManagementService(wsdl).getApuManagementPort();
		((BindingProvider) managementPort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				"http://localhost:" + port + "/cxf/management");
		return managementPort;
	}

	private static UuidList uuidList(String... uuids) {
		var list = new UuidList();
		list.getUuid().addAll(List.of(uuids));
		return list;
	}

	/** Imports one minimal APUSRC transfer through the internal import mechanism. */
	private void importTransfer(Path transfer, UUID apuSourceUuid, UUID apuUuid) throws IOException {
		var xml = """
				<?xml version="1.0"?>
				<apusrc xmlns="http://www.aron.cz/apux/2020" uuid="%s">
				 <apus>
				  <apu type="Institution" uuid="%s">
				   <name>Instituce ke smazani</name>
				  </apu>
				 </apus>
				</apusrc>
				""".formatted(apuSourceUuid, apuUuid);
		Files.writeString(transfer.resolve("apusrc-management.xml"), xml, StandardCharsets.UTF_8);
		importDataProcessingService.processData(transfer, TransferType.APUSRC);
	}

	private List<String> indexedUuids() {
		ApuSearchResult result = searchIndex.search(new ApuSearchQuery(null, null, List.of(), List.of(), Set.of(),
				0, 1000, ApuSearchQuery.SortMode.NAME));
		return result.hits().stream().map(ApuSearchResult.Hit::uuid).toList();
	}

}
