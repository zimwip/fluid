
var stompClient = null;

function setConnected(connected) {

    if (connected) {
        document.getElementById("connect").setAttribute("disabled","");
        document.getElementById("disconnect").removeAttribute("disabled");
        document.getElementById("conversation").removeAttribute("style");
    }
    else {
        document.getElementById("connect").removeAttribute("disabled");
        document.getElementById("disconnect").setAttribute("disabled", "");
        document.getElementById("conversation").style.display = 'none';
    }
    document.getElementById("events").innerHtml = "";
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
    var domEvent = document.createElement("tr");
    domEvent.innerHTML = "<td>" + event.event +"/"+ event.date +"</td>";
    document.getElementById("events").appendChild(domEvent);
}

function showData(data) {
    var domEvent = document.createElement("tr");
    domEvent.innerHTML = "<td>" + JSON.stringify(data) +"</td>";
    document.getElementById("events").appendChild(domEvent);
}


document.addEventListener('DOMContentLoaded', function () {

    [].forEach.call(document.querySelectorAll('form'), function(el) {
        el.addEventListener('submit', function (event) {event.preventDefault();});
    });
    document.getElementById("connect" ).addEventListener('click', connect);
    document.getElementById("disconnect" ).addEventListener('click', function() { disconnect(); });
    document.getElementById("config" ).addEventListener('click', function() { sendConfig(); });
    document.getElementById("start" ).addEventListener('click', function() { sendStart(); });
    document.getElementById("send" ).addEventListener('click', function() { sendSend(); });
    document.getElementById("stop" ).addEventListener('click', function() { sendStop(); });

});
