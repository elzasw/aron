package cz.aron.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="metadatum")
public class Metadatum {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="metadatum_id")
    private long id;
	
    private String value;

    private String type;

    @ManyToOne
    @JsonIgnore
    private DigitalObjectFile file;        

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

	public DigitalObjectFile getFile() {
		return file;
	}

	public void setFile(DigitalObjectFile file) {
		this.file = file;
	}

}
