package cz.aron.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.ItemType;
import cz.inqool.eas.common.domain.index.DomainIndexedObject;
import cz.inqool.eas.common.utils.ApplicationContextUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lukas Jane (inQool) 29.10.2020.
 */
@Getter
@Setter
@Slf4j
@Document(indexName = "apu")
public class IndexedApu extends DomainIndexedObject<ApuEntity, ApuEntity> {

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String type;

    @Transient
    private Map<String, Object> additionalDataToIndex = new HashMap<>();

    @Override
    public void toIndexedObject(ApuEntity obj) {
        super.toIndexedObject(obj);
        name = obj.getName();
        description = obj.getDescription();
        type = obj.getType().name();

        TypesHolder typesHolder = ApplicationContextUtils.getApplicationContext().getBean(TypesHolder.class);
        ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
        for (ApuPart part : obj.getParts()) {
            for (ApuPartItem item : part.getItems()) {
                String value = item.getValue();
                ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                if (itemType == null) {
                    log.warn("Item type not recognized: " + item.getType());
                    continue;
                }
                Object data;
                switch (itemType.getType()) {
                    case STRING:
                        data = value;
                        break;
                    case ENUM:
                        data = value;
                        break;
                    case INTEGER:
                        data = Integer.valueOf(value);
                        break;
                    case APU_REF:
                        data = value;
                        break;
                    case UNITDATE:
                        try {
                            UniversalDate universalDate = objectMapper.readValue(value, UniversalDate.class);
                            data = universalDate.getFrom();   //todo other subfields and where to? also create date object?
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    default:
                        throw new RuntimeException("Unknown type");
                }
                additionalDataToIndex.put(itemType.getCode(), data);
            }
        }
    }
}
