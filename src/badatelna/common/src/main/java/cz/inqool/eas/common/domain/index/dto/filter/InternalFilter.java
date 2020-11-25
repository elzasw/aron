package cz.inqool.eas.common.domain.index.dto.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.index.query.QueryBuilder;

import javax.validation.constraints.NotNull;
import java.util.Objects;

import static cz.inqool.eas.common.utils.JsonUtils.toJsonString;

/**
 * Data transfer object for internal filter conditions. Used for additional complicated filters added on back-end.
 * <p>
 * Note that deserialization of this class is disabled and it can be used only on back-end.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize // to override parent annotation
public class InternalFilter implements Filter {

    /**
     * Elastic search query for additional filtering on back-end.
     */
    @JsonIgnore
    @NotNull
    protected QueryBuilder query;


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalFilter that = (InternalFilter) o;
        return toJsonString(this.getQuery()).equals(toJsonString(that.getQuery()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQuery());
    }
}
