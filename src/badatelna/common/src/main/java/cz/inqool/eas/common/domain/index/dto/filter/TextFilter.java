package cz.inqool.eas.common.domain.index.dto.filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Filter representing a comparison filter condition on given text {@link FieldFilter#field}.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
abstract public class TextFilter<FILTER extends TextFilter<FILTER>> extends FieldValueFilter<FILTER> {

    /**
     * Modifies the operation execution behaviour
     */
    protected Modifier modifier;

    /**
     * Indicates whether use folding
     */
    protected Boolean useFolding = true;


    protected TextFilter(@NotNull String operation) {
        super(operation);
    }

    protected TextFilter(@NotNull String operation, @NotBlank String field, @NotBlank String value, Modifier modifier) {
        super(operation, field, value);
        this.modifier = modifier;
    }


    /**
     * Filter operation modifier for text values.
     */
    public enum Modifier {

        /**
         * Enables fuzzy searching.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/query-dsl-fuzzy-query.html#query-dsl-fuzzy-query">Fuzzy Query</a>
         */
        FUZZY,

        /**
         * Enables wildcard characters:
         * <ul>
         *   <li><strong>*</strong>: matches any character sequence (including the empty one)</li>
         *   <li><strong>?</strong>: matches any single character</li>
         * </ul>
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/query-dsl-wildcard-query.html#query-dsl-wildcard-query">Wildcard Query</a>
         */
        WILDCARD,

        /**
         * Enables querying with regular expressions.
         *
         * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.8/query-dsl-regexp-query.html">Regexp Query</a>
         */
        REGEXP
    }
}
