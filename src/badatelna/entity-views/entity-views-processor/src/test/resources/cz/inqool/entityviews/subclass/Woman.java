package cz.inqool.entityviews.subclass;

import cz.inqool.entityviews.ViewableClass;
import cz.inqool.entityviews.ViewableMapping;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@ViewableClass(views = {"detail"})
@ViewableMapping(views = {"detail"}, mappedTo = "detail")
@Entity
@DiscriminatorValue("WOMAN")
public class Woman extends Person {

    public int kindness;

    @Override
    public String getType() {
        return "WOMAN";
    }
}
