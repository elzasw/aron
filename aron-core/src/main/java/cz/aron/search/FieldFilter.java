package cz.aron.search;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Engine-neutral field-level filter of the search port. Facet semantics (facet
 * code → field + filter kind, per searchConfig.yaml) are resolved above the
 * port (cz.aron.web.v1), so adapters work with plain index fields.
 */
public sealed interface FieldFilter {

	String field();

	/** The document matches ANY of the values (OR); exact keyword semantics. */
	record Values(String field, List<String> values) implements FieldFilter {
	}

	/**
	 * Analyzed text match on one field. Both engines fold diacritics and match
	 * all words with the last one as a prefix; exact word-order semantics are
	 * engine-specific.
	 */
	record Text(String field, String text) implements FieldFilter {
	}

	/**
	 * Interval intersection on a UNITDATE field: matches documents whose
	 * {@code [field~L, field~H]} interval intersects {@code [from, to]};
	 * {@code null} = open bound.
	 */
	record Range(String field, LocalDateTime from, LocalDateTime to) implements FieldFilter {
	}

}
