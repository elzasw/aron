package cz.aron.service;

public enum ScriptType {

    GROOVY("groovy");

    private final String engineName;

    ScriptType(String engineName) {
        this.engineName = engineName;
    }

    /**
     * JSR-223 short name used to look the engine up via {@link javax.script.ScriptEngineManager}.
     */
    public String getEngineName() {
        return engineName;
    }
}
