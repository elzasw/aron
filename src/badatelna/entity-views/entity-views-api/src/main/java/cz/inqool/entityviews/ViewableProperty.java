package cz.inqool.entityviews;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure the default generator behaviour to include annotated property only in specified views.
 * <pre>
 *     &#64;ViewableClass(views = {"list", "detail"})
 *     &#64;Entity
 *     public class Operator {
 *         private String id;
 *         private String name;
 *         &#64;ViewableProperty(views = "detail")
 *         private Integer age;
 *     }
 * </pre>
 * will generate two view classes:
 * <pre>
 *     &#64;Entity
 *     public class OperatorDetail implements View {
 *         private String id;
 *         private String name;
 *         private Integer age;
 *
 *         ...
 *     }
 * </pre>
 * <pre>
 *     &#64;Entity
 *     public class OperatorList implements View {
 *         private String id;
 *         private String name;
 *
 *         ...
 *     }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ViewableProperty {

    /**
     * Include annotated property only in specified views.
     */
    String[] views() default {};
}
