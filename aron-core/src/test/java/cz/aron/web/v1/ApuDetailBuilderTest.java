package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.api.v1.model.DetailItem;
import cz.aron.api.v1.model.DetailItemKind;
import cz.aron.api.v1.model.DetailPart;
import cz.aron.api.v1.model.PartViewType;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.TypesLoader;

/**
 * Unit tests of the detail render model assembly (no Spring, no database):
 * display ordering, visibility filtering, label resolution, per-kind value
 * conversion including the Czech UNITDATE formatting. The ApuDocumentBuilderTest
 * pattern.
 */
class ApuDetailBuilderTest {

	private static ApuDetailBuilder builder;

	@BeforeAll
	static void setUp() {
		var typesLoader = new TypesLoader(null, "src/test/resources/test-config/types.yaml");
		var typesHolder = new TypesHolder(typesLoader);
		ReflectionTestUtils.invokeMethod(typesHolder, "loadData");
		builder = new ApuDetailBuilder(typesHolder, new ObjectMapper());
	}

	private static ApuPartItem item(String type, String value) {
		var item = new ApuPartItem();
		item.setType(type);
		item.setValue(value);
		item.setVisible(true);
		return item;
	}

	private static ApuPart part(String type, ApuPartItem... items) {
		var part = new ApuPart();
		part.setType(type);
		for (var i : items) {
			part.addItemsItem(i);
		}
		return part;
	}

	private static String unitDate(String from, String to, String format, boolean fromEstimated,
			boolean toEstimated) {
		return """
				{"from":"%s","to":"%s","format":"%s","valueFromEstimated":%s,"valueToEstimated":%s}"""
				.formatted(from, to, format, fromEstimated, toEstimated);
	}

	@Test
	void partsFollowDisplayModelOrderAndItemsFollowViewOrder() {
		// XML order scrambled on purpose: PT~BODY before PT~TITLE, items reversed
		var parts = builder.buildParts(List.of(
				part("PT~BODY", item("LANG~CODE", "cze"), item("TITLE~MAIN", "Hlavní titul")),
				part("PT~TITLE", item("TITLE~MAIN", "Titulní část"))), Map.of());

		// part order = types.yaml partTypes order (PT~TITLE first), labels resolved
		assertThat(parts).extracting(DetailPart::getCode).containsExactly("PT~TITLE", "PT~BODY");
		assertThat(parts.get(0).getLabel()).isEqualTo("Title");
		// view types follow the display model
		assertThat(parts.get(0).getViewType()).isEqualTo(PartViewType.STANDALONE);
		assertThat(parts.get(1).getViewType()).isEqualTo(PartViewType.GROUPED);
		// item order = types.yaml viewOrder (TITLE~MAIN before LANG~CODE)
		assertThat(parts.get(1).getItems()).extracting(DetailItem::getCode)
				.containsExactly("TITLE~MAIN", "LANG~CODE");
		assertThat(parts.get(1).getItems().get(0).getLabel()).isEqualTo("Main title");
	}

	@Test
	void invisibleAndEmptyItemsAreFilteredAndEmptyPartsOmitted() {
		var invisible = item("TITLE~MAIN", "Skrytý");
		invisible.setVisible(false);
		// null visibility means visible (the importer's default)
		var nullVisibility = item("LANG~CODE", "cze");
		nullVisibility.setVisible(null);

		var parts = builder.buildParts(List.of(
				part("PT~BODY", invisible, item("TITLE~MAIN", ""), nullVisibility),
				part("PT~TITLE", invisible)), Map.of());

		assertThat(parts).hasSize(1);
		assertThat(parts.get(0).getItems()).extracting(DetailItem::getCode).containsExactly("LANG~CODE");
	}

	@Test
	void unknownTypesAreSkipped() {
		var parts = builder.buildParts(List.of(
				part("PT~NEZNAMY", item("TITLE~MAIN", "Ve známém typu položky")),
				part("PT~BODY", item("NEZNAMY~TYP", "hodnota"), item("LANG~CODE", "cze"))), Map.of());

		// unknown part type keeps its part (code as label), unknown item type is dropped
		assertThat(parts).extracting(DetailPart::getCode).containsExactly("PT~BODY", "PT~NEZNAMY");
		assertThat(parts.get(1).getLabel()).isEqualTo("PT~NEZNAMY");
		assertThat(parts.get(0).getItems()).extracting(DetailItem::getCode).containsExactly("LANG~CODE");
	}

	@Test
	void childPartsFollowTheirParent() {
		var parent = part("PT~TITLE", item("TITLE~MAIN", "Rodič"));
		parent.addChildPartsItem(part("PT~BODY", item("LANG~CODE", "cze")));

		var parts = builder.buildParts(List.of(parent), Map.of());

		assertThat(parts).extracting(DetailPart::getCode).containsExactly("PT~TITLE", "PT~BODY");
	}

	@Test
	void refItemsResolveFromLabelsWithUuidFallback() {
		var resolvedUuid = UUID.fromString("aaaaaaaa-1111-2222-3333-444444444444");
		var parts = builder.buildParts(List.of(part("PT~BODY",
				item("REL~ENTITY", resolvedUuid.toString()),
				item("REL~ENTITY", "bbbbbbbb-1111-2222-3333-444444444444"))),
				Map.of(resolvedUuid.toString(), new IdLabelDto(1L, resolvedUuid, "Václav Novák", null)));

		var items = parts.get(0).getItems();
		assertThat(items.get(0).getKind()).isEqualTo(DetailItemKind.REF);
		assertThat(items.get(0).getValue()).isEqualTo("Václav Novák");
		assertThat(items.get(0).getRef().getUuid()).isEqualTo(resolvedUuid.toString());
		assertThat(items.get(0).getRef().getName()).isEqualTo("Václav Novák");
		// unresolved reference still links, labeled by the uuid
		assertThat(items.get(1).getValue()).isEqualTo("bbbbbbbb-1111-2222-3333-444444444444");
	}

	@Test
	void linkItemsCarryHrefWithCaptionFallback() {
		var link = item("LINK~SOURCE", "Zdroj");
		link.setHref("https://example.org");
		var bare = item("LINK~SOURCE", null);
		bare.setHref("https://example.org/bare");

		var items = builder.buildParts(List.of(part("PT~BODY", link, bare)), Map.of()).get(0).getItems();

		assertThat(items.get(0).getKind()).isEqualTo(DetailItemKind.LINK);
		assertThat(items.get(0).getValue()).isEqualTo("Zdroj");
		assertThat(items.get(0).getHref()).isEqualTo("https://example.org");
		// no caption - the target doubles as the caption
		assertThat(items.get(1).getValue()).isEqualTo("https://example.org/bare");
	}

	@Test
	void unitDatesAreFormattedForDisplay() {
		var items = builder.buildParts(List.of(part("PT~BODY",
				item("UNIT~DATE", unitDate("1850-01-01T00:00:00", "1910-12-31T23:59:59", "Y-Y", false, false)),
				item("UNIT~DATE", unitDate("1850-01-01T00:00:00", "1850-12-31T23:59:59", "Y-Y", false, false)),
				item("UNIT~DATE", unitDate("1850-05-01T00:00:00", "1850-05-01T00:00:00", "D", false, false)),
				item("UNIT~DATE", unitDate("1850-01-01T00:00:00", "1910-12-31T23:59:59", "Y-Y", true, false)),
				item("UNIT~DATE", unitDate("1801-01-01T00:00:00", "1900-12-31T23:59:59", "C-C", false, false)))),
				Map.of()).get(0).getItems();

		assertThat(items.get(0).getValue()).isEqualTo("1850–1910");
		// equal sides collapse to a single value
		assertThat(items.get(1).getValue()).isEqualTo("1850");
		assertThat(items.get(2).getValue()).isEqualTo("1. 5. 1850");
		// estimated bound in brackets
		assertThat(items.get(3).getValue()).isEqualTo("[1850]–1910");
		// both bounds fall into the same century - collapses like any equal sides
		assertThat(items.get(4).getValue()).isEqualTo("19. století");
	}

}
