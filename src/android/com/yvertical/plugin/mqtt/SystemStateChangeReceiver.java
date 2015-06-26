package com.yvertical.plugin.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

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
			execAction(context, MqttPluginConstants.ACTION_CONNECT);
		} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (MqttPluginUtils.isNetworkAvaliable(context)) {
				MqttPlugin.debug(this.getClass(), "action_network_avaliable");
				execAction(context, MqttPluginConstants.ACTION_CONNECT);
			} else {
				MqttPlugin.debug(this.getClass(), "action_network_unavaliable");
			}
		} else if (action.equals(Intent.ACTION_USER_PRESENT)) {
			MqttPlugin.debug(this.getClass(), "the user is present after device wakes up");
			execAction(context, MqttPluginConstants.ACTION_CONNECT);
		}
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
}
