package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.domain.DomainIndexed;
import cz.inqool.eas.common.exception.GeneralException;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.Nullable;

import static cz.inqool.eas.common.utils.AssertionUtils.cast;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

/**
 * Represents one index object field ES mapping, provides methods for retrieval of field names with proper suffixes
 * during search query build.
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
@Slf4j
@EqualsAndHashCode
public abstract class IndexFieldNode implements Comparable<IndexFieldNode> {

    protected final Class<? extends DomainIndexed<?, ?>> rootClass;

    protected final String javaFieldName;

    protected final IndexFieldInnerNode parent;


    public IndexFieldNode(Class<? extends DomainIndexed<?, ?>> rootClass, String javaFieldName, @Nullable IndexFieldInnerNode parent) {
        this.rootClass = rootClass;
        this.javaFieldName = javaFieldName;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }


    public abstract FieldType getType();

    public String getJavaPath() {
        StringBuilder sb = new StringBuilder();

        IndexFieldNode indexFieldNode = this;
        while (indexFieldNode != null) {
            if (sb.length() != 0) {
                sb.insert(0, ".");
            }
            sb.insert(0, indexFieldNode.javaFieldName);
            indexFieldNode = indexFieldNode.parent;
        }

        sb.insert(0, rootClass.getSimpleName() + ".");
        return sb.toString();
    }

    public String getElasticSearchPath() {
        StringBuilder sb = new StringBuilder();

        IndexFieldNode indexFieldNode = this;
        while (indexFieldNode != null) {
            if (sb.length() != 0) {
                sb.insert(0, ".");
            }
            sb.insert(0, indexFieldNode.javaFieldName);
            indexFieldNode = indexFieldNode.parent;
        }

        return sb.toString().replaceAll("_", ".");
    }

    public boolean isOfNested() {
        return getNestedParent() != null;
    }

    public IndexFieldInnerNode getNestedParent() {
        IndexFieldNode indexFieldNode = this;
        while (indexFieldNode != null) {
            if (indexFieldNode.getType() == FieldType.Nested) {
                return cast(indexFieldNode, IndexFieldInnerNode.class, () -> new IllegalStateException("Nested field musn't be a leaf node"));
            }
            indexFieldNode = indexFieldNode.parent;
        }

        return null;
    }

    public String getNestedPath() {
        IndexFieldInnerNode nestedParent = getNestedParent();
        notNull(nestedParent, () -> new GeneralException("The field is not of type 'Nested'."));

        return nestedParent.getElasticSearchPath();
    }

    @Override
    public int compareTo(IndexFieldNode o) {
        return this.getJavaPath().compareTo(o.getJavaPath());
    }
}
