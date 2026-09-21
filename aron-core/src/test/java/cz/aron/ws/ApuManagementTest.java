package cz.aron.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientResponseException;

import cz.aron.AbstractTest;
import cz.aron.apux._2020.UuidList;
import cz.aron.integration.ApuxTransfers;
import cz.aron.integration.ImportDataProcessingService;
import cz.aron.management.v1.ApuManagementPort;
import cz.aron.management.v1.AronManagementService;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.ApuSourceRepository;
import cz.aron.repository.IndexDirtyRepository;
import cz.aron.search.ApuSearchQuery;
import cz.aron.search.ApuSearchResult;
import cz.aron.search.DocumentFixtures;
import cz.aron.search.FieldFilter;
import cz.aron.search.IndexSynchronizer;
import cz.aron.search.SearchIndex;
import cz.aron.test.api.v1.ApuApi;
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

	@Autowired
	private IndexSynchronizer indexSynchronizer;

	@Autowired
	private IndexDirtyRepository indexDirtyRepository;

	/**
	 * The whole life of a record, in the order a deployment lives it: Transfagent
	 * delivers it, the portal serves and finds it, a corrected delivery replaces
	 * it, and a withdrawal removes it again.
	 *
	 * <p>One scenario rather than three, because the interesting assertions are
	 * about the steps agreeing with each other - a re-import that updates the
	 * database but leaves the old document in the index, or a delete that empties
	 * the database while the search keeps offering a record whose page is gone,
	 * are exactly the failures no single-step test can see.
	 */
	@Test
	void aDeliveredRecordIsServedAndFoundUntilItIsWithdrawn(@TempDir Path transfer) throws IOException {
		var apuSourceUuid = UUID.randomUUID();
		var apuUuid = UUID.randomUUID();

		// delivered
		importTransfer(transfer, apuSourceUuid, apuUuid, "Lifecycle instituce");
		assertThat(apuSourceRepository.findByUuid(apuSourceUuid)).isNotNull();
		assertThat(apuEntityRepository.findByUuid(apuUuid)).isNotNull();
		assertThat(indexedName(apuUuid)).isEqualTo("Lifecycle instituce");
		assertThat(detail(apuUuid).getName()).isEqualTo("Lifecycle instituce");

		// corrected: the same source delivered again replaces what it carried, in
		// the index as well as in the database
		importTransfer(transfer, apuSourceUuid, apuUuid, "Prejmenovana instituce");
		assertThat(indexedName(apuUuid)).isEqualTo("Prejmenovana instituce");
		assertThat(detail(apuUuid).getName()).isEqualTo("Prejmenovana instituce");

		// withdrawn
		management().deleteApuSources(uuidList(apuSourceUuid.toString()));
		assertThat(apuSourceRepository.findByUuid(apuSourceUuid)).isNull();
		assertThat(apuEntityRepository.findByUuid(apuUuid)).isNull();
		assertThat(indexedName(apuUuid)).isNull();
		assertThatThrownBy(() -> detail(apuUuid)).isInstanceOfSatisfying(RestClientResponseException.class,
				e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void deleteOfUnknownApuSourceIsAccepted() {
		// withdrawing data twice must not fail the caller
		management().deleteApuSources(uuidList(UUID.randomUUID().toString()));
	}

	@Test
	void malformedUuidRejectsTheWholeRequest(@TempDir Path transfer) throws IOException {
		var apuSourceUuid = UUID.randomUUID();
		importTransfer(transfer, apuSourceUuid, UUID.randomUUID(), "Instituce k zachovani");

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

	/**
	 * A record's document carries the labels of the records it references, so it
	 * has to follow their life too: gain the label when the target arrives after
	 * it (a fund delivered before its description's root), change it when the
	 * target is renamed, lose it when the target is withdrawn - all without the
	 * referrer being delivered again.
	 */
	@Test
	void aReferrersDocumentFollowsTheTargetsLife(@TempDir Path folder) throws IOException {
		var referrerSource = UUID.randomUUID();
		var referrer = UUID.randomUUID();
		var targetSource = UUID.randomUUID();
		var target = UUID.randomUUID();

		// referenced before it exists
		new ApuxTransfers.Transfer(referrerSource).apu(referrer, "Institution", "Odkazujici zaznam", null)
				.ref("REL_ENTITY", target.toString()).importInto(importDataProcessingService, folder.resolve("ref"));
		assertThat(referrersOf(target)).containsExactly(referrer.toString());
		assertThat(referrersLabelled(target, "Cilova entita")).isEmpty();

		// the target arrives
		importTransfer(folder.resolve("target"), targetSource, target, "Cilova entita");
		assertThat(referrersLabelled(target, "Cilova entita")).containsExactly(referrer.toString());

		// renamed
		importTransfer(folder.resolve("target"), targetSource, target, "Prejmenovana entita");
		assertThat(referrersLabelled(target, "Cilova entita")).isEmpty();
		assertThat(referrersLabelled(target, "Prejmenovana entita")).containsExactly(referrer.toString());

		// withdrawn: the reference stays - it is the referrer's own data - but the label goes
		management().deleteApuSources(uuidList(targetSource.toString()));
		assertThat(referrersLabelled(target, "Prejmenovana entita")).isEmpty();
		assertThat(referrersOf(target)).containsExactly(referrer.toString());
		assertThat(indexedName(target)).isNull();
	}

	/** Nothing reaches the index before the commit, so a failed import changes nothing anywhere. */
	@Test
	void aFailedImportLeavesDatabaseAndIndexUntouched(@TempDir Path folder) throws IOException {
		var source = UUID.randomUUID();
		var first = UUID.randomUUID();
		var second = UUID.randomUUID();
		importTransfer(folder.resolve("v1"), source, first, "Puvodni verze");

		// a malformed reference value fails the import once the first APU is already processed
		var broken = new ApuxTransfers.Transfer(source).apu(first, "Institution", "Nova verze", null)
				.apu(second, "Institution", "S vadnym odkazem", null).ref("REL_ENTITY", "not-a-uuid");
		assertThatThrownBy(() -> broken.importInto(importDataProcessingService, folder.resolve("v2")))
				.isInstanceOf(RuntimeException.class);

		assertThat(apuEntityRepository.findByUuid(first).getName()).isEqualTo("Puvodni verze");
		assertThat(apuEntityRepository.findByUuid(second)).isNull();
		assertThat(indexedName(first)).isEqualTo("Puvodni verze");
		assertThat(indexedName(second)).isNull();
		assertThat(indexDirtyRepository.count()).isZero();
	}

	@Test
	void aReDeliveryDropsWhatItOmits(@TempDir Path folder) throws IOException {
		var source = UUID.randomUUID();
		var kept = UUID.randomUUID();
		var omitted = UUID.randomUUID();
		new ApuxTransfers.Transfer(source).apu(kept, "Institution", "Ponechana", null)
				.apu(omitted, "Institution", "Vynechana", null)
				.importInto(importDataProcessingService, folder.resolve("v1"));
		assertThat(indexedName(omitted)).isEqualTo("Vynechana");

		new ApuxTransfers.Transfer(source).apu(kept, "Institution", "Ponechana v2", null)
				.importInto(importDataProcessingService, folder.resolve("v2"));

		assertThat(indexedName(kept)).isEqualTo("Ponechana v2");
		assertThat(indexedName(omitted)).isNull();
		assertThat(apuEntityRepository.findByUuid(omitted)).isNull();
	}

	/**
	 * What a crash between the commit and the index writes leaves behind - dirty
	 * rows, among them one whose record is already gone from the database - is
	 * finished by the next synchronization, which is what the startup runs.
	 */
	@Test
	void interruptedSynchronizationIsFinishedByTheNextRun(@TempDir Path folder) throws IOException {
		var sourceUuid = UUID.randomUUID();
		var apu = UUID.randomUUID();
		var stale = UUID.randomUUID();
		importTransfer(folder, sourceUuid, apu, "Pred vypadkem");
		var source = apuSourceRepository.findByUuid(sourceUuid);
		var entity = apuEntityRepository.findByUuid(apu);
		entity.setName("Po vypadku");
		apuEntityRepository.save(entity);
		searchIndex.indexApus(List.of(DocumentFixtures.apu(stale.toString(), "Z predchozi davky", "INSTITUTION",
				source.getId(), Map.of())));
		indexDirtyRepository.markDirty(apu);
		indexDirtyRepository.markDirty(stale);

		indexSynchronizer.synchronize();

		assertThat(indexedName(apu)).isEqualTo("Po vypadku");
		assertThat(indexedName(stale)).isNull();
		assertThat(indexDirtyRepository.count()).isZero();
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

	private void importTransfer(Path transfer, UUID apuSourceUuid, UUID apuUuid, String name) throws IOException {
		ApuxTransfers.writeAndImport(importDataProcessingService, transfer, apuSourceUuid, apuUuid, name);
	}

	/** What the search index holds for that APU, or null when it holds nothing. */
	private String indexedName(UUID apuUuid) {
		ApuSearchResult result = searchIndex.search(new ApuSearchQuery(null, null, List.of(), List.of(), List.of(),
				0, 1000, ApuSearchQuery.SortMode.NAME));
		return result.hits().stream()
				.filter(hit -> hit.uuid().equals(apuUuid.toString()))
				.map(ApuSearchResult.Hit::name)
				.findFirst()
				.orElse(null);
	}

	/** Records whose documents reference the target - the raw uuid in the item field. */
	private List<String> referrersOf(UUID target) {
		return hits(new FieldFilter.Values("REL~ENTITY", List.of(target.toString())));
	}

	/** Records whose documents carry the target under that label. */
	private List<String> referrersLabelled(UUID target, String name) {
		return hits(new FieldFilter.Values("REL~ENTITY~ID~LABEL", List.of(target + "|" + name)));
	}

	private List<String> hits(FieldFilter filter) {
		var result = searchIndex.search(new ApuSearchQuery(null, null, List.of(filter), List.of(), List.of(), 0, 100,
				ApuSearchQuery.SortMode.NAME));
		return result.hits().stream().map(ApuSearchResult.Hit::uuid).toList();
	}

	/** The record as the portal's own API serves it. */
	private cz.aron.test.api.v1.model.ApuDetail detail(UUID apuUuid) {
		return new ApuApi(v1ApiClient()).apuGetDetail(apuUuid.toString(), null, null, null);
	}

}
