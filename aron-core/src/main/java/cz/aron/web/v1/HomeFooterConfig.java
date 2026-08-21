package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.aron.api.v1.model.FooterColumn;
import cz.aron.api.v1.model.FooterParagraph;
import cz.aron.api.v1.model.HomeFooter;
import cz.aron.api.v1.model.TextRun;

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

	private static final Set<String> PARAGRAPH_KEYS = Set.of("text", "links");

	private static final Set<String> LINK_KEYS = Set.of("label", "url");

	/** A placeholder and its name; anything else between braces is a mistake. */
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_-]{1,40})\\}");

	private final List<ColumnConfig> columns;

	private record ColumnConfig(ConfiguredText heading, List<ParagraphConfig> paragraphs,
			List<FooterLinkConfig> links) {
	}

	/** One paragraph: the sentence per language, plus the links its placeholders name. */
	private record ParagraphConfig(ConfiguredText text, Map<String, InlineLink> links) {
	}

	private record InlineLink(ConfiguredText label, String url) {
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
			for (ParagraphConfig paragraph : column.paragraphs()) {
				paragraphs.add(new FooterParagraph(runs(paragraph, locale)));
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

	/**
	 * Splits one paragraph into runs for this reader. Cannot fail: every language
	 * variant was resolved against the same link map at startup.
	 */
	private static List<TextRun> runs(ParagraphConfig paragraph, Locale locale) {
		var runs = new ArrayList<TextRun>();
		String text = paragraph.text().pick(locale);
		Matcher placeholder = PLACEHOLDER.matcher(text);
		int from = 0;
		while (placeholder.find()) {
			if (placeholder.start() > from) {
				runs.add(new TextRun(text.substring(from, placeholder.start())));
			}
			InlineLink link = paragraph.links().get(placeholder.group(1));
			runs.add(new TextRun(link.label().pick(locale)).url(link.url()));
			from = placeholder.end();
		}
		if (from < text.length()) {
			runs.add(new TextRun(text.substring(from)));
		}
		return runs;
	}

	private static ColumnConfig column(Map<?, ?> entry, String where, Predicate<String> imageIsServable) {
		ConfigNodes.reject(entry.keySet(), COLUMN_KEYS, where);
		var paragraphs = new ArrayList<ParagraphConfig>();
		for (Map<?, ?> paragraph : ConfigNodes.mappings(entry.get("paragraphs"), where + " paragraphs")) {
			paragraphs.add(paragraph(paragraph, where + " paragraph " + (paragraphs.size() + 1)));
		}
		var links = FooterLinkConfig.parse(entry.get("links"), where + " links", imageIsServable);
		if (paragraphs.isEmpty() && links.isEmpty()) {
			throw new IllegalStateException("pageTemplate " + where + ": has neither paragraphs nor links");
		}
		return new ColumnConfig(ConfiguredText.of(entry.get("heading")), paragraphs, links);
	}

	private static ParagraphConfig paragraph(Map<?, ?> entry, String where) {
		ConfigNodes.reject(entry.keySet(), PARAGRAPH_KEYS, where);
		ConfiguredText text = ConfiguredText.of(entry.get("text"));
		if (text == null) {
			throw new IllegalStateException("pageTemplate " + where + ": has no 'text'");
		}
		var links = new LinkedHashMap<String, InlineLink>();
		if (entry.get("links") != null) {
			if (!(entry.get("links") instanceof Map<?, ?> configured)) {
				throw new IllegalStateException(
						"pageTemplate " + where + ": 'links' must be a mapping of placeholder name to link");
			}
			configured.forEach((name, link) -> links.put(String.valueOf(name), inlineLink(link, where, name)));
		}
		checkPlaceholders(text, links.keySet(), where);
		return new ParagraphConfig(text, links);
	}

	private static InlineLink inlineLink(Object node, String where, Object name) {
		if (!(node instanceof Map<?, ?> link)) {
			throw new IllegalStateException(
					"pageTemplate " + where + ": link '" + name + "' must be a mapping with 'label' and 'url'");
		}
		ConfigNodes.reject(link.keySet(), LINK_KEYS, where + " link '" + name + "'");
		ConfiguredText label = ConfiguredText.of(link.get("label"));
		String url = ConfigNodes.text(link.get("url"));
		if (label == null || url == null) {
			throw new IllegalStateException(
					"pageTemplate " + where + ": link '" + name + "' needs both a 'label' and a 'url'");
		}
		return new InlineLink(label, url);
	}

	/**
	 * Every variant of the sentence is checked, because the reader's language
	 * decides which one is rendered - a placeholder that only the English text
	 * gets wrong would otherwise surface as a broken English page.
	 */
	private static void checkPlaceholders(ConfiguredText text, Set<String> configured, String where) {
		for (String variant : text.variants()) {
			var used = new LinkedHashSet<String>();
			Matcher placeholder = PLACEHOLDER.matcher(variant);
			int from = 0;
			while (placeholder.find()) {
				checkNoStrayBrace(variant.substring(from, placeholder.start()), where);
				String name = placeholder.group(1);
				if (!configured.contains(name)) {
					throw new IllegalStateException(
							"pageTemplate " + where + ": '{" + name + "}' has no link of that name");
				}
				used.add(name);
				from = placeholder.end();
			}
			checkNoStrayBrace(variant.substring(from), where);
			// per variant, not across them: a link the English sentence forgets is
			// a link the English reader never gets, however right the Czech one is
			for (String name : configured) {
				if (!used.contains(name)) {
					throw new IllegalStateException("pageTemplate " + where + ": link '" + name
							+ "' is not used in every language of the text");
				}
			}
		}
	}

	private static void checkNoStrayBrace(String literal, String where) {
		if (literal.indexOf('{') >= 0 || literal.indexOf('}') >= 0) {
			throw new IllegalStateException(
					"pageTemplate " + where + ": stray brace in the text - a placeholder is written {name}");
		}
	}

}
