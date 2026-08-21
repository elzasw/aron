package cz.aron.search;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Engine-neutral field-level filter of the search port. Facet semantics (facet
 * code -> field + filter kind, per searchConfig.yaml) are resolved above the
 * port (cz.aron.web.v1), so adapters work with plain index fields.
 */
public sealed interface FieldFilter {

	/**
	 * Reserved field of {@link Range} and of a bounds request meaning "the
	 * record's dating, whichever item type carries it": the document-level hull
	 * of every UNITDATE item, indexed by every adapter as {@code dateL}/
	 * {@code dateH} (see {@code ApuDocument}). Named here rather than in an
	 * adapter because both must agree on it - each maps it to its own bound
	 * field names.
	 *
	 * <p>Being a hull, it is the record's widest dating: a record dated 1800-1810
	 * and again in 1990 spans 1800-1990 and so matches a filter for 1900. That
	 * favours recall, which is the right direction for a facet spanning record
	 * types whose datings are recorded in different item types; per-interval
	 * precision would need a structured range field only one engine has.
	 */
	String ANY_DATING = "~DATE";

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
	 * {@code null} = open bound. {@link #ANY_DATING} filters the record's
	 * document-level dating instead of one item type's.
	 *
	 * <p>{@code includeUndated} adds the documents carrying no dating in that
	 * field at all. They are out by default - an undated record is not in
	 * 1805-1852 - but a result set mixing dated and undated records can hide
	 * many of them for a reason the reader cannot see, so the choice has to be
	 * expressible.
	 */
	record Range(String field, LocalDateTime from, LocalDateTime to, boolean includeUndated) implements FieldFilter {

		/** Strict intersection - undated documents do not match. */
		public Range(String field, LocalDateTime from, LocalDateTime to) {
			this(field, from, to, false);
		}
	}

	/**
	 * Relation to other APUs, matching a document that either
	 * <ul>
	 * <li>references one of {@code targets} through one of {@code refFields}
	 * (the reference item fields the facet spans), or</li>
	 * <li>is itself one of {@code uuids}.</li>
	 * </ul>
	 * The two halves are ORed - one relation, either end of it. Both lists are
	 * resolved above the port: {@code refFields} from the facet's scope,
	 * {@code uuids} by expanding the named APUs' own visible references, so the
	 * adapters need no knowledge of types.yaml or of the reference direction.
	 * Empty on both sides matches nothing.
	 */
	record Related(List<String> refFields, List<String> targets, List<String> uuids) implements FieldFilter {
	}

}
