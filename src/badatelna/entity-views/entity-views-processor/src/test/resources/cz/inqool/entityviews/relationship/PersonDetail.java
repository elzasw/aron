package cz.inqool.entityviews.relationship;

@javax.persistence.Entity
public class PersonDetail implements cz.inqool.entityviews.View {
    public String firstName;

    @javax.persistence.ManyToOne
    @javax.persistence.JoinColumn(name = "address_id")
    public cz.inqool.entityviews.relationship.Address address;

    public static void toEntity(Person entity, PersonDetail view) {
        if (view == null) {
            return;
        }

        entity.firstName = view.firstName;
        entity.address = view.address;
    }

    public static Person toEntity(PersonDetail view) {
        if (view == null) {
            return null;
        }

        Person entity = new Person();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Person>> EVCollection toEntities(java.util.Collection<PersonDetail> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(PersonDetail view, Person entity) {
        if (entity == null) {
            return;
        }

        view.firstName = entity.firstName;
        view.address = entity.address;
    }

    public static PersonDetail toView(Person entity) {
        if (entity == null) {
            return null;
        }

        PersonDetail view = new PersonDetail();
        toView(view, entity);

        return view;
    }

    public static <EVCollection extends java.util.Collection<PersonDetail>> EVCollection toViews(java.util.Collection<Person> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
