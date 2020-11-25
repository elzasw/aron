package cz.inqool.eas.common.domain.index.dto.params;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

import static cz.inqool.eas.common.utils.JsonUtils.fromJsonString;

public class SearchAfterDeserializer extends JsonDeserializer<Object[]> {

    @Override
    public Object[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return fromJsonString(p.getText(), Object[].class);
    }
}
