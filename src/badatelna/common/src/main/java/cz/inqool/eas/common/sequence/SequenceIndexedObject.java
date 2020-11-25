package cz.inqool.eas.common.sequence;

import cz.inqool.eas.common.dictionary.index.DictionaryIndexedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.*;
import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.SORTING;
import static cz.inqool.eas.common.domain.index.field.ES.Suffix.*;

@Getter
@Setter
@Document(indexName = "eas_sequence")
public class SequenceIndexedObject extends DictionaryIndexedObject<Sequence, Sequence> {

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = TEXT_SHORT_KEYWORD, searchAnalyzer = TEXT_SHORT_KEYWORD),
            otherFields = {
                    @InnerField(suffix = FOLD, type = FieldType.Text, analyzer = FOLDING, searchAnalyzer = FOLDING),
                    @InnerField(suffix = SEARCH, type = FieldType.Text, analyzer = FOLDING_AND_TOKENIZING, searchAnalyzer = FOLDING_AND_TOKENIZING),
                    @InnerField(suffix = SORT, type = FieldType.Text, analyzer = SORTING, searchAnalyzer = SORTING, fielddata = true)
            }
    )
    protected String description;

    @Field(type = FieldType.Text, analyzer = TEXT_SHORT_KEYWORD, searchAnalyzer = TEXT_SHORT_KEYWORD)
    protected String format;

    @Field(type = FieldType.Long)
    protected Long counter;

    @Field(type = FieldType.Boolean)
    protected boolean local;

    @Override
    public void toIndexedObject(Sequence obj) {
        super.toIndexedObject(obj);

        this.description = obj.getDescription();
        this.format = obj.getFormat();
        this.counter = obj.getCounter();
        this.local = obj.isLocal();
    }
}
