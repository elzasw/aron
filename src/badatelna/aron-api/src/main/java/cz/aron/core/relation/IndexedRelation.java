package cz.aron.core.relation;

import cz.inqool.eas.common.domain.index.DomainIndexedObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Lukas Jane (inQool) 11.12.2020.
 */
@Getter
@Setter
@Slf4j
@Document(indexName = "relation")
public class IndexedRelation extends DomainIndexedObject<Relation, Relation> {

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String relation;

    @Field(type = FieldType.Keyword)
    private String target;

    @Override
    public void toIndexedObject(Relation obj) {
        super.toIndexedObject(obj);
        source = obj.getSource();
        relation = obj.getRelation();
        target = obj.getTarget();
    }
}
