package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.api.v1.model.ApuLink;
import cz.aron.api.v1.model.DetailItem;
import cz.aron.api.v1.model.DetailItemKind;
import cz.aron.api.v1.model.DetailPart;
import cz.aron.domain.UniversalDate;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ApuPartType;
import cz.aron.domain.types.dto.ItemType;

/**
 * Builds the display-ready parts of the APU detail (render model, D-9): parts
 * ordered by the types.yaml display model, items ordered by {@code viewOrder},
 * invisible and empty items filtered out, labels resolved from the display
 * model, APU_REF targets resolved to links, UNITDATE values formatted. Pure
 * logic without I/O - the {@code ApuDocumentBuilder} pattern, unit-tested
 * without an engine or database.
 */
@Component
public class ApuDetailBuilder {

	private static final Logger log = LoggerFactory.getLogger(ApuDetailBuilder.class);

	private final TypesHolder typesHolder;

	private final ObjectMapper objectMapper;

	public ApuDetailBuilder(TypesHolder typesHolder, ObjectMapper objectMapper) {
		this.typesHolder = typesHolder;
		this.objectMapper = objectMapper;
	}

	/**
	 * Display-ready parts of one APU. {@code refLabels} resolves APU_REF values
	 * (uuid strings) to display names; unresolved references fall back to the
	 * uuid so the link still works.
	 */
	public List<DetailPart> buildParts(List<ApuPart> parts, Map<String, IdLabelDto> refLabels) {
		// display order of parts = declaration order of the types.yaml part types
		Map<String, Integer> partOrder = new HashMap<>();
		for (ApuPartType partType : typesHolder.getAllApuPartTypes()) {
			partOrder.put(partType.getCode(), partOrder.size());
		}

		var flattened = new ArrayList<ApuPart>();
		flatten(parts, flattened);

		var result = new ArrayList<DetailPart>();
		for (ApuPart part : flattened) {
			List<DetailItem> items = buildItems(part.getItems(), refLabels);
			if (items.isEmpty()) {
				continue;
			}
			var detailPart = new DetailPart(part.getType(), partLabel(part.getType()), items);
			if (part.getValue() != null && !part.getValue().isBlank()) {
				detailPart.setValue(part.getValue());
			}
			result.add(detailPart);
		}
		result.sort((a, b) -> Integer.compare(
				partOrder.getOrDefault(a.getCode(), Integer.MAX_VALUE),
				partOrder.getOrDefault(b.getCode(), Integer.MAX_VALUE)));
		return result;
	}

	/** Child parts follow their parent (the stored model nests them). */
	private static void flatten(List<ApuPart> parts, List<ApuPart> result) {
		if (parts == null) {
			return;
		}
		for (ApuPart part : parts) {
			result.add(part);
			flatten(part.getChildParts(), result);
		}
	}

	private List<DetailItem> buildItems(List<ApuPartItem> items, Map<String, IdLabelDto> refLabels) {
		record Ordered(int viewOrder, int position, DetailItem item) {
		}
		var ordered = new ArrayList<Ordered>();
		if (items != null) {
			for (ApuPartItem item : items) {
				// the importer defaults visibility to true; null therefore means visible
				if (Boolean.FALSE.equals(item.getVisible())) {
					continue;
				}
				ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
				if (itemType == null) {
					log.warn("Item type not recognized: {}", item.getType());
					continue;
				}
				DetailItem detailItem = buildItem(item, itemType, refLabels);
				if (detailItem != null) {
					ordered.add(new Ordered(itemType.getViewOrder(), ordered.size(), detailItem));
				}
			}
		}
		ordered.sort((a, b) -> a.viewOrder != b.viewOrder
				? Integer.compare(a.viewOrder, b.viewOrder)
				: Integer.compare(a.position, b.position));
		return ordered.stream().map(Ordered::item).toList();
	}

	private DetailItem buildItem(ApuPartItem item, ItemType itemType, Map<String, IdLabelDto> refLabels) {
		String value = item.getValue();
		if ((value == null || value.isBlank()) && (item.getHref() == null || item.getHref().isBlank())) {
			return null;
		}
		String label = itemLabel(itemType);
		switch (itemType.getType()) {
			case STRING, ENUM, INTEGER:
				return new DetailItem(itemType.getCode(), label, DetailItemKind.TEXT, value);
			case UNITDATE: {
				String formatted = formatUnitDate(value);
				return formatted != null
						? new DetailItem(itemType.getCode(), label, DetailItemKind.TEXT, formatted)
						: null;
			}
			case LINK: {
				String href = item.getHref() != null && !item.getHref().isBlank() ? item.getHref() : value;
				String caption = value != null && !value.isBlank() ? value : href;
				var detailItem = new DetailItem(itemType.getCode(), label, DetailItemKind.LINK, caption);
				detailItem.setHref(href);
				return detailItem;
			}
			case APU_REF: {
				IdLabelDto refLabel = refLabels.get(value);
				String name = refLabel != null && refLabel.name() != null ? refLabel.name() : value;
				var detailItem = new DetailItem(itemType.getCode(), label, DetailItemKind.REF, name);
				detailItem.setRef(new ApuLink(value, name));
				return detailItem;
			}
			case JSON:
				return new DetailItem(itemType.getCode(), label, DetailItemKind.JSON, value);
			default:
				log.warn("Item type {} has no detail rendering.", itemType.getCode());
				return null;
		}
	}

	private String formatUnitDate(String value) {
		try {
			return UnitDateFormatter.format(objectMapper.readValue(value, UniversalDate.class));
		} catch (Exception e) {
			log.warn("Cannot parse UNITDATE value: {}", value, e);
			return value;
		}
	}

	/** Labels come from the display model (Czech source names); code is the last resort. */
	private String partLabel(String code) {
		ApuPartType partType = typesHolder.getApuPartTypeForCode(code);
		return partType != null && partType.getName() != null ? partType.getName() : code;
	}

	private static String itemLabel(ItemType itemType) {
		return itemType.getName() != null ? itemType.getName() : itemType.getCode();
	}

}
