package cz.aron.search;

import java.text.Collator;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Language of the described material, configured by
 * {@code search.content-locale} (an IETF language tag, default {@code cs-CZ}):
 * its alphabet orders name-sorted results. The platform is configuration-driven,
 * so the ordering rules are a deployment setting, not a compiled-in
 * specialization.
 * <p>
 * This is deliberately <em>not</em> the visitor's locale. A sorted list has one
 * comparator, and the alphabet that belongs to it is the catalogue's, not the
 * reader's - a German visitor browsing Czech fonds still finds Chalupa filed
 * after Hrad, as in the archive's own finding aids. The visitor's locale governs
 * presentation (UI strings, date formats) instead.
 * <p>
 * The locale is materialized into the index as the {@code nameSort} collation
 * key, so it is a property of the schema: {@link SearchIndexManager} folds the
 * language tag into the schema fingerprint and a changed value rebuilds and
 * reindexes at startup. It could therefore not be a per-request choice even if
 * it should be one.
 */
@Component
public class ContentLocale {

	private static final Logger log = LoggerFactory.getLogger(ContentLocale.class);

	private final Locale locale;

	// Collator is not thread-safe, hence one instance per thread
	private final ThreadLocal<Collator> collator;

	public ContentLocale(@Value("${search.content-locale:cs-CZ}") String languageTag) {
		this.locale = parse(languageTag);
		this.collator = ThreadLocal.withInitial(() -> Collator.getInstance(locale));
		boolean collationAvailable = Arrays.stream(Collator.getAvailableLocales())
				.anyMatch(available -> available.getLanguage().equals(locale.getLanguage()));
		if (collationAvailable) {
			log.info("Search locale {} - names are sorted by its alphabet.", locale.toLanguageTag());
		} else {
			log.warn("Search locale {} has no collation rules in this JVM - names fall back to the root alphabet.",
					locale.toLanguageTag());
		}
	}

	/**
	 * Hex-encoded collation key of the configured locale - hex preserves byte
	 * order, so a plain string/keyword sort of the keys yields correct
	 * alphabetical order in any engine (Czech {@code c < h < ch < i} included).
	 * Computed at index time, which keeps sorting engine-neutral (the Elza
	 * pattern - ElzaLocale).
	 */
	public String sortKey(String name) {
		if (name == null) {
			return null;
		}
		byte[] key = collator.get().getCollationKey(name).toByteArray();
		return HexFormat.of().formatHex(key);
	}

	public Locale getLocale() {
		return locale;
	}

	/** Canonical form of the configured tag - the schema fingerprint is computed from it. */
	public String getLanguageTag() {
		return locale.toLanguageTag();
	}

	private static Locale parse(String languageTag) {
		Locale parsed = Locale.forLanguageTag(languageTag.trim());
		if (parsed.getLanguage().isEmpty()) {
			// configuration errors must surface at startup, not silently degrade
			throw new IllegalStateException("search.content-locale: '" + languageTag + "' is not a valid language tag");
		}
		return parsed;
	}

}
