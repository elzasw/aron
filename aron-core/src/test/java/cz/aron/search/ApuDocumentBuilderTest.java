package cz.aron.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.ApuSource;
import cz.aron.domain.ApuType;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.TypesLoader;
import cz.aron.mapper.ApuSerializer;
import cz.aron.mapper.KryoSerializer;

/**
 * Unit tests of the shared APU -> search-document conversion (no engine, no
 * Spring context): field typing per types.yaml, UNITDATE bounds, APU_REF labels
 * and relations, the INT~NAME~INDEX override, and skipping of non-indexed or
 * unknown item types. See doc/search-port.md §3.1.
 */
class ApuDocumentBuilderTest {

	private static ApuDocumentBuilder builder;

	@BeforeAll
	static void setUp() {
		var typesLoader = new TypesLoader(null, "src/test/resources/test-config/types.yaml");
		var typesHolder = new TypesHolder(typesLoader);
		ReflectionTestUtils.invokeMethod(typesHolder, "loadData");
		builder = new ApuDocumentBuilder(typesHolder, new ObjectMapper(), new ContentLocale("cs-CZ"));
	}

	private static ApuPartItem item(String type, String value) {
		var item = new ApuPartItem();
		item.setType(type);
		item.setValue(value);
		return item;
	}

	private static ApuEntity apu(ApuPartItem... items) {
		var part = new ApuPart();
		part.setType("PT~BODY");
		for (var i : items) {
			part.addItemsItem(i);
		}
		var source = new ApuSource();
		source.setId(42L);
		var apu = new ApuEntity();
		apu.setId(1L);
		apu.setUuid(UUID.fromString("11111111-2222-3333-4444-555555555555"));
		apu.setName("Testovací jednotka");
		apu.setDescription("Popis jednotky");
		apu.setType(ApuType.ARCH_DESC);
		apu.setSource(source);
		apu.setData(ApuSerializer.serialize(List.of(part)));
		return apu;
	}

	private static ApuDocument build(ApuEntity apu, Map<String, IdLabelDto> refLabels) {
		return KryoSerializer.doWithKryo(kryo -> builder.build(kryo, apu, refLabels));
	}

	@Test
	void typesValuesPerItemType() {
		var apu = apu(
				item("TITLE~MAIN", "Václav Novák"),
				item("LANG~CODE", "cze"),
				item("CNT~ITEMS", "5"));

		var doc = build(apu, Map.of());

		assertThat(doc.getUuid()).isEqualTo("11111111-2222-3333-4444-555555555555");
		assertThat(doc.getApuSourceId()).isEqualTo(42L);
		assertThat(doc.getName()).isEqualTo("Testovací jednotka");
		assertThat(doc.getDescription()).isEqualTo("Popis jednotky");
		assertThat(doc.getType()).isEqualTo("ARCH_DESC");
		assertThat(doc.isContainsDigitalObjects()).isFalse();
		assertThat(doc.getNameSort()).isNotBlank();
		assertThat(doc.getValues().get("TITLE~MAIN")).containsExactly("Václav Novák");
		assertThat(doc.getValues().get("LANG~CODE")).containsExactly("cze");
		assertThat(doc.getValues().get("CNT~ITEMS")).containsExactly(5);
	}

	@Test
	void indexedNameOverrideWinsOverEntityNames() {
		var apu = apu(item("INT~NAME~INDEX", "Přepsané indexované jméno"));
		apu.setIndexedName("Indexované jméno");

		var doc = build(apu, Map.of());

		assertThat(doc.getName()).isEqualTo("Přepsané indexované jméno");
		// the override item itself is not indexed as a value
		assertThat(doc.getValues()).doesNotContainKey("INT~NAME~INDEX");
	}

	@Test
	void indexedNameFallsBackToEntityIndexedName() {
		var apu = apu(item("TITLE~MAIN", "x"));
		apu.setIndexedName("Indexované jméno");

		assertThat(build(apu, Map.of()).getName()).isEqualTo("Indexované jméno");
	}

	@Test
	void apuRefBuildsRelationAndLabelFields() {
		var target = "99999999-8888-7777-6666-555555555555";
		var apu = apu(item("REL~ENTITY", target));
		var labels = Map.of(target,
				new IdLabelDto(9L, UUID.fromString(target), "Entita Železný", "Entita Zelezny"));

		var doc = build(apu, labels);

		assertThat(doc.getValues().get("REL~ENTITY")).containsExactly(target);
		assertThat(doc.getValues().get("REL~ENTITY~LABEL")).containsExactly("Entita Zelezny");
		assertThat(doc.getValues().get("REL~ENTITY~ID~LABEL")).containsExactly(target + "|Entita Železný");
		// the combined reference-labels scoring field carries the same label
		assertThat(doc.getRefLabels()).containsExactly("Entita Zelezny");
		assertThat(doc.getRels()).hasSize(1);
		var rel = doc.getRels().get(0);
		assertThat(rel.targetId()).isEqualTo(target);
		assertThat(rel.type()).isEqualTo("REL~ENTITY");
		assertThat(rel.groups()).containsExactly("GRP~RELS");
		assertThat(rel.label()).isEqualTo("Entita Zelezny");
		assertThat(rel.idLabel()).isEqualTo(target + "|Entita Železný");
	}

	@Test
	void apuRefWithoutResolvedLabelIndexesOnlyTheId() {
		var target = "99999999-8888-7777-6666-555555555555";
		var doc = build(apu(item("REL~ENTITY", target)), Map.of());

		assertThat(doc.getValues().get("REL~ENTITY")).containsExactly(target);
		assertThat(doc.getValues()).doesNotContainKey("REL~ENTITY~LABEL");
		assertThat(doc.getRels()).isEmpty();
		assertThat(doc.getRefLabels()).isEmpty();
	}

	@Test
	void unitdateProducesRangeAndOverallBounds() {
		// from/to are ISO date-times (UniversalDate compares via LocalDateTime.parse)
		var apu = apu(
				item("UNIT~DATE", "{\"from\":\"1850-01-01T00:00:00\",\"to\":\"1880-12-31T23:59:59\"}"),
				item("UNIT~DATE", "{\"from\":\"1830-01-01T00:00:00\",\"to\":\"1900-12-31T23:59:59\"}"));

		var doc = build(apu, Map.of());

		assertThat(doc.getValues().get("UNIT~DATE")).containsExactly(
				Map.of("gte", "1850-01-01T00:00:00", "lte", "1880-12-31T23:59:59"),
				Map.of("gte", "1830-01-01T00:00:00", "lte", "1900-12-31T23:59:59"));
		// bounds across all date items: lowest from, highest to
		assertThat(doc.getValues().get("UNIT~DATE~L")).containsExactly("1830-01-01T00:00:00");
		assertThat(doc.getValues().get("UNIT~DATE~H")).containsExactly("1900-12-31T23:59:59");
	}

	@Test
	void nameVariantItemsFeedTheVariantFieldsAndAllText() {
		var apu = apu(
				item("NAME~ALT", "Czech Republic"),
				item("NAME~ALT", "Česká republika"),
				item("TITLE~MAIN", "obyčejný titul"));

		var doc = build(apu, Map.of());

		// only items marked nameVariant enter the variant fields
		assertThat(doc.getNameVariants()).containsExactly("Czech Republic", "Česká republika");
		// exact companions come through the shared normalizer
		assertThat(doc.getNameVariantsExact()).containsExactly("czech republic", "česká republika");
		assertThat(doc.getNameVariantsExactFolded()).containsExactly("czech republic", "ceska republika");
		// variants stay part of the general fulltext too
		assertThat(doc.getAllText()).contains("Czech Republic", "Česká republika");
	}

	@Test
	void allTextCollectsEverySearchableValue() {
		var target = "99999999-8888-7777-6666-555555555555";
		var apu = apu(
				item("TITLE~MAIN", "Kronika obce"),
				item("LANG~CODE", "cze"),
				item("CNT~ITEMS", "12"),
				item("NOTE~NOFT", "vyloučeno z fulltextu"),
				item("REL~ENTITY", target),
				item("UNIT~DATE", "{\"from\":\"1850-01-01T00:00:00\",\"to\":\"1910-12-31T23:59:59\"}"));
		apu.setDescription("Popis jednotky");
		var labels = Map.of(target, new IdLabelDto(9L, UUID.fromString(target), "Entita Železný", "Entita Zelezny"));

		var doc = build(apu, labels);

		// item values, the reference label, integers and boundary years as text,
		// plus name and description - fulltext:false stays out (B13, R-3/R-4)
		assertThat(doc.getAllText()).containsExactlyInAnyOrder(
				"Kronika obce", "cze", "12", "Entita Zelezny", "1850", "1910",
				"Testovací jednotka", "Popis jednotky");
	}

	@Test
	void exactNamesAreNormalizedByTheSharedNormalizer() {
		var apu = apu();
		apu.setName("  Václav   NOVÁK  ");

		var doc = build(apu, Map.of());

		assertThat(doc.getNameExact()).isEqualTo("václav novák");
		assertThat(doc.getNameExactFolded()).isEqualTo("vaclav novak");
		// the exact keywords are length-capped (Lucene term limit, keyword hygiene)
		apu.setName("x".repeat(500));
		assertThat(build(apu, Map.of()).getNameExactFolded()).hasSize(200);
	}

	@Test
	void globalDateBoundsSpanAllUnitdateItems() {
		var apu = apu(
				item("UNIT~DATE", "{\"from\":\"1850-01-01T00:00:00\",\"to\":\"1880-12-31T23:59:59\"}"),
				item("UNIT~DATE", "{\"from\":\"1830-01-01T00:00:00\",\"to\":\"1900-12-31T23:59:59\"}"));

		var doc = build(apu, Map.of());

		assertThat(doc.getDateL()).isEqualTo("1830-01-01T00:00:00");
		assertThat(doc.getDateH()).isEqualTo("1900-12-31T23:59:59");
		// no dating = no derived bounds
		assertThat(build(apu(), Map.of()).getDateL()).isNull();
	}

	@Test
	void nameSortCarriesTheContentLocaleKey() {
		// the alphabet itself is ContentLocaleTest's subject; here only the wiring -
		// the builder must sort by the configured locale, not by the raw name
		var apu = apu();
		apu.setName("Chalupa");
		assertThat(build(apu, Map.of()).getNameSort()).isEqualTo(new ContentLocale("cs-CZ").sortKey("Chalupa"));
	}

	@Test
	void notIndexedAndUnknownItemTypesAreSkipped() {
		var apu = apu(
				item("NOTE~SECRET", "must not be indexed"),
				item("NO~SUCH~TYPE", "unknown"),
				item("TITLE~MAIN", "kept"));

		var doc = build(apu, Map.of());

		assertThat(doc.getValues()).doesNotContainKeys("NOTE~SECRET", "NO~SUCH~TYPE");
		assertThat(doc.getValues()).contains(entry("TITLE~MAIN", List.of("kept")));
	}

}
