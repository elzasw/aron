package cz.aron.search;

import java.text.Collator;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.Kryo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.DataType;
import cz.aron.domain.UniversalDate;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.mapper.ApuSerializer;

/**
 * Builds the engine-neutral {@link ApuDocument} from an {@link ApuEntity}: walks
 * the Kryo-serialized parts and types every value according to the types.yaml
 * display model. This is the single conversion shared by all search-engine
 * adapters (see doc/search-port.md).
 */
@Component
public class ApuDocumentBuilder {

	private static final Logger log = LoggerFactory.getLogger(ApuDocumentBuilder.class);

	/** Item-type code whose STRING value overrides the indexed name of the APU. */
	public static final String INDEXED_NAME_OVERRIDE_TYPE = "INT~NAME~INDEX";

	// Collator is not thread-safe; Czech collation (c < h < ch < i) computed at
	// index time keeps sorting engine-neutral (the Elza pattern - ElzaLocale)
	private static final ThreadLocal<Collator> CZECH_COLLATOR = ThreadLocal
			.withInitial(() -> Collator.getInstance(Locale.of("cs", "CZ")));

	private final TypesHolder typesHolder;

	private final ObjectMapper objectMapper;

	public ApuDocumentBuilder(TypesHolder typesHolder, ObjectMapper objectMapper) {
		this.typesHolder = typesHolder;
		this.objectMapper = objectMapper;
	}

	/**
	 * The parts blob is deserialized with the supplied {@link Kryo}, so a caller
	 * converting a whole batch borrows a single instance (see
	 * {@code KryoSerializer.doWithKryo}) instead of one per APU.
	 */
	public ApuDocument build(Kryo kryo, ApuEntity apu, Map<String, IdLabelDto> apuRefLabels) {
		var document = new ApuDocument();
		document.setUuid(apu.getUuid().toString());
		document.setApuSourceId(apu.getSource().getId());
		Map<String, List<Object>> values = document.getValues();

		var indexedName = apu.getIndexedName();
		if (indexedName == null) {
			indexedName = apu.getName();
		}

		for (ApuPart part : ApuSerializer.deserialize(kryo, apu.getData())) {
			for (ApuPartItem item : part.getItems()) {
				String value = item.getValue();
				ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
				if (itemType == null) {
					log.warn("Item type not recognized: " + item.getType());
					continue;
				}
				if (!itemType.isIndexed()) {
					continue;
				}
				Object data;
				switch (itemType.getType()) {
					case STRING:
						if (INDEXED_NAME_OVERRIDE_TYPE.equals(item.getType())) {
							// alternativni hodnota pro indexovani "name"
							indexedName = value;
							continue;
						} else {
							data = value;
						}
						break;
					case ENUM:
						data = value;
						break;
					case INTEGER:
						data = Integer.valueOf(value);
						break;
					case APU_REF:
						data = value;
						break;
					case UNITDATE:
						try {
							UniversalDate universalDate = objectMapper.readValue(value, UniversalDate.class);
							var fromYear = universalDate.getFrom();
							var toYear = universalDate.getTo();
							var r = new LinkedHashMap<String, String>();
							r.put("gte", fromYear);
							r.put("lte", toYear);
							data = r;
							var origL = values.get(itemType.getCode() + "~L");
							if (origL == null || UniversalDate.isLower(fromYear, (String) origL.get(0))) {
								values.put(itemType.getCode() + "~L", Collections.singletonList(fromYear));
							}
							var origH = values.get(itemType.getCode() + "~H");
							if (origH == null || UniversalDate.isHigher(toYear, (String) origH.get(0))) {
								values.put(itemType.getCode() + "~H", Collections.singletonList(toYear));
							}
						} catch (JsonProcessingException e) {
							throw new RuntimeException(e);
						}
						break;
					case LINK:
						data = value;
						break;
					default:
						throw new RuntimeException("Unknown type");
				}
				values.computeIfAbsent(itemType.getCode(), k -> new java.util.ArrayList<>()).add(data);
				if (itemType.getType() == DataType.APU_REF) {
					IdLabelDto refLabel = apuRefLabels.get(value);
					if (refLabel != null && refLabel.name() != null) {
						List<String> itemTypeGroups = typesHolder.getItemGroupsForItemType(item.getType());
						var targetLabel = refLabel.name();
						var indexedLabel = refLabel.indexedName() != null ? refLabel.indexedName() : targetLabel;
						document.getRels().add(new ApuDocument.Rel((String) data, item.getType(), itemTypeGroups,
								indexedLabel, data + "|" + targetLabel));
						values.computeIfAbsent(itemType.getCode() + "~LABEL", k -> new java.util.ArrayList<>())
								.add(indexedLabel);
						values.computeIfAbsent(itemType.getCode() + "~ID~LABEL", k -> new java.util.ArrayList<>())
								.add(data + "|" + targetLabel);
					}
				}
			}
		}

		document.setContainsDigitalObjects(!apu.getDigitalObjects().isEmpty());
		document.setDescription(apu.getDescription());
		document.setName(indexedName);
		document.setNameSort(czechSortKey(indexedName));
		document.setType(apu.getType().toString());
		return document;
	}

	/**
	 * Hex-encoded Czech collation key - hex preserves byte order, so a plain
	 * string/keyword sort of the keys yields correct Czech alphabetical order.
	 */
	static String czechSortKey(String name) {
		if (name == null) {
			return null;
		}
		byte[] key = CZECH_COLLATOR.get().getCollationKey(name).toByteArray();
		return HexFormat.of().formatHex(key);
	}

}
