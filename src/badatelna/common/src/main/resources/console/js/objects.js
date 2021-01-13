function ExecuteScriptRequest(script, scriptType, transaction) {
    this.script = script;
    this.scriptType = scriptType;
    this.transaction = transaction;
}

function ExecuteScriptResponse(result, error, durationMs) {
    this.result = result;
    this.error = error;
    this.durationMs = durationMs;
    this.duration = function() {
        return msToTime(this.durationMs);
    }
}

function ScriptType(languageName, languageVersion, engineName, engineVersion) {
    this.languageName = languageName;
    this.languageVersion = languageVersion;
    this.engineName = engineName;
    this.engineVersion = engineVersion;
    this.toString = function() {
        return "language: " + this.languageName + ":" + this.languageVersion + "; engine: " + this.engineName + ":" + this.engineVersion;
    }
}