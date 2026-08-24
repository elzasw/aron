package cz.aron;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import cz.aron.test.api.v1.ApuApi;
import cz.aron.test.api.v1.UiApi;
import cz.aron.test.api.v1.model.ApuType;
import cz.aron.test.api.v1.model.Citation;
import cz.aron.test.api.v1.model.CitationForm;

/**
 * Citations of a record, end to end: the SHIPPED configuration and script
 * ({@code distribution/config/citation.yaml} + {@code citation.groovy}, which
 * {@code application-test.yml} points at) over the fixture transfer
 * {@code test-config/import/citation-transfer}.
 * <p>
 * The expected strings are asserted whole and exactly, because a citation's
 * wording is agreed with the archivists - this is the guard on that agreement,
 * not a smoke test. It also pins what a deployment's script may rely on: the
 * {@code id}, {@code apuRepository}, {@code objectMapper} and {@code lang}
 * bindings, and the {@code citation}/{@code error} shape of its result.
 */
class CitationTest extends AbstractTest {

	/** Fixture uuids from test-config/import/citation-transfer/apusrc-citation.xml. */
	private static final String FUND = "c17a0000-0000-4000-8000-c00000000002";

	private static final String ARCH_DESC = "c17a0000-0000-4000-8000-c00000000003";

	private static final String ARCH_DESC_BARE_FUND = "c17a0000-0000-4000-8000-c00000000004";

	private static final String ARCH_DESC_WITHOUT_FUND = "c17a0000-0000-4000-8000-c00000000005";

	private static final String ENTITY = "c17a0000-0000-4000-8000-c00000000007";

	@Test
	void anArchivalDescriptionIsCitedByItsFundAndEveryIdentifierItCarries() {
		var citations = new ApuApi(v1ApiClient()).apuGetCitations(ARCH_DESC, null);

		assertThat(citations).extracting(Citation::getCode, Citation::getLabel)
				.containsExactly(tuple("DEFAULT", "Citace"));
		// the institution and the fund with its NAD number, then the identifiers in
		// the agreed order - the invisible file number among them, both signatures
		// each with its own prefix - and the unit's own description last
		assertThat(citations.get(0).getText()).isEqualTo("CIT Státní okresní archiv Testov, "
				+ "Farní úřad Testov (NAD 4711), ref. ozn. I/7, inv. č. 3, čj. 42/1850, "
				+ "sign. A 12, sign. A 12b, ukl. j. kart. 5, Kronika obce Testov 1850–1900");
	}

	@Test
	void aFundWithoutTitleOrNumberStillCitesTheRecord() {
		// the fund's own name stands in for a missing title part, the number is
		// simply absent, and the record's name stands in for a missing description
		assertThat(new ApuApi(v1ApiClient()).apuGetCitations(ARCH_DESC_BARE_FUND, null).get(0).getText())
				.isEqualTo("CIT Státní okresní archiv Testov, CIT Sbírka bez názvové části, CIT Zlomek spisu");
	}

	@Test
	void aFundIsCitedByItsInstitutionAndNumber() {
		// the fund form writes the number without the "NAD" prefix - deliberate
		assertThat(new ApuApi(v1ApiClient()).apuGetCitations(FUND, null).get(0).getText())
				.isEqualTo("CIT Státní okresní archiv Testov, Farní úřad Testov (4711)");
	}

	@Test
	void aRecordThatCannotBeCitedAnswersWithTheDiagnosis() {
		// an archival description always belongs to a fund, so a missing one is a
		// data error: 422. Which piece was missing goes to the server log, not to
		// the client - Spring keeps the exception reason out of the error body
		assertThatThrownBy(() -> new ApuApi(v1ApiClient()).apuGetCitations(ARCH_DESC_WITHOUT_FUND, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(422));

		// a record type no form covers is the same answer - the UI never offers the
		// action there, so this is for whoever calls the API directly
		assertThatThrownBy(() -> new ApuApi(v1ApiClient()).apuGetCitations(ENTITY, null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(422));
	}

	@Test
	void anUnknownRecordIsNotFound() {
		assertThatThrownBy(() -> new ApuApi(v1ApiClient())
				.apuGetCitations("c17a0000-0000-4000-8000-c000000000ff", null))
				.isInstanceOfSatisfying(RestClientResponseException.class,
						e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void theConfiguredFormsSayWhereACitationIsOffered() {
		var uiApi = new UiApi(v1ApiClient());

		assertThat(uiApi.uiGetConfig(null).getCitations())
				.extracting(CitationForm::getCode, CitationForm::getLabel, CitationForm::getApuTypes)
				.containsExactly(tuple("DEFAULT", "Citace", java.util.List.of(ApuType.ARCH_DESC, ApuType.FUND)));

		// the form's name is deployment text, translated in the sibling file; the
		// citation itself stays in the language of the archives
		assertThat(uiApi.uiGetConfig("en").getCitations().get(0).getLabel()).isEqualTo("Citation");
		assertThat(uiApi.uiGetConfig("de").getCitations().get(0).getLabel()).isEqualTo("Citace");
	}

	@Test
	void theOldApiServesTheSameScriptForTheOldPortalsUi() throws Exception {
		// the old UI reads {citation} out of /script/citation/{id}; the script it
		// runs is the deployment's configured one, so both APIs cite identically
		var response = get("/api/aron/script/citation/" + ARCH_DESC);
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"citation\"")
				.contains("Farní úřad Testov (NAD 4711), ref. ozn. I/7");

		// a record the script cannot cite keeps the old shape too: 200 with a
		// diagnostic the old dialog shows instead of a citation
		assertThat(get("/api/aron/script/citation/" + ARCH_DESC_WITHOUT_FUND).body())
				.contains("\"error\"").contains("Fund part not exist");

		// a script name no configured form owns is 404 - the endpoint no longer
		// runs a file named by the request
		assertThat(get("/api/aron/script/neznamy/" + ARCH_DESC).statusCode()).isEqualTo(404);
	}

	@Test
	void aCitationIsNotStoredByCaches() throws Exception {
		// the text follows the deployment's citation configuration rather than the
		// record alone, so it carries no ETag to revalidate against
		var response = get("/api/v1/apu/" + ARCH_DESC + "/citations");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
		assertThat(response.headers().firstValue("ETag")).isEmpty();
	}

}
