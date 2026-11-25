package cz.aron.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;

class ApuSerializerTest {

	@Test
	void roundTripPreservesNestedPartsAndItems() {
		ApuPartItem item = new ApuPartItem();
		item.setId("1");
		item.setType("INT~NAME");
		item.setValue("hello");
		item.setVisible(true);
		item.setHref("http://example.test");

		ApuPart child = new ApuPart();
		child.setId("2");
		child.setType("CHILD~PART");
		child.setValue("child");

		ApuPart part = new ApuPart();
		part.setId("3");
		part.setType("ROOT~PART");
		part.setValue("root");
		part.getItems().add(item);
		part.getChildParts().add(child);

		byte[] data = ApuSerializer.serialize(List.of(part));
		assertTrue(data != null && data.length > 0);

		List<ApuPart> restored = ApuSerializer.deserialize(data);
		assertEquals(1, restored.size());

		ApuPart rp = restored.get(0);
		assertEquals("3", rp.getId());
		assertEquals("ROOT~PART", rp.getType());
		assertEquals("root", rp.getValue());
		assertEquals(1, rp.getChildParts().size());
		assertEquals("2", rp.getChildParts().get(0).getId());

		assertEquals(1, rp.getItems().size());
		ApuPartItem ri = rp.getItems().get(0);
		assertEquals("1", ri.getId());
		assertEquals("INT~NAME", ri.getType());
		assertEquals("hello", ri.getValue());
		assertEquals(Boolean.TRUE, ri.getVisible());
		assertEquals("http://example.test", ri.getHref());
		// label is a runtime-only concern, never populated at serialize time
		assertNull(ri.getLabel());
	}

	@Test
	void emptyAndNullAreHandled() {
		assertNull(ApuSerializer.serialize(null));
		assertNull(ApuSerializer.serialize(List.of()));
		assertTrue(ApuSerializer.deserialize(null).isEmpty());
		assertTrue(ApuSerializer.deserialize(new byte[0]).isEmpty());
	}

}
