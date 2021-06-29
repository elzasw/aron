package cz.inqool.eas.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;

/**
 * Class representing an exception raised during execution of a REST endpoint
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"status", "error", "path", "timestamp", "code", "exception", "message", "cause", "details"})
public class RestException extends BaseException {

    private int status;
    private String error;
    private String path;


    public RestException(HttpServletRequest request, HttpStatus status, Throwable exception, String message) {
        super(exception, message);
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.path = request.getMethod() + ": " + request.getRequestURI();
    }


    @Override
    public ObfuscatedException toObfuscatedException() {
        return new ObfuscatedException(throwable.getClass().getName(), message, path);
    }
}
