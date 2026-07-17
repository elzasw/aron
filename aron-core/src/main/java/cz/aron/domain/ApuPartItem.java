package cz.aron.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "apu_part_item")
public class ApuPartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="apu_part_item_id")
	private long id;

	private String value;

	private boolean visible; // !indexOnly

	private String href; // only for link items

	private String type;

	@ManyToOne
	@JoinColumn(name="apu_part_id")
    @JsonIgnore
	private ApuPart apuPart;

	@Transient
	private String targetLabel; // for APU_REF types, we preload referred item's name here (before indexing)
	
	@Transient
	private String targetLabelIndex; // for APU_REF types, we preload referred item's name index here (before indexing)

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ApuPart getApuPart() {
		return apuPart;
	}

	public void setApuPart(ApuPart apuPart) {
		this.apuPart = apuPart;
	}

	public String getTargetLabel() {
		return targetLabel;
	}

	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}

	public String getTargetLabelIndex() {
		return targetLabelIndex;
	}

	public void setTargetLabelIndex(String targetLabelIndex) {
		this.targetLabelIndex = targetLabelIndex;
	}

}
