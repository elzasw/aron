package cz.inqool.eas.common.dictionary;

import cz.inqool.entityviews.ViewableAnnotation;
import cz.inqool.entityviews.ViewableClass;
import cz.inqool.entityviews.ViewableMapping;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static cz.inqool.eas.common.domain.DomainViews.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ViewableClass(views = {DETAIL, LIST, CREATE, UPDATE, DictionaryViews.LABELED}, generateRef = true)
@ViewableMapping(views = {DETAIL, LIST}, mappedTo = DEFAULT)
@ViewableMapping(views = CREATE, mappedTo = CREATE)
@ViewableMapping(views = UPDATE, mappedTo = UPDATE)
@ViewableMapping(views = DictionaryViews.LABELED, mappedTo = DictionaryViews.LABELED)
@ViewableAnnotation(value = {Entity.class, BatchSize.class, Table.class}, views = {DETAIL, LIST, DictionaryViews.LABELED})
public @interface DictionaryViews {
    String LABELED = "labeled";
}
