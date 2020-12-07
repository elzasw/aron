package cz.aron.core.model;

import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.ItemType;
import cz.inqool.eas.common.domain.index.DomainIndex;
import cz.inqool.eas.common.domain.index.DynamicIndex;
import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexFieldNode;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.elasticsearch.core.document.Document.parse;

/**
 * @author Lukas Jane (inQool) 09.11.2020.
 */
//@Service it gets half-beaned through ApuRepository init, this would make second bean
public class ApuIndex extends DomainIndex<ApuEntity, ApuEntity, IndexedApu> implements DynamicIndex {

    @Inject private TypesHolder typesHolder;

    public ApuIndex() {
        super(IndexedApu.class);
    }

    @Override
    protected IndexRequest createIndexRequest(IndexedApu obj) {
        Document document = converter.mapObject(obj);
        document.putAll(obj.getAdditionalDataToIndex());
        return new IndexRequest().
                index(getIndexName()).
                id(obj.getId()).
                source(document);
    }

    @Override
    protected PutMappingRequest createPutMappingRequest() {
        String mapping = new MappingBuilder(converter).
                buildPropertyMapping(indexedType);

        Document parse = parse(mapping);
        Map<String, Object> properties = (Map<String, Object>) parse.get("properties");
        properties.putAll(createCustomMapping());
        return new PutMappingRequest(getIndexName()).
                source(parse);
    }

    private Map<String, Object> createCustomMapping() {
        Map<String, Object> customMapping = new HashMap<>();
        for (ItemType allItemType : typesHolder.getAllItemTypes()) {
            Map<String, Object> fieldProperties = new HashMap<>();
            String dataType;
            switch (allItemType.getType()) {
                case STRING:
                    dataType = "text";
                    break;
                case ENUM:
                    dataType = "keyword";
                    break;
                case INTEGER:
                    dataType = "integer";
                    break;
                case APU_REF:
                    dataType = "keyword";
                    break;
                case UNITDATE:
                    dataType = "date";
                    break;
                default:
                    throw new RuntimeException("unknown type");
            }
            fieldProperties.put("type", dataType);
            customMapping.put(allItemType.getCode(), fieldProperties);
        }
        return customMapping;
    }

    @Override
    public Map<String, IndexFieldNode> getDynamicFields() {
        Map<String, IndexFieldNode> dynamicFields = new HashMap<>();
        for (ItemType allItemType : typesHolder.getAllItemTypes()) {
            FieldType fieldType;
            switch (allItemType.getType()) {
                case STRING:
                    fieldType = FieldType.Text;
                    break;
                case ENUM:
                    fieldType = FieldType.Keyword;
                    break;
                case INTEGER:
                    fieldType = FieldType.Integer;
                    break;
                case APU_REF:
                    fieldType = FieldType.Keyword;
                    break;
                case UNITDATE:
                    fieldType = FieldType.Date;
                    break;
                default:
                    throw new RuntimeException("unknown type");
            }

            FakeField field = new FakeField();
            String fieldName = allItemType.getCode();
            field.setName(fieldName);
            field.setType(fieldType);
            String elasticSearchPath = fieldName;
            IndexFieldLeafNode indexFieldLeafNode = new IndexFieldLeafNode(IndexedApu.class, fieldName, field, null);
            if (indexFieldLeafNode.getType() == FieldType.Text) {
                try {
                    java.lang.reflect.Field fulltext = indexFieldLeafNode.getClass().getDeclaredField("fulltext");
                    fulltext.setAccessible(true);
                    fulltext.setBoolean(indexFieldLeafNode, true);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            dynamicFields.put(elasticSearchPath, indexFieldLeafNode);
        }
        return dynamicFields;
    }
}