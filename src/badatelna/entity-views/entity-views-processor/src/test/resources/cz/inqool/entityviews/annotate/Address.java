package cz.inqool.entityviews.annotate;

import cz.inqool.entityviews.ViewableAnnotation;
import cz.inqool.entityviews.ViewableClass;

import javax.persistence.Embeddable;

@ViewableClass(views = {"person_list", "person_detail", "person_create"})
@ViewableAnnotation(views = {"person_list", "person_detail"}, value = Embeddable.class)
@Embeddable
public class Address {
    public String country;
}
