package cz.aron.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.aron.api.rest.model.ApuPart;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@NamedNativeQueries({
@NamedNativeQuery(name = "entityTree",
query = """
WITH RECURSIVE cte(uuid, name, description, ordr, type, apu_id, parent_id) AS
(
	SELECT uuid, name, description, ordr, type, apu_id, parent_id
	FROM apu a
	WHERE a.uuid=:uuid
	UNION ALL
	SELECT a.uuid, a.name, a.description, a.ordr, a.type, a.apu_id, a.parent_id
	FROM cte
	JOIN apu a ON a.parent_id=cte.apu_id
)
SELECT uuid as uuid, name as name, description as description, ordr as ordr, type as type, apu_id as apu_id, parent_id as parent_id
FROM cte
""",
resultSetMapping = "entityTreeResult"),
@NamedNativeQuery(name = "apuAncestors",
query = """
WITH RECURSIVE cte(apu_id, uuid, name, description, parent_id, depth, child_cnt, ordr, pos, lvl) AS
(
	SELECT a.apu_id, a.uuid, a.name, a.description, a.parent_id, a.depth, a.child_cnt, a.ordr, a.pos, 0 as lvl
	FROM apu a
	WHERE a.apu_id=:id
	UNION ALL
	SELECT p.apu_id, p.uuid, p.name, p.description, p.parent_id, p.depth, p.child_cnt, p.ordr, p.pos, c.lvl+1
	FROM cte c
	JOIN apu p ON p.apu_id=c.parent_id
)
SELECT apu_id as apu_id, uuid as uuid, name as name, description as description, parent_id as parent_id, depth as depth, child_cnt as child_cnt, ordr as ordr, pos as pos
FROM cte
WHERE lvl>0
ORDER BY lvl
""",
resultSetMapping = "apuAncestorsResult")
})

@SqlResultSetMappings({
@SqlResultSetMapping(
	    name="entityTreeResult",
	    classes={
	      @ConstructorResult(
	        targetClass=cz.aron.domain.types.dto.ApuEntityViewType.class,
	        columns={
	          @ColumnResult(name="uuid", type=UUID.class),
	          @ColumnResult(name="name", type=String.class),
	          @ColumnResult(name="description", type=String.class),
	          @ColumnResult(name="ordr", type=Integer.class),
	          @ColumnResult(name="type", type=ApuType.class),
	          @ColumnResult(name="apu_id", type=Long.class),
	          @ColumnResult(name="parent_id", type=Long.class),})}),
@SqlResultSetMapping(
	    name="apuAncestorsResult",
	    classes={
	      @ConstructorResult(
	        targetClass=cz.aron.domain.dto.IdUuidNameDescriptionParentDto.class,
	        columns={
	          @ColumnResult(name="apu_id", type=Long.class),
	          @ColumnResult(name="uuid", type=UUID.class),
	          @ColumnResult(name="name", type=String.class),
	          @ColumnResult(name="description", type=String.class),
	          @ColumnResult(name="parent_id", type=Long.class),
	          @ColumnResult(name="depth", type=Integer.class),
	          @ColumnResult(name="child_cnt", type=Integer.class),
	          @ColumnResult(name="ordr", type=Integer.class),
	          @ColumnResult(name="pos", type=Integer.class),})})
})

@Entity
@Table(name = "apu")
public class ApuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="apu_id")
	private long id;

	private UUID uuid;

	private String name;
	private String indexedName;
	private String description;
	private String result;
	private String permalink;
	
	@Column(name = "ordr")
	private int order;
	
	private boolean published;
	
    private int depth;
    private int pos;
    private int childCnt;
    private boolean indexed;
    private boolean reindex;

    @Column(name = "has_attachments")
    private boolean hasAttachments;
    @Column(name = "has_daos")
    private boolean hasDaos;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name="apu_source_id")
    @JsonIgnore
	private ApuSource source;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="parent_id")
	private ApuEntity parent;

	// parts (and their items) are serialized with Kryo into this blob instead of
	// being stored in their own tables
	@Column(name = "data")
	@JsonIgnore
	private byte[] data;

	// runtime-only view of the parts, lazily deserialized from data / serialized back on save
	@Transient
	private transient List<ApuPart> parts;

	@OneToMany(mappedBy = "apu", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ApuAttachment> attachments = new ArrayList<>();

	@OneToMany(mappedBy = "apu", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DigitalObject> digitalObjects = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	private ApuType type;

	@Transient
	private List<String> incomingRelTypeGroups = new ArrayList<>();

	@Transient
	private List<String> incomingRelTypes = new ArrayList<>();

	public long getId() {
		return id;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIndexedName() {
		return indexedName;
	}

	public void setIndexedName(String indexedName) {
		this.indexedName = indexedName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
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

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public int getChildCnt() {
		return childCnt;
	}

	public void setChildCnt(int childCnt) {
		this.childCnt = childCnt;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}

	public ApuSource getSource() {
		return source;
	}

	public void setSource(ApuSource source) {
		this.source = source;
	}

	public ApuEntity getParent() {
		return parent;
	}

	public void setParent(ApuEntity parent) {
		this.parent = parent;
	}

	public List<ApuPart> getParts() {
		if (parts == null) {
			parts = ApuPartSerializer.deserialize(data);
		}
		return parts;
	}

	public void setParts(List<ApuPart> parts) {
		this.parts = parts;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * Serializes the in-memory parts into {@link #data} before insert/update. Only runs when the
	 * parts were actually loaded/modified ({@code parts != null}); otherwise the stored blob is
	 * left untouched so entities updated without touching parts don't get their data wiped.
	 */
	@PrePersist
	@PreUpdate
	private void serializeParts() {
		if (parts != null) {
			this.data = ApuPartSerializer.serialize(parts);
		}
	}

	public List<ApuAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<ApuAttachment> attachments) {
		this.attachments = attachments;
	}

	public List<DigitalObject> getDigitalObjects() {
		return digitalObjects;
	}

	public void setDigitalObjects(List<DigitalObject> digitalObjects) {
		this.digitalObjects = digitalObjects;
	}

	public ApuType getType() {
		return type;
	}

	public void setType(ApuType type) {
		this.type = type;
	}

	public List<String> getIncomingRelTypeGroups() {
		return incomingRelTypeGroups;
	}

	public void setIncomingRelTypeGroups(List<String> incomingRelTypeGroups) {
		this.incomingRelTypeGroups = incomingRelTypeGroups;
	}

	public List<String> getIncomingRelTypes() {
		return incomingRelTypes;
	}

	public void setIncomingRelTypes(List<String> incomingRelTypes) {
		this.incomingRelTypes = incomingRelTypes;
	}

	public boolean isReindex() {
		return reindex;
	}

	public void setReindex(boolean reindex) {
		this.reindex = reindex;
	}

	public boolean isHasAttachments() {
		return hasAttachments;
	}

	public void setHasAttachments(boolean hasAttachments) {
		this.hasAttachments = hasAttachments;
	}

	public boolean isHasDaos() {
		return hasDaos;
	}

	public void setHasDaos(boolean hasDaos) {
		this.hasDaos = hasDaos;
	}

}
