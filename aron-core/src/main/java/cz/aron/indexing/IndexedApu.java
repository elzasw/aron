package cz.aron.indexing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "apu")
public class IndexedApu {
    
	@Field(type = FieldType.Keyword)
	private String id;
	
    @Field(type = FieldType.Text, analyzer = IndexConfig.FOLDING_AND_TOKENIZING_STOP)
    private String name;

    /**
     * Czech collation key of the name, computed at index time (hex-encoded; see
     * ApuDocumentBuilder). Replaces the former ICU-collation subfield - sorting is
     * engine-neutral and needs no analysis plugin.
     */
    @Field(type = FieldType.Keyword)
    private String nameSort;

    /** Normalized name, diacritics preserved - the exact-match tier (ApuDocumentBuilder.normalizeCs). */
    @Field(type = FieldType.Keyword)
    private String nameExactCs;

    /** Normalized name, diacritics folded - the folded exact and prefix tiers (ApuDocumentBuilder.normalize). */
    @Field(type = FieldType.Keyword)
    private String nameExact;

    /**
     * The general-fulltext catch-all (one entry per searchable value; see
     * ApuDocument.getAllText). The NON-stop folding analyzer, so stop-word-only
     * queries stay answerable; ES's default position_increment_gap (100) keeps
     * phrases from matching across two values.
     */
    @Field(type = FieldType.Text, analyzer = IndexConfig.FOLDING_AND_TOKENIZING)
    private List<String> allText = new ArrayList<>();

    @Field(type = FieldType.Text, analyzer = IndexConfig.FOLDING_AND_TOKENIZING_STOP)
    private String description;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Boolean)
    private boolean containsDigitalObjects;

    @Field(type = FieldType.Nested)
    private List<NestedRelation> rels = new ArrayList<>();

    @Field(type = FieldType.Keyword)
    private List<String> incomingRelTypeGroups = new ArrayList<>();

    @Field(type = FieldType.Keyword)
    private List<String> incomingRelTypes = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameSort() {
		return nameSort;
	}

	public void setNameSort(String nameSort) {
		this.nameSort = nameSort;
	}

	public String getNameExactCs() {
		return nameExactCs;
	}

	public void setNameExactCs(String nameExactCs) {
		this.nameExactCs = nameExactCs;
	}

	public String getNameExact() {
		return nameExact;
	}

	public void setNameExact(String nameExact) {
		this.nameExact = nameExact;
	}

	public List<String> getAllText() {
		return allText;
	}

	public void setAllText(List<String> allText) {
		this.allText = allText;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isContainsDigitalObjects() {
		return containsDigitalObjects;
	}

	public void setContainsDigitalObjects(boolean containsDigitalObjects) {
		this.containsDigitalObjects = containsDigitalObjects;
	}

	public List<NestedRelation> getRels() {
		return rels;
	}

	public void setRels(List<NestedRelation> rels) {
		this.rels = rels;
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
	
	public static class NestedRelation {
		
	    @Field(type = FieldType.Keyword)
	    private String targetId;
	    @Field(type = FieldType.Keyword)
	    private String type;
	    @Field(type = FieldType.Keyword)
	    private List<String> groups;
	    @Field(type = FieldType.Text, analyzer = IndexConfig.FOLDING_AND_TOKENIZING)
	    private String label;
	    @Field(type = FieldType.Keyword)
	    private String idLabel;

	    public NestedRelation() {
	    	
	    }

		public NestedRelation(String targetId, String type, List<String> groups, String label, String idLabel) {
			this.targetId = targetId;
			this.type = type;
			this.groups = groups;
			this.label = label;
			this.idLabel = idLabel;
		}

		public String getTargetId() {
			return targetId;
		}

		public void setTargetId(String targetId) {
			this.targetId = targetId;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public List<String> getGroups() {
			return groups;
		}

		public void setGroups(List<String> groups) {
			this.groups = groups;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getIdLabel() {
			return idLabel;
		}

		public void setIdLabel(String idLabel) {
			this.idLabel = idLabel;
		}

	}

}
