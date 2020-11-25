package cz.inqool.entityviews.abs;

@javax.persistence.Entity
public class ManSimple extends cz.inqool.entityviews.abs.Person implements cz.inqool.entityviews.View {
    public int strength;

    public static void toEntity(Man entity, ManSimple view) {
        if (view == null) {
            return;
        }

        entity.strength = view.strength;
    }

    public static Man toEntity(ManSimple view) {
        if (view == null) {
            return null;
        }

        Man entity = new Man();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Man>> EVCollection toEntities(java.util.Collection<ManSimple> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(ManSimple view, Man entity) {
        if (entity == null) {
            return;
        }

        view.strength = entity.strength;
    }

    public static ManSimple toView(Man entity) {
        if (entity == null) {
            return null;
        }

        ManSimple view = new ManSimple();
        toView(view, entity);

        return view;
    }

    public static <EVCollection extends java.util.Collection<ManSimple>> EVCollection toViews(java.util.Collection<Man> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
