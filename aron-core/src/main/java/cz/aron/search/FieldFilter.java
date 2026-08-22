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
	 * Reserved field of a bounds request meaning "the record's dating, whichever
	 * item type carries it": the document-level hull of every UNITDATE item,
	 * indexed by every adapter as {@code dateL}/{@code dateH} (see
	 * {@code ApuDocument}). Named here rather than in an adapter because both
	 * must agree on it - each maps it to its own bound field names.
	 *
	 * <p>A hull is what a slider's ends want - the span the matching records
	 * cover. It is deliberately NOT what {@link Range} matches on: see there.
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
	 * Dating intersection: matches a document one of whose datings in one of
	 * {@code fields} overlaps {@code [from, to]}; {@code null} = open bound.
	 *
	 * <p>Per interval, not per record. A record dated 1850-1860 and again in 1600
	 * does not match 1700 - the two datings are separate facts about it, and
	 * collapsing them into one 1600-1860 span invents a period the record was
	 * never assigned. The engines therefore match the intervals themselves
	 * (Elasticsearch a {@code date_range} field, Lucene a multi-valued
	 * {@code LongRange}), which is also what the old portal has always done on
	 * Elasticsearch.
	 *
	 * <p>Several {@code fields} mean "a dating of any of these item types", which
	 * is how one facet dates records whose datings live in different item types.
	 * The list is resolved above the port, from the display model.
	 *
	 * <p>{@code includeUndated} adds the documents carrying no such dating at
	 * all. They are out by default - an undated record is not in 1805-1852 - but
	 * a result set mixing dated and undated records can hide many of them for a
	 * reason the reader cannot see, so the choice has to be expressible.
	 */
	record Range(List<String> fields, LocalDateTime from, LocalDateTime to, boolean includeUndated)
			implements FieldFilter {

		/** One item type's datings, strictly (undated documents do not match). */
		public Range(String field, LocalDateTime from, LocalDateTime to) {
			this(List.of(field), from, to, false);
		}

		/** One item type's datings. */
		public Range(String field, LocalDateTime from, LocalDateTime to, boolean includeUndated) {
			this(List.of(field), from, to, includeUndated);
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
