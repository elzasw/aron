package cz.aron.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import cz.aron.api.rest.model.ResultRowItem;
import cz.aron.api.rest.model.ResultRowItemValue;
import cz.aron.api.v1.model.ResultField;
import cz.aron.api.v1.model.ResultValue;

/**
 * The stored-to-contract mapping of a structured result - pure logic, so a plain
 * unit test. The stored shape is what the source system delivers (abbreviated
 * field names, no guarantees about completeness), which is exactly why the
 * mapping has to drop what cannot be rendered.
 */
class StructuredResultMapperTest {

	/** Stands in for the deployment's image resolution; only bare names reach it. */
	private static final UnaryOperator<String> IMAGES = name -> name == null || name.startsWith("http")
			? name
			: "/ctx/api/v1/ui/result-images/" + name;

	@Test
	void mapsRowsFieldsAndReferencedValues() {
		var stored = new cz.aron.api.rest.model.StructuredResult();
		stored.setId("ignored-id");
		stored.setT("A_IB");
		stored.setL(List.of(
				List.of(item("N", value("Kronika obce Testov", null))),
				List.of(item("J_S", value("K-12", null), value("K-12a", null)),
						item("J_F", value("Sbírka kronik", "9f1d0000-0000-4000-8000-90000000000f")))));

		var result = StructuredResultMapper.toApi(stored, IMAGES);

		assertThat(result.getCode()).isEqualTo("A_IB");
		assertThat(result.getRows()).hasSize(2);
		assertThat(result.getRows().get(1).getFields()).extracting(ResultField::getCode)
				.containsExactly("J_S", "J_F");
		assertThat(result.getRows().get(0).getFields().get(0).getValues())
				.extracting(ResultValue::getText, ResultValue::getRefUuid)
				.containsExactly(tuple("Kronika obce Testov", null));
		assertThat(result.getRows().get(1).getFields().get(1).getValues())
				.extracting(ResultValue::getText, ResultValue::getRefUuid)
				.containsExactly(tuple("Sbírka kronik", "9f1d0000-0000-4000-8000-90000000000f"));
	}

	@Test
	void resolvesAThumbnailNameButPassesAnAbsoluteUrlThrough() {
		var named = minimal();
		named.setTn("record.svg");
		named.setTnLink("https://example.org/nahled");
		var mapped = StructuredResultMapper.toApi(named, IMAGES);
		assertThat(mapped.getThumbnailUrl()).isEqualTo("/ctx/api/v1/ui/result-images/record.svg");
		assertThat(mapped.getThumbnailLinkUrl()).isEqualTo("https://example.org/nahled");

		var external = minimal();
		external.setTn("https://images.example.org/x.jp2");
		assertThat(StructuredResultMapper.toApi(external, IMAGES).getThumbnailUrl())
				.isEqualTo("https://images.example.org/x.jp2");
	}

	@Test
	void dropsAThumbnailLinkWithNoThumbnailToShow() {
		// nothing to click on - a link without its image would be invisible
		var stored = minimal();
		stored.setTnLink("https://example.org/nahled");
		var mapped = StructuredResultMapper.toApi(stored, name -> null);
		assertThat(mapped.getThumbnailUrl()).isNull();
		assertThat(mapped.getThumbnailLinkUrl()).isNull();
	}

	@Test
	void skipsWhatCannotBeRendered() {
		// no stored result at all
		assertThat(StructuredResultMapper.toApi(null, IMAGES)).isNull();

		// a result whose rows carry nothing usable is no result: a field without a
		// code or without values would render as a bare prefix
		var empty = new cz.aron.api.rest.model.StructuredResult();
		empty.setT("A_I");
		empty.setL(Arrays.asList(
				List.of(item(null, value("bez kódu", null))),
				List.of(item("J_S")),
				Arrays.asList((ResultRowItem) null)));
		assertThat(StructuredResultMapper.toApi(empty, IMAGES)).isNull();

		// a value without text is skipped, the field's remaining values survive
		var partial = new cz.aron.api.rest.model.StructuredResult();
		partial.setT("A_I");
		partial.setL(List.of(List.of(item("J_S", value(null, "uuid"), value("K-12", null)))));
		assertThat(StructuredResultMapper.toApi(partial, IMAGES).getRows().get(0).getFields().get(0).getValues())
				.extracting(ResultValue::getText).containsExactly("K-12");
	}

	private static cz.aron.api.rest.model.StructuredResult minimal() {
		var stored = new cz.aron.api.rest.model.StructuredResult();
		stored.setT("A_I");
		stored.setL(List.of(List.of(item("N", value("Název", null)))));
		return stored;
	}

	private static ResultRowItem item(String code, ResultRowItemValue... values) {
		var item = new ResultRowItem();
		item.setT(code);
		item.setV(Arrays.asList(values));
		return item;
	}

	private static ResultRowItemValue value(String text, String refUuid) {
		var value = new ResultRowItemValue();
		value.setV(text);
		value.setId(refUuid);
		return value;
	}

}
