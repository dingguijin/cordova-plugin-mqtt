package com.yvertical.plugin.mqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

public class PushService extends Service {
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		MqttPlugin.debug(this.getClass(), "onCreate");
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MqttPlugin.debug(this.getClass(), "onStartCommand");
		// 1. auto connect to server
		
		// if (intent != null) {
		// 	String action = intent.getAction();

		// 	if (action != null) {
		// 		if (action.equals(MqttPluginConstants.ACTION_CONNECT)) {
		// 			MqttPlugin.debug(this.getClass(), "connect mqtt service ");
		// 			MqttServiceManager.getInstance(getBaseContext()).connect(
		// 					readConfig(), MqttPlugin.DEFAULT);
		// 		}
		// 	}
		// }

        MqttPlugin.debug(this.getClass(), "connect mqtt service ");
        MqttServiceManager.getInstance(getBaseContext()).connect(
                                                                 readConfig(), MqttPlugin.DEFAULT);
		
		return START_STICKY;
	}
	
	/**
	 * read the last connect [host,username,...]
	 * 
	 * @return
	 */
	private MqttConnectConfig readConfig() {
		SharedPreferences sharedPref = getBaseContext().getSharedPreferences(MqttPluginConstants.MQTT_PREF_NAME, Context.MODE_PRIVATE);
		
		String host = sharedPref.getString(
				MqttPluginConstants.MQTT_CONFIG_HOST, null);
		if (host == null) {
			return null;
		}
		
		return new MqttConnectConfig(host, sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_DEVICE_UUID, null), 
				sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_USER_NAME, null),
				sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_PASSWORD, null),
				sharedPref.getInt(MqttPluginConstants.MQTT_CONFIG_TIMEOUT, 3),
				sharedPref.getInt(MqttPluginConstants.MQTT_CONFIG_KEEP_ALIVE_INTERVAL, 20 * 60));
	}

	@Override
	public void onDestroy() {
		MqttPlugin.debug(this.getClass(), "onDestroy");
		super.onDestroy();
	}
	
}
