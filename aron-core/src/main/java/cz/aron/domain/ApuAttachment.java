package cz.aron.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "apu_attachment")
@AttributeOverride(name = "id", column = @Column(name = "apu_attachment_id"))
public class ApuAttachment extends PersistableBase {

    private String name;
    @Column(name = "\"order\"")
    private int order;

    @OneToOne(mappedBy = "attachment", cascade = CascadeType.ALL, orphanRemoval = true)
    private DigitalObjectFile file;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="apu_id")
    private ApuEntity apu;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public DigitalObjectFile getFile() {
		return file;
	}

	public void setFile(DigitalObjectFile file) {
		this.file = file;
	}

	public ApuEntity getApu() {
		return apu;
	}

	public void setApu(ApuEntity apu) {
		this.apu = apu;
	}

}
