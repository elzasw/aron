package cz.aron.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="digital_object_file")
public class DigitalObjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="digital_object_file_id")
	private long id;
	
	private UUID uuid;

    @Column(name = "file_id")
    private UUID fileId;

    private String permalink;

    @Column(name = "\"order\"")
    private int order;

    @Enumerated(EnumType.STRING)
    private DigitalObjectType type;

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
    
    private boolean selected;

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

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public UUID getFileId() {
		return fileId;
	}

	public void setFileId(UUID fileId) {
		this.fileId = fileId;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
}
