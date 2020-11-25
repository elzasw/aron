package cz.inqool.entityviews;

import com.google.testing.compile.JavaSourcesSubjectFactory;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;

import static com.google.common.truth.Truth.assert_;

public class InheritanceTest extends TestBase {
    @Test
    public void run() {
        JavaFileObject[] sourceFiles = loadJavaFiles(
                "cz/inqool/entityviews/abs/Person.java",
                "cz/inqool/entityviews/abs/Man.java"
        );

        JavaFileObject[] generatedFiles = loadJavaFiles(
                "cz/inqool/entityviews/abs/PersonDetail.java",
                "cz/inqool/entityviews/abs/ManDetail.java",
                "cz/inqool/entityviews/abs/ManSimple.java"
        );

        assert_().
                about(JavaSourcesSubjectFactory.javaSources()).
                that(Arrays.asList(sourceFiles)).
                processedWith(new EntityViewsProcessor()).
                compilesWithoutError().
                and().generatesSources(generatedFiles[0], tail(generatedFiles));
    }

    @Test
    public void emptyExtends() {
        JavaFileObject[] sourceFiles = loadJavaFiles(
                "cz/inqool/entityviews/abs/Person.java",
                "cz/inqool/entityviews/abs/Cyborg.java"
        );

        JavaFileObject[] generatedFiles = loadJavaFiles(
                "cz/inqool/entityviews/abs/PersonDetail.java",
                "cz/inqool/entityviews/abs/CyborgSimple.java"
        );

        assert_().
                about(JavaSourcesSubjectFactory.javaSources()).
                that(Arrays.asList(sourceFiles)).
                processedWith(new EntityViewsProcessor()).
                compilesWithoutError().
                and().generatesSources(generatedFiles[0], tail(generatedFiles));
    }
}
