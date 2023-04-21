package cz.aron.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "apu_part")
public class ApuPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="apu_part_id")
	private long id;

	private String value;
	private String type;

	@OneToMany(mappedBy = "parentPart", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ApuPart> childParts = new ArrayList<>();

	@ManyToOne
	@JoinColumn(name="apu_id")
	@JsonIgnore
	private ApuEntity apu;
	@ManyToOne
	@JoinColumn(name="parent_id")
	@JsonIgnore
	private ApuPart parentPart;

	@OneToMany(mappedBy = "apuPart", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ApuPartItem> items = new ArrayList<>();

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<ApuPart> getChildParts() {
		return childParts;
	}

	public void setChildParts(List<ApuPart> childParts) {
		this.childParts = childParts;
	}

	public ApuEntity getApu() {
		return apu;
	}

	public void setApu(ApuEntity apu) {
		this.apu = apu;
	}

	public ApuPart getParentPart() {
		return parentPart;
	}

	public void setParentPart(ApuPart parentPart) {
		this.parentPart = parentPart;
	}

	public List<ApuPartItem> getItems() {
		return items;
	}

	public void setItems(List<ApuPartItem> items) {
		this.items = items;
	}

	public ApuEntity findRootApuEntity() {
		if (apu != null) {
			return apu;
		}
		if (parentPart != null) {
			return parentPart.findRootApuEntity();
		}
		return null;
	}

}
