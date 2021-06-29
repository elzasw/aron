package cz.inqool.eas.common.client;

import lombok.Getter;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

/**
 * Builder for inter service requests.
 */
@Getter
public abstract class ClientRequestBuilder<RESPONSE, CRB extends ClientRequestBuilder<RESPONSE, CRB>> {

    protected String urlPath;
    protected HttpMethod method;
    protected HttpHeaders headers;
    protected Class<RESPONSE> responseType = (Class<RESPONSE>) Object.class;
    protected Map<String, Object> uriVariables = new LinkedHashMap<>();


    public ClientRequestBuilder(HttpMethod method) {
        this.method = method;
    }


    public static <RESPONSE> ClientRequestWithoutPayloadBuilder<RESPONSE> get() {
        return new ClientRequestWithoutPayloadBuilder<>(HttpMethod.GET);
    }

    public static <RESPONSE> ClientRequestWithPayloadBuilder<RESPONSE> post() {
        return new ClientRequestWithPayloadBuilder<>(HttpMethod.POST);
    }

    public static <RESPONSE> ClientRequestWithPayloadBuilder<RESPONSE> put() {
        return new ClientRequestWithPayloadBuilder<>(HttpMethod.PUT);
    }

    public static <RESPONSE> ClientRequestWithoutPayloadBuilder<RESPONSE> delete() {
        return new ClientRequestWithoutPayloadBuilder<>(HttpMethod.DELETE);
    }


    public abstract RESPONSE execute(RestTemplate template, String serverUrl);

    public abstract ResponseEntity<RESPONSE> executeEntity(RestTemplate template, String serverUrl);

    protected void validate() {
        notNull(urlPath, () -> new IllegalArgumentException("'urlPath' not set"));
    }

    public CRB urlPath(String urlPath) {
        this.urlPath = urlPath;
        //noinspection unchecked
        return (CRB) this;
    }

    public HttpHeadersBuilder<CRB> headers() {
        //noinspection unchecked
        return new HttpHeadersBuilder<>((CRB) this);
    }

    public CRB withDefaultHeaders() {
        //noinspection unchecked
        return new HttpHeadersBuilder<>((CRB) this)
                .setContentType(MediaType.APPLICATION_JSON)
                .set();
    }

    public CRB setUriVariable(String name, Object value) {
        this.uriVariables.put(name, value);
        //noinspection unchecked
        return (CRB) this;
    }

    public CRB responseType(Class<RESPONSE> responseType) {
        this.responseType = responseType;
        //noinspection unchecked
        return (CRB) this;
    }

    CRB setHeaders(HttpHeaders headers) {
        this.headers = headers;
        //noinspection unchecked
        return (CRB) this;
    }


    public static class HttpHeadersBuilder<REQUEST_BUILDER extends ClientRequestBuilder<?, REQUEST_BUILDER>> {

        private final HttpHeaders headers = new HttpHeaders();
        private final REQUEST_BUILDER requestBuilder;


        public HttpHeadersBuilder(REQUEST_BUILDER requestBuilder) {
            this.requestBuilder = requestBuilder;
        }


        public HttpHeadersBuilder<REQUEST_BUILDER> setContentType(MediaType contentType) {
            headers.setContentType(contentType);
            return this;
        }

        public HttpHeadersBuilder<REQUEST_BUILDER> setAccept(List<MediaType> accept) {
            headers.setAccept(accept);
            return this;
        }

        public HttpHeadersBuilder<REQUEST_BUILDER> add(String headerName, String headerValue) {
            headers.add(headerName, headerValue);
            return this;
        }

        public HttpHeadersBuilder<REQUEST_BUILDER> modify(Consumer<HttpHeaders> modifier) {
            modifier.accept(headers);
            return this;
        }

        public REQUEST_BUILDER set() {
            return requestBuilder.setHeaders(headers);
        }
    }


    public static class ClientRequestWithoutPayloadBuilder<RESPONSE> extends ClientRequestBuilder<RESPONSE, ClientRequestWithoutPayloadBuilder<RESPONSE>> {

        public ClientRequestWithoutPayloadBuilder(HttpMethod method) {
            super(method);
        }


        @Override
        public RESPONSE execute(RestTemplate template, String serverUrl) {
            validate();

            String url = serverUrl + urlPath;

            switch (method) {
                case GET:
                    return template.getForObject(url, responseType, uriVariables);
                case DELETE:
                    template.delete(url, uriVariables);
                    return null;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public ResponseEntity<RESPONSE> executeEntity(RestTemplate template, String serverUrl) {
            validate();

            String url = serverUrl + urlPath;

            switch (method) {
                case GET:
                    return template.exchange(url, HttpMethod.GET, null, responseType, uriVariables);
                case DELETE:
                    return template.exchange(url, HttpMethod.DELETE, null, responseType, uriVariables);
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }


    public static class ClientRequestWithPayloadBuilder<RESPONSE> extends ClientRequestBuilder<RESPONSE, ClientRequestWithPayloadBuilder<RESPONSE>> {

        private Object payload;


        public ClientRequestWithPayloadBuilder(HttpMethod method) {
            super(method);
        }


        public ClientRequestWithPayloadBuilder<RESPONSE> payload(Object payload) {
            this.payload = payload;
            return this;
        }

        @Override
        protected void validate() {
            super.validate();
        }

        @Override
        public RESPONSE execute(RestTemplate template, String serverUrl) {
            return executeEntity(template, serverUrl).getBody();
        }

        @Override
        public ResponseEntity<RESPONSE> executeEntity(RestTemplate template, String serverUrl) {
            validate();

            String url = serverUrl + urlPath;

            HttpEntity<?> request = new HttpEntity<>(payload, headers);

            switch (method) {
                case POST:
                    return template.exchange(url, HttpMethod.POST, request, responseType, uriVariables);
                case PUT:
                    return template.exchange(url, HttpMethod.PUT, request, responseType, uriVariables);
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
