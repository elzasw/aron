package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.domain.DomainIndexed;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents an inner field in ElasticSearch mapping, annotated with {@link Field}.
 */
public class IndexFieldInnerNode extends IndexFieldNode {

    private final Field mainField;

    private Set<IndexFieldNode> children = Set.of();


    public IndexFieldInnerNode(Class<? extends DomainIndexed<?, ?>> rootClass, String javaFieldName, Field mainField, IndexFieldInnerNode parent) {
        super(rootClass, javaFieldName, parent);
        this.mainField = mainField;
    }


    public Field getMainField() {
        return mainField;
    }

    public FieldType getType() {
        return mainField.type();
    }

    public boolean isIndexed() {
        return mainField.index();
    }

    void addChild(IndexFieldNode child) {
        Set<IndexFieldNode> allChildren = new LinkedHashSet<>(this.children);
        allChildren.add(child);
        this.children = Set.copyOf(allChildren);
    }
}
