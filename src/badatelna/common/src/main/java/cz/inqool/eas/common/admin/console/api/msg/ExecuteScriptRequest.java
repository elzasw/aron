package cz.inqool.eas.common.admin.console.api.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.admin.console.script.ScriptType;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.OutputStream;
import java.util.Map;

@Getter
@Setter
public class ExecuteScriptRequest {

    @NotNull
    private String script;

    @NotNull
    private ScriptType scriptType;

    private Boolean transaction = false;

    private Map<String, Object> params;

    /**
     * Additional output stream for console log access
     */
    @Hidden
    @JsonIgnore
    private OutputStream outputStream;
}
