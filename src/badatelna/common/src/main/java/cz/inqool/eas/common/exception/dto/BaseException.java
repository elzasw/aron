package cz.inqool.eas.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import cz.inqool.eas.common.exception.CodedException;
import cz.inqool.eas.common.exception.CodedException.ExceptionCodeEnum;
import cz.inqool.eas.common.exception.DetailedException;
import cz.inqool.eas.common.exception.GeneralException;

import java.time.Instant;

import static cz.inqool.eas.common.utils.JsonUtils.toJsonString;

/**
 * Abstract class serving as a base for exception handling objects.
 */
@JsonInclude(Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public abstract class BaseException {

    protected String timestamp;
    protected Enum<? extends ExceptionCodeEnum<?>> code;
    protected Class<?> exception;
    protected String message;
    protected String cause;
    protected Object details;


    protected BaseException(Throwable exception) {
        this(exception, exception.getMessage());
    }

    protected BaseException(Throwable exception, String message) {
        this.timestamp = Instant.now().toString();
        if (exception instanceof CodedException) {
            this.code = ((CodedException) exception).getErrorCode();
        }
        if (exception instanceof DetailedException) {
            this.details = ((DetailedException) exception).getDetails();
        }
        this.message = message;
        this.exception = exception.getClass();


        Throwable cause = getLastCause(exception);
        if (cause != exception) {
            if (cause instanceof GeneralException) {
                this.cause = cause.toString();
            } else {
                this.cause = cause.getClass().getName() + ": " + cause.getMessage();
            }
        }
    }


    protected ObfuscatedException toObfuscatedException() {
        return new ObfuscatedException(exception.getName(), message, null);
    }

    @Override
    public String toString() {
        return toJsonString(this, true);
    }

    private static Throwable getLastCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause;
    }
}
