package cz.inqool.eas.common.db;

import com.fasterxml.jackson.databind.JavaType;

import javax.persistence.AttributeConverter;

import static cz.inqool.eas.common.utils.JsonUtils.fromJsonString;
import static cz.inqool.eas.common.utils.JsonUtils.toJsonString;

/**
 * Converter for database columns containing JSON-serialized objects.
 */
abstract public class JsonConverter<X> implements AttributeConverter<X, String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String convertToDatabaseColumn(X attribute) {
        if (attribute == null) {
            return null;
        }

        return toJsonString(attribute, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return fromJsonString(dbData, getType());
    }

    /**
     * Return the type of serialized object.
     */
    public abstract JavaType getType();
}
