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
    
    
}
