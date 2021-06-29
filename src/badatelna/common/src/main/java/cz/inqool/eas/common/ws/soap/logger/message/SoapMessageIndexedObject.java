package cz.inqool.eas.common.ws.soap.logger.message;

import cz.inqool.eas.common.authored.index.AuthoredIndexedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.*;

@Getter
@Setter
@Document(indexName = "eas_soap_message")
public class SoapMessageIndexedObject extends AuthoredIndexedObject<SoapMessage, SoapMessage> {

    @Field(type = FieldType.Text, analyzer = TEXT_SHORT_KEYWORD, searchAnalyzer = TEXT_SHORT_KEYWORD, fielddata = true)
    protected String service;

    @Override
    public void toIndexedObject(SoapMessage obj) {
        super.toIndexedObject(obj);

        this.service = obj.getService();
    }
}
