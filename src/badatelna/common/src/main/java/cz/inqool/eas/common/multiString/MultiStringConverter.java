package cz.inqool.eas.common.multiString;

import com.fasterxml.jackson.databind.JavaType;
import cz.inqool.eas.common.db.JsonConverter;
import cz.inqool.eas.common.utils.JsonUtils;

import javax.persistence.Converter;

/**
 * JPA converter for {@link MultiString}
 */
@Converter(autoApply = true)
public class MultiStringConverter extends JsonConverter<MultiString> {

    @Override
    public JavaType getType() {
        return JsonUtils.typeFactory().constructType(MultiString.class);
    }

    @Override
    public MultiString convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        try {
            return JsonUtils.fromJsonString(dbData, MultiString.class);
        } catch (Exception ex) {
            // ignore
            return null;
        }
    }
}
