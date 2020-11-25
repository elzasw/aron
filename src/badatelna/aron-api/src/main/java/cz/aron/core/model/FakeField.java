package cz.aron.core.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.lang.annotation.Annotation;

/**
 * @author Lukas Jane (inQool) 16.11.2020.
 */
@Getter
@Setter
class FakeField implements Field {

    private String name = "";
    private FieldType type = FieldType.Auto;

    @Override
    public String value() {
        return name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public FieldType type() {
        return type;
    }

    @Override
    public boolean index() {
        return true;
    }

    @Override
    public DateFormat format() {
        return DateFormat.none;
    }

    @Override
    public String pattern() {
        return "";
    }

    @Override
    public boolean store() {
        return false;
    }

    @Override
    public boolean fielddata() {
        return false;
    }

    @Override
    public String searchAnalyzer() {
        return "";
    }

    @Override
    public String analyzer() {
        return "";
    }

    @Override
    public String normalizer() {
        return "";
    }

    @Override
    public String[] ignoreFields() {
        return new String[0];
    }

    @Override
    public boolean includeInParent() {
        return false;
    }

    @Override
    public String[] copyTo() {
        return new String[0];
    }

    @Override
    public int ignoreAbove() {
        return -1;
    }

    @Override
    public boolean coerce() {
        return true;
    }

    @Override
    public boolean docValues() {
        return true;
    }

    @Override
    public boolean ignoreMalformed() {
        return false;
    }

    @Override
    public IndexOptions indexOptions() {
        return IndexOptions.none;
    }

    @Override
    public boolean indexPhrases() {
        return false;
    }

    @Override
    public IndexPrefixes[] indexPrefixes() {
        return new IndexPrefixes[0];
    }

    @Override
    public boolean norms() {
        return true;
    }

    @Override
    public String nullValue() {
        return "";
    }

    @Override
    public int positionIncrementGap() {
        return -1;
    }

    @Override
    public Similarity similarity() {
        return Similarity.Default;
    }

    @Override
    public TermVector termVector() {
        return TermVector.none;
    }

    @Override
    public double scalingFactor() {
        return 1;
    }

    @Override
    public int maxShingleSize() {
        return -1;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
