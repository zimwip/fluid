
var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#events").html("");
}

var error_callback = function(error) {
  // display the error's message header:
  alert(error);
};

var connect_callback = function (frame) {
    setConnected(true);
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/events', function (event) {
        showEvent(JSON.parse(event.body));
    });
    stompClient.subscribe('/topic/data', function (event) {
        showData(JSON.parse(event.body));
    });
}

function connect() {
    var url = 'ws://'+window.location.host+'/ws';
    var socket = new WebSocket(url);
    stompClient = Stomp.over(socket);
    stompClient.connect({}, connect_callback, error_callback);
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendConfig() {
    stompClient.send("/app/command", {}, JSON.stringify({'name': 'config'}));
}

function sendStart() {
    stompClient.send("/app/command", {}, JSON.stringify({'name': 'start'}));
}

function sendSend() {
    stompClient.send("/app/command", {}, JSON.stringify({'name': 'send'}));
}

function sendStop() {
    stompClient.send("/app/command", {}, JSON.stringify({'name': 'stop'}));
}

function showEvent(event) {
    $("#events").append("<tr><td>" + event.event +"/"+ event.date +"</td></tr>");
}

function showData(data) {
    $("#events").append("<tr><td>" + JSON.stringify(data) +"</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#config" ).click(function() { sendConfig(); });
    $( "#start" ).click(function() { sendStart(); });
    $( "#send" ).click(function() { sendSend(); });
    $( "#stop" ).click(function() { sendStop(); });
});