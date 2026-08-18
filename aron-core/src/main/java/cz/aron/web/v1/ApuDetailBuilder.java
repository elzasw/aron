package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.api.v1.model.ApuLink;
import cz.aron.api.v1.model.DetailItem;
import cz.aron.api.v1.model.DatingPrecision;
import cz.aron.api.v1.model.DetailItemKind;
import cz.aron.api.v1.model.DetailPart;
import cz.aron.api.v1.model.JsonItem;
import cz.aron.api.v1.model.LinkItem;
import cz.aron.api.v1.model.RefItem;
import cz.aron.api.v1.model.TextItem;
import cz.aron.api.v1.model.UnitDateItem;
import cz.aron.api.v1.model.PartViewType;
import cz.aron.domain.UniversalDate;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.types.LocalizedText;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ApuPartType;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.domain.types.dto.ViewType;

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
	public List<DetailPart> buildParts(List<ApuPart> parts, Map<String, IdLabelDto> refLabels, Locale locale) {
		// display order of parts = declaration order of the types.yaml part types
		Map<String, Integer> partOrder = new HashMap<>();
		for (ApuPartType partType : typesHolder.getAllApuPartTypes()) {
			partOrder.put(partType.getCode(), partOrder.size());
		}

		var flattened = new ArrayList<ApuPart>();
		flatten(parts, flattened);

		var result = new ArrayList<DetailPart>();
		for (ApuPart part : flattened) {
			List<DetailItem> items = buildItems(part.getItems(), refLabels, locale);
			if (items.isEmpty()) {
				continue;
			}
			var detailPart = new DetailPart(part.getType(), partLabel(part.getType(), locale), viewType(part.getType()),
					items);
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

	private List<DetailItem> buildItems(List<ApuPartItem> items, Map<String, IdLabelDto> refLabels, Locale locale) {
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
				DetailItem detailItem = buildItem(item, itemType, refLabels, locale);
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

	private DetailItem buildItem(ApuPartItem item, ItemType itemType, Map<String, IdLabelDto> refLabels,
			Locale locale) {
		String value = item.getValue();
		if ((value == null || value.isBlank()) && (item.getHref() == null || item.getHref().isBlank())) {
			return null;
		}
		String code = itemType.getCode();
		String label = itemLabel(itemType, locale);
		switch (itemType.getType()) {
			case STRING, ENUM, INTEGER:
				return new TextItem(value, code, label, DetailItemKind.TEXT);
			case UNITDATE: {
				UniversalDate parsed = parseUnitDate(value);
				String formatted = parsed != null ? UnitDateFormatter.format(parsed, locale) : value;
				if (formatted == null) {
					return null;
				}
				var unitDate = new UnitDateItem(formatted, code, label, DetailItemKind.UNITDATE);
				if (parsed != null) {
					// the machine-readable bounds travel with the rendered string
					unitDate.setFrom(parsed.getFrom());
					unitDate.setTo(parsed.getTo());
					unitDate.setFromPrecision(precision(UnitDateFormatter.precisionOf(parsed, true)));
					unitDate.setToPrecision(precision(UnitDateFormatter.precisionOf(parsed, false)));
					unitDate.setFromEstimated(parsed.isValueFromEstimated());
					unitDate.setToEstimated(parsed.isValueToEstimated());
				}
				return unitDate;
			}
			case LINK: {
				String href = item.getHref() != null && !item.getHref().isBlank() ? item.getHref() : value;
				String caption = value != null && !value.isBlank() ? value : href;
				return new LinkItem(href, caption, code, label, DetailItemKind.LINK);
			}
			case APU_REF: {
				IdLabelDto refLabel = refLabels.get(value);
				String name = refLabel != null && refLabel.name() != null ? refLabel.name() : value;
				return new RefItem(new ApuLink(value, name), code, label, DetailItemKind.REF);
			}
			case JSON:
				return new JsonItem(value, code, label, DetailItemKind.JSON);
			default:
				log.warn("Item type {} has no detail rendering.", itemType.getCode());
				return null;
		}
	}

	private UniversalDate parseUnitDate(String value) {
		try {
			return objectMapper.readValue(value, UniversalDate.class);
		} catch (Exception e) {
			log.warn("Cannot parse UNITDATE value: {}", value, e);
			return null;
		}
	}

	private static DatingPrecision precision(String formatCode) {
		return switch (formatCode) {
			case "C" -> DatingPrecision.CENTURY;
			case "Y" -> DatingPrecision.YEAR;
			case "YM" -> DatingPrecision.MONTH;
			case "D" -> DatingPrecision.DAY;
			default -> DatingPrecision.TIME;
		};
	}

	/**
	 * Labels come from the display model: the translation for the reader's
	 * language, else the source name, else the code.
	 */
	private String partLabel(String code, Locale locale) {
		ApuPartType partType = typesHolder.getApuPartTypeForCode(code);
		if (partType == null) {
			return code;
		}
		return LocalizedText.pick(partType.getLang(), partType.getName() != null ? partType.getName() : code, locale);
	}

	/** Unknown part types display standalone (always visible). */
	private PartViewType viewType(String code) {
		ApuPartType partType = typesHolder.getApuPartTypeForCode(code);
		return partType != null && partType.getViewType() == ViewType.GROUPED
				? PartViewType.GROUPED
				: PartViewType.STANDALONE;
	}

	private static String itemLabel(ItemType itemType, Locale locale) {
		return LocalizedText.pick(itemType.getLang(),
				itemType.getName() != null ? itemType.getName() : itemType.getCode(), locale);
	}

}
