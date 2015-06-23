package com.yvertical.plugin.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 1. listen for system boot
 * 2. listen for network turn on/off
 * 
 * @author zhaokun
 *
 */
public class SystemStateChangeReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			MqttPlugin.debug(this.getClass(), "action_boot_complected");
			if (!isQuit(context)) {
				execAction(context, MqttPluginConstants.ACTION_CONNECT);
			}
		} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (isNetworkAvaliable(context)) {
				MqttPlugin.debug(this.getClass(), "action_network_avaliable");
				if (!isQuit(context)) {
					execAction(context, MqttPluginConstants.ACTION_CONNECT);
				}
			} else {
				MqttPlugin.debug(this.getClass(), "action_network_unavaliable");
			}
		}
	}
	
	/**
	 * is user fully leave our app?
	 * 
	 * @return
	 */
	private boolean isQuit(Context context) {
		SharedPreferences sharedPref = context.getSharedPreferences(
				MqttPluginConstants.CLIENT_PREF_NAME, Context.MODE_PRIVATE);
		boolean exit = sharedPref.getBoolean(
				MqttPluginConstants.CLIENT_CONFIG_EXIT, false);
		MqttPlugin.debug(this.getClass(), "user fully leave our app : " + exit);
		return exit;
	}
	
	/**
	 * start service
	 * @param context
	 * @param action
	 */
	private void execAction(Context context, String action) {
		Intent serviceIntent = new Intent(context, PushService.class);
		serviceIntent.setAction(action);
		context.startService(serviceIntent);
	}

	public static boolean isNetworkAvaliable(Context context) {
		ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}
}
