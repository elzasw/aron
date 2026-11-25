package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import cz.aron.test.api.v1.ApuApi;
import cz.aron.test.api.v1.UiApi;

/**
 * Orders, as far as the portal takes part in them, end to end: the SHIPPED
 * configuration and script ({@code distribution/config/order.yaml} +
 * {@code order-item-info.groovy}, which {@code application-test.yml} points
 * at) over the citation fixture - an order item is composed from the same
 * record, fund and institution a citation is.
 * <p>
 * The item is asserted field by field: the reading room finds the material by
 * these values and the order service receives them unchanged, so their shape
 * is an agreement, not an implementation detail.
 */
class OrderTest extends AbstractTest {

	/** Fixture uuids from test-config/import/citation-transfer/apusrc-citation.xml. */
	private static final String FUND = "c17a0000-0000-4000-8000-c00000000002";

	private static final String ARCH_DESC = "c17a0000-0000-4000-8000-c00000000003";

	private static final String ARCH_DESC_BARE_FUND = "c17a0000-0000-4000-8000-c00000000004";

	private static final String ARCH_DESC_WITHOUT_FUND = "c17a0000-0000-4000-8000-c00000000005";

	@Test
	void anArchivalDescriptionIsDescribedForTheReadingRoom() {
		var item = new ApuApi(v1ApiClient()).apuGetOrderItem(ARCH_DESC, null);

		var record = item.getArchivalie();
		assertThat(record.getCisloNad()).isEqualTo(4711);
		assertThat(record.getDilciList()).isNull();
		assertThat(record.getNazevArchivnihoSouboru()).isEqualTo("Farní úřad Testov");
		assertThat(record.getArchivniIdentifikator()).isEqualTo("ref. ozn. I/7, inv. č. 3");
		// the invisible file number among them, both signatures each with its prefix
		assertThat(record.getJineOznaceni()).isEqualTo("čj. 42/1850, sign. A 12, sign. A 12b, ukl. j. kart. 5");
		assertThat(record.getPopis()).isEqualTo("CIT Kronika obce (jméno APU)");
		assertThat(record.getUplnyPopis()).isEqualTo("CIT Kronika obce (jméno APU)");
		// the address is the client's to fill in - the script does not know it
		assertThat(record.getUrl()).isNull();

		// the record names only its fund; the institution comes from that fund
		assertThat(item.getInstitution().getName()).isEqualTo("CIT Státní okresní archiv Testov");
		assertThat(item.getInstitution().getCode()).isEqualTo("226101010");
		assertThat(item.getBadatelna()).isNull();
	}

	@Test
	void aRecordTheScriptCannotDescribeAnswersWithTheDiagnosis() {
		// never a 200 carrying an error: the client puts the answer into the basket
		assertThatThrownBy(() -> new ApuApi(v1ApiClient()).apuGetOrderItem(ARCH_DESC_WITHOUT_FUND, null))
				.isInstanceOfSatisfying(RestClientResponseException.class, e -> {
					assertThat(e.getStatusCode().value()).isEqualTo(422);
					assertThat(e.getResponseBodyAsString()).contains("Fund part not exist");
				});
		// the reading room finds the material by the fund's number
		assertThatThrownBy(() -> new ApuApi(v1ApiClient()).apuGetOrderItem(ARCH_DESC_BARE_FUND, null))
				.isInstanceOfSatisfying(RestClientResponseException.class, e -> {
					assertThat(e.getStatusCode().value()).isEqualTo(422);
					assertThat(e.getResponseBodyAsString()).contains("Fund has no NAD number");
				});
	}

	@Test
	void theDetailSaysWhichRecordsCanBeOrdered() {
		var api = new ApuApi(v1ApiClient());

		assertThat(api.apuGetDetail(ARCH_DESC, null, null, null).getOrderable()).isTrue();
		// a record type the configuration does not list: not offered, and refused
		assertThat(api.apuGetDetail(FUND, null, null, null).getOrderable()).isNull();
		assertThatThrownBy(() -> api.apuGetOrderItem(FUND, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
	}

	@Test
	void anUnknownRecordIsNotFound() {
		assertThatThrownBy(() -> new ApuApi(v1ApiClient())
				.apuGetOrderItem("c17a0000-0000-4000-8000-c000000000ff", null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void theUiConfigurationSaysThatOrdersAreTaken() {
		assertThat(new UiApi(v1ApiClient()).uiGetConfig(null).getOrders()).isTrue();
	}

	@Test
	void anOrderItemIsNotStoredByCaches() throws Exception {
		var response = get("/api/v1/apu/" + ARCH_DESC + "/order-item");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
	}

}
