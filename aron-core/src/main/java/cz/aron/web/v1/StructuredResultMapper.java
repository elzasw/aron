package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import cz.aron.api.rest.model.ResultRowItem;
import cz.aron.api.rest.model.ResultRowItemValue;
import cz.aron.api.v1.model.ResultField;
import cz.aron.api.v1.model.ResultRow;
import cz.aron.api.v1.model.ResultValue;
import cz.aron.api.v1.model.StructuredResult;

/**
 * The single reader of the stored structured result: maps the persisted
 * {@link cz.aron.api.rest.model.StructuredResult} - the abbreviated exchange
 * shape the source system delivers, Kryo-encoded in {@code ApuEntity#result} -
 * to the {@code /api/v1} model.
 * <p>
 * Nothing else in the new API touches the stored shape. That keeps the abbreviated
 * field names ({@code t}, {@code l}, {@code v}, {@code tn}) out of the new
 * contract, and keeps the persisted bytes unchanged: the mapping happens at read
 * time, so no data migration is involved.
 */
public final class StructuredResultMapper {

	private StructuredResultMapper() {
	}

	/**
	 * Maps one stored result. {@code thumbnailUrl} resolves a thumbnail that
	 * arrives as a deployment image name rather than a URL (see
	 * {@link DeploymentImages#thumbnailUrl(String)}).
	 * <p>
	 * The stored {@code id} is deliberately dropped: the search hit already
	 * carries the record's uuid, and a stored id disagreeing with it is a data
	 * defect rather than a second identity.
	 */
	public static StructuredResult toApi(cz.aron.api.rest.model.StructuredResult stored,
			UnaryOperator<String> thumbnailUrl) {
		if (stored == null) {
			return null;
		}
		var rows = new ArrayList<ResultRow>();
		for (List<ResultRowItem> storedRow : nullToEmpty(stored.getL())) {
			var fields = new ArrayList<ResultField>();
			for (ResultRowItem storedField : nullToEmpty(storedRow)) {
				if (storedField == null || storedField.getT() == null) {
					continue;
				}
				var values = new ArrayList<ResultValue>();
				for (ResultRowItemValue storedValue : nullToEmpty(storedField.getV())) {
					if (storedValue == null || storedValue.getV() == null) {
						continue;
					}
					var value = new ResultValue(storedValue.getV());
					value.setRefUuid(storedValue.getId());
					values.add(value);
				}
				// a field with no usable value would render as a bare prefix
				if (!values.isEmpty()) {
					fields.add(new ResultField(storedField.getT(), values));
				}
			}
			if (!fields.isEmpty()) {
				rows.add(new ResultRow(fields));
			}
		}
		if (rows.isEmpty()) {
			// nothing to lay out - the caller falls back to name + description
			return null;
		}
		var result = new StructuredResult(stored.getT() != null ? stored.getT() : "", rows);
		result.setThumbnailUrl(thumbnailUrl.apply(stored.getTn()));
		if (result.getThumbnailUrl() != null) {
			result.setThumbnailLinkUrl(blankToNull(stored.getTnLink()));
		}
		return result;
	}

	private static <T> List<T> nullToEmpty(List<T> values) {
		return values != null ? values : List.of();
	}

	private static String blankToNull(String value) {
		return value != null && !value.isBlank() ? value : null;
	}

}
