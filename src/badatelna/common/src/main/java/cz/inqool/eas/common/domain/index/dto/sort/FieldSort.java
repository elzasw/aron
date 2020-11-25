package cz.inqool.eas.common.domain.index.dto.sort;

import cz.inqool.eas.common.domain.index.field.ES.Suffix;
import cz.inqool.eas.common.domain.index.field.IndexFieldLeafNode;
import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import cz.inqool.eas.common.exception.InvalidAttribute;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.elasticsearch.search.sort.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static cz.inqool.eas.common.exception.InvalidAttribute.ErrorCode.FIELDDATA_DISABLED;
import static cz.inqool.eas.common.exception.InvalidAttribute.ErrorCode.FIELD_NOT_INDEXED;
import static cz.inqool.eas.common.utils.AssertionUtils.ifPresent;
import static cz.inqool.eas.common.utils.AssertionUtils.isTrue;

/**
 * Field Sort
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#sort-search-results">Sort
 * search results</a>
 */
@Getter
@Setter
@Schema(description = "Allows to define sorting on fields.")
public class FieldSort extends AbstractSort<FieldSortBuilder> {

    /**
     * Attribute name to sort on.
     */
    @NotBlank
    @Schema(description = "Attribute name to sort on.")
    protected String field;

    /**
     * Specifies how attributes with mising values will be treated.
     */
    @NotNull
    @Schema(description = "Order of sorting.", defaultValue = "LAST")
    protected MissingValues missing = MissingValues.LAST;

    /**
     * Elasticsearch supports sorting by array or multi-valued fields. This option controls what array value is picked
     * for sorting the document it belongs to. The default sort mode in the ascending sort order is {@code min} - the
     * lowest value is picked. The default sort mode in the descending order is {@code max} — the highest value is
     * picked.
     */
    @NotNull
    @Schema(description = "Elasticsearch supports sorting by array or multi-valued fields. " +
            "This option controls what array value is picked for sorting the document it belongs to.")
    protected SortMode sortMode;


    FieldSort() {
        super(Type.FIELD);
    }

    public FieldSort(@NotBlank String field, @NotNull SortOrder order) {
        super(Type.FIELD, order);
        this.field = field;
    }

    public FieldSort(@NotBlank String field, @NotNull SortOrder order, @NotNull MissingValues missing) {
        this(field, order);
        this.missing = missing;
    }

    public FieldSort(@NotBlank String field, @NotNull SortOrder order, @NotNull SortMode sortMode) {
        this(field, order);
        this.sortMode = sortMode;
    }

    @Builder
    public FieldSort(@NotBlank String field, @NotNull SortOrder order, @NotNull MissingValues missing, @NotNull SortMode sortMode) {
        this(field, order, missing);
        this.sortMode = sortMode;
    }


    @Override
    public FieldSort withReversedOrder() {
        return new FieldSort(
                field,
                getReversedOrder(),
                (missing == MissingValues.LAST) ? MissingValues.FIRST : MissingValues.LAST,
                sortMode
        );
    }

    @Override
    public org.elasticsearch.search.sort.FieldSortBuilder toSortBuilder(IndexObjectFields indexedFields) {
        IndexFieldLeafNode leafNode = indexedFields.get(field, IndexFieldLeafNode.class);
        if (!leafNode.isIndexed()) {
            throw new InvalidAttribute(this.getClass(), null, field, FIELD_NOT_INDEXED);
        }

        InnerField sortField = leafNode.getInnerField(Suffix.SORT);
        if (sortField != null) {
            if (sortField.type() == FieldType.Text) {
                isTrue(sortField.fielddata(), () -> new InvalidAttribute(this.getClass(), null, field, FIELDDATA_DISABLED));
            }
        } else {
            Field mainField = leafNode.getMainField();
            if (mainField.type() == FieldType.Text) {
                isTrue(mainField.fielddata(), () -> new InvalidAttribute(this.getClass(), null, field, FIELDDATA_DISABLED));
            }
        }

        org.elasticsearch.search.sort.FieldSortBuilder sortBuilder = SortBuilders
                .fieldSort(leafNode.getSortable())
                .order(order)
                .missing(missing.getValue());

        ifPresent(sortMode, sortBuilder::sortMode);
        if (leafNode.isOfNested()) {
            NestedSortBuilder nestedSortBuilder = new NestedSortBuilder(leafNode.getNestedPath());
            sortBuilder.setNestedSort(nestedSortBuilder);
        }

        return sortBuilder;
    }


    /**
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html#_missing_values">Missing
     * Values</a>
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public enum MissingValues {

        FIRST("_first"),
        LAST("_last");

        @Getter
        private final String value;
    }
}
