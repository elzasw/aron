package cz.aron.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One entry of the dirty set: the search-index state of the record with this
 * uuid must be re-derived from the database - indexed if its row exists,
 * its document deleted if not. Rows are produced by plain {@code INSERT ... SELECT}
 * statements inside the import's transaction and may therefore repeat; the
 * consumer ({@code IndexSynchronizer}) reads distinct uuids and deletes every
 * row of what it processed, in the transaction of the successful index write.
 * <p>
 * The uuid is mapped as the JPA id only so the entity can be read; it is not
 * unique in the table.
 */
@Entity
@Table(name = "index_dirty")
public class IndexDirty {

	@Id
	@Column(name = "apu_uuid")
	private UUID apuUuid;

	public UUID getApuUuid() {
		return apuUuid;
	}

}
