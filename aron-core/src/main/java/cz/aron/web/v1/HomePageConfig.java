package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import cz.aron.api.v1.model.ApuType;
import cz.aron.api.v1.model.FilterKind;
import cz.aron.api.v1.model.HomePage;
import cz.aron.api.v1.model.HomeTile;
import cz.aron.api.v1.model.LinkTile;
import cz.aron.api.v1.model.RangeFilter;
import cz.aron.api.v1.model.SearchFilter;
import cz.aron.api.v1.model.SearchTile;
import cz.aron.api.v1.model.TextFilter;
import cz.aron.api.v1.model.TileGroup;
import cz.aron.api.v1.model.TileKind;
import cz.aron.api.v1.model.TileStyle;
import cz.aron.api.v1.model.ValuesFilter;
import cz.aron.domain.facets.dto.FacetConfigDto;

/**
 * The {@code homepage:} section of pageTemplate.yaml: the curated entry points a
 * deployment offers a reader who has not typed anything yet. Parsed and
 * validated once at startup, rendered per reader by
 * {@link #render(Locale, UnaryOperator)}.
 *
 * <pre>
 * homepage:
 *   groups:
 *     - label: { cs: Mohlo by vas zajimat, en: You might be interested in }
 *       style: GRID
 *       tiles:
 *         - label: { cs: Matriky, en: Parish registers }
 *           image: { name: matriky.jpg, positionY: 30% }
 *           columnSpan: 2
 *           apuType: ARCH_DESC
 *           filters:
 *             - facet: UNIT_TYPE
 *               values: [matrika]
 *         - label: Porta fontium
 *           url: https://www.portafontium.eu
 * </pre>
 *
 * A tile either leads out of the portal ({@code url}) or into one section's
 * search ({@code apuType} plus optional {@code query}/{@code filters}) - never
 * both, and never neither.
 * <p>
 * Validation fails the startup rather than dropping a tile, because a tile the
 * server cannot honour is worse than a missing one: a filter on a facet the
 * section does not have makes the search endpoint answer 400, so the reader
 * would follow the tile into an error page. Facet codes are written with
 * underscores here, exactly as in searchConfig.yaml; the tilde form the API uses
 * is derived, so the two configuration files stay readable side by side.
 * <p>
 * What cannot be validated is a filter's <em>values</em>: those are data, so a
 * tile may legitimately find nothing in a deployment whose records use other
 * terms.
 */
final class HomePageConfig {

	private static final Set<String> ROOT_KEYS = Set.of("groups", "footer");

	private static final Set<String> GROUP_KEYS = Set.of("label", "style", "tiles");

	private static final Set<String> TILE_KEYS = Set.of("label", "note", "image", "columnSpan", "rowSpan", "url",
			"apuType", "query", "filters");

	private static final Set<String> IMAGE_KEYS = Set.of("name", "positionX", "positionY");

	private static final Set<String> FILTER_KEYS = Set.of("facet", "values", "q", "from", "to");

	/** Grid cells one tile may span; a wider one would break the responsive grid. */
	private static final int MAX_SPAN = 2;

	/** Accepted CSS background-position values - a percentage or a keyword. */
	private static final Pattern POSITION = Pattern
			.compile("-?\\d{1,3}(?:\\.\\d{1,3})?%|left|right|top|bottom|center");

	/** A tile's link leaves the portal, so it carries a scheme; in-app targets are SEARCH tiles. */
	private static final Pattern EXTERNAL_URL = Pattern.compile("(?i)(?:https?://|mailto:)\\S{1,500}");

	private final List<GroupConfig> groups;

	private final HomeFooterConfig footer;

	private record GroupConfig(ConfiguredText label, TileStyle style, List<TileConfig> tiles) {
	}

	private record TileConfig(ConfiguredText label, ConfiguredText note, String image, String positionX,
			String positionY, Integer columnSpan, Integer rowSpan, String url, ApuType apuType, String query,
			List<SearchFilter> filters) {
	}

	private HomePageConfig(List<GroupConfig> groups, HomeFooterConfig footer) {
		this.groups = groups;
		this.footer = footer;
	}

	/**
	 * Reads the {@code homepage:} node. Returns {@code null} when the deployment
	 * configures no groups - the home page is then the search box alone, which is
	 * a legitimate configuration and not a missing one.
	 *
	 * @param facets          the facets the new API serves, for filter validation
	 * @param imageIsServable whether an image name can actually be served from the
	 *                        deployment's image directory
	 */
	static HomePageConfig parse(Object node, FacetScope facets, Predicate<String> imageIsServable) {
		if (node == null) {
			return null;
		}
		if (!(node instanceof Map<?, ?> homepage)) {
			throw new IllegalStateException("pageTemplate homepage: must be a mapping");
		}
		ConfigNodes.reject(homepage.keySet(), ROOT_KEYS, "homepage");
		var groups = new ArrayList<GroupConfig>();
		for (Map<?, ?> entry : ConfigNodes.mappings(homepage.get("groups"), "homepage groups")) {
			groups.add(group(entry, facets, imageIsServable));
		}
		HomeFooterConfig footer = HomeFooterConfig.parse(homepage.get("footer"), imageIsServable);
		return groups.isEmpty() && footer == null ? null : new HomePageConfig(groups, footer);
	}

	/** Typed home page for one reader's language; image URLs are built per request. */
	HomePage render(Locale locale, UnaryOperator<String> imageUrl) {
		var rendered = new ArrayList<TileGroup>();
		for (GroupConfig group : groups) {
			var tiles = new ArrayList<HomeTile>();
			for (TileConfig tile : group.tiles()) {
				tiles.add(render(tile, locale, imageUrl));
			}
			rendered.add(new TileGroup(group.label().pick(locale), group.style(), tiles));
		}
		var page = new HomePage(rendered);
		if (footer != null) {
			page.setFooter(footer.render(locale, imageUrl));
		}
		return page;
	}

	private static HomeTile render(TileConfig tile, Locale locale, UnaryOperator<String> imageUrl) {
		String label = tile.label().pick(locale);
		HomeTile rendered;
		if (tile.url() != null) {
			rendered = new LinkTile(tile.url(), TileKind.LINK, label);
		} else {
			var search = new SearchTile(tile.apuType(), tile.filters(), TileKind.SEARCH, label);
			search.setQuery(tile.query());
			rendered = search;
		}
		if (tile.note() != null) {
			rendered.setNote(tile.note().pick(locale));
		}
		if (tile.image() != null) {
			rendered.setImageUrl(imageUrl.apply(tile.image()));
			rendered.setImagePositionX(tile.positionX());
			rendered.setImagePositionY(tile.positionY());
		}
		rendered.setColumnSpan(tile.columnSpan());
		rendered.setRowSpan(tile.rowSpan());
		return rendered;
	}

	private static GroupConfig group(Map<?, ?> entry, FacetScope facets, Predicate<String> imageIsServable) {
		ConfigNodes.reject(entry.keySet(), GROUP_KEYS, "homepage groups");
		ConfiguredText label = ConfiguredText.of(entry.get("label"));
		if (label == null) {
			throw new IllegalStateException("pageTemplate homepage groups: a group has no 'label'");
		}
		String where = "homepage group '" + label.fallback() + "'";
		TileStyle style = style(entry.get("style"), where);
		var tiles = new ArrayList<TileConfig>();
		for (Map<?, ?> tile : ConfigNodes.mappings(entry.get("tiles"), where + " tiles")) {
			tiles.add(tile(tile, facets, imageIsServable, where));
		}
		if (tiles.isEmpty()) {
			throw new IllegalStateException("pageTemplate " + where + ": no tiles");
		}
		return new GroupConfig(label, style, tiles);
	}

	/** LIST is the default: a plain row of links is right wherever no picture is supplied. */
	private static TileStyle style(Object node, String where) {
		if (node == null) {
			return TileStyle.LIST;
		}
		try {
			return TileStyle.fromValue(String.valueOf(node));
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("pageTemplate " + where + ": unknown style '" + node + "'", e);
		}
	}

	private static TileConfig tile(Map<?, ?> entry, FacetScope facets, Predicate<String> imageIsServable,
			String group) {
		ConfigNodes.reject(entry.keySet(), TILE_KEYS, group + " tiles");
		ConfiguredText label = ConfiguredText.of(entry.get("label"));
		if (label == null) {
			throw new IllegalStateException("pageTemplate " + group + " tiles: a tile has no 'label'");
		}
		String where = "pageTemplate " + group + ", tile '" + label.fallback() + "'";

		String url = ConfigNodes.text(entry.get("url"));
		Object apuTypeNode = entry.get("apuType");
		if ((url == null) == (apuTypeNode == null)) {
			throw new IllegalStateException(
					where + ": needs either 'url' (out of the portal) or 'apuType' (a section search), not both");
		}
		ApuType apuType = null;
		List<SearchFilter> filters = List.of();
		String query = null;
		if (url != null) {
			if (!EXTERNAL_URL.matcher(url).matches()) {
				throw new IllegalStateException(
						where + ": 'url' must be an absolute http(s) or mailto address, not '" + url + "'");
			}
		} else {
			apuType = apuType(apuTypeNode, where);
			query = ConfigNodes.text(entry.get("query"));
			var parsed = new ArrayList<SearchFilter>();
			for (Map<?, ?> filter : ConfigNodes.mappings(entry.get("filters"), where + " filters")) {
				parsed.add(filter(filter, apuType, facets, where));
			}
			filters = List.copyOf(parsed);
		}

		String image = null;
		String positionX = null;
		String positionY = null;
		if (entry.get("image") != null) {
			if (!(entry.get("image") instanceof Map<?, ?> node)) {
				throw new IllegalStateException(where + ": 'image' must be a mapping with a 'name'");
			}
			ConfigNodes.reject(node.keySet(), IMAGE_KEYS, where + " image");
			image = ConfigNodes.text(node.get("name"));
			if (image == null) {
				throw new IllegalStateException(where + ": 'image' has no 'name'");
			}
			if (!imageIsServable.test(image)) {
				throw new IllegalStateException(where + ": image '" + image
						+ "' is not a readable file of the webResources.resultImages directory");
			}
			positionX = position(node.get("positionX"), where);
			positionY = position(node.get("positionY"), where);
		}
		return new TileConfig(label, ConfiguredText.of(entry.get("note")), image, positionX, positionY,
				span(entry.get("columnSpan"), where, "columnSpan"), span(entry.get("rowSpan"), where, "rowSpan"), url,
				apuType, query, filters);
	}

	/**
	 * COLLECTION is rejected: it is the one record type the portal has no section
	 * for, so a tile could not lead anywhere.
	 */
	private static ApuType apuType(Object node, String where) {
		ApuType apuType;
		try {
			apuType = ApuType.fromValue(String.valueOf(node));
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(where + ": unknown apuType '" + node + "'", e);
		}
		if (apuType == ApuType.COLLECTION) {
			throw new IllegalStateException(where + ": apuType COLLECTION has no section of its own to search in");
		}
		return apuType;
	}

	/**
	 * One filter of a tile. The facet decides the filter's kind - the deployment
	 * already declared the type in searchConfig.yaml, so repeating it here would
	 * only be a second chance to disagree with it.
	 */
	private static SearchFilter filter(Map<?, ?> entry, ApuType apuType, FacetScope facets, String where) {
		ConfigNodes.reject(entry.keySet(), FILTER_KEYS, where + " filters");
		String configured = ConfigNodes.text(entry.get("facet"));
		if (configured == null) {
			throw new IllegalStateException(where + ": a filter has no 'facet'");
		}
		String code = configured.replace('_', '~');
		FacetConfigDto facet = facets.facet(apuType, code);
		if (facet == null) {
			throw new IllegalStateException(where + ": '" + configured + "' is not a facet of " + apuType.getValue()
					+ " in searchConfig.yaml");
		}
		String what = where + ", filter '" + configured + "'";
		return switch (facet.getType()) {
			case ENUM, MULTI_REF -> new ValuesFilter(values(entry.get("values"), what), FilterKind.VALUES, code);
			case FULLTEXT, FULLTEXTF -> {
				String q = ConfigNodes.text(entry.get("q"));
				if (q == null) {
					throw new IllegalStateException(what + " is a text facet and needs a 'q'");
				}
				yield new TextFilter(q, FilterKind.TEXT, code);
			}
			case UNITDATE -> {
				String from = ConfigNodes.text(entry.get("from"));
				String to = ConfigNodes.text(entry.get("to"));
				if (from == null && to == null) {
					throw new IllegalStateException(what + " is a dating facet and needs a 'from' or a 'to'");
				}
				var range = new RangeFilter(FilterKind.RANGE, code);
				range.setFrom(from);
				range.setTo(to);
				yield range;
			}
			default -> throw new IllegalStateException(what + " has a type no tile can filter on");
		};
	}

	private static List<String> values(Object node, String what) {
		if (!(node instanceof List<?> entries) || entries.isEmpty()) {
			throw new IllegalStateException(what + " is a value facet and needs a non-empty 'values' list");
		}
		return entries.stream().map(String::valueOf).toList();
	}

	private static String position(Object node, String where) {
		String value = ConfigNodes.text(node);
		if (value == null) {
			return null;
		}
		if (!POSITION.matcher(value).matches()) {
			throw new IllegalStateException(
					where + ": image position '" + value + "' is not a percentage or a CSS position keyword");
		}
		return value;
	}

	private static Integer span(Object node, String where, String key) {
		if (node == null) {
			return null;
		}
		if (!(node instanceof Number number) || number.intValue() < 1 || number.intValue() > MAX_SPAN) {
			throw new IllegalStateException(where + ": '" + key + "' must be 1 to " + MAX_SPAN + ", not '" + node + "'");
		}
		return number.intValue();
	}

}
