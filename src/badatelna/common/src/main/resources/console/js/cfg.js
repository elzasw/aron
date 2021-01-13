function WebSocketSettings(projectName, contextPath, endpoint, queue, topic, app, debug = false) {
    this.projectName = projectName;
    this.contextPath = contextPath;
    this.endpoint = endpoint;
    this.queue = queue;
    this.topic = topic;
    this.app = app;
    this.debug = debug;
    this.endpointPath = function() {
        return this.contextPath + endpoint;
    }
    this.scriptExecRequestMapping = function () {
        return this.app + '/script/execute/request';
    }
    this.scriptExecResponseMapping = function () {
        return this.topic + '/script/execute/response';
    }
    this.scriptExecConsoleOutMapping = function () {
        return this.topic + '/script/execute/out';
    }
    this.supportedScriptTypeRequestMapping = function () {
        return this.app + '/script/supported/request';
    }
    this.supportedScriptTypeResponseMapping = function () {
        return this.topic + '/script/supported/response';
    }
    this.toString = function () {
        var jsonObject = {
            projectName: this.projectName,
            contextPath: this.contextPath,
            endpoint: this.endpoint,
            queue: this.queue,
            topic: this.topic,
            app: this.app,
            debug: this.debug,
            endpointPath: this.endpointPath(),
            scriptExecRequestMapping: this.scriptExecRequestMapping(),
            scriptExecResponseMapping: this.scriptExecResponseMapping(),
            scriptExecConsoleOutMapping: this.scriptExecConsoleOutMapping(),
            supportedScriptTypeRequestMapping: this.supportedScriptTypeRequestMapping(),
            supportedScriptTypeResponseMapping: this.supportedScriptTypeResponseMapping(),
        };
        return JSON.stringify(jsonObject, null, 2);
    }
}

const DEFAULT_SETTINGS = new WebSocketSettings('EAS', '/api/eas', '/webSocket', '/queue', '/topic', '/app', false);

var currentSettings = DEFAULT_SETTINGS;