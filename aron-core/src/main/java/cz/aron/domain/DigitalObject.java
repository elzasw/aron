package cz.aron.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="digital_object")
@AttributeOverride(name = "id", column = @Column(name = "digital_object_id"))
public class DigitalObject extends PersistableBase {

	private UUID uuid;

    private String name;
    private String permalink;
    @Column(name = "\"order\"")
    private int order;

    private LocalDateTime published;

    @OneToMany(mappedBy = "digitalObject", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DigitalObjectFile> files = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name="apu_id")
    private ApuEntity apu;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPermalink() {
		return permalink;
	}

	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public List<DigitalObjectFile> getFiles() {
		return files;
	}

	public void setFiles(List<DigitalObjectFile> files) {
		this.files = files;
	}

	public ApuEntity getApu() {
		return apu;
	}

	public void setApu(ApuEntity apu) {
		this.apu = apu;
	}

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
