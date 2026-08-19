package cz.aron.web.v1;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Presentation languages of the deployment - those declared as
 * {@code localizations} in pageTemplate.yaml - and the negotiation of one
 * request's {@code lang} against them.
 * <p>
 * This is the <em>reader's</em> language: it selects the display text a
 * response is rendered in (item and part labels, facet labels, formatted
 * datings). It is not {@link cz.aron.search.ContentLocale}, which is the
 * language of the described material and decides alphabetical order for every
 * reader alike.
 * <p>
 * Matching ignores regions in both directions: a requested {@code en-GB} is
 * served by a configured {@code en}, and a requested {@code en} by a configured
 * {@code en_US} (see {@link #byLanguage(List)}).
 * <p>
 * An unusable {@code lang} - malformed, or matching no configured localization
 * - falls back to the first configured one rather than failing the request: a
 * reader following someone else's link should still get the page.
 */
@Component
public class PresentationLocales {

	private static final Logger log = LoggerFactory.getLogger(PresentationLocales.class);

	private final List<Locale> supported;

	public PresentationLocales(UiConfigLoader uiConfigLoader) {
		this.supported = uiConfigLoader.getLocalizations().stream()
				.map(PresentationLocales::parse)
				.filter(locale -> locale != null)
				.toList();
		if (supported.isEmpty()) {
			throw new IllegalStateException(
					"pageTemplate localizations: no usable language tag among "
							+ uiConfigLoader.getLocalizations());
		}
		log.info("Presentation languages {}, default {}.", supported, getDefault().toLanguageTag());
	}

	/**
	 * Language to render a response in. Accepts a single tag ({@code cs-CZ}) or
	 * an {@code Accept-Language}-style list with quality values.
	 */
	public Locale resolve(String requested) {
		if (requested != null && !requested.isBlank()) {
			try {
				// RFC 4647 lookup: cs-CZ matches a configured cs, and q-values are honoured
				List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(requested.replace('_', '-'));
				Locale match = Locale.lookup(ranges, supported);
				if (match != null) {
					return match;
				}
				match = byLanguage(ranges);
				if (match != null) {
					return match;
				}
			} catch (IllegalArgumentException e) {
				log.debug("Unusable lang '{}' - falling back to {}.", requested, getDefault().toLanguageTag());
			}
		}
		return getDefault();
	}

	/**
	 * Match on the language alone, for the direction RFC 4647 lookup does not
	 * cover: it truncates the <em>range</em> and never the tag, so {@code en}
	 * does not find a deployment that configured {@code en_US} - while
	 * {@code en-GB} does find a configured {@code en}.
	 * <p>
	 * Both directions have to work, because the two sides are written by
	 * different people for different reasons: a deployment names its
	 * localizations with the region it thinks in, and the reader's language is
	 * chosen per language (the UI's switcher and its string bundles have no
	 * regions at all). Without this, an English reader of a deployment that
	 * wrote {@code en_US} silently got every server-rendered label in the
	 * default language.
	 * <p>
	 * Ranges come from {@code parse} in descending priority, so an
	 * {@code Accept-Language} list keeps its order here too.
	 */
	private Locale byLanguage(List<Locale.LanguageRange> ranges) {
		for (Locale.LanguageRange range : ranges) {
			String language = range.getRange().split("-")[0];
			if (language.isEmpty() || "*".equals(language)) {
				continue;
			}
			for (Locale candidate : supported) {
				if (candidate.getLanguage().equalsIgnoreCase(language)) {
					return candidate;
				}
			}
		}
		return null;
	}

	/** The deployment's first configured localization. */
	public Locale getDefault() {
		return supported.get(0);
	}

	public List<Locale> getSupported() {
		return supported;
	}

	private static Locale parse(String tag) {
		// localizations are written IETF-like but with an underscore (cs_CZ)
		Locale locale = Locale.forLanguageTag(tag.trim().replace('_', '-'));
		if (locale.getLanguage().isEmpty()) {
			log.warn("pageTemplate localizations: '{}' is not a valid language tag - ignored.", tag);
			return null;
		}
		return locale;
	}

}
