package cz.inqool.eas.common.reporting.convert;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.MapType;
import cz.inqool.eas.common.db.JsonConverter;
import cz.inqool.eas.common.utils.JsonUtils;

import javax.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Convert serialized report data.
 */
@Converter
public class ReportDataConverter extends JsonConverter<List<Map<String, Object>>> {

    @Override
    public JavaType getType() {
        MapType itemType = JsonUtils.typeFactory().constructMapType(Map.class, String.class, Object.class);
        return JsonUtils.typeFactory().constructCollectionType(ArrayList.class, itemType);
    }
}
