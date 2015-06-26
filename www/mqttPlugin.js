var exec = require('cordova/exec');

var PLUGIN_NAME = "mqttPlugin";

var PLUGIN_ACTION = {
    
    CONNECT: "connect",

    DISCONNECT: "disconnect",

    SET_ON_MESSAGE_ARRIVED_CALLBACK: "setOnMessageArrivedCallbackAction",
};

var mqttPlugin = {
    
    connect: function(url, clientHandle, optJsonObj, successCallback, failCallback) {
        exec(function() {
            exec(successCallback, "", PLUGIN_NAME, PLUGIN_ACTION.SET_ON_MESSAGE_ARRIVED_CALLBACK, []);
        }, failCallback, PLUGIN_NAME, PLUGIN_ACTION.CONNECT, [url, clientHandle, optJsonObj]);
    },

    setOnMessageArrivedCallback: function(successCallback) {
        exec(successCallback, "", PLUGIN_NAME, PLUGIN_ACTION.SET_ON_MESSAGE_ARRIVED_CALLBACK, []);
    },

    disconnect: function(callback) {
        exec(callback, "", PLUGIN_NAME, PLUGIN_ACTION.DISCONNECT, []);
    },
};

module.exports = mqttPlugin;
