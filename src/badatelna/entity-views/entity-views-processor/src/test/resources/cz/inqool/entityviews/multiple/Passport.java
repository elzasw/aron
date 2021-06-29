package cz.inqool.entityviews.multiple;

import cz.inqool.entityviews.ViewableClass;
import cz.inqool.entityviews.ViewableProperty;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@ViewableClass(views = {"person_list"})
@Entity
public class Passport {
    @ViewableProperty(views = {})
    @Fetch(FetchMode.SELECT)
    @ManyToOne
    @JoinColumn(name = "person_id")
    public Person person;

    @ViewableProperty(views = {})
    public void setPerson(Person person) {
        this.person = person;
    }
}
