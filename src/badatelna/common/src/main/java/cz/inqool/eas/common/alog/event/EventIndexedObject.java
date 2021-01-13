package cz.inqool.eas.common.alog.event;

import cz.inqool.eas.common.authored.index.AuthoredIndexedObject;
import cz.inqool.eas.common.domain.index.reference.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Document(indexName = "eas_alog_event")
public class EventIndexedObject extends AuthoredIndexedObject<Event, Event> {

    @Field(type = FieldType.Object)
    private LabeledReference severity;

    @Override
    public void toIndexedObject(Event obj) {
        super.toIndexedObject(obj);

        this.severity = LabeledReference.of(obj.severity);
    }
}
