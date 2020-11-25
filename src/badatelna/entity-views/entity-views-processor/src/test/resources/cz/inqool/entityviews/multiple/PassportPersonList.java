package cz.inqool.entityviews.multiple;

import org.hibernate.annotations.FetchMode;

@javax.persistence.Entity
public class PassportPersonList implements cz.inqool.entityviews.View {
    public static void toEntity(Passport entity, PassportPersonList view) {
        if (view == null) {
            return;
        }
    }

    public static Passport toEntity(PassportPersonList view) {
        if (view == null) {
            return null;
        }

        Passport entity = new Passport();
        toEntity(entity, view);

        return entity;
    }

    public static <EVCollection extends java.util.Collection<Passport>> EVCollection toEntities(java.util.Collection<PassportPersonList> views, java.util.function.Supplier<EVCollection> supplier) {
        if (views == null) {
            return null;
        }

        return views.stream().map(view -> toEntity(view)).collect(java.util.stream.Collectors.toCollection(supplier));
    }

    public static void toView(PassportPersonList view, Passport entity) {
        if (entity == null) {
            return;
        }
    }

    public static PassportPersonList toView(Passport entity) {
        if (entity == null) {
            return null;
        }

        PassportPersonList view = new PassportPersonList();
        toView(view, entity);
        
        return view;
    }

    public static <EVCollection extends java.util.Collection<PassportPersonList>> EVCollection toViews(java.util.Collection<Passport> entities, java.util.function.Supplier<EVCollection> supplier) {
        if (entities == null) {
            return null;
        }

        return entities.stream().map(entity -> toView(entity)).collect(java.util.stream.Collectors.toCollection(supplier));
    }
}
