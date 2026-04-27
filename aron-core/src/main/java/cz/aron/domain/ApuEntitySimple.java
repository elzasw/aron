package cz.aron.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Immutable
@Entity(name = "apu_entity_simple")
@Table(name = "apu")
@BatchSize(size = 100)
public class ApuEntitySimple {
	
	@Id
	@Column(name="apu_id")
	private long id;
	private String uuid;
    private String name;
    private String description;
    @Column(name = "ordr")
    private int order;

    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.JOIN)
    private List<ApuPart> parts = new ArrayList<>();
    
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public List<ApuPart> getParts() {
		return parts;
	}

	public void setParts(List<ApuPart> parts) {
		this.parts = parts;
	}

}
