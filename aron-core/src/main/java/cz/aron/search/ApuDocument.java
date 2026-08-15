package cz.aron.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine-neutral representation of one APU in the search index, produced by
 * {@link ApuDocumentBuilder} and consumed by every search-engine adapter - all
 * engines index identical input.
 * <p>
 * {@link #getValues()} holds the dynamic fields keyed by item-type code (tilde
 * form, e.g. {@code TITLE~MAIN}), values already typed per the types.yaml model:
 * String (STRING/ENUM/APU_REF/LINK), Integer (INTEGER), and for UNITDATE a
 * {@code Map} with {@code gte}/{@code lte} keys plus separate single-element
 * {@code <code>~L} / {@code <code>~H} bound entries. APU_REF items additionally
 * produce {@code <code>~LABEL} and {@code <code>~ID~LABEL} entries. These
 * representations intentionally mirror the historical Elasticsearch document
 * layout so the ES adapter's output stays byte-identical.
 */
public class ApuDocument {

	private String uuid;
	private String name;
	/** Czech collation key of the name (hex) - engine-neutral, index-time sorting. */
	private String nameSort;
	private String description;
	private String type;
	private boolean containsDigitalObjects;
	private long apuSourceId;
	private final List<Rel> rels = new ArrayList<>();
	private final Map<String, List<Object>> values = new HashMap<>();

	/** Outgoing reference to another APU, resolved with its display label. */
	public record Rel(String targetId, String type, List<String> groups, String label, String idLabel) {
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameSort() {
		return nameSort;
	}

	public void setNameSort(String nameSort) {
		this.nameSort = nameSort;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isContainsDigitalObjects() {
		return containsDigitalObjects;
	}

	public void setContainsDigitalObjects(boolean containsDigitalObjects) {
		this.containsDigitalObjects = containsDigitalObjects;
	}

	public long getApuSourceId() {
		return apuSourceId;
	}

	public void setApuSourceId(long apuSourceId) {
		this.apuSourceId = apuSourceId;
	}

	public List<Rel> getRels() {
		return rels;
	}

	public Map<String, List<Object>> getValues() {
		return values;
	}

}
