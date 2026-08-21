package cz.aron.web.v1;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import cz.aron.domain.facets.dto.DisplayType;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.FacetType;
import cz.aron.search.FieldFilter;

/**
 * The facets of the general search - the one spanning every record type, where a
 * deployment's section facets cannot apply because their item types belong to a
 * section. They are therefore built into the product rather than configured: a
 * portal offers them without a line of searchConfig.yaml, the way
 * {@link #RELATED_FACET} always has.
 * <p>
 * Their codes are reserved and tilde-prefixed. A leading tilde cannot occur in
 * an item-type code, so a reserved code can never collide with a facet a
 * deployment configures - which is what lets both kinds share one filter
 * vocabulary and one validation path.
 * <p>
 * A facet's code and its index field are separate here, the one thing the
 * configured facets never need: {@link #TYPE_FACET} filters and counts the document's
 * {@code type} field and {@link #DATE_FACET} the record's document-level dating
 * ({@link FieldFilter#ANY_DATING}), neither of which is an item-type code.
 * <p>
 * Their display text lives in the {@code builtinfacets} bundles (English base,
 * one file per further language, looked up fallback-free so the output never
 * depends on the server's own locale) rather than in deployment configuration -
 * a built-in facet is the product's own vocabulary. Adding a language is a
 * properties file, not code.
 */
@Component
public class BuiltInFacets {

	/** The record's dating, whichever item type carries it. */
	public static final String DATE_FACET = FieldFilter.ANY_DATING;

	/** The record's type - narrowing without leaving the general search. */
	public static final String TYPE_FACET = "~TYPE";

	/**
	 * Relation to named APUs, spanning every reference item type. Older than the
	 * other two and accepted in a section search as well, because a record page's
	 * "find related" action leads there.
	 */
	public static final String RELATED_FACET = "~RELATED";

	/** The index field {@link #TYPE_FACET} filters and enumerates. */
	static final String TYPE_FIELD = "type";

	private static final String BUNDLE = "cz/aron/web/v1/builtinfacets";

	private static final ResourceBundle.Control NO_FALLBACK = ResourceBundle.Control
			.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

	/**
	 * One built-in facet.
	 *
	 * @param code       reserved facet code, as clients send it in a filter
	 * @param type       facet kind, in the configuration vocabulary the rest of
	 *                   the controller already switches on
	 * @param indexField the field the engines filter and aggregate on
	 * @param enumerable whether its values can be counted into buckets.
	 *                   {@link #RELATED_FACET} cannot: enumerating the targets of every
	 *                   reference type at once is a union no engine answers
	 *                   cheaply, so its options come from searching records by
	 *                   name instead
	 * @param bundleKey  prefix of its texts in the {@code builtinfacets} bundle
	 */
	record Definition(String code, FacetType type, String indexField, boolean enumerable, String bundleKey) {
	}

	/** In offering order - the order a client renders them in. */
	private static final List<Definition> DEFINITIONS = List.of(
			new Definition(TYPE_FACET, FacetType.ENUM, TYPE_FIELD, true, "type"),
			new Definition(DATE_FACET, FacetType.UNITDATE, DATE_FACET, false, "date"),
			new Definition(RELATED_FACET, FacetType.MULTI_REF, RELATED_FACET, false, "related"));

	private static final Map<String, Definition> BY_CODE = DEFINITIONS.stream()
			.collect(LinkedHashMap::new, (map, def) -> map.put(def.code(), def), Map::putAll);

	/**
	 * The general search's facets, as configuration DTOs so that filter
	 * validation, aggregation requests and result mapping stay the one code path
	 * shared with the configured facets.
	 */
	public List<FacetConfigDto> facets() {
		return DEFINITIONS.stream().map(BuiltInFacets::toConfig).toList();
	}

	/** The built-in facet of this code, or {@code null} when the code is not reserved. */
	Definition definition(String code) {
		return BY_CODE.get(code);
	}

	/**
	 * The field the engines use for a facet code - the built-in's own field, or
	 * the code itself, which for a configured facet is its item-type code.
	 */
	String indexField(String code) {
		Definition definition = BY_CODE.get(code);
		return definition != null ? definition.indexField() : code;
	}

	/** Whether the facet's values can be counted into buckets. */
	boolean isEnumerable(String code) {
		Definition definition = BY_CODE.get(code);
		return definition == null || definition.enumerable();
	}

	/** Display label of a built-in facet; {@code null} when the code is not reserved. */
	String label(String code, Locale locale) {
		return text(code, "label", locale);
	}

	/** Description of a built-in facet, where it has one; {@code null} otherwise. */
	String description(String code, Locale locale) {
		return text(code, "description", locale);
	}

	private String text(String code, String suffix, Locale locale) {
		Definition definition = BY_CODE.get(code);
		return definition != null ? value(BUNDLE, definition.bundleKey() + "." + suffix, locale) : null;
	}

	private static String value(String bundle, String key, Locale locale) {
		ResourceBundle texts = ResourceBundle.getBundle(bundle, locale, NO_FALLBACK);
		return texts.containsKey(key) ? texts.getString(key) : null;
	}

	private static FacetConfigDto toConfig(Definition definition) {
		var config = new FacetConfigDto();
		config.setSource(definition.code());
		config.setType(definition.type());
		config.setDisplay(DisplayType.ALWAYS);
		return config;
	}

}
