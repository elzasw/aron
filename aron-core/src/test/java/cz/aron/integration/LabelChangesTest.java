package cz.aron.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import cz.aron.integration.LabelChanges.Label;

/** Which re-delivered records make their referrers' documents stale - and which do not. */
class LabelChangesTest {

	private static final UUID KEPT = UUID.fromString("00000000-0000-4000-8000-000000000001");
	private static final UUID RENAMED = UUID.fromString("00000000-0000-4000-8000-000000000002");
	private static final UUID GONE = UUID.fromString("00000000-0000-4000-8000-000000000003");
	private static final UUID NEW = UUID.fromString("00000000-0000-4000-8000-000000000004");
	private static final UUID REINDEXED_NAME = UUID.fromString("00000000-0000-4000-8000-000000000005");

	@Test
	void newRenamedAndGoneRecordsChangeTheirLabel() {
		var before = Map.of(
				KEPT, new Label("Fond", null),
				RENAMED, new Label("Stary nazev", null),
				GONE, new Label("Zruseny", null),
				REINDEXED_NAME, new Label("Jmeno", "jmeno"));
		var after = Map.of(
				KEPT, new Label("Fond", null),
				RENAMED, new Label("Novy nazev", null),
				NEW, new Label("Pridany", null),
				// the indexed name is what the referrer's document carries, so it counts as a rename
				REINDEXED_NAME, new Label("Jmeno", "jmeno-2"));

		assertThat(LabelChanges.of(before, after)).containsExactlyInAnyOrder(RENAMED, GONE, NEW, REINDEXED_NAME);
	}

	@Test
	void anUnchangedDeliveryChangesNothing() {
		var labels = Map.of(KEPT, new Label("Fond", null), REINDEXED_NAME, new Label("Jmeno", "jmeno"));

		assertThat(LabelChanges.of(labels, Map.copyOf(labels))).isEmpty();
	}

	@Test
	void aFirstDeliveryIsAllNew() {
		var after = Map.of(NEW, new Label("Pridany", null));

		assertThat(LabelChanges.of(Map.of(), after)).containsExactly(NEW);
	}

}
