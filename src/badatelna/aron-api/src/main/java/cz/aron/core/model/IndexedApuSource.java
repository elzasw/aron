package cz.aron.core.model;

import cz.inqool.eas.common.domain.index.DomainIndexedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * @author Lukas Jane (inQool) 29.10.2020.
 */
@Getter
@Setter
@Document(indexName = "apu_source")
public class IndexedApuSource extends DomainIndexedObject<ApuSource, ApuSource> {

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime published;

    @Override
    public void toIndexedObject(ApuSource obj) {
        super.toIndexedObject(obj);
        published = obj.getPublished();
    }
}
