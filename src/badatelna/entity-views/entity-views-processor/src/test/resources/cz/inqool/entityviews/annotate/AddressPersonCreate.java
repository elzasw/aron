package cz.inqool.entityviews.annotate;

public class AddressPersonCreate implements cz.inqool.entityviews.View {
    public String country;

    public static void toEntity(Address entity, AddressPersonCreate view) {
        if (view == null) {
            return;
        }
        entity.country = view.country;
    }

    public static Address toEntity(AddressPersonCreate view) {
        if (view == null) {
            return null;
        }
        Address entity = new Address();
        toEntity(entity, view);
        return entity;
    }

    public static <EVCollection extends java.util.Collection<Address>>EVCollection toEntities(java.util.Collection<AddressPersonCreate> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(AddressPersonCreate view, Address entity) {
        if (entity == null) {
            return;
        }
        view.country = entity.country;
    }

    public static AddressPersonCreate toView(Address entity) {
        if (entity == null) {
            return null;
        }
        AddressPersonCreate view = new AddressPersonCreate();
        toView(view, entity);
        return view;
    }

    public static <EVCollection extends java.util.Collection<AddressPersonCreate>>EVCollection toViews(java.util.Collection<Address> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
