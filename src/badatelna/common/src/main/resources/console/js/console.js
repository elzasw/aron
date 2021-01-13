const settings = currentSettings;

var stompClient = null;

var username = null;
var password = null;

var supportedScripts;

$(document).ready(function () {
    console.log("Console settings:\n" + settings);

    /* Set page title */
    let pageTitle = settings.projectName + " Admin console";
    $(document).attr("title", pageTitle);
    $("#pageTitle").text(pageTitle);

    /* register login button action */
    $("#loginBtn").click(function(){
        username = $("#username").val();
        password = $("#password").val();

        $("#loginForm").hide().removeClass("was-validated");
        $("#scriptForm").show();
        if (!stompClient || !stompClient.connected) {
            connect();
        }
    });

    /* register execute script button action */
    $("#scriptExecuteBtn").click(function(){
        $('#scriptExecuteBtn').hide();
        $("#loader").show();
        $("#consoleForm").css("visibility", "visible");
        scriptOutputClear();
        scriptMessageLogPrintLn("running script");

        let scriptType = supportedScripts.get($("#scriptType").val());
        let msg = new ExecuteScriptRequest($("#script").val(), scriptType, $("#scriptTransactionChk").prop("checked"));

        localStorage.setItem('lastScript', msg.script);
        localStorage.setItem('lastScriptType', msg.scriptType.languageName);
        localStorage.setItem('lastScriptTransaction', msg.transaction);

        stompClient.send(settings.scriptExecRequestMapping(), {}, JSON.stringify(msg));
        return false;
    });

    /* register console clear button action */
    $("#clearConsoleBtn").click(function(){
        scriptConsoleLogClear();
    });

    /* show login form (disabled by default) */
    $("#loginForm").show();
});

/* Establish a websocket connection, register message callbacks */
function connect() {
    let socket = new SockJS(settings.endpointPath());

    stompClient = Stomp.over(socket);
    stompClient.reconnect_delay = 10_000; // automatic reconnect (delay is in milli seconds)
    if (!settings.debug) {
        stompClient.debug = null; // disable logging
    }

    let connect_callback = function(frame) {
        setConnected(true);
        console.log('Connected: ' + frame);

        /* register callback for supprted script engines */
        stompClient.subscribe(settings.supportedScriptTypeResponseMapping(), function(msg) {
            let supportedScriptTypes = JSON.parse(msg.body);
            if (Array.isArray(supportedScriptTypes) && !supportedScriptTypes.length) {
                alert("No script engine supported.")
            } else {
                supportedScripts = new Map();

                let scriptSelect = $('#scriptType');
                supportedScriptTypes.forEach(value => {
                    let scriptType = Object.assign(new ScriptType, value);
                    supportedScripts.set(scriptType.languageName, scriptType);

                    let option = $('<option>', {
                        value: scriptType.languageName,
                        text : scriptType.languageName,
                        title: scriptType.toString()
                    });

                    if (localStorage.getItem('lastScriptType') === scriptType.languageName) {
                        option.attr('selected', 'selected');
                    }

                    scriptSelect.append(option);
                });
            }

            $("#script").val(localStorage.getItem('lastScript'));
            $("#scriptTransactionChk").prop('checked', getBool(localStorage.getItem('lastScriptTransaction')));
        });

        /* register callback for script console output - print console output in textarea */
        stompClient.subscribe(settings.scriptExecConsoleOutMapping(), function(msg) {
            scriptConsoleLogPrintLn(msg.body);
        });

        /* register callback for script return value */
        stompClient.subscribe(settings.scriptExecResponseMapping(), function(msg) {
            let msgBody = JSON.parse(msg.body);
            scriptMessageLogPrintLn("finished" + '\n' + "duration: " + msgBody.duration + "ms");
            if (msgBody.error) {
                scriptOutputPrintLn(msgBody.error);
            }
            if (msgBody.result) {
                scriptOutputPrintLn(msgBody.result);
            }
            $('#scriptExecuteBtn').delay(600).show(200);
            $("#loader").delay(500).hide(100);
        });

        stompClient.send(settings.supportedScriptTypeRequestMapping(), {}, JSON.stringify({}));
    };
    let error_callback = function(error) {
        console.log(error);
        if (error.headers) {
            alert(error.headers.message);
        }
    };

    stompClient.connect(username, password, connect_callback, error_callback);
}

function disconnect() {
    if(stompClient != null) {
        stompClient.disconnect();
        stompClient = null;
    }
    setConnected(false);
    console.log("Disconnected");
}

function setConnected(connected) {
    $('#scriptExecuteBtn').prop('disabled', !connected);
}

function scriptOutputClear() {
    $('#script_output').val("");
}

function scriptOutputPrintLn(s) {
    $('#script_output').val(function(i, text) {
        return text + s + '\n';
    });
}

function scriptConsoleLogClear() {
    $('#script_console_log').val("");
}

function scriptConsoleLogPrintLn(s) {
    $('#script_console_log').val(function(i, text) {
        return text + stripAnsi(s) + '\n';
    });
}

function scriptMessageLogClear() {
    $('#script_message_log').val("");
}

function scriptMessageLogPrintLn(s) {
    $('#script_message_log').val(function(i, text) {
        return text + new Date().toLocaleString() + "\n" + s + "\n\n";
    });
}