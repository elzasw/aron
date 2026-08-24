package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.aron.api.v1.model.TextRun;

/**
 * One configured sentence with links inside it: the deployment writes named
 * {@code {placeholder}}s over a link map instead of markup, and the portal
 * renders anchors it controls - no sanitizer stands between configuration and
 * reader, and a link's place in the sentence moves with the target language's
 * grammar. The model of the home page's footer band, shared with every other
 * configured prose (the digital objects' attribution).
 * <p>
 * Every placeholder must resolve and every configured link must be used, in
 * every language variant; either way round it is a typo, and it fails the
 * startup. There is no escape for a literal brace - configured prose has never
 * needed one.
 */
final class ConfiguredParagraph {

	private static final Set<String> PARAGRAPH_KEYS = Set.of("text", "links");

	private static final Set<String> LINK_KEYS = Set.of("label", "url");

	/** A placeholder and its name; anything else between braces is a mistake. */
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_-]{1,40})\\}");

	private final ConfiguredText text;

	private final Map<String, InlineLink> links;

	private record InlineLink(ConfiguredText label, String url) {
	}

	private ConfiguredParagraph(ConfiguredText text, Map<String, InlineLink> links) {
		this.text = text;
		this.links = links;
	}

	/**
	 * Reads one {@code {text, links}} node; {@code where} names the place in the
	 * configuration for the startup error.
	 */
	static ConfiguredParagraph parse(Map<?, ?> entry, String where) {
		ConfigNodes.rejectAt(entry.keySet(), PARAGRAPH_KEYS, where);
		ConfiguredText text = ConfiguredText.of(entry.get("text"));
		if (text == null) {
			throw new IllegalStateException(where + ": has no 'text'");
		}
		var links = new LinkedHashMap<String, InlineLink>();
		if (entry.get("links") != null) {
			if (!(entry.get("links") instanceof Map<?, ?> configured)) {
				throw new IllegalStateException(
						where + ": 'links' must be a mapping of placeholder name to link");
			}
			configured.forEach((name, link) -> links.put(String.valueOf(name), inlineLink(link, where, name)));
		}
		checkPlaceholders(text, links.keySet(), where);
		return new ConfiguredParagraph(text, links);
	}

	/**
	 * Splits the paragraph into runs for this reader. Cannot fail: every language
	 * variant was resolved against the same link map at startup.
	 */
	List<TextRun> runs(Locale locale) {
		var runs = new ArrayList<TextRun>();
		String text = this.text.pick(locale);
		Matcher placeholder = PLACEHOLDER.matcher(text);
		int from = 0;
		while (placeholder.find()) {
			if (placeholder.start() > from) {
				runs.add(new TextRun(text.substring(from, placeholder.start())));
			}
			InlineLink link = links.get(placeholder.group(1));
			runs.add(new TextRun(link.label().pick(locale)).url(link.url()));
			from = placeholder.end();
		}
		if (from < text.length()) {
			runs.add(new TextRun(text.substring(from)));
		}
		return runs;
	}

	private static InlineLink inlineLink(Object node, String where, Object name) {
		if (!(node instanceof Map<?, ?> link)) {
			throw new IllegalStateException(
					where + ": link '" + name + "' must be a mapping with 'label' and 'url'");
		}
		ConfigNodes.rejectAt(link.keySet(), LINK_KEYS, where + " link '" + name + "'");
		ConfiguredText label = ConfiguredText.of(link.get("label"));
		String url = ConfigNodes.text(link.get("url"));
		if (label == null || url == null) {
			throw new IllegalStateException(
					where + ": link '" + name + "' needs both a 'label' and a 'url'");
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
					throw new IllegalStateException(where + ": '{" + name + "}' has no link of that name");
				}
				used.add(name);
				from = placeholder.end();
			}
			checkNoStrayBrace(variant.substring(from), where);
			// per variant, not across them: a link the English sentence forgets is
			// a link the English reader never gets, however right the Czech one is
			for (String name : configured) {
				if (!used.contains(name)) {
					throw new IllegalStateException(
							where + ": link '" + name + "' is not used in every language of the text");
				}
			}
		}
	}

	private static void checkNoStrayBrace(String literal, String where) {
		if (literal.indexOf('{') >= 0 || literal.indexOf('}') >= 0) {
			throw new IllegalStateException(
					where + ": stray brace in the text - a placeholder is written {name}");
		}
	}

}
