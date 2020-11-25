package cz.inqool.entityviews;

import java.lang.annotation.*;

/**
 * Configure the default generator behaviour to restrict the set of interfaces implemented on generated views. By
 * default, all interfaces of base class will be implemented by the generated view. This annotation can restrict to
 * implement some interfaces only in a particular view.
 *
 * <pre>
 *     &#64;ViewableClass(views = {"first", "second"})
 *     &#64;ViewableImplement(views = {"second"}, value = {Serializable.class})
 *     &#64;Entity
 *     public class Foo implements Serializable {...}
 * </pre>
 * <p>
 * The generator will create two hibernate view classes:
 * <pre>
 *     &#64;Entity
 *     public class FooFirst implements View {...}
 * </pre>
 * <pre>
 *     &#64;Entity
 *     public class FooSecond implements View, Serializable {...}
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(ViewableImplements.class)
public @interface ViewableImplement {

    /**
     * Specifies the views for which the annotation will be applied (the view name has to be defined also with {@link
     * ViewableClass}) annotation.
     */
    String[] views() default {};

    /**
     * Specifies interfaces which the generated views (configured via {@link #views()}) will implement. The annotated
     * class must implement all the specified interfaces.
     */
    Class<?>[] value();
}
