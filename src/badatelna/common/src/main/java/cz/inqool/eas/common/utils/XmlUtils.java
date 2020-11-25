package cz.inqool.eas.common.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cz.inqool.eas.common.exception.GeneralException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static cz.inqool.eas.common.exception.ExceptionUtils.checked;

/**
 * Utility methods for working with XML.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XmlUtils {

    @SuppressWarnings("UnstableApiUsage")
    private static final Cache<Class<?>, JAXBContext> JAXB_CONTEXT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();


    /**
     * Converts given object to an XML string
     */
    public static <T> String toXmlString(@Nullable T object) {
        StringWriter stringWriter = new StringWriter();

        marshall(object, (marshaller, obj) -> checked(() -> marshaller.marshal(obj, stringWriter)));

        return stringWriter.toString();
    }

    /**
     * Converts given object to an XML using given
     *
     * @param object object to be converted
     * @param marshallerConsumer marshalling function
     */
    public static <T> void toXml(@Nullable T object, @Nonnull BiConsumer<Marshaller, T> marshallerConsumer) {
        marshall(object, marshallerConsumer);
    }

    /**
     * Converts given object to an XML response entity (for API endpoints - XML file providers)
     *
     * @param object object to be converted
     * @param filename name of response filename attachment
     */
    public static <T> ResponseEntity<StreamingResponseBody> toXmlResponse(@Nullable T object, @Nonnull String filename) {
        StreamingResponseBody responseBody = outputStream -> toXml(object, (marshaller, obj) -> checked(() -> marshaller.marshal(obj, outputStream)));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_XML)
                .body(responseBody);
    }

    private static <T> void marshall(@Nullable T object, @Nonnull BiConsumer<Marshaller, T> marshallerConsumer) {
        if (object == null) {
            return;
        }

        try {
            JAXBContext context = getContext(object.getClass());

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            marshallerConsumer.accept(marshaller, object);
        } catch (JAXBException e) {
            throw new GeneralException("Failed to marshall object.", e);
        }
    }

    /**
     * Reads an XML input stream into POJO of given type.
     *
     * @param stream input stream with XML data
     * @param type type of object to convert the stream to
     */
    public static <T> T fromXml(@Nonnull InputStream stream, @Nonnull Class<T> type) {
        //noinspection unchecked
        return unmarshall(stream, type, (unmarshaller, inputStream) -> (T) checked(() -> unmarshaller.unmarshal(inputStream)));
    }

    private static <T, S> T unmarshall(@Nullable S source, @Nonnull Class<T> type, @Nonnull BiFunction<Unmarshaller, S, T> marshallerConsumer) {
        if (source == null) {
            return null;
        }

        try {
            JAXBContext context = getContext(type);

            Unmarshaller unmarshaller = context.createUnmarshaller();
            return marshallerConsumer.apply(unmarshaller, source);
        } catch (JAXBException e) {
            throw new GeneralException("Failed to marshall object.", e);
        }
    }

    @SneakyThrows
    private static JAXBContext getContext(@Nonnull Class<?> type) {
        //noinspection UnstableApiUsage
        return JAXB_CONTEXT_CACHE.get(type, () -> JAXBContext.newInstance(type));
    }
}
