package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import cz.aron.api.v1.model.FooterColumn;
import cz.aron.api.v1.model.FooterParagraph;
import cz.aron.api.v1.model.HomeFooter;

/**
 * The {@code homepage.footer:} section of pageTemplate.yaml: the band a
 * deployment puts at the foot of its home page - who runs the portal, what it
 * presents, how to reach them.
 *
 * <pre>
 * homepage:
 *   footer:
 *     columns:
 *       - paragraphs:
 *           - text:
 *               cs: "Prezentacni web je webova aplikace {archiv} slouzici ke zpristupneni popisu archivalii."
 *               en: "This portal is a web application of the {archiv}, presenting archival descriptions."
 *             links:
 *               archiv:
 *                 label: { cs: Moravskeho zemskeho archivu v Brne, en: Moravian Provincial Archives }
 *                 url: https://www.mza.cz
 *       - heading: { cs: Kontakt, en: Contact }
 *         links:
 *           - label: badatelna@example.org
 *             url: "mailto:badatelna@example.org"
 * </pre>
 *
 * A sentence carries its links <em>inside</em> it, which is why prose is a run
 * list rather than a string, and why the deployment writes named
 * {@code {placeholder}}s instead of markup: the portal then renders anchors it
 * controls, the accessibility gate keeps covering the whole page, and no
 * sanitizer stands between configuration and reader. Placeholders rather than
 * fixed positions because a link's place in a sentence moves with the target
 * language's grammar - the same reason i18next interpolates.
 * <p>
 * Every placeholder must resolve and every configured link must be used, in
 * every language variant; either way round it is a typo, and it fails the
 * startup. There is no escape for a literal brace - configured prose has never
 * needed one.
 */
final class HomeFooterConfig {

	private static final Set<String> ROOT_KEYS = Set.of("columns");

	private static final Set<String> COLUMN_KEYS = Set.of("heading", "paragraphs", "links");

	private final List<ColumnConfig> columns;

	private record ColumnConfig(ConfiguredText heading, List<ConfiguredParagraph> paragraphs,
			List<FooterLinkConfig> links) {
	}

	private HomeFooterConfig(List<ColumnConfig> columns) {
		this.columns = columns;
	}

	/** Reads the {@code footer:} node; {@code null} when there is no band to show. */
	static HomeFooterConfig parse(Object node, Predicate<String> imageIsServable) {
		if (node == null) {
			return null;
		}
		if (!(node instanceof Map<?, ?> footer)) {
			throw new IllegalStateException("pageTemplate homepage footer: must be a mapping");
		}
		ConfigNodes.reject(footer.keySet(), ROOT_KEYS, "homepage footer");
		var columns = new ArrayList<ColumnConfig>();
		for (Map<?, ?> entry : ConfigNodes.mappings(footer.get("columns"), "homepage footer columns")) {
			columns.add(column(entry, "homepage footer column " + (columns.size() + 1), imageIsServable));
		}
		return columns.isEmpty() ? null : new HomeFooterConfig(columns);
	}

	/** The typed band for one reader's language; image URLs are built per request. */
	HomeFooter render(Locale locale, UnaryOperator<String> imageUrl) {
		var rendered = new ArrayList<FooterColumn>();
		for (ColumnConfig column : columns) {
			var paragraphs = new ArrayList<FooterParagraph>();
			for (ConfiguredParagraph paragraph : column.paragraphs()) {
				paragraphs.add(new FooterParagraph(paragraph.runs(locale)));
			}
			var links = column.links().stream().map(link -> link.render(locale, imageUrl)).toList();
			var footerColumn = new FooterColumn(paragraphs, links);
			if (column.heading() != null) {
				footerColumn.setHeading(column.heading().pick(locale));
			}
			rendered.add(footerColumn);
		}
		return new HomeFooter(rendered);
	}

	private static ColumnConfig column(Map<?, ?> entry, String where, Predicate<String> imageIsServable) {
		ConfigNodes.reject(entry.keySet(), COLUMN_KEYS, where);
		var paragraphs = new ArrayList<ConfiguredParagraph>();
		for (Map<?, ?> paragraph : ConfigNodes.mappings(entry.get("paragraphs"), where + " paragraphs")) {
			paragraphs.add(ConfiguredParagraph.parse(paragraph,
					"pageTemplate " + where + " paragraph " + (paragraphs.size() + 1)));
		}
		var links = FooterLinkConfig.parse(entry.get("links"), where + " links", imageIsServable);
		if (paragraphs.isEmpty() && links.isEmpty()) {
			throw new IllegalStateException("pageTemplate " + where + ": has neither paragraphs nor links");
		}
		return new ColumnConfig(ConfiguredText.of(entry.get("heading")), paragraphs, links);
	}

}
