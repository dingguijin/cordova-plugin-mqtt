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
		// 1. auto connect to server
		MqttServiceManager.getInstance(getBaseContext()).connect(false,
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

		String host = sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_HOST, null);
		if (host == null) {
			return null;
		}

		return new MqttConnectConfig(host,
                                     sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_DEVICE_UUID, null),
                                     sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_USER_NAME, null),
                                     sharedPref.getString(MqttPluginConstants.MQTT_CONFIG_PASSWORD, null),
                                     sharedPref.getInt(MqttPluginConstants.MQTT_CONFIG_TIMEOUT, MqttPluginConstants.MQTT_CONFIG_DEFAULT_TIMEOUT),
                                     sharedPref.getInt(MqttPluginConstants.MQTT_CONFIG_KEEP_ALIVE_INTERVAL, MqttPluginConstants.MQTT_CONFIG_DEFAULT_KEEP_ALIVE_INTERVAL),
                                     sharedPref.getString(
                                                          MqttPluginConstants.MQTT_CONFIG_NOTIFICATION_TITLE,
                                                          MqttPluginConstants.MQTT_CONFIG_DEFAULT_NOTIFICATION_TITLE));
	}

	@Override
	public void onDestroy() {
		MqttPlugin.debug(this.getClass(), "onDestroy");
		super.onDestroy();
	}

}
