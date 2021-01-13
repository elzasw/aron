package cz.inqool.eas.common.utils;

import org.reflections.Reflections;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Set;

public class ReflectionUtils {

    private static Reflections REFLECTIONS;


    /**
     * Returns project classes reflections (in package "cz.inqool.peva")
     */
    private static Reflections getReflections() {
        if (REFLECTIONS == null) {
            REFLECTIONS = new Reflections("cz.inqool");
        }
        return REFLECTIONS;
    }

    public static <T> Set<Class<? extends T>> getSubTypesOf(Class<T> superClass) {
        return getReflections().getSubTypesOf(superClass);
    }

    public static Class<?> resolveType(java.lang.reflect.Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            Object unresolvedType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (unresolvedType instanceof TypeVariable) {
                return (Class<?>) ((TypeVariable<?>) unresolvedType).getBounds()[0];
            } else if (unresolvedType instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) unresolvedType).getRawType();
            } else {
                return (Class<?>) unresolvedType;
            }
        } else if (field.getType().isArray()) {
            return field.getType().getComponentType();
        } else {
            return field.getType();
        }
    }
}
