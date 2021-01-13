package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.domain.DomainIndexed;
import cz.inqool.eas.common.domain.index.reindex.reference.IndexReferenceField;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents an inner field in ElasticSearch mapping, annotated with {@link Field}.
 */
public class IndexFieldInnerNode extends IndexFieldNode {

    private final IndexedFieldProps mainField;

    private Set<IndexFieldNode> children = Set.of();

    public IndexFieldInnerNode(Class<? extends DomainIndexed<?, ?>> rootClass, java.lang.reflect.Field javaField, Field mainField, IndexFieldInnerNode parent) {
        super(rootClass, javaField, parent);
        this.mainField = new IndexedFieldProps(mainField.type(), mainField.index(), mainField.fielddata());
    }

    /**
     * Used when indexed field does not really exist in Indexed object, but is added dynamically
     */
    public IndexFieldInnerNode(Class<? extends DomainIndexed<?, ?>> rootClass, String javaFieldName, Class<?> javaFieldClass, IndexedFieldProps mainField, IndexFieldInnerNode parent, Set<IndexReferenceField> indexReferenceFields) {
        super(rootClass, javaFieldName, javaFieldClass, parent, indexReferenceFields);
        this.mainField = mainField;
    }

    public FieldType getType() {
        return mainField.getFieldType();
    }

    public boolean isIndexed() {
        return mainField.isIndexed();
    }

    void addChild(IndexFieldNode child) {
        Set<IndexFieldNode> allChildren = new LinkedHashSet<>(this.children);
        allChildren.add(child);
        this.children = Set.copyOf(allChildren);
    }
}
