package cz.inqool.eas.common.domain.index.dto.params;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static cz.inqool.eas.common.utils.JsonUtils.toJsonString;

public class SearchAfterSerializer extends JsonSerializer<Object[]> {

    @Override
    public void serialize(Object[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(toJsonString(value));
    }
}
