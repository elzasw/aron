package cz.inqool.entityviews.implement;

public class CyborgList implements cz.inqool.entityviews.View {

    public static void toEntity(Cyborg entity, CyborgList view) {
        if (view == null) {
            return;
        }
    }

    public static Cyborg toEntity(CyborgList view) {
        if (view == null) {
            return null;
        }

        Cyborg entity = new Cyborg();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Cyborg>> EVCollection toEntities(java.util.Collection<CyborgList> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(CyborgList view, Cyborg entity) {
        if (entity == null) {
            return;
        }
    }

    public static CyborgList toView(Cyborg entity) {
        if (entity == null) {
            return null;
        }

        CyborgList view = new CyborgList();
        toView(view, entity);

        return view;
    }

    public static <EVCollection extends java.util.Collection<CyborgList>> EVCollection toViews(java.util.Collection<Cyborg> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}