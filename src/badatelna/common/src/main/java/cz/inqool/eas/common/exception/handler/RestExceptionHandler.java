package cz.inqool.eas.common.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import cz.inqool.eas.common.exception.*;
import cz.inqool.eas.common.exception.dto.ObfuscatedException;
import cz.inqool.eas.common.exception.dto.RestException;
import cz.inqool.eas.common.exception.parser.ExceptionParser;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.search.SearchParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import java.util.HashSet;
import java.util.Set;

import static cz.inqool.eas.common.exception.handler.RestExceptionHandler.Statics.HEADERS;
import static cz.inqool.eas.common.exception.handler.RestExceptionHandler.Statics.IGNORED_EXCEPTIONS;

/**
 * Class handling exceptions raised during REST endpoint executions. Provides unified and more detailed information
 * about exceptions.
 */
@Slf4j
@ControllerAdvice
public class RestExceptionHandler {
    private ExceptionParser exceptionParser;

    @ExceptionHandler({
            BadArgument.class,
            BindException.class,
            HttpMediaTypeException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            InvalidAttribute.class,
            InvalidArgument.class,
            MissingAttribute.class,
            MethodArgumentNotValidException.class,
            //DeletionException.class,
            JsonProcessingException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<RestException> badRequest(HttpServletRequest request, Exception e) {
        return defaultExceptionHandling(request, e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            HttpMessageConversionException.class
    })
    public ResponseEntity<RestException> badConversion(HttpServletRequest request, Exception e) {
        if (e.getCause() instanceof JsonMappingException) {
            JsonMappingException cause = (JsonMappingException) e.getCause();
            if (cause.getCause() instanceof GeneralException) {
                return badRequest(request, (GeneralException) cause.getCause());
            }
        }
        return defaultExceptionHandling(request, e, HttpStatus.BAD_REQUEST);
    }

    /*@ExceptionHandler({
            BadCredentialsException.class
    })
    public ResponseEntity<RestException> unauthorized(HttpServletRequest request, Exception e) {
        return defaultExceptionHandling(request, e, HttpStatus.UNAUTHORIZED);
    }*/

    @ExceptionHandler({
            //UserPermissionException.class,
            //InstitutionPermissionException.class,
            ForbiddenObject.class,
            ForbiddenOperation.class,
            AccessDeniedException.class
    })
    public ResponseEntity<RestException> forbidden(HttpServletRequest request, Exception e) {
        return defaultExceptionHandling(request, e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            MissingObject.class,
            HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<RestException> notFound(HttpServletRequest request, Exception e) {
        return defaultExceptionHandling(request, e, HttpStatus.NOT_FOUND);
    }

//    @ExceptionHandler({ // TODO uncomment after exception classes are added
//    })
//    public ResponseEntity<RestException> conflict(HttpServletRequest request, Exception e) {
//        return defaultExceptionHandling(request, e, HttpStatus.CONFLICT);
//    }

    @ExceptionHandler({
            SearchPhaseExecutionException.class
    })
    public ResponseEntity<RestException> preconditionFailed(HttpServletRequest request, Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e.getCause() != null && e.getCause() instanceof SearchParseException && e.getCause().getMessage() != null && e.getCause().getMessage().startsWith("No mapping found for")) {
            status = HttpStatus.PRECONDITION_FAILED;
        }
        return defaultExceptionHandling(request, e, status);
    }

    @ExceptionHandler({
            Exception.class
    })
    public ResponseEntity<RestException> exception(HttpServletRequest request, Exception e) {
        return defaultExceptionHandling(request, e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<RestException> defaultExceptionHandling(HttpServletRequest request, Exception e, HttpStatus status) {
        return defaultExceptionHandling(request, e, status, true);
    }

    private ResponseEntity<RestException> defaultExceptionHandling(HttpServletRequest request, Exception e, HttpStatus status, boolean logError) {
        RestException error = new RestException(request, status, e, exceptionParser.getMessage(e));
        if (logError) {
            String msg = "\n" + error.toString();
            if (IGNORED_EXCEPTIONS.contains(error.toObfuscatedException())) { // do not log stacktrace of ignored exceptions
                log.error(msg);
            } else {
                log.error(msg, e);
            }
        }
        return new ResponseEntity<>(error, HEADERS, status);
    }


    @Autowired
    public void setExceptionParser(ExceptionParser parser) {
        this.exceptionParser = parser;
    }

    /**
     * Class containing static configuration variables
     */
    static class Statics {

        /** Set of exceptions which will be logged only limitedly */
        static final Set<ObfuscatedException> IGNORED_EXCEPTIONS = new HashSet<>();

        /** HTTP Headers which will be included on all REST error responses */
        static final HttpHeaders HEADERS = new HttpHeaders();

        static {
            IGNORED_EXCEPTIONS.addAll(Set.of(
                    new ObfuscatedException(MethodArgumentNotValidException.class, null, null),
                    new ObfuscatedException(AccessDeniedException.class, null, null)
//                    new IgnoredException(SearchPhaseExecutionException.class, "all shards failed", null)
            ));

            HEADERS.setContentType(MediaType.APPLICATION_JSON);
        }
    }


}
