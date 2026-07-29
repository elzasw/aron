package cz.aron.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "apu_source")
@AttributeOverride(name = "id", column = @Column(name = "apu_source_id"))
public class ApuSource extends PersistableBase {

	private UUID uuid;

    private LocalDateTime published;

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public LocalDateTime getPublished() {
		return published;
	}

	public void setPublished(LocalDateTime published) {
		this.published = published;
	}

}
