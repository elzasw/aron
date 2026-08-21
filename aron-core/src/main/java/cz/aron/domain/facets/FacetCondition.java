package cz.aron.domain.facets;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.aron.domain.ApuType;

/**
 * The parsed {@code when} condition of one facet in searchConfig.yaml - when the
 * facet is offered. Two forms, and only these two:
 *
 * <pre>
 * when:
 *   apuType: FUND
 *
 * when:
 *   all:
 *     - apuType: ARCH_DESC
 *     - filter: UNIT_TYPE
 *       value: matrika
 * </pre>
 *
 * The first binds the facet to one section. The second is a conjunction, which
 * exists so that a facet can additionally depend on what the reader has already
 * selected in another facet - the register facets of an archival description are
 * offered once its kind of register is chosen, rather than all seven at once.
 * <p>
 * An unrecognised shape is a **typo that fails the startup**, the rule the
 * deployment's other configuration follows ({@code ConfigNodes}). Silently
 * treating a condition we cannot read as "applies everywhere" is how eight
 * facets of one section came to be advertised for every section, and nothing
 * ever said so.
 * <p>
 * <b>Only the apuType half is evaluated so far.</b> The value conditions are
 * validated - an unreadable one must not pass - and then dropped, because
 * offering a facet on the strength of another facet's selection is not
 * implemented yet: the definitions endpoint is asked per section, without the
 * active filters. Until it is, such a facet is offered throughout its section.
 * Anything added here must be evaluated, or it becomes another silently ignored
 * setting.
 */
public final class FacetCondition {

	private static final String APU_TYPE = "apuType";
	private static final String ALL = "all";
	private static final String FILTER = "filter";
	private static final String VALUE = "value";

	private static final Set<String> WHEN_KEYS = Set.of(APU_TYPE, ALL);
	private static final Set<String> CONDITION_KEYS = Set.of(APU_TYPE, FILTER, VALUE);

	/** No condition at all - the facet belongs to every section. */
	private static final FacetCondition UNCONDITIONAL = new FacetCondition(null);

	/**
	 * Name of the one APU type the facet belongs to, {@code null} = every type.
	 * The name rather than the enum: it is the vocabulary the contract's own
	 * ApuType shares with {@link ApuType}, so the two never have to be converted
	 * into each other. Which names are valid is settled at parse time.
	 */
	private final String apuType;

	private FacetCondition(String apuType) {
		this.apuType = apuType;
	}

	/**
	 * Reads a facet's {@code when} node, rejecting anything the grammar above
	 * does not describe.
	 *
	 * @param when  the raw node ({@code null} = no condition)
	 * @param facet the facet's source code, as the file spells it - so an error
	 *              names something the operator can search for
	 */
	public static FacetCondition parse(Object when, String facet) {
		if (when == null) {
			return UNCONDITIONAL;
		}
		if (!(when instanceof Map<?, ?> node)) {
			throw error(facet, "'when' must be a mapping");
		}
		rejectUnknownKeys(node.keySet(), WHEN_KEYS, facet, "'when'");
		Object all = node.get(ALL);
		if (all != null) {
			if (node.containsKey(APU_TYPE)) {
				throw error(facet, "'when' combines 'apuType' with 'all' - put the apuType inside 'all'");
			}
			return parseAll(all, facet);
		}
		return new FacetCondition(apuTypeName(node.get(APU_TYPE), facet));
	}

	private static FacetCondition parseAll(Object all, String facet) {
		if (!(all instanceof List<?> conditions) || conditions.isEmpty()) {
			throw error(facet, "'when.all' must be a non-empty list of conditions");
		}
		String apuType = null;
		for (Object condition : conditions) {
			if (!(condition instanceof Map<?, ?> mapping)) {
				throw error(facet, "each condition of 'when.all' must be a mapping");
			}
			rejectUnknownKeys(mapping.keySet(), CONDITION_KEYS, facet, "'when.all'");
			boolean names = mapping.containsKey(APU_TYPE);
			boolean filters = mapping.containsKey(FILTER) || mapping.containsKey(VALUE);
			if (names == filters) {
				throw error(facet, "each condition of 'when.all' is either an 'apuType'"
						+ " or a 'filter' with its 'value'");
			}
			if (names) {
				if (apuType != null) {
					throw error(facet, "'when.all' names more than one apuType, so it can never match");
				}
				apuType = apuTypeName(mapping.get(APU_TYPE), facet);
			} else if (text(mapping.get(FILTER)) == null || text(mapping.get(VALUE)) == null) {
				// the value is what the condition compares against, so a filter
				// without one says nothing at all
				throw error(facet, "a 'filter' condition of 'when.all' needs both 'filter' and 'value'");
			}
		}
		return new FacetCondition(apuType);
	}

	/** Whether the facet is offered in this section. */
	public boolean appliesTo(String apuTypeName) {
		return apuType == null || apuType.equals(apuTypeName);
	}

	private static String apuTypeName(Object node, String facet) {
		String name = text(node);
		if (name == null) {
			throw error(facet, "'apuType' has no value");
		}
		try {
			return ApuType.valueOf(name).name();
		} catch (IllegalArgumentException e) {
			// today such a facet matches no section at all and is simply never
			// offered - invisible for a reason nobody can see
			throw error(facet, "unknown apuType '" + name + "'");
		}
	}

	private static void rejectUnknownKeys(Iterable<?> keys, Set<String> known, String facet, String where) {
		for (Object key : keys) {
			if (!known.contains(String.valueOf(key))) {
				throw error(facet, where + ": unknown key '" + key + "'");
			}
		}
	}

	private static String text(Object node) {
		if (node == null || node instanceof Map || node instanceof List) {
			return null;
		}
		String value = String.valueOf(node).trim();
		return value.isEmpty() ? null : value;
	}

	private static IllegalStateException error(String facet, String message) {
		return new IllegalStateException("searchConfig facet '" + facet + "': " + message);
	}

}
