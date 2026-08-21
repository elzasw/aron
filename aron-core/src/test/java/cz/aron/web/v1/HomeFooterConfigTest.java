package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import cz.aron.api.v1.model.FooterLink;
import cz.aron.api.v1.model.FooterLinkCode;
import cz.aron.api.v1.model.HomeFooter;
import cz.aron.api.v1.model.TextRun;

/**
 * Plain unit test of the home page's footer band (no Spring). The interesting
 * part is prose that carries links inside a sentence: the deployment writes
 * named placeholders, and the reader gets runs the portal renders as its own
 * anchors - never configured markup.
 */
class HomeFooterConfigTest {

	private static final Locale CZECH = Locale.forLanguageTag("cs");

	private static HomeFooterConfig parse(String yaml) {
		Map<String, Object> template = new Yaml().load(yaml);
		return HomeFooterConfig.parse(template.get("footer"), name -> name.endsWith(".svg"));
	}

	private static HomeFooter render(String yaml, Locale locale) {
		return parse(yaml).render(locale, name -> "/aron/api/v1/ui/images/" + name);
	}

	/** The real MZA footer: prose naming the operating archive, plus a contact column. */
	private static final String REAL_WORLD = """
			footer:
			  columns:
			    - paragraphs:
			        - text:
			            cs: "Prezentační web ARchiv ONline je webová aplikace {archiv} sloužící ke zpřístupnění popisu archiválií a jejich digitalizátů."
			            en: "ARchiv ONline is a web application of the {archiv}, presenting archival descriptions."
			          links:
			            archiv:
			              label:
			                cs: Moravského zemského archivu v Brně
			                en: Moravian Provincial Archives in Brno
			              url: https://www.mza.cz
			    - heading: { cs: "Kontakt:", en: "Contact:" }
			      links:
			        - label: badatelna@mza.cz
			          url: "mailto:badatelna@mza.cz"
			""";

	@Test
	void noFooterSectionMeansNoBand() {
		assertThat(HomeFooterConfig.parse(null, name -> true)).isNull();
		// an empty column list is a configuration, not an error
		assertThat(parse("footer:\n  columns: []\n")).isNull();
	}

	@Test
	void proseCarriesItsLinkInsideTheSentence() {
		var columns = render(REAL_WORLD, CZECH).getColumns();

		assertThat(columns).hasSize(2);
		assertThat(columns.get(0).getHeading()).isNull();
		assertThat(columns.get(0).getParagraphs()).singleElement().satisfies(paragraph ->
				assertThat(paragraph.getRuns()).extracting(TextRun::getText, TextRun::getUrl).containsExactly(
						tuple("Prezentační web ARchiv ONline je webová aplikace ", null),
						tuple("Moravského zemského archivu v Brně", "https://www.mza.cz"),
						tuple(" sloužící ke zpřístupnění popisu archiválií a jejich digitalizátů.", null)));

		assertThat(columns.get(1).getHeading()).isEqualTo("Kontakt:");
		assertThat(columns.get(1).getParagraphs()).isEmpty();
		assertThat(columns.get(1).getLinks()).extracting(FooterLink::getLabel, FooterLink::getUrl)
				.containsExactly(tuple("badatelna@mza.cz", "mailto:badatelna@mza.cz"));
	}

	@Test
	void everyLanguageGetsItsOwnRuns() {
		// the link sits in a different place in the English sentence - which is why
		// the placeholder travels with the text instead of being a fixed slot
		var english = render(REAL_WORLD, Locale.ENGLISH).getColumns().get(0);

		assertThat(english.getParagraphs().get(0).getRuns())
				.extracting(TextRun::getText, TextRun::getUrl).containsExactly(
						tuple("ARchiv ONline is a web application of the ", null),
						tuple("Moravian Provincial Archives in Brno", "https://www.mza.cz"),
						tuple(", presenting archival descriptions.", null));
	}

	@Test
	void aParagraphNeedsNoLinkAtAll() {
		var runs = render("""
				footer:
				  columns:
				    - paragraphs:
				        - text: "© 2026 Státní oblastní archiv v Plzni"
				""", CZECH).getColumns().get(0).getParagraphs().get(0).getRuns();

		assertThat(runs).extracting(TextRun::getText, TextRun::getUrl)
				.containsExactly(tuple("© 2026 Státní oblastní archiv v Plzni", null));
	}

	@Test
	void aSentenceOfNothingButLinksKeepsItsSeparators() {
		// the inventare.cz shape: a copyright line, three links and a mail address,
		// separated by literal characters of the sentence itself
		var runs = render("""
				footer:
				  columns:
				    - paragraphs:
				        - text: "© 2025 Státní oblastní archiv v Plzni | {web} · {inv} | {mail}"
				          links:
				            web: { label: soaplzen.gov.cz, url: https://soaplzen.gov.cz }
				            inv: { label: inventare.cz, url: https://www.inventare.cz }
				            mail: { label: inventare@soaplzen.cz, url: "mailto:inventare@soaplzen.cz" }
				""", CZECH).getColumns().get(0).getParagraphs().get(0).getRuns();

		assertThat(runs).extracting(TextRun::getText, TextRun::getUrl).containsExactly(
				tuple("© 2025 Státní oblastní archiv v Plzni | ", null),
				tuple("soaplzen.gov.cz", "https://soaplzen.gov.cz"),
				tuple(" · ", null),
				tuple("inventare.cz", "https://www.inventare.cz"),
				tuple(" | ", null),
				tuple("inventare@soaplzen.cz", "mailto:inventare@soaplzen.cz"));
	}

	@Test
	void linksCarryDeploymentSuppliedMarksAndWellKnownCodes() {
		var links = render("""
				footer:
				  columns:
				    - links:
				        - label: Facebook
				          url: https://facebook.com/archiv
				          image: { name: facebook.svg }
				        - label: X
				          url: https://x.com/archiv
				          image: { name: x.svg }
				        - code: ACCESSIBILITY
				          url: https://archiv.example/pristupnost
				""", CZECH).getColumns().get(0).getLinks();

		// the mark is a file of the deployment's own image directory, so a brand
		// that renames itself is a file swap - no vocabulary of ours to change
		assertThat(links).extracting(FooterLink::getLabel, FooterLink::getImageUrl, FooterLink::getCode)
				.containsExactly(
						tuple("Facebook", "/aron/api/v1/ui/images/facebook.svg", null),
						tuple("X", "/aron/api/v1/ui/images/x.svg", null),
						// a well-known code may sit in a column too; the UI labels it
						tuple(null, null, FooterLinkCode.ACCESSIBILITY));
	}

	@Test
	void anUnservableMarkFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - links:
				        - label: Facebook
				          url: https://facebook.com/archiv
				          image: { name: ../secrets.txt }
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("webResources.images");
	}

	@Test
	void aPlaceholderWithoutALinkFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - paragraphs:
				        - text: "Aplikace {archiv} zpřístupňuje popis archiválií."
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("{archiv}");
	}

	@Test
	void aPlaceholderMissingFromOneLanguageFailsTheStartup() {
		// the reader's language decides which sentence is rendered, so a link the
		// English text never uses is a mistake even when the Czech one is right
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - paragraphs:
				        - text:
				            cs: "Aplikace {archiv} zpřístupňuje popis archiválií."
				            en: "The portal presents archival descriptions."
				          links:
				            archiv: { label: Archivu, url: https://archiv.example }
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not used in every language");
	}

	@Test
	void aStrayBraceFailsTheStartup() {
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - paragraphs:
				        - text: "Aplikace {archiv zpřístupňuje popis."
				          links:
				            archiv: { label: Archivu, url: https://archiv.example }
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("stray brace");
	}

	@Test
	void anInlineLinkNeedsBothALabelAndAUrl() {
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - paragraphs:
				        - text: "Aplikace {archiv} zpřístupňuje popis."
				          links:
				            archiv: { label: Archivu }
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("'label' and a 'url'");
	}

	@Test
	void anEmptyColumnOrTypoFailsTheStartup() {
		assertThatThrownBy(() -> parse("footer:\n  columns:\n    - heading: Kontakt\n"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("neither paragraphs nor links");
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - headline: Kontakt
				      links:
				        - label: X
				          url: https://x.example
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("headline");
		assertThatThrownBy(() -> parse("""
				footer:
				  columns:
				    - links:
				        - label: Facebook
				          url: https://facebook.com/archiv
				          icon: FACEBOOK
				"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("icon");
	}

	@Test
	void aLinkStillNeedsACodeOrALabel() {
		assertThatThrownBy(() -> parse("footer:\n  columns:\n    - links:\n        - url: https://x.example\n"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("https://x.example");
	}

	@Test
	void configuredTextVariantsCoverTheFallbackAndEveryTranslation() {
		// the strings the placeholder check has to iterate over: a plain scalar is
		// one variant, a mapping is every language it lists (the first doubling as
		// the fallback, hence no duplicate)
		assertThat(ConfiguredText.of("jeden").variants()).containsExactly("jeden");
		var byLanguage = new LinkedHashMap<String, String>();
		byLanguage.put("cs", "cesky");
		byLanguage.put("en", "in English");
		assertThat(ConfiguredText.of(byLanguage).variants()).containsExactly("cesky", "in English");
	}

}
