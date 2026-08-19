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
				Locale match = Locale.lookup(Locale.LanguageRange.parse(requested.replace('_', '-')), supported);
				if (match != null) {
					return match;
				}
			} catch (IllegalArgumentException e) {
				log.debug("Unusable lang '{}' - falling back to {}.", requested, getDefault().toLanguageTag());
			}
		}
		return getDefault();
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
