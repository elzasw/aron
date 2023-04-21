package cz.aron.domain;

import java.util.ArrayList;
import java.util.List;

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
@Table(name="digital_object")
public class DigitalObject {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="digital_object_id")
	private long id;
	
	private String uuid;
	
    private String name;
    private String permalink;
    private int order;

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

}
