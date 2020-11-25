package cz.inqool.eas.common.exception;

import cz.inqool.eas.common.domain.Domain;
import lombok.Getter;

@Getter
public class BadArgument extends GeneralException {

    private Object argument;


    public BadArgument() {
        super();
    }

    public BadArgument(Object argument) {
        super();
        this.argument = argument;
    }

    public BadArgument(Class<?> clazz, String objectId) {
        super();
        try {
            this.argument = clazz.getDeclaredConstructor().newInstance();

            if (Domain.class.isAssignableFrom(clazz)) {
                ((Domain<?>) this.argument).setId(objectId);
            }
        } catch (Exception ignored) {
        }
    }


    @Override
    public String toString() {
        if (argument != null) {
            return "BadArgument{" +
                    "argument=" + argument +
                    '}';
        } else {
            return "BadArgument{argument not specified}";
        }
    }
}
