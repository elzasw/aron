package cz.inqool.eas.common.dictionary.index;

import cz.inqool.eas.common.authored.index.AuthoredIndexedObject;
import cz.inqool.eas.common.dictionary.Dictionary;
import cz.inqool.eas.common.dictionary.DictionaryIndexed;
import cz.inqool.eas.common.multiString.MultiStringIndexedObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.*;
import static cz.inqool.eas.common.domain.index.field.ES.Suffix.*;

/**
 * Building block for Index entities, which want to save name and track validity in time.
 *
 * @param <ROOT> Root of the projection type system
 * @param <PROJECTED> Index projection type
 */
@Getter
@Setter
@FieldNameConstants
abstract public class DictionaryIndexedObject<ROOT extends Dictionary<ROOT>, PROJECTED extends Dictionary<ROOT>> extends AuthoredIndexedObject<ROOT, PROJECTED> implements DictionaryIndexed<ROOT, PROJECTED> {

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = TEXT_SHORT_KEYWORD, searchAnalyzer = TEXT_SHORT_KEYWORD),
            otherFields = {
                    @InnerField(suffix = FOLD, type = FieldType.Text, analyzer = FOLDING, searchAnalyzer = FOLDING),
                    @InnerField(suffix = SEARCH, type = FieldType.Text, analyzer = FOLDING_AND_TOKENIZING, searchAnalyzer = FOLDING_AND_TOKENIZING),
                    @InnerField(suffix = SORT, type = FieldType.Text, analyzer = SORTING, searchAnalyzer = SORTING, fielddata = true)
            }
    )
    protected String name;

    @Field(type = FieldType.Object)
    protected MultiStringIndexedObject multiName;

    @Field(type = FieldType.Text, analyzer = TEXT_SHORT_KEYWORD, searchAnalyzer = TEXT_SHORT_KEYWORD)
    protected String code;

    @Field(type = FieldType.Boolean)
    protected boolean active;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    protected Instant validFrom;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    protected Instant validTo;

    @Field(type = FieldType.Integer)
    protected Integer order;

    @Override
    public void toIndexedObject(PROJECTED obj) {
        super.toIndexedObject(obj);

        this.name = obj.getName();
        this.multiName = MultiStringIndexedObject.of(obj.getMultiName());
        this.code = obj.getCode();
        this.active = obj.isActive();
        this.validFrom = obj.getValidFrom();
        this.validTo = obj.getValidTo();
        this.order = obj.getOrder();
    }
}
