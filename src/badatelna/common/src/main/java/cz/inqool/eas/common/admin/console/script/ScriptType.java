package cz.inqool.eas.common.admin.console.script;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.script.ScriptEngineFactory;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"languageName", "languageVersion"})
public class ScriptType {

    private String languageName;
    private String languageVersion;

    private String engineName;
    private String engineVersion;


    public static ScriptType obtainFrom(ScriptEngineFactory factory) {
        ScriptType scriptType = new ScriptType();
        scriptType.languageName = factory.getLanguageName();
        scriptType.languageVersion = factory.getLanguageVersion();
        scriptType.engineName = factory.getEngineName();
        scriptType.engineVersion = factory.getEngineVersion();

        return scriptType;
    }

    @Override
    public String toString() {
        return "ScriptType{" +
                "languageName='" + languageName + '\'' +
                ", languageVersion='" + languageVersion + '\'' +
                '}';
    }
}
