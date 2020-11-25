package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.IndexFieldGeoPointLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

/**
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-geo-distance-query.html">Geo-distance query</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class GeoDistanceFilter extends GeoFilter {

    @NotNull
    private GeoPoint point;

    @NotNull
    @PositiveOrZero
    private double distance;

    @NotNull
    private DistanceUnit distanceUnit;


    GeoDistanceFilter() {
        super(FilterOperation.GEO_DISTANCE);
    }

    @Builder
    public GeoDistanceFilter(@NotNull String field, @NotNull GeoPoint point, @NotNull @PositiveOrZero double distance, @NotNull DistanceUnit distanceUnit) {
        super(FilterOperation.GEO_DISTANCE, field);
        this.point = point;
        this.distance = distance;
        this.distanceUnit = distanceUnit;
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldGeoPointLeafNode leafNode = indexedFields.get(field, IndexFieldGeoPointLeafNode.class);

        return QueryBuilders.geoDistanceQuery(leafNode.getElasticSearchPath())
                .point(point)
                .distance(distance, distanceUnit);
    }
}
