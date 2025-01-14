package cz.inqool.entityviews.multiple;

import org.hibernate.annotations.FetchMode;

@javax.persistence.Entity
public class AddressPersonList implements cz.inqool.entityviews.View {
    @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SELECT)
    @javax.persistence.ManyToOne
    @javax.persistence.JoinColumn(name = "person_id")
    public cz.inqool.entityviews.multiple.PersonList person;

    public static void toEntity(Address entity, AddressPersonList view) {
        if (view == null) {
            return;
        }

        entity.person = cz.inqool.entityviews.multiple.PersonList.toEntity(view.person);
    }

    public static Address toEntity(AddressPersonList view) {
        if (view == null) {
            return null;
        }

        Address entity = new Address();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Address>> EVCollection toEntities(java.util.Collection<AddressPersonList> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(AddressPersonList view, Address entity) {
        if (entity == null) {
            return;
        }

        view.person = cz.inqool.entityviews.multiple.PersonList.toView(entity.person);
    }

    public static AddressPersonList toView(Address entity) {
        if (entity == null) {
            return null;
        }

        AddressPersonList view = new AddressPersonList();
        toView(view, entity);
        return view;
    }

    public static <EVCollection extends java.util.Collection<AddressPersonList>> EVCollection toViews(java.util.Collection<Address> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
