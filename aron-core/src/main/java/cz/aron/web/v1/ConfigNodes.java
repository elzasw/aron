package cz.aron.web.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reading raw YAML nodes of the deployment's configuration files. Shared by the
 * readers of pageTemplate.yaml so a mistake in the file is reported the same way
 * wherever it sits, and so an unknown key is never silently ignored.
 */
final class ConfigNodes {

	private ConfigNodes() {
	}

	/**
	 * One scalar value. Numbers and booleans count as text because YAML makes
	 * them what they look like: a dating bound is written {@code from: 1800}, not
	 * {@code "1800"}. Returns {@code null} for a blank, a mapping or a list.
	 */
	static String text(Object node) {
		if (node == null || node instanceof Map || node instanceof List) {
			return null;
		}
		String value = String.valueOf(node).trim();
		return value.isEmpty() ? null : value;
	}

	/**
	 * A list of mappings; an absent key is an empty list, anything else an error.
	 * {@code where} is relative to pageTemplate.yaml - a reader of another file
	 * uses {@link #mappingsAt(Object, String)} and names its own place whole.
	 */
	static List<Map<?, ?>> mappings(Object node, String where) {
		return mappingsAt(node, "pageTemplate " + where);
	}

	/** As {@link #mappings(Object, String)}, with {@code where} naming the place completely. */
	static List<Map<?, ?>> mappingsAt(Object node, String where) {
		if (node == null) {
			return List.of();
		}
		if (!(node instanceof List<?> entries)) {
			throw new IllegalStateException(where + ": must be a list");
		}
		var mappings = new ArrayList<Map<?, ?>>();
		for (Object entry : entries) {
			if (!(entry instanceof Map<?, ?> mapping)) {
				throw new IllegalStateException(where + ": each item must be a mapping");
			}
			mappings.add(mapping);
		}
		return mappings;
	}

	/** An unknown key is a typo that would otherwise be silently ignored; {@code where} as in {@link #mappings}. */
	static void reject(Collection<?> keys, Set<String> known, String where) {
		rejectAt(keys, known, "pageTemplate " + where);
	}

	/** As {@link #reject}, with {@code where} naming the place completely. */
	static void rejectAt(Collection<?> keys, Set<String> known, String where) {
		for (Object key : keys) {
			if (!known.contains(String.valueOf(key))) {
				throw new IllegalStateException(where + ": unknown key '" + key + "'");
			}
		}
	}

}
