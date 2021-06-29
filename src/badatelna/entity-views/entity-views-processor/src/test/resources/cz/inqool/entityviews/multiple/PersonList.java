package cz.inqool.entityviews.multiple;

@javax.persistence.Entity
public class PersonList implements cz.inqool.entityviews.View {
    @javax.persistence.OneToMany(mappedBy = "person", fetch = javax.persistence.FetchType.EAGER)
    public java.util.List<cz.inqool.entityviews.multiple.AddressPersonList> addresses;

    @javax.persistence.OneToMany(fetch = javax.persistence.FetchType.EAGER )
    @javax.persistence.JoinColumn(name = "person_id")
    public java.util.List<cz.inqool.entityviews.multiple.PassportPersonList> passports;

    public static void toEntity(Person entity, PersonList view) {
        if (view == null) {
            return;
        }

        entity.addresses = cz.inqool.entityviews.multiple.AddressPersonList.toEntities(view.addresses, java.util.ArrayList::new);
        entity.passports = cz.inqool.entityviews.multiple.PassportPersonList.toEntities(view.passports, java.util.ArrayList::new);
        if (entity.passports != null) entity.passports.stream().filter((o)->o != null).forEach((o)->o.setPerson(entity));
    }

    public static Person toEntity(PersonList view) {
        if (view == null) {
            return null;
        }

        Person entity = new Person();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Person>> EVCollection toEntities(java.util.Collection<PersonList> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(PersonList view, Person entity) {
        if (entity == null) {
            return;
        }

        view.addresses = cz.inqool.entityviews.multiple.AddressPersonList.toViews(entity.addresses, java.util.ArrayList::new);
        view.passports = cz.inqool.entityviews.multiple.PassportPersonList.toViews(entity.passports, java.util.ArrayList::new);
    }

    public static PersonList toView(Person entity) {
        if (entity == null) {
            return null;
        }

        PersonList view = new PersonList();
        toView(view, entity);

        return view;
    }

    public static <EVCollection extends java.util.Collection<PersonList>> EVCollection toViews(java.util.Collection<Person> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
