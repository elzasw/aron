package cz.inqool.eas.common.domain.index.dto.filter;

import cz.inqool.eas.common.domain.index.field.ES;
import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.EqualsAndHashCode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotBlank;

import static cz.inqool.eas.common.domain.index.QueryUtils.*;

/**
 * Filter representing the 'ends with' filter condition on given text {@link FieldFilter#field}.
 */
@EqualsAndHashCode(callSuper = true)
public class EndWithFilter extends TextFilter<EndWithFilter> {

    EndWithFilter() {
        super(FilterOperation.END_WITH);
    }

    public EndWithFilter(@NotBlank String field, @NotBlank String value) {
        this(field, value, null);
    }

    public EndWithFilter(@NotBlank String field, @NotBlank String value, Modifier modifier) {
        super(FilterOperation.END_WITH, field, value, modifier);
    }


    @Override
    public QueryBuilder toQueryBuilder(IndexObjectFields indexedFields) {
        IndexFieldLeafNode indexField = getIndexFieldLeafNode(indexedFields);

        // regexp and wildcard query do not analyze given query string, so it needs to be folded in the application
        boolean hasDedicatedFoldField = indexField.hasInnerField(ES.Suffix.FOLD);
        boolean mainFieldIsFolded = indexField.getType() == FieldType.Text && indexField.getAnalyzer() != null && (indexField.getAnalyzer().isEmpty() || indexField.getAnalyzer().contains("folding"));
        String normalizedValue = (useFolding && (hasDedicatedFoldField || mainFieldIsFolded)) ? asciiFolding(lowercase(value)) : value;
        String field = useFolding ? indexField.getFolded() : indexField.getElasticSearchPath();

        if (modifier != null) {
            QueryBuilder queryBuilder;
            switch (modifier) {
                case FUZZY: // fixme not working properly
                    queryBuilder = QueryBuilders.fuzzyQuery(indexField.getSearchable(), escapeAll(value))
                            .fuzziness(Fuzziness.AUTO)
                            .transpositions(true); // maybe remove for speed increase
                    break;
                case REGEXP:
                    queryBuilder = QueryBuilders.regexpQuery(field, ".*(" + normalizedValue + ")");
                    break;
                case WILDCARD:
                    queryBuilder = QueryBuilders.wildcardQuery(field, "*" + escapeAllButWildcard(normalizedValue));
                    break;
                default:
                    throw new RuntimeException("Unknown modifier");
            }
            return wrapIfNested(indexField, queryBuilder);
        }

        QueryBuilder queryBuilder = QueryBuilders.wildcardQuery(field, "*" + escapeAll(normalizedValue));
        return wrapIfNested(indexField, queryBuilder);
    }
}
