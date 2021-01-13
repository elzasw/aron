package cz.inqool.eas.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.io.InputStream;

import static cz.inqool.eas.common.exception.ExceptionUtils.checked;
import static java.lang.String.format;

/**
 * Utility methods for working with JSON.
 *
 * todo: add missing comments
 */
public class JsonUtils {
    private static ObjectMapper objectMapper;
    private static JsonSchemaGenerator jsonSchemaGenerator;

    /**
     * Converts object to JSON string.
     *
     * @param o Object to convert
     * @return JSON string
     */
    public static String toJsonString(Object o) {
        return toJsonString(o, false);
    }

    /**
     * Converts object to JSON string.
     *
     * @param o Object to convert
     * @param prettyPrint Pretty print the output
     * @return JSON string
     */
    public static String toJsonString(Object o, boolean prettyPrint) {
        ensureObjectMapperInitialized();

        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, prettyPrint);
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(format("Failed when serializing %s to JSON string", (o != null) ? o.getClass() : null), e);
        }
    }

    /**
     * Converts JSON string to object using specified class.
     *
     * @param json JSON string
     * @param type Class to use
     * @param <T> Type of object
     * @return Object of provided type
     */
    public static <T> T fromJsonString(@NotNull String json, @NotNull Class<T> type) {
        return fromJsonString(json, typeFactory().constructType(type));
    }

    /**
     * Converts JSON string to object using specified class.
     *
     * @param json JSON stream
     * @param type Class to use
     * @param <T> Type of object
     * @return Object of provided type
     */
    public static <T> T fromJsonStream(@NotNull InputStream json, @NotNull Class<T> type) {
        return fromJsonStream(json, typeFactory().constructType(type));
    }

    /**
     * Converts JSON string to object using specified type reference.
     *
     * @param json JSON string
     * @param type Type reference to use
     * @param <T> Type of object
     * @return Object of provided type
     */
    public static <T> T fromJsonString(@NotNull String json, @NotNull TypeReference<T> type) {
        return fromJsonString(json, typeFactory().constructType(type));
    }

    /**
     * Converts JSON string to object using specified type reference.
     *
     * @param json JSON stream
     * @param type Type reference to use
     * @param <T> Type of object
     * @return Object of provided type
     */
    public static <T> T fromJsonStream(@NotNull InputStream json, @NotNull TypeReference<T> type) {
        return fromJsonStream(json, typeFactory().constructType(type));
    }

    public static <T> T fromJsonString(@NotNull String json, @NotNull JavaType type) {
        ensureObjectMapperInitialized();

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(format("Failed to read JSON value as %s", type.getTypeName()), e);
        }
    }

    public static <T> T fromJsonStream(@NotNull InputStream json, @NotNull JavaType type) {
        ensureObjectMapperInitialized();

        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(format("Failed to read JSON value as %s", type.getTypeName()), e);
        }
    }

    public static <T> T fromJsonStringParametrized(@NotNull String json, @NotNull Class<T> parametrized, Class<?>... parameterTypes) {
        return fromJsonString(json, typeFactory().constructParametricType(parametrized, parameterTypes));
    }

    public static <IN, OUT> OUT convert(IN input, @NotNull Class<OUT> outputType) {
        ensureObjectMapperInitialized();

        try {
            return objectMapper.convertValue(input, outputType);
        } catch (Exception e) {
            throw new RuntimeException(format("Failed to convert %s to %s", (input != null) ? input.getClass() : null, outputType), e);
        }
    }

    public static <IN, OUT> OUT convert(IN input, @NotNull Class<OUT> outputType, Class<?>... parameterTypes) {
        ensureObjectMapperInitialized();

        try {
            JavaType parametricType = objectMapper.getTypeFactory().constructParametricType(outputType, parameterTypes);
            return objectMapper.convertValue(input, parametricType);
        } catch (Exception e) {
            throw new RuntimeException(format("Failed to convert %s to %s", (input != null) ? input.getClass() : null, outputType), e);
        }
    }

    public static TypeFactory typeFactory() {
        ensureObjectMapperInitialized();
        return objectMapper.getTypeFactory();
    }

    /**
     * Creates a JSON schema for given class
     */
    public static JsonSchema createJsonSchema(Class<?> clazz) {
        ensureJsonSchemaGeneratorInitialized();

        return checked(() -> jsonSchemaGenerator.generateSchema(clazz));
    }

    private static void ensureObjectMapperInitialized() {
        if (objectMapper == null) {
            ApplicationContext applicationContext = ApplicationContextUtils.getApplicationContext();
            if (applicationContext != null) {
                objectMapper = applicationContext.getBean(ObjectMapper.class);
            } else {
                throw new RuntimeException("Application not properly initialized yet.");
            }
        }
    }

    private static void ensureJsonSchemaGeneratorInitialized() {
        if (jsonSchemaGenerator == null) {
            ensureObjectMapperInitialized();

            VisitorContext visitorContext = new VisitorContext() {
                @Override
                public String addSeenSchemaUri(JavaType aSeenSchema) {
                    return null; // not to send property "id" (such as 'urn:jsonschema:cz:inqool:peva:system:notification:template:model:DelimitationRequest')
                }

                @Override
                public String getSeenSchemaUri(JavaType aSeenSchema) {
                    return null; // to not include $ref attribute on repeated models and unwind all model attributes
                }
            };

            jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, new WrapperFactory().getWrapper(null, visitorContext));
        }
    }
}
