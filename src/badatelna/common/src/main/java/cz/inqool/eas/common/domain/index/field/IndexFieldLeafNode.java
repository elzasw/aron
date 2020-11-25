package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.domain.DomainIndexed;
import cz.inqool.eas.common.domain.index.field.ES.Suffix;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static cz.inqool.eas.common.utils.CollectionUtils.join;

/**
 * Represents a field in ElasticSearch mapping, annotated with {@link Field} or {@link MultiField}.
 */
@Slf4j
public class IndexFieldLeafNode extends IndexFieldNode {

    private final Field mainField;

    private final Map<String, InnerField> innerFields = new HashMap<>();

    @Getter
    boolean fulltext;

    @Getter
    float boost = 1.0f;


    public IndexFieldLeafNode(Class<? extends DomainIndexed<?, ?>> rootClass, String javaFieldName, Field mainField, IndexFieldInnerNode parent) {
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

    public InnerField getInnerField(String suffix) {
        return innerFields.get(suffix);
    }

    public void registerInnerField(String suffix, InnerField innerField) {
        innerFields.put(suffix, innerField);
    }

    public String getFolded() {
        String suffix = Optional.ofNullable(getInnerField(Suffix.FOLD)).map(InnerField::suffix).orElse(null);
        return join(".", getElasticSearchPath(), suffix);
    }

    public String getSearchable() {
        String suffix = Optional.ofNullable(getInnerField(Suffix.SEARCH)).map(InnerField::suffix).orElse(null);
        return join(".", getElasticSearchPath(), suffix);
    }

    public String getSortable() {
        String suffix = Optional.ofNullable(getInnerField(Suffix.SORT)).map(InnerField::suffix).orElse(null);
        return join(".", getElasticSearchPath(), suffix);
    }
}
