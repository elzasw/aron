package cz.aron.domain.facets.dto;

/**
 * Facet kinds accepted in searchConfig.yaml. This vocabulary is published
 * verbatim by the old API's {@code /facets} endpoint, so it stays as the old
 * portal wrote it; the new API's own {@code FacetType} is mapped from it in
 * {@code cz.aron.web.v1.SearchController}.
 *
 * <p>{@code MULTI_REF_EXT} is served by the old API only - the new API neither
 * advertises nor serves it (its option enumeration needs the nested relation
 * aggregations and the never-populated {@code incomingRelTypeGroups}).
 */
public enum FacetType {
    FULLTEXT,
    FULLTEXTF,
    ENUM,
    MULTI_REF,
    UNITDATE,
    MULTI_REF_EXT
}
