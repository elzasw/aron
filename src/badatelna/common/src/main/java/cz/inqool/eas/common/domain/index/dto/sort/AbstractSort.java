package cz.inqool.eas.common.domain.index.dto.sort;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cz.inqool.eas.common.domain.index.dto.sort.AbstractSort.Fields;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import javax.validation.constraints.NotNull;

import static cz.inqool.eas.common.domain.index.dto.sort.Sort.Type.*;

/**
 * ElasticSearch sorting specification.
 */
@JsonDeserialize // to override parent annotation
@JsonTypeInfo(
        use = Id.NAME,
        include = As.EXISTING_PROPERTY,
        property = Fields.type,
        visible = true,
        defaultImpl = FieldSort.class) // todo remove defaultImpl after FE is adjusted to changes
@JsonSubTypes({
        @Type(name = FIELD,        value = FieldSort.class),
        @Type(name = GEO_DISTANCE, value = GeoDistanceSort.class),
        @Type(name = SCRIPT,       value = ScriptSort.class),
        @Type(name = SCORE,        value = ScoreSort.class)
})
@Getter
@Setter
@EqualsAndHashCode
@FieldNameConstants
public abstract class AbstractSort<SB extends SortBuilder<SB>> implements Sort<SB> {

    /**
     * Sort type.
     */
    @NotNull
    private final String type;

    /**
     * Order of sorting.
     */
    @NotNull
    @Schema(description = "Order of sorting.")
    protected SortOrder order;


    protected AbstractSort(@NotNull String type) {
        this.type = type;
    }

    protected AbstractSort(@NotNull String type, @NotNull SortOrder order) {
        this.type = type;
        this.order = order;
    }


    protected SortOrder getReversedOrder() {
        return (order == SortOrder.ASC) ? SortOrder.DESC : SortOrder.ASC;
    }
}
