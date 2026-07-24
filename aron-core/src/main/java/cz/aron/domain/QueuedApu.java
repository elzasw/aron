package cz.aron.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="queued_apu")
public class QueuedApu {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="queued_apu_id")
	private long id;

	protected Instant created;

    private UUID apuId;

    private boolean requestSent;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

	public UUID getApuId() {
		return apuId;
	}

	public void setApuId(UUID apuId) {
		this.apuId = apuId;
	}

	public boolean isRequestSent() {
		return requestSent;
	}

	public void setRequestSent(boolean requestSent) {
		this.requestSent = requestSent;
	}    

}
