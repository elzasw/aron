package cz.inqool.eas.common.domain.index.dto.filter;

import com.google.common.annotations.Beta;
import cz.inqool.eas.common.domain.index.field.IndexFieldGeoPointLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotNull;

/**
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-geo-bounding-box-query.html">Geo-bounding box query</a>
 */
@Beta
@Getter
@EqualsAndHashCode(callSuper = true)
public class GeoBoundingBoxFilter extends GeoFilter {

    @NotNull
    private GeoPoint topLeft;

    @NotNull
    private GeoPoint bottomRight;


    GeoBoundingBoxFilter() {
        super(FilterOperation.GEO_DISTANCE);
    }

    @Builder
    public GeoBoundingBoxFilter(@NotNull String field, @NotNull GeoPoint topLeft, @NotNull GeoPoint bottomRight) {
        super(FilterOperation.GEO_DISTANCE, field);
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldGeoPointLeafNode leafNode = indexedFields.get(field, IndexFieldGeoPointLeafNode.class);

        return QueryBuilders.geoBoundingBoxQuery(leafNode.getElasticSearchPath())
                .setCorners(topLeft, bottomRight);
    }
}
