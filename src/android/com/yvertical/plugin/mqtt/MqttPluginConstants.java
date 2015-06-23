package com.yvertical.plugin.mqtt;

public interface MqttPluginConstants {

	//plugin action
	public static final String CONNECT_ACTION = "connect";
	public static final String DISCONNECT_ACTION = "disconnect";
	public static final String SET_ON_MESSAGE_ARRIVED_CALLBACK_ACTION = "setOnMessageArrivedCallbackAction";
	
	//mqtt
	public static final int MQTT_QOS = 1;
	
	/** pref xml name **/
	String MQTT_PREF_NAME = "yvertical_matt";
	String CLIENT_PREF_NAME = "client_config";
	
	String KEY_ACTION = "action";
	
	String ACTION_CONNECT = "connect";
	
	// mqtt config key
	String MQTT_CONFIG_HOST = "host";
	String MQTT_CONFIG_DEVICE_UUID = "deviceUuid";
	String MQTT_CONFIG_TIMEOUT = "timeout";
	String MQTT_CONFIG_KEEP_ALIVE_INTERVAL = "keepAliveInterval";
	String MQTT_CONFIG_USER_NAME = "userName";
	String MQTT_CONFIG_PASSWORD = "password";
	/**
	 * if exit == true, then will not start service, it represents the user
	 * fully quit our app; else if exit == true, then will try to start our app when system boot complected, or network became avaliable.
	 * see {@link SystemStateChangeReceiver}
	 */
	String CLIENT_CONFIG_EXIT = "exit";
	
}
