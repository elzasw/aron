package cz.aron.indexing;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


public class IndexedRelation {
	
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

    public IndexedRelation() {
    	
    }

	public IndexedRelation(String targetId, String type, List<String> groups, String label, String idLabel) {
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
