package cz.inqool.entityviews;


import java.lang.annotation.*;

/**
 * Maps views to specified views in other side of relationship or inheritance.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ViewableMappings.class)
public @interface ViewableMapping {
    public static final String SKIP = "INTERNAL_SKIP";

    /**
     * Specifies the views for which the annotation will be applied (the view name has to be defined also with {@link
     * ViewableClass}) annotation.
     */
    String[] views();

    /**
     * Specify the view of target entity used in mapping in generated view. Defaults to annotated property type.
     */
    String mappedTo() default "";

    boolean useRef() default false;

    boolean useOneWay() default false;
}
