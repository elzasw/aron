package cz.inqool.eas.common.domain.index.dto.params;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SearchAfterSerializer extends JsonSerializer<Object[]> {
    private final ObjectMapper objectMapper;

    public SearchAfterSerializer() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void serialize(Object[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(objectMapper.writeValueAsString(value));
    }
}
