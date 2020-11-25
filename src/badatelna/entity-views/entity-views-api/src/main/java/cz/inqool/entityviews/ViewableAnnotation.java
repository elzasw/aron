package cz.inqool.entityviews;

import java.lang.annotation.*;

/**
 * Configure the default generator behaviour to restrict the set of annotatios placed on generated views. By default,
 * all annotations of base class will be placed on the generated view. This annotation can restrict to place some
 * annotations only in a particular view.
 *
 * <pre>
 *     &#64;ViewableClass(views = {"first", "second"})
 *     &#64;ViewableAnnotation(views = {"second"}, value = {Entity.class})
 *     &#64;Entity
 *     public class Foo {...}
 * </pre>
 * <p>
 * The generator will create two hibernate view classes:
 * <pre>
 *     public class FooFirst implements View {...}
 * </pre>
 * <pre>
 *     &#64;Entity
 *     public class FooSecond implements View {...}
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ViewableAnnotations.class)
public @interface ViewableAnnotation {

    /**
     * Specifies the views for which the annotation will be applied (the view name has to be defined also with {@link
     * ViewableClass}) annotation.
     */
    String[] views() default {};

    /**
     * Specifies annotations placed on generated views (configured via {@link #views()}). The annotated (base) class
     * must be annotated with all specified annotations.
     */
    Class<?>[] value();
}
