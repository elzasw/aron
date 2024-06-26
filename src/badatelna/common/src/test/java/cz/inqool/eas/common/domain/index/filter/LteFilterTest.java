package cz.inqool.eas.common.domain.index.filter;

import cz.inqool.eas.common.dao.simple.multiple.MultipleFieldsIndexedObject.IndexFields;
import cz.inqool.eas.common.domain.index.dto.filter.Filter;
import cz.inqool.eas.common.domain.index.dto.filter.LteFilter;
import cz.inqool.eas.common.domain.index.dto.params.Params;
import cz.inqool.eas.common.exception.InvalidAttribute;
import cz.inqool.eas.common.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LteFilterTest extends IndexFilterTestBase {

    @Test
    void serialize() {
        Filter filter = new LteFilter(IndexFields.integerObject, 40);

        String jsonFilter = JsonUtils.toJsonString(filter, true);
        String expectedJsonFilter = "{\r\n" +
                "  \"operation\" : \"LTE\",\r\n" +
                "  \"nestedQueryEnabled\" : true,\r\n" +
                "  \"field\" : \"integerObject\",\r\n" +
                "  \"value\" : \"40\",\r\n" +
                "  \"relation\" : null\r\n" +
                "}";

        assertEquals(expectedJsonFilter, jsonFilter);
    }

    @Test
    void serializeWithParams() {
        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.integerObject, 40)
        );

        String jsonParams = JsonUtils.toJsonString(params, true);
        String expectedJsonParams = "{\r\n" +
                "  \"sort\" : [ ],\r\n" +
                "  \"offset\" : null,\r\n" +
                "  \"size\" : 10,\r\n" +
                "  \"searchAfter\" : null,\r\n" +
                "  \"flipDirection\" : false,\r\n" +
                "  \"filters\" : [ {\r\n" +
                "    \"operation\" : \"LTE\",\r\n" +
                "    \"nestedQueryEnabled\" : true,\r\n" +
                "    \"field\" : \"integerObject\",\r\n" +
                "    \"value\" : \"40\",\r\n" +
                "    \"relation\" : null\r\n" +
                "  } ],\r\n" +
                "  \"aggregations\" : [ ],\r\n" +
                "  \"fields\" : null,\r\n" +
                "  \"include\" : [ ]\r\n" +
                "}";

        assertEquals(expectedJsonParams, jsonParams);
    }

    @Test
    void deserialize() {
        String jsonFilter = "{\r\n" +
                "  \"operation\" : \"LTE\",\r\n" +
                "  \"nestedQueryEnabled\" : true,\r\n" +
                "  \"field\" : \"integerObject\",\r\n" +
                "  \"value\" : \"40\",\r\n" +
                "  \"relation\" : null\r\n" +
                "}";
        Filter filter = JsonUtils.fromJsonString(jsonFilter, Filter.class);

        Filter expectedFilter = new LteFilter(IndexFields.integerObject, 40);

        assertThat(filter).isEqualToComparingFieldByField(expectedFilter);
    }

    @Test
    void deserializeWithParams() {
        String jsonParams = "{\r\n" +
                "  \"sort\" : [ ],\r\n" +
                "  \"offset\" : null,\r\n" +
                "  \"size\" : 10,\r\n" +
                "  \"searchAfter\" : null,\r\n" +
                "  \"flipDirection\" : false,\r\n" +
                "  \"filters\" : [ {\r\n" +
                "    \"operation\" : \"LTE\",\r\n" +
                "    \"nestedQueryEnabled\" : true,\r\n" +
                "    \"field\" : \"integerObject\",\r\n" +
                "    \"value\" : \"40\",\r\n" +
                "    \"relation\" : null\r\n" +
                "  } ],\r\n" +
                "  \"aggregations\" : [ ],\r\n" +
                "  \"fields\" : null,\r\n" +
                "  \"include\" : [ ]\r\n" +
                "}";
        Params params = JsonUtils.fromJsonString(jsonParams, Params.class);

        Params expectedParams = new Params();
        expectedParams.addFilter(
                new LteFilter(IndexFields.integerObject, 40)
        );

        assertThat(params).isEqualToComparingFieldByField(expectedParams);
    }

    @Test
    void integerLte_first() {
        entity_1.setIntegerObject(40);
        entity_2.setIntegerObject(43);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.integerObject, 40)
        );

        assertMatchesFirst(() -> repository.listByParams(params));
    }

    @Test
    void integerLte_second() {
        entity_1.setIntegerObject(45);
        entity_2.setIntegerObject(43);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.integerObject, 43)
        );

        assertMatchesSecond(() -> repository.listByParams(params));
    }

    @Test
    void integerLte_all() {
        entity_1.setIntegerObject(45);
        entity_2.setIntegerObject(43);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.integerObject, 50)
        );

        assertMatchesBoth(() -> repository.listByParams(params));
    }

    @Test
    void integerLte_none() {
        entity_1.setIntegerObject(45);
        entity_2.setIntegerObject(43);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.integerObject, 41)
        );

        assertMatchesNone(() -> repository.listByParams(params));
    }

    @Test
    void longLte_first() {
        entity_1.setLongObject(40L);
        entity_2.setLongObject(43L);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.longObject, 40L)
        );

        assertMatchesFirst(() -> repository.listByParams(params));
    }

    @Test
    void longLte_second() {
        entity_1.setLongObject(45L);
        entity_2.setLongObject(43L);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.longObject, 43L)
        );

        assertMatchesSecond(() -> repository.listByParams(params));
    }

    @Test
    void longLte_all() {
        entity_1.setLongObject(45L);
        entity_2.setLongObject(43L);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.longObject, 50L)
        );

        assertMatchesBoth(() -> repository.listByParams(params));
    }

    @Test
    void longLte_none() {
        entity_1.setLongObject(45L);
        entity_2.setLongObject(43L);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.longObject, 41L)
        );

        assertMatchesNone(() -> repository.listByParams(params));
    }

    @Test
    void doubleLte_first() {
        entity_1.setDoubleObject(40.3);
        entity_2.setDoubleObject(43.4);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.doubleObject, 40.3)
        );

        assertMatchesFirst(() -> repository.listByParams(params));
    }

    @Test
    void doubleLte_second() {
        entity_1.setDoubleObject(45.3);
        entity_2.setDoubleObject(43.4);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.doubleObject, 43.4)
        );

        assertMatchesSecond(() -> repository.listByParams(params));
    }

    @Test
    void doubleLte_all() {
        entity_1.setDoubleObject(45.3);
        entity_2.setDoubleObject(43.4);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.doubleObject, 45.3)
        );

        assertMatchesBoth(() -> repository.listByParams(params));
    }

    @Test
    void doubleLte_none() {
        entity_1.setDoubleObject(45.3);
        entity_2.setDoubleObject(43.4);
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.doubleObject, 41.8)
        );

        assertMatchesNone(() -> repository.listByParams(params));
    }

    @Test
    void dateLte_first() {
        entity_1.setLocalDate(LocalDate.parse("2016-01-01"));
        entity_2.setLocalDate(LocalDate.parse("2017-01-01"));
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.localDate, "2016-01-01")
        );

        assertMatchesFirst(() -> repository.listByParams(params));
    }

    @Test
    void dateLte_second() {
        entity_1.setLocalDate(LocalDate.parse("2018-01-01"));
        entity_2.setLocalDate(LocalDate.parse("2017-01-01"));
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.localDate, "2017-01-01")
        );

        assertMatchesSecond(() -> repository.listByParams(params));
    }

    @Test
    void dateLte_all() {
        entity_1.setLocalDate(LocalDate.parse("2018-01-01"));
        entity_2.setLocalDate(LocalDate.parse("2017-01-01"));
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.localDate, "2018-01-01")
        );

        assertMatchesBoth(() -> repository.listByParams(params));
    }

    @Test
    void dateLte_none() {
        entity_1.setLocalDate(LocalDate.parse("2018-01-01"));
        entity_2.setLocalDate(LocalDate.parse("2017-01-01"));
        repository.update(Set.of(entity_1, entity_2));

        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.localDate, "2016-12-31")
        );

        assertMatchesNone(() -> repository.listByParams(params));
    }

    @Test
    void filterFieldNotMapped() {
        Params params = new Params();
        params.addFilter(
                new LteFilter("nonMapped", "Hammer")
        );

        assertThrows(InvalidAttribute.class, () -> repository.listByParams(params));
    }

    @Test
    void filterFieldNotLeaf() {
        Params params = new Params();
        params.addFilter(
                new LteFilter(IndexFields.toOneRelationshipNested, "47")
        );

        assertThrows(InvalidAttribute.class, () -> repository.listByParams(params));
    }
}
