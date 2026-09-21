package cz.aron.integration;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Which records of a re-delivered source look different to the records that
 * reference them. A referrer's search document carries the target's label
 * ({@code <code>~LABEL}, {@code rels}), so it has to be rebuilt when the target
 * is <em>new</em> (a reference may have been waiting for it), <em>renamed</em>,
 * or <em>gone</em>. Records delivered again under an unchanged label are not in
 * the set - re-delivering a fund with a hundred thousand descriptions pointing
 * at it must not rebuild them all for nothing.
 */
final class LabelChanges {

	/** What of a record its referrers' documents depend on. */
	record Label(String name, String indexedName) {
	}

	private LabelChanges() {
	}

	static Set<UUID> of(Map<UUID, Label> before, Map<UUID, Label> after) {
		var changed = new HashSet<UUID>();
		for (var entry : after.entrySet()) {
			var previous = before.get(entry.getKey());
			if (previous == null || !Objects.equals(previous, entry.getValue())) {
				changed.add(entry.getKey());
			}
		}
		for (var uuid : before.keySet()) {
			if (!after.containsKey(uuid)) {
				changed.add(uuid);
			}
		}
		return changed;
	}

}
