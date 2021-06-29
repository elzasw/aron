package cz.inqool.eas.common.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * Intersects two collections and return the result.
     */
    public static <T> Set<T> intersect(Collection<T> one, Collection<T> two) {
        boolean oneIsSmaller = one.size() <= two.size();
        Collection<T> smaller = oneIsSmaller ? one : two;
        Collection<T> larger  = oneIsSmaller ? two : one;

        return smaller.stream()
                .distinct()
                .filter(larger::contains)
                .collect(Collectors.toSet());
    }

    /**
     * Intersects two collections and return true if there is at least one object.
     */
    public static <T> boolean isIntersect(Collection<T> one, Collection<T> two) {
        return intersect(one, two).size() > 0;
    }

    public static <T> Set<T> merge(Set<T> first, Set<T> second) {
        return Stream
                .concat(first.stream(), second.stream())
                .collect(Collectors.toSet());
    }

    public static <T> Set<T> merge(Set<T> first, T second) {
        return Stream
                .concat(first.stream(), Stream.of(second))
                .collect(Collectors.toSet());
    }

    public static <T> List<T> merge(List<T> first, List<T> second) {
        return Stream
                .concat(first.stream(), second.stream())
                .collect(Collectors.toList());
    }

    @SafeVarargs
    public static <T> List<T> merge(List<? extends T>... lists) {
        return Arrays
                .stream(lists)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static <T> List<T> merge(List<T> first, T second) {
        return Stream
                .concat(first.stream(), Stream.of(second))
                .collect(Collectors.toList());
    }

    public static <T,U> Map<T, U> merge(Map<T, U> first, Map<T, U> second) {
        return Stream
                .concat(first.entrySet().stream(), second.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <T,U> Map<T, U> merge(Map<T, U> first, T secondKey, U secondValue) {
        return Stream
                .concat(first.entrySet().stream(), Stream.of(Map.entry(secondKey, secondValue)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
