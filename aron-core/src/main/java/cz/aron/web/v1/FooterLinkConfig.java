package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import cz.aron.api.v1.model.FooterLink;
import cz.aron.api.v1.model.FooterLinkCode;

/**
 * One configured footer link, in both places pageTemplate.yaml has them: the
 * frame's {@code footer.links} row, which carries what a deployment is required
 * to publish, and the link list of a home-page footer column. One reader for
 * both, so the same YAML behaves the same wherever it sits.
 *
 * <pre>
 * - code: ACCESSIBILITY
 *   url: https://archiv.example/pristupnost
 * - url: https://archiv.example/kontakt
 *   label: { cs: Kontakt, en: Contact }
 * - url: https://facebook.com/archiv
 *   label: Facebook
 *   image: { name: facebook.svg }
 * </pre>
 *
 * A link needs either a well-known {@code code} - which the UI labels itself, in
 * the reader's language - or a {@code label}. An optional {@code image} adds the
 * mark a reader recognizes the link by; it is a file of the deployment's own
 * image directory rather than a name this portal knows, so a mark that changes
 * (Twitter became X) is a file the deployment swaps, not a release of ours. The
 * label remains the accessible name, so a link is never reduced to a picture.
 * <p>
 * The {@code url} is passed through as written: unlike a tile, which has a
 * second possible meaning (a section search) and so has to be told apart from
 * one, a footer link is always just a link.
 */
record FooterLinkConfig(FooterLinkCode code, ConfiguredText label, String image, String url) {

	private static final Set<String> KEYS = Set.of("code", "label", "image", "url");

	private static final Set<String> IMAGE_KEYS = Set.of("name");

	/** Reads a {@code links:} list; an absent one is no links at all. */
	static List<FooterLinkConfig> parse(Object node, String where, Predicate<String> imageIsServable) {
		var links = new ArrayList<FooterLinkConfig>();
		for (Map<?, ?> entry : ConfigNodes.mappings(node, where)) {
			ConfigNodes.reject(entry.keySet(), KEYS, where);
			String url = ConfigNodes.text(entry.get("url"));
			if (url == null) {
				throw new IllegalStateException("pageTemplate " + where + ": an item has no 'url'");
			}
			FooterLinkCode code = entry.get("code") != null ? code(String.valueOf(entry.get("code")), where) : null;
			ConfiguredText label = ConfiguredText.of(entry.get("label"));
			if (code == null && label == null) {
				throw new IllegalStateException(
						"pageTemplate " + where + ": item '" + url + "' needs a known 'code' or a 'label'");
			}
			links.add(new FooterLinkConfig(code, label, image(entry.get("image"), where, url, imageIsServable), url));
		}
		return links;
	}

	/** The typed link for one reader's language; the image URL is built per request. */
	FooterLink render(Locale locale, UnaryOperator<String> imageUrl) {
		var link = new FooterLink(url);
		if (code != null) {
			link.code(code);
		}
		if (label != null) {
			link.label(label.pick(locale));
		}
		if (image != null) {
			link.imageUrl(imageUrl.apply(image));
		}
		return link;
	}

	private static FooterLinkCode code(String value, String where) {
		try {
			return FooterLinkCode.fromValue(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("pageTemplate " + where + ": unknown link code '" + value + "'", e);
		}
	}

	private static String image(Object node, String where, String url, Predicate<String> imageIsServable) {
		if (node == null) {
			return null;
		}
		if (!(node instanceof Map<?, ?> image)) {
			throw new IllegalStateException(
					"pageTemplate " + where + ": item '" + url + "': 'image' must be a mapping with a 'name'");
		}
		ConfigNodes.reject(image.keySet(), IMAGE_KEYS, where + " item '" + url + "' image");
		String name = ConfigNodes.text(image.get("name"));
		if (name == null) {
			throw new IllegalStateException(
					"pageTemplate " + where + ": item '" + url + "': 'image' has no 'name'");
		}
		if (!imageIsServable.test(name)) {
			throw new IllegalStateException("pageTemplate " + where + ": item '" + url + "': image '" + name
					+ "' is not a readable file of the webResources.resultImages directory");
		}
		return name;
	}

}
