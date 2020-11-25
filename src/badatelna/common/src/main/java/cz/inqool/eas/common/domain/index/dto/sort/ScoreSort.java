package cz.inqool.eas.common.domain.index.dto.sort;

import com.google.common.annotations.Beta;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import javax.validation.constraints.NotNull;

/**
 * Score Sort
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#_track_scores">Track Scores</a>
 */
@Beta
@Getter
@Setter
public class ScoreSort extends AbstractSort<ScoreSortBuilder> {

    ScoreSort() {
        super(Type.SCRIPT);
    }

    @Builder
    public ScoreSort(@NotNull SortOrder order) {
        super(Type.SCRIPT, order);
    }


    @Override
    public org.elasticsearch.search.sort.ScoreSortBuilder toSortBuilder(IndexObjectFields indexedFields) {
        return SortBuilders
                .scoreSort()
                .order(order);
    }

    @Override
    public Sort<org.elasticsearch.search.sort.ScoreSortBuilder> withReversedOrder() {
        return new ScoreSort(getReversedOrder());
    }
}
