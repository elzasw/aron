package cz.inqool.eas.common.exception.parser;

import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolationException;

@Component
public class ConstraintViolationExceptionParser implements ExceptionMessageParser<ConstraintViolationException> {

    @Override
    public Class<ConstraintViolationException> getType() {
        return ConstraintViolationException.class;
    }

    @Override
    public String getMessage(ConstraintViolationException throwable) {
        return throwable.getMessage(); // todo add more complex message parsing
    }
}
