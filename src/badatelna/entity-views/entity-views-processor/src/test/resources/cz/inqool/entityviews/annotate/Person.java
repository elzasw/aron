package cz.inqool.entityviews.annotate;

import javax.persistence.Entity;

import cz.inqool.entityviews.ViewableAnnotation;
import cz.inqool.entityviews.ViewableClass;
import cz.inqool.entityviews.ViewableProperty;

@ViewableClass(views = {"list", "detail"})
@ViewableAnnotation(value = javax.persistence.Entity.class, views = "detail")
@Entity
public class Person {
    public boolean active;

    public String firstName;

    @ViewableProperty(views = "detail")
    public String address;
}
