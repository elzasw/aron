package cz.aron.indexing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Document(indexName = "apu")
public class IndexedApu {
    
	@Field(type = FieldType.Keyword)
	private String id;
	
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = IndexConfig.FOLDING_AND_TOKENIZING_STOP),
            otherFields = {
                    @InnerField(suffix = IndexConfig.SUFFIX_SORT, type = FieldType.Text, analyzer = IndexConfig.SORTING, searchAnalyzer = IndexConfig.SORTING, fielddata = true)
            }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = IndexConfig.FOLDING_AND_TOKENIZING_STOP)
    private String description;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Boolean)
    private boolean containsDigitalObjects;

    @Field(type = FieldType.Nested)
    private List<IndexedRelation> rels = new ArrayList<>();

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

	public List<IndexedRelation> getRels() {
		return rels;
	}

	public void setRels(List<IndexedRelation> rels) {
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

}
