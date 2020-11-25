package cz.inqool.eas.common.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility methods for working with collections.
 */
public class CollectionUtils {
    /**
     * Sorts given collection of objects by the order specified in the list of their IDs.
     */
    public static <T> List<T> sortByIds(List<String> ids, Collection<T> objects, Function<T, String> mapper) {
        return objects.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(o -> ids.indexOf(mapper.apply(o))))
                .collect(Collectors.toList());
    }

    /**
     * Returns concatenated list of base plus any supplied values
     *
     * @param base Base collection of values
     * @param a Other values
     * @param <T> Type of values
     * @return Concatenated List
     */
    public static <T> List<T> asList(Collection<T> base, T... a) {
        List<T> list = new ArrayList<>(base);
        list.addAll(Arrays.asList(a));

        return list;
    }

    /**
     * Concatenates given items using provided separator. This method filters out all {@code null} or empty values.
     */
    public static String join(CharSequence separator, Object... items) {
        return Arrays.stream(items)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(separator));
    }

    /**
     * Concatenates given items using providen separator. This method filters out all {@code null} or empty values.
     */
    public static String join(CharSequence separator, CharSequence... items) {
        return join(separator, Arrays.asList(items));
    }

    /**
     * Concatenates given items using providen separator. This method filters out all {@code null} or empty values.
     */
    public static String join(CharSequence separator, Collection<? extends CharSequence> items) {
        return items.stream()
                .filter(s -> s != null && s.length() > 0)
                .collect(Collectors.joining(separator));
    }
}
