package cz.inqool.eas.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;

/**
 * Class representing an exception raised during execution of a REST endpoint
 */
@Getter
@JsonPropertyOrder({"status", "error", "path", "timestamp", "code", "exception", "message", "cause", "details"})
public class RestException extends BaseException {

    private final int status;
    private final String error;
    private final String path;


    public RestException(HttpServletRequest request, HttpStatus status, Throwable exception, String message) {
        super(exception, message);
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.path = request.getMethod() + ": " + request.getRequestURI();
    }


    @Override
    public ObfuscatedException toObfuscatedException() {
        return new ObfuscatedException(exception.getName(), message, path);
    }
}
