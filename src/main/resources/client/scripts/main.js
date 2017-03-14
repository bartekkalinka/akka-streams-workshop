"use strict";

var wsUri = "ws://" + window.location.hostname + ":" + window.location.port + "/websockets?clientId=aaa";
var websocket;
var span = document.getElementById("span");

// INIT MODULE
function init() {
    initWebSocket();
}
function initWebSocket() {
    websocket = new WebSocket(wsUri);
    websocket.onopen = function(evt) {
        onOpen(evt)
    };
    websocket.onclose = function(evt) {
        onClose(evt)
    };
    websocket.onmessage = function(evt) {
        onMessage(evt)
    };
    websocket.onerror = function(evt) {
        onError(evt)
    };
}
// WEBSOCKET EVENTS MODULE
function onOpen(evt) {
}
function onClose(evt) {
}
function onMessage(evt) {
    var input = evt.data;
    span.innerHTML = input
}
function onError(evt) {
}
function doSend(message) {
    websocket.send(message);
}
function reset() {
    doSend("reset");
}
init();
