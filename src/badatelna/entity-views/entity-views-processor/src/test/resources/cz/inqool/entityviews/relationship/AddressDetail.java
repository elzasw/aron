package cz.inqool.entityviews.relationship;

@javax.persistence.Entity
public class AddressDetail implements cz.inqool.entityviews.View {
    public String country;

    public String street;

    public static void toEntity(Address entity, AddressDetail view) {
        if (view == null) {
            return;
        }

        entity.country = view.country;
        entity.street = view.street;
    }

    public static Address toEntity(AddressDetail view) {
        if (view == null) {
            return null;
        }

        Address entity = new Address();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Address>> EVCollection toEntities(java.util.Collection<AddressDetail> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(AddressDetail view, Address entity) {
        if (entity == null) {
            return;
        }

        view.country = entity.country;
        view.street = entity.street;
    }

    public static AddressDetail toView(Address entity) {
        if (entity == null) {
            return null;
        }

        AddressDetail view = new AddressDetail();
        toView(view, entity);

        return view;
    }

    public static <EVCollection extends java.util.Collection<AddressDetail>> EVCollection toViews(java.util.Collection<Address> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
