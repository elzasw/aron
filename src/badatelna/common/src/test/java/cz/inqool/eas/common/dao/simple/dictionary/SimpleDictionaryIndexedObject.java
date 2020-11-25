package cz.inqool.eas.common.dao.simple.dictionary;

import cz.inqool.eas.common.dictionary.index.DictionaryIndexedObject;
import cz.inqool.eas.common.domain.index.field.ES;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed object class for {@link SimpleDictionaryEntity}
 *
 * @author : olda
 * @since : 02/10/2020, Fri
 **/
@Getter
@Setter
@Document(indexName = "eas_simple_dictionary_entity")
@FieldNameConstants(innerTypeName = "IndexFields")
public class SimpleDictionaryIndexedObject extends DictionaryIndexedObject<SimpleDictionaryEntity, SimpleDictionaryEntity> {

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = ES.Analyzer.TEXT_SHORT_KEYWORD, searchAnalyzer = ES.Analyzer.TEXT_SHORT_KEYWORD),
            otherFields = {
                    @InnerField(suffix = ES.Suffix.FOLD, type = FieldType.Text, analyzer = ES.Analyzer.FOLDING, searchAnalyzer = ES.Analyzer.FOLDING),
                    @InnerField(suffix = ES.Suffix.SEARCH, type = FieldType.Text, analyzer = ES.Analyzer.FOLDING_AND_TOKENIZING, searchAnalyzer = ES.Analyzer.FOLDING_AND_TOKENIZING),
                    @InnerField(suffix = ES.Suffix.SORT, type = FieldType.Text, analyzer = ES.Analyzer.SORTING, searchAnalyzer = ES.Analyzer.SORTING, fielddata = true)
            }
    )
    private String otherUselessValue;

    @Override
    public void toIndexedObject(SimpleDictionaryEntity obj) {
        super.toIndexedObject(obj);

        this.otherUselessValue = obj.getOtherUselessValue();
    }
}
