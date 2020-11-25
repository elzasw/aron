package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.domain.DomainIndexed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;

/**
 * Represents a field in ElasticSearch mapping, annotated with {@link GeoPointField}.
 */
@Slf4j
public class IndexFieldGeoPointLeafNode extends IndexFieldNode {

    private final GeoPointField mainField;


    public IndexFieldGeoPointLeafNode(Class<? extends DomainIndexed<?, ?>> rootClass, String javaFieldName, GeoPointField mainField, IndexFieldInnerNode parent) {
        super(rootClass, javaFieldName, parent);
        this.mainField = mainField;
    }


    @Override
    public FieldType getType() {
        return null;
    }
}
