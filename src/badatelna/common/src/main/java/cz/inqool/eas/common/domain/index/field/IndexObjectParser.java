package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.domain.DomainIndexed;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.data.elasticsearch.annotations.*;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static cz.inqool.eas.common.domain.index.field.ES.Suffix.SEARCH;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexObjectParser {

    /**
     * Max depth to which the parser scans fields of an indexed object
     */
    private static final int MAX_PROPERTY_DEPTH = 5;


    public static IndexObjectFields parse(Class<? extends DomainIndexed<?, ?>> indexedType) {
        log.info("Parsing indexed fields of class '{}'", indexedType.getSimpleName());

        IndexObjectFields indexObjectFields = new IndexObjectFields();
        for (java.lang.reflect.Field field : FieldUtils.getAllFields(indexedType)) {
            parse(indexedType, field, null, MAX_PROPERTY_DEPTH)
                    .forEach(indexField -> indexObjectFields.put(indexField.getElasticSearchPath(), indexField));
        }

        return indexObjectFields;
    }

    public static Set<IndexFieldNode> parse(Class<? extends DomainIndexed<?, ?>> rootClass, java.lang.reflect.Field field, @Nullable IndexFieldInnerNode parent, int maxDepth) {
        if (log.isDebugEnabled()) {
            log.debug("Parsing field '{}'", field.getName());
        }
        Set<IndexFieldNode> indexFieldNodes = new TreeSet<>();

        GeoPointField geoPointFieldAnnotation = field.getAnnotation(GeoPointField.class);
        if (geoPointFieldAnnotation != null) {
            indexFieldNodes.add(parseGeoPointLeaf(geoPointFieldAnnotation, rootClass, field, parent));
        }

        Field fieldAnnotation = field.getAnnotation(Field.class);
        if (fieldAnnotation != null) {
            if (fieldAnnotation.type() == FieldType.Object || fieldAnnotation.type() == FieldType.Nested) {
                indexFieldNodes.addAll(parseFieldObject(fieldAnnotation, rootClass, field, parent, maxDepth));
            } else {
                indexFieldNodes.add(parseFieldLeaf(fieldAnnotation, rootClass, field, parent));
            }
        }

        MultiField multiFieldAnnotation = field.getAnnotation(MultiField.class);
        if (multiFieldAnnotation != null) {
            indexFieldNodes.add(parseMultifieldLeaf(multiFieldAnnotation, rootClass, field, parent));
        }

        return indexFieldNodes;
    }

    private static Set<IndexFieldNode> parseFieldObject(Field fieldAnnotation, Class<? extends DomainIndexed<?, ?>> rootClass, java.lang.reflect.Field field, @Nullable IndexFieldInnerNode parent, int maxDepth) {
        if (maxDepth <= 0) {
            log.debug("Max depth reached, skipping parsing of field '{}' of root class '{}'", field.getName(), rootClass.getSimpleName());
            return Set.of();
        }

        IndexFieldInnerNode node = new IndexFieldInnerNode(rootClass, field.getName(), fieldAnnotation, parent);

        if (fieldAnnotation.type() == FieldType.Nested) {
            // Shouldn't use nested fields due to low performance and scalability. Also they do not work properly in conjunction with logical filters.
            log.warn("Nested field encountered: " + node.getJavaPath());
        }

        Set<IndexFieldNode> indexFieldNodes = new TreeSet<>();
        indexFieldNodes.add(node);

        for (java.lang.reflect.Field nestedField : FieldUtils.getAllFields(resolveType(field))) {
            indexFieldNodes.addAll(parse(rootClass, nestedField, node, --maxDepth));
        }

        return indexFieldNodes;
    }

    private static IndexFieldLeafNode parseFieldLeaf(Field fieldAnnotation, Class<? extends DomainIndexed<?, ?>> rootClass, java.lang.reflect.Field field, @Nullable IndexFieldInnerNode parent) {
        IndexFieldLeafNode leaf = new IndexFieldLeafNode(rootClass, field.getName(), fieldAnnotation, parent);

        // keyword and text fields are available for fulltext search
        leaf.fulltext = fieldAnnotation.type() == FieldType.Text;

        Boost boost = field.getAnnotation(Boost.class);
        if (boost != null) {
            leaf.boost = boost.value();
        }

        return leaf;
    }

    private static IndexFieldLeafNode parseMultifieldLeaf(MultiField multiFieldAnnotation, Class<? extends DomainIndexed<?, ?>> rootClass, java.lang.reflect.Field field, @Nullable IndexFieldInnerNode parent) {
        IndexFieldLeafNode leaf = new IndexFieldLeafNode(rootClass, field.getName(), multiFieldAnnotation.mainField(), parent);
        leaf.fulltext = multiFieldAnnotation.mainField().type() == FieldType.Text;

        for (InnerField innerField : multiFieldAnnotation.otherFields()) {
            leaf.registerInnerField(innerField.suffix(), innerField);
            if (SEARCH.equals(innerField.suffix())) {
                leaf.fulltext = innerField.type() == FieldType.Text;
            }
        }

        Boost boost = field.getAnnotation(Boost.class);
        if (boost != null) {
            leaf.boost = boost.value();
        }

        return leaf;
    }

    private static IndexFieldGeoPointLeafNode parseGeoPointLeaf(GeoPointField geoPointFieldAnnotation, Class<? extends DomainIndexed<?, ?>> rootClass, java.lang.reflect.Field field, @Nullable IndexFieldInnerNode parent) {
        return new IndexFieldGeoPointLeafNode(rootClass, field.getName(), geoPointFieldAnnotation, parent);
    }

    private static Class<?> resolveType(java.lang.reflect.Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            Object unresolvedType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (unresolvedType instanceof TypeVariable) {
                return (Class<?>) ((TypeVariable<?>) unresolvedType).getBounds()[0];
            } else if (unresolvedType instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) unresolvedType).getRawType();
            } else {
                return (Class<?>) unresolvedType;
            }
        } else {
            return field.getType();
        }
    }
}
