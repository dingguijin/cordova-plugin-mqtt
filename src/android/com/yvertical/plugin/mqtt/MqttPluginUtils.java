package com.yvertical.plugin.mqtt;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;

public class MqttPluginUtils {
	
	/** store noti id **/
	private static final AtomicInteger mNotiID = new AtomicInteger(0);

	/**
	 * is out app running in background ?
	 * 
	 * when system reboot, this method will return false although our app is not already running, so is this a bug?
	 * 
	 * @param context
	 * @return true if in background; false if not.
	 */
	public static boolean isInBackground(Context context) {
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager
				.getRunningAppProcesses();
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(context.getPackageName())) {
				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND || 
						appProcess.importance == RunningAppProcessInfo.IMPORTANCE_SERVICE) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * show a notification
	 * 
	 * @param context
	 * @param title
	 * @param message
	 * @return
	 */
	public static void showNotification(Context context, String title,
			String message, int smallIcon, Class<?> openClass) {
		NotificationManager notiManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification noti = new NotificationCompat.Builder(context)
				.setSmallIcon(smallIcon)
				.setDefaults(Notification.DEFAULT_ALL)
				.setWhen(System.currentTimeMillis())
				.setPriority(Notification.PRIORITY_HIGH)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setAutoCancel(true)
				.setCategory(Notification.CATEGORY_MESSAGE)
				.setGroupSummary(true)
				.setGroup("message")
				.setContentIntent(
						PendingIntent.getActivity(context, 0, new Intent(
								context, openClass), 0)).setContentTitle(title)
				.setContentText(message).build();
		notiManager.notify(mNotiID.getAndIncrement(), noti);
	}

	/**
	 * get notification icon
	 * 
	 * @param context
	 * @return
	 */
	public static int getNotificationIcon(Context context) {
		String packageName = context.getPackageName();
		MqttPlugin.debug(MqttPluginUtils.class,
				String.format("packageName:%s", packageName));

		try {
			Class<?> c = Class.forName(packageName + ".R$drawable");
			try {
				try {
					return c.getField("app_logo").getInt(c);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					MqttPlugin.debug(MqttPluginUtils.class, e.toString());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					MqttPlugin.debug(MqttPluginUtils.class, e.toString());
				}
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
				MqttPlugin.debug(MqttPluginUtils.class, e.toString());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			MqttPlugin.debug(MqttPluginUtils.class, e.toString());
		}

		return -1;
	}

	/**
	 * find MainActivity.class when click notification.
	 * 
	 * @param context
	 * @return
	 */
	public static Class<?> getNotificationOpenActivityClass(Context context) {
		String packageName = context.getPackageName();
		MqttPlugin.debug(MqttPluginUtils.class,
				String.format("packageName:%s", packageName));

		try {
			return Class.forName(packageName + ".MainActivity");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * is network avaliable
	 * 
	 * @param context
	 * @return
	 */
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
