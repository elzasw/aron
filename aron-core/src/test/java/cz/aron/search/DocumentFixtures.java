package cz.aron.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The shapes {@link ApuDocumentBuilder} produces, for the tests that build an
 * {@link ApuDocument} by hand instead of importing one. Several test classes say
 * their fixtures mirror the builder; this is where that is true, so a change to
 * how a real APU is indexed does not leave them quietly describing the old world.
 */
public final class DocumentFixtures {

	private DocumentFixtures() {
	}

	/**
	 * Completes a fixture's datings: every dating as an interval on the item's own
	 * field - which is what the engines match a range filter against - the hull of
	 * one item type's datings in {@code ~L}/{@code ~H}, and the hull across all of
	 * them in the document's {@code dateL}/{@code dateH}, which order the dating
	 * sorts and bound a slider.
	 *
	 * <p>A fixture may write either end. Bounds are the shorter way to say "one
	 * dating"; the intervals themselves are the only way to say "two datings of
	 * one item type", which is the case that tells hull matching apart from
	 * interval matching. Whichever is given, the other is derived here.
	 */
	public static void addDatings(ApuDocument document) {
		var values = document.getValues();
		for (var entry : new ArrayList<>(values.entrySet())) {
			var intervals = entry.getValue().stream()
					.filter(Map.class::isInstance)
					.map(value -> (Map<?, ?>) value)
					.toList();
			if (intervals.isEmpty()) {
				continue;
			}
			String low = null;
			String high = null;
			for (Map<?, ?> interval : intervals) {
				String from = String.valueOf(interval.get("gte"));
				String to = String.valueOf(interval.get("lte"));
				low = low == null || from.compareTo(low) < 0 ? from : low;
				high = high == null || to.compareTo(high) > 0 ? to : high;
			}
			values.put(entry.getKey() + "~L", List.of(low));
			values.put(entry.getKey() + "~H", List.of(high));
		}
		for (var entry : new ArrayList<>(values.entrySet())) {
			String field = entry.getKey();
			if (!field.endsWith("~L")) {
				continue;
			}
			String code = field.substring(0, field.length() - 2);
			var upper = values.get(code + "~H");
			if (upper == null || values.containsKey(code)) {
				continue;
			}
			values.put(code, List.of(Map.of(
					"gte", String.valueOf(entry.getValue().get(0)),
					"lte", String.valueOf(upper.get(0)))));
		}
		values.forEach((field, itemValues) -> itemValues.forEach(value -> {
			String bound = String.valueOf(value);
			if (field.endsWith("~L") && (document.getDateL() == null || bound.compareTo(document.getDateL()) < 0)) {
				document.setDateL(bound);
			}
			if (field.endsWith("~H") && (document.getDateH() == null || bound.compareTo(document.getDateH()) > 0)) {
				document.setDateH(bound);
			}
		}));
	}

}
