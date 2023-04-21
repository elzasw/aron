package cz.aron.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="digital_object_file")
public class DigitalObjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="digital_object_file_id")
	private long id;
	
	private String uuid;
	
    private String permalink;
    private int order;

    @Enumerated(EnumType.STRING)
    private DigitalObjectType type;

    @OneToMany(mappedBy = "file", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Metadatum> metadata = new ArrayList<>();

    //@OneToOne
    //private File file;

    @ManyToOne
    @JoinColumn(name="digital_object_id")
    private DigitalObject digitalObject;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="attachment_id")
    private ApuAttachment attachment;

    private String name;

    private String referencedFile;

    private String contentType;

    private Long size;

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

	public DigitalObjectType getType() {
		return type;
	}

	public void setType(DigitalObjectType type) {
		this.type = type;
	}

	public List<Metadatum> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<Metadatum> metadata) {
		this.metadata = metadata;
	}

	public DigitalObject getDigitalObject() {
		return digitalObject;
	}

	public void setDigitalObject(DigitalObject digitalObject) {
		this.digitalObject = digitalObject;
	}

	public ApuAttachment getAttachment() {
		return attachment;
	}

	public void setAttachment(ApuAttachment attachment) {
		this.attachment = attachment;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReferencedFile() {
		return referencedFile;
	}

	public void setReferencedFile(String referencedFile) {
		this.referencedFile = referencedFile;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
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
