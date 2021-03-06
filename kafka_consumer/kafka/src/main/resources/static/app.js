
var stompClient = null;

function setConnected(connected) {

    if (connected) {
        document.getElementById("connect").setAttribute("disabled", "");
        document.getElementById("disconnect").removeAttribute("disabled");
        document.getElementById("config").removeAttribute("disabled");
        document.getElementById("start").removeAttribute("disabled");
        document.getElementById("stop").removeAttribute("disabled");
        document.getElementById("send").removeAttribute("disabled");
        document.getElementById("conversation").removeAttribute("style");
    } else {
        document.getElementById("connect").removeAttribute("disabled");
        document.getElementById("disconnect").setAttribute("disabled", "");
        document.getElementById("config").setAttribute("disabled", "");
        document.getElementById("start").setAttribute("disabled", "");
        document.getElementById("stop").setAttribute("disabled", "");
        document.getElementById("send").setAttribute("disabled", "");
        document.getElementById("conversation").style.display = 'none';
    }
    var myNode = document.getElementById("events");
    while (myNode.firstChild) {
        myNode.removeChild(myNode.firstChild);
    }
}

var error_callback = function (error) {
    // display the error's message header:
    alert(error);
};

var connect_callback = function (frame) {
    setConnected(true);
    stompClient.subscribe('/topic/events', function (event) {
        showEvent(JSON.parse(event.body));
    }, {receipt: 'my receipt'});
    stompClient.subscribe('/topic/data', function (event) {
        showData(JSON.parse(event.body));
    });
    stompClient.subscribe('/user/queue/errors', function (event) {
        showError(event.body);
    });
}

function connect() {
    var url = 'wss://' + window.location.host + '/ws';
    var socket = new WebSocket(url);
    stompClient = Stomp.over(socket);
    stompClient.heartbeat.outgoing = 20000;
    stompClient.heartbeat.incoming = 20000;
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
    domEvent.innerHTML = "<td>" + event.event + "/" + event.date + "</td>";
    document.getElementById("events").appendChild(domEvent);
}

function showData(data) {
    var domEvent = document.createElement("tr");
    domEvent.innerHTML = "<td>" + JSON.stringify(data) + "</td>";
    document.getElementById("events").appendChild(domEvent);
}

function showError(message) {
    var domEvent = document.createElement("tr");
    domEvent.innerHTML = "<td class=\"alert alert-danger\">" + message + "</td>";
    document.getElementById("events").appendChild(domEvent);
}


document.addEventListener('DOMContentLoaded', function () {

    [].forEach.call(document.querySelectorAll('form'), function (el) {
        el.addEventListener('submit', function (event) {
            event.preventDefault();
        });
    });
    document.getElementById("connect").addEventListener('click', connect);
    document.getElementById("disconnect").addEventListener('click', function () {
        disconnect();
    });
    document.getElementById("config").addEventListener('click', function () {
        sendConfig();
    });
    document.getElementById("start").addEventListener('click', function () {
        sendStart();
    });
    document.getElementById("send").addEventListener('click', function () {
        sendSend();
    });
    document.getElementById("stop").addEventListener('click', function () {
        sendStop();
    });

});
