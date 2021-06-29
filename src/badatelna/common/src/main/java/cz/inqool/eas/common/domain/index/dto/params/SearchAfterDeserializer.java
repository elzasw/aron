package cz.inqool.eas.common.domain.index.dto.params;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;

public class SearchAfterDeserializer extends JsonDeserializer<Object[]> {
    private final ObjectMapper objectMapper;

    public SearchAfterDeserializer() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Object[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JavaType type = objectMapper.getTypeFactory().constructType(Object[].class);
        return objectMapper.readValue(p.getText(), type);
    }
}
