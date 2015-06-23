## how to use:
```
var host = 192.168.0.101;
var port = 1883;

var url = "tcp://" + host + ":" + port;
var device_uuid = "733d7714-1964-11e5-8340-0c4de9b21073";
var user_name = "eedcbf1c-fdf6-11e4-a989-0c4de9b21073";
var pass_word = "xxxxxx-xxxxx-xxxx-xxx-xxxxxx"; //sessionUuid;

var options = {
    timeout: 10, //optional,
    keepAliveInterval: 20 * 60, //optional
    userName: user_name,
    password: pass_word,
    notificationTitle: "ThatsApp", //optional,
};

//mqttPlugin.connect('your url', 'device uuid', 'options', new message arrived callback, connect failed callback);
mqttPlugin.connect(url, device_uuid, options, function(message) {
   alert("New message arrived : " + message);
}, function(message) {
   alert("connect failed, reason:" + message);
});

//disconnect
mqttPlugin.disconnect(function(){

});

```

## PluginBuilder
PluginBuilder.java was designed to help to map the src code to <src-file> in the plugin.xml.