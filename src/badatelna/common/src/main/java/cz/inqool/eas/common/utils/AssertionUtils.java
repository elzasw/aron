package cz.inqool.eas.common.utils;

import cz.inqool.eas.common.exception.GeneralException;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static cz.inqool.eas.common.utils.AopUtils.isProxy;
import static cz.inqool.eas.common.utils.AopUtils.unwrap;

/**
 * Utility methods for asserting properties of supplied object.
 */
public class AssertionUtils {
    /**
     * Checks if provided value is greater than another and throw conditional exception if not.
     *
     * @param x Value to check
     * @param y Value to check against
     * @param supplier Supplier of exception
     */
    public static void gt(Integer x, Integer y, Supplier<RuntimeException> supplier) {
        if (x <= y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#gt(Integer, Integer, Supplier)
     */
    public static void gt(Long x, Long y, Supplier<RuntimeException> supplier) {
        if (x <= y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#gt(Integer, Integer, Supplier)
     */
    public static void gt(BigDecimal x, BigDecimal y, Supplier<RuntimeException> supplier) {
        if (x.compareTo(y) <= 0) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is greater or equal as another and throw conditional exception if not.
     *
     * @param x Value to check
     * @param y Value to check against
     * @param supplier Supplier of exception
     */
    public static void gte(Integer x, Integer y, Supplier<RuntimeException> supplier) {
        if (x < y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#gte(Integer, Integer, Supplier)
     */
    public static void gte(Long x, Long y, Supplier<RuntimeException> supplier) {
        if (x < y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#gte(Integer, Integer, Supplier)
     */
    public static void gte(BigDecimal x, BigDecimal y, Supplier<RuntimeException> supplier) {
        if (x.compareTo(y) < 0) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is less than another and throw conditional exception if not.
     *
     * @param x Value to check
     * @param y Value to check against
     * @param supplier Supplier of exception
     */
    public static void lt(Integer x, Integer y, Supplier<RuntimeException> supplier) {
        if (x >= y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#lt(Integer, Integer, Supplier)
     */
    public static void lt(Long x, Long y, Supplier<RuntimeException> supplier) {
        if (x >= y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#lt(Integer, Integer, Supplier)
     */
    public static void lt(BigDecimal x, BigDecimal y, Supplier<RuntimeException> supplier) {
        if (x.compareTo(y) >= 0) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is less or equal as another and throw conditional exception if not.
     *
     * @param x Value to check
     * @param y Value to check against
     * @param supplier Supplier of exception
     */
    public static void lte(Integer x, Integer y, Supplier<RuntimeException> supplier) {
        if (x > y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#lte(Integer, Integer, Supplier)
     */
    public static void lte(Long x, Long y, Supplier<RuntimeException> supplier) {
        if (x > y) {
            throw supplier.get();
        }
    }

    /**
     * @see AssertionUtils#lte(Integer, Integer, Supplier)
     */
    public static void lte(BigDecimal x, BigDecimal y, Supplier<RuntimeException> supplier) {
        if (x.compareTo(y) > 0) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is equal as another and throw conditional exception if not.
     *
     * @param x Value to check
     * @param y Value to check against
     * @param supplier Supplier of exception
     */
    public static <U> void eq(U x, U y, Supplier<RuntimeException> supplier) {
        if (!Objects.equals(x, y)) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is not equal as another and throw conditional exception if not.
     *
     * @param x Value to check
     * @param y Value to check against
     * @param supplier Supplier of exception
     */
    public static <U> void ne(U x, U y, Supplier<RuntimeException> supplier) {
        if (Objects.equals(x, y)) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is true and throw conditional exception if not.
     *
     * @param o Value to check
     * @param supplier Supplier of exception
     */
    public static void isTrue(Boolean o, Supplier<RuntimeException> supplier) {
        if (o != null && !o) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is false and throw conditional exception if not.
     *
     * @param o Value to check
     * @param supplier Supplier of exception
     */
    public static void isFalse(Boolean o, Supplier<RuntimeException> supplier) {
        if (o != null && o) {
            throw supplier.get();
        }
    }

    /**
     * Checks if provided value is not null and throw conditional exception if not.
     *
     * @param o Value to check
     * @param supplier Supplier of exception
     */
    public static void notNull(Object o, Supplier<RuntimeException> supplier) {
        if (o == null) {
            throw supplier.get();
        } else if (o instanceof Optional) {
            if (((Optional<?>) o).isEmpty()) {
                throw supplier.get();
            }
        } else if (isProxy(o)) {
            if (unwrap(o) == null) {
                throw supplier.get();
            }
        }
    }

    /**
     * Checks if provided value is null and throw conditional exception if not.
     *
     * @param o Value to check
     * @param supplier Supplier of exception
     */
    public static void isNull(Object o, Supplier<RuntimeException> supplier) {
        if (o instanceof Optional) {
            if (((Optional<?>) o).isPresent()) {
                throw supplier.get();
            }
        } else if (isProxy(o)) {
            if (unwrap(o) != null) {
                throw supplier.get();
            }
        } else if (o != null) {
            throw supplier.get();
        }
    }

    /**
     * Checks whether object is an instance of provided class. Throws an exception if not.
     *
     * @param o        object to be checked
     * @param clazz    class that the object should be instance of
     * @param supplier provides an exception instance
     * @param <O>      type of object to be checked
     * @param <C>      type of class that the object should be instance of
     * @see Class#isInstance(Object)
     */
    public static <O, C extends O> void isInstance(@Nullable O o, @Nonnull Class<C> clazz, @Nonnull Supplier<RuntimeException> supplier) {
        if (!clazz.isInstance(o)) {
            throw supplier.get();
        }
    }

    /**
     * Cast object to provided type or throw conditional exception
     *
     * @param o        object to be casted
     * @param clazz    type that the object will be casted to
     * @param supplier provides an exception instance
     * @param <O>      type of object to be casted
     * @param <C>      type of class that the object will be casted to
     * @return casted object
     * @see Class#cast(Object)
     */
    public static <O, C extends O> C cast(@Nullable O o, @Nonnull Class<C> clazz, @Nonnull Supplier<RuntimeException> supplier) {
        isInstance(o, clazz, supplier);
        return clazz.cast(o);
    }

    /**
     * Executes given consumer operation on the value if it's not {@code null}
     *
     * @param value    to be used as an argument for given consumer
     * @param consumer operation to be executed on given value
     * @param <T>      type of the value
     */
    public static <T> void ifPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    /**
     * Method for checking of if String is empty or null
     *
     * @param string   String to be checked
     * @param supplier Exception supplier
     */
    public static void notEmpty(String string, Supplier<RuntimeException> supplier) {
        if (string == null || string.isEmpty()) {
            throw supplier.get();
        }
    }

    /**
     * Checks whether given {@code collection} is not empty. Throws an exception provided by given {@code supplier}, if
     * the collection is {@code null} or empty.
     *
     * @param collection collection to be checked
     * @param supplier   provides an exception instance to be thrown if the collection is {@code null} or empty
     */
    public static void notEmpty(Collection<?> collection, Supplier<RuntimeException> supplier) {
        if (collection == null || collection.isEmpty()) {
            throw supplier.get();
        }
    }

    /**
     * Checks whether given {@code collection} contains given object.
     *
     * @param o          object that will be checked whether it's contained in given collection
     * @param collection Collection of objects
     * @param supplier   provides an exception instance to be thrown if the objects is not contained in given
     *                   collection
     * @param <U>        type of elements in given collection
     */
    public static <U> void isIn(U o, Collection<U> collection, Supplier<RuntimeException> supplier) {
        if (collection == null || !collection.contains(o)) {
            throw supplier.get();
        }
    }

    /**
     * Returns supplied value or conditional value from factory if the value is null.
     *
     * @param value Value
     * @param factory Factory to create value
     * @param <T> Type of value
     */
    public static <T> T coalesce(T value, Supplier<T> factory) {
        return value != null ? value : factory.get();
    }

    /**
     * Wraps any exception in {@link GeneralException} if not already one.
     *
     * @param e Exception to wrap
     */
    public static GeneralException wrapException(Exception e) {
        if (e instanceof GeneralException) {
            return (GeneralException) e;
        } else {
            return new GeneralException(e);
        }
    }
}
