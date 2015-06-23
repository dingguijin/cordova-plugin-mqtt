package com.yvertical.plugin.mqtt;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MqttServiceManager {

	private final Context context;
	
	/** current connect host **/
	private String host;
	/** current connect device uuid **/
	private String deviceUuid;
	/** current connect userName **/
	private String userName;
	
	/** mqtt client **/
	private MqttAndroidClient client;
	/** mqtt token **/
	private IMqttToken mToken;
	/** single instance **/
	private static MqttServiceManager mInstance;
	/** status **/
	private enum Status {
		
		CONNECTING,
		
		CONNECTED,
		
		DISCONNECTING,
		
		DISCONNECTED,
		
	}
	/** current status **/
	private Status mStatus;

	/** connect action listener **/
	private ConnectActionListener mConnectActionListener;

    /** the activity that want to open when click notification **/
	private Class<?> notificationOpenActivity;
	
	/** notification icon **/
	private int notificationSmallIcon;
	
	/**
	 * mqtt event listener
	 * 
	 * @author zhaokun
	 *
	 */
	interface MqttListener {
		
		/**
		 * on message arrived
		 * @param message
		 */
		void onMessageArrived(MqttMessage message);
		
		/**
		 * connect successful
		 */
		void onConnectSucc();
		
		/**
		 * on connect failure
		 * @param exception
		 */
		void onConnectFailure(Throwable exception);
		
	}
	
	/**
	 * after disconnet failed or succ, this method will get called then.
	 * 
	 * @author zhaokun
	 *
	 */
	private interface DisconnectListener {

		void onDisconnectFinish();

	}

	private MqttServiceManager(Context ctx) {
		context = ctx;
		mStatus = Status.DISCONNECTED;
	}
	
	/**
	 * get a single instance
	 * 
	 * @param ctx
	 * @return
	 */
	public static MqttServiceManager getInstance(Context ctx) {
		if (mInstance == null) {
			synchronized (MqttServiceManager.class) {
				if (mInstance == null) {
					mInstance = new MqttServiceManager(ctx);
				}
			}
		}
		
		return mInstance;
	}
	
	/**
	 * connect to host 
	 * 
	 * @param host
	 * @param deviceUuid
	 * @param timeOut
	 * @param keepAliveInterval
	 * @param userName
	 * @param password
	 */
	public synchronized void connect(final String host,
			final String deviceUuid, final int timeOut,
			final int keepAliveInterval, final String userName,
			final String password, final MqttListener listener) {
		
		if (mStatus == Status.DISCONNECTING) {
			if (listener != null) {
				listener.onConnectFailure(new Throwable("disconnecting..."));
			}
			return;
		}

		// assert != null
		if (host == null || deviceUuid == null || userName == null
				|| password == null) {
			if (listener != null) {
				listener.onConnectFailure(new Throwable(
						"host or deviceUuid or username or password can not be empty."));
			}
			return;
		}

		// already connect
		if (isConnectedOrConnecting(host, deviceUuid, userName)) {
			MqttPlugin
					.debug(this.getClass(),
							String.format(
									"mqtt client already connected host:%s, deviceUuid:%s, userName:%s.",
									host, deviceUuid, userName));
			if (listener != null) {
				listener.onConnectSucc();
			}
			return;
		}

		// if client is exit, then first disconnect, then reconnect the new-one.
		if (this.client != null) {
			disconnect(false, false, new DisconnectListener() {
				@Override
				public void onDisconnectFinish() {
					safeConnect(host, deviceUuid, timeOut, keepAliveInterval,
							userName, password, listener);
				}
			});
		} else {
			safeConnect(host, deviceUuid, timeOut, keepAliveInterval, userName,
					password, listener);
		}
	}

	/**
	 * @param host
	 * @param deviceUuid
	 * @param timeOut
	 * @param keepAliveInterval
	 * @param userName
	 * @param password
	 * @param listener
	 */
	private void safeConnect(String host, String deviceUuid, int timeOut,
			int keepAliveInterval, String userName, String password,
			MqttListener listener) {
		
		MqttPlugin
				.debug(this.getClass(), String
						.format("try to connect url:%s,clientHandle:%s,timeout:%d,keepAliveInterval:%d,userName:%s,password:%s",
								host, deviceUuid, timeOut, keepAliveInterval,
								userName, password));

		storeClient(host, deviceUuid, timeOut, keepAliveInterval, userName, password);
		
		startPushService();

		MqttConnectOptions conOpt = new MqttConnectOptions();
		conOpt.setCleanSession(true);
		conOpt.setConnectionTimeout(timeOut);
		conOpt.setKeepAliveInterval(keepAliveInterval);
		conOpt.setUserName(userName);
		conOpt.setPassword(password.toCharArray());

		client = newClient(host, deviceUuid, listener);
		connect(client, conOpt, deviceUuid, listener);
	}
	
	public void connect(MqttConnectConfig config, MqttListener listener) {
		if (config == null) {
			return;
		}

		connect(config.getHost(), config.getDeviceUuid(), config.getTimeout(),
				config.getKeepAliveInterval(), config.getUserName(),
				config.getPassword(), listener);
	}
	
	/**
	 * check is connected
	 * 
	 * @param host like tcp://192.168.0.101:1883
	 * @param deviceUuid xxxx-xxxx-xxx-xxxx
	 * @param userName userName
	 * @return
	 */
	private boolean isConnectedOrConnecting(String host, String deviceUuid, String userName) {
        MqttPlugin.debug(this.getClass(), "mStatus: " + mStatus);
		if (this.client != null && this.host != null && this.deviceUuid != null && this.userName != null) {
			return this.host.equals(host)
                && this.deviceUuid.equals(deviceUuid)
                && this.userName.equals(userName)
                && (mStatus == Status.CONNECTED || mStatus == Status.CONNECTING);
		}
		
		return false;
	}
	
	/**
	 * disconnect
	 * 
	 * @param stopPushService
	 * @param fullyExit
	 */
	public void disconnect(boolean stopPushService, boolean fullyExit) {
		disconnect(stopPushService, fullyExit, null);
	}
	
	/**
	 * disconnect
	 * 
	 * @param stopPushService
	 * @param fullyExit
	 * @param listener
	 */
	public void disconnect(boolean stopPushService, boolean fullyExit, final DisconnectListener listener) {
		MqttPlugin.debug(this.getClass(), String.format(
				"disconnect host:%s, deviceUuid:%s, userName:%s.",
				(host != null ? host : "unknown host."),
				(deviceUuid != null ? deviceUuid : "unknown deviceUuid."),
				(userName != null ? userName : "unknown userName")));
		
		publishStatus(Status.DISCONNECTING);
		
		if (client != null) {
			try {
				client.disconnect(null, new IMqttActionListener() {
					
					@Override
					public void onSuccess(IMqttToken asyncActionToken) {
						resetClient();
						publishStatus(Status.DISCONNECTED);
						if (listener != null) {
							listener.onDisconnectFinish();
						}
					}

					@Override
					public void onFailure(IMqttToken asyncActionToken,
							Throwable exception) {
						resetClient();
						publishStatus(Status.DISCONNECTED);
						if (listener != null) {
							listener.onDisconnectFinish();
						}
					}

				});
			} catch (MqttException e) {
				e.printStackTrace();
				MqttPlugin.debug(this.getClass(), "disconnect mqtt exception: " + e.toString());
			}
		} else {
			publishStatus(Status.DISCONNECTED);
		}
		
		if (fullyExit) {
			fullyExit();
		}
		
		if (stopPushService) {
			stopPushService();
		}
	}
	
	/**
	 * publish new status
	 * 
	 * @param newStatus
	 */
	private void publishStatus(Status newStatus) {
		MqttPlugin.debug(this.getClass(), "status: " + newStatus.name());
		mStatus = newStatus;
	}
	
	/**
	 * reset client
	 */
	private void resetClient() {
		client = null;
		host = null;
		deviceUuid = null;
		userName = null;
	}
	
	/**
	 * store
	 * @param host
	 * @param deviceUuid
	 */
	private void storeClient(String host, String deviceUuid, int timeout, int keepAliveInterval, String userName, String password) {
		this.host = host;
		this.deviceUuid = deviceUuid;
		this.userName = userName;
		
		SharedPreferences sharedPref = context.getSharedPreferences(MqttPluginConstants.MQTT_PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		
		editor.putString(MqttPluginConstants.MQTT_CONFIG_HOST, host);
		editor.putString(MqttPluginConstants.MQTT_CONFIG_DEVICE_UUID, deviceUuid);
		editor.putInt(MqttPluginConstants.MQTT_CONFIG_TIMEOUT, timeout);
		editor.putInt(MqttPluginConstants.MQTT_CONFIG_KEEP_ALIVE_INTERVAL, keepAliveInterval);
		editor.putString(MqttPluginConstants.MQTT_CONFIG_USER_NAME, userName);
		editor.putString(MqttPluginConstants.MQTT_CONFIG_PASSWORD, password);
		
		editor.commit();
		
		context.getSharedPreferences(MqttPluginConstants.CLIENT_PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(MqttPluginConstants.CLIENT_CONFIG_EXIT, false).commit();
	}
	
	/**
	 * clear client when disconnect
	 * 
	 */
	private void fullyExit() {
		SharedPreferences sharedPref = context.getSharedPreferences(MqttPluginConstants.CLIENT_PREF_NAME, Context.MODE_PRIVATE);
		sharedPref.edit().putBoolean(MqttPluginConstants.CLIENT_CONFIG_EXIT, true).commit();
	}
	
	/**
	 * create a new client with host and deviceUuid
	 * @param host
	 * @param deviceUuid
	 * @param listener
	 * @return
	 */
	private MqttAndroidClient newClient(String host, String deviceUuid, final MqttListener listener) {
		client = new MqttAndroidClient(context, host, deviceUuid);
		client.setCallback(new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				MqttPlugin.debug(MqttServiceManager.class, "connectionLost:" + (cause != null ? cause.toString() : "unknown reason"));
			}

			@Override
			public void messageArrived(String topic, MqttMessage message)
					throws Exception {
				MqttPlugin.debug(MqttServiceManager.class, "messageArrived:" + message.toString());
				
				if (!interceptMessage()) {
					if (listener != null) {
						listener.onMessageArrived(message);
					}
				}
				
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				MqttPlugin.debug(MqttServiceManager.this.getClass(), "deliveryComplete");
			}

		});
		
		return client;
	}
	
	/**
	 * connnect to speceified host
	 * 
	 * @param client
	 * @param conOpt
	 * @param deviceUuid
	 * @param listener
	 */
	private void connect(final MqttAndroidClient client,
			final MqttConnectOptions conOpt, final String deviceUuid,
			final MqttListener listener) {
		
		try {
			publishStatus(Status.CONNECTING);
			mToken = client.connect(conOpt, null, getConnectActionListener(deviceUuid, conOpt.getUserName(), listener));
			
		} catch (MqttException e) {
			e.printStackTrace();
			MqttPlugin.debug(this.getClass(), "mqtt connect exception : " + e.toString());
			onConnectFailure(listener, e);
		}
	}

	/**
	 * @param listener
	 * @param e
	 */
	private void onConnectFailure(final MqttListener listener, Throwable e) {
		resetClient();
		publishStatus(Status.DISCONNECTED);
		
		if (mToken != null) {
			mToken.setActionCallback(null);
		}
		
		if (listener != null) {
			listener.onConnectFailure(e);
		}
	}
	
	/**
	 * start service
	 */
	private void startPushService() {
		context.startService(new Intent(context, PushService.class));
	}

	/**
	 * stop service
	 */
	private void stopPushService() {
		context.stopService(new Intent(context, PushService.class));
	}
	
	private void interceptConnectSuccedEvent() {
//		LocalBroadcastManager.getInstance(context).registerReceiver(
//				new LocaleStateChangeReceiver(),
//				new IntentFilter(
//						SystemStateChangeReceiver.ACTION_BOOT_COMPLECTED));
	}
	
	/**
	 * intercept message
	 */
	private boolean interceptMessage() {
		if (MqttPluginUtils.isInBackground(context)) {
            MqttPlugin.debug(this.getClass(), "Our app is running in background.");
			
			if (notificationOpenActivity == null) {
				notificationOpenActivity = MqttPluginUtils
                    .getNotificationOpenActivityClass(context);
			}

			if (notificationSmallIcon == 0) {
				notificationSmallIcon = MqttPluginUtils
                    .getNotificationIcon(context);
			}
			
			if (notificationOpenActivity != null && notificationSmallIcon > 0) {
				MqttPluginUtils.showNotification(context, "title",
                                                 "You have unread messages.", notificationSmallIcon, notificationOpenActivity);
			} else {
				MqttPlugin.debug(this.getClass(), "get notificationOpenActivity error or get notificationSmallIcon error.");
			}
			
			return true;
		}
		
		MqttPlugin.debug(this.getClass(), "Our app is not running in background.");
		return false;
	}
	
	/** get the single connect action listener **/
	private IMqttActionListener getConnectActionListener(String deviceUuid,
			String userUuid, MqttListener listener) {
		if (mConnectActionListener == null) {
			mConnectActionListener = new ConnectActionListener(deviceUuid,
					userUuid, listener);
		} else {
			mConnectActionListener.setDeviceUuid(deviceUuid)
					.setUserUuid(userUuid).setListener(listener);
		}

		return mConnectActionListener;
	}
	
	/** connect action listener **/
	private final class ConnectActionListener implements IMqttActionListener {
		
		private String deviceUuid;
		private String userUuid;
		private MqttListener listener;
		
		public ConnectActionListener(String deviceUuid, String userUuid,
				MqttListener listener) {
			this.deviceUuid = deviceUuid;
			this.userUuid = userUuid;
			this.listener = listener;
		}
		
		/** set device uuid **/
		public ConnectActionListener setDeviceUuid(String deviceUuid) {
			this.deviceUuid = deviceUuid;
			return this;
		}
		
		/** set user uuid **/
		public ConnectActionListener setUserUuid(String userUuid){
			this.userUuid = userUuid;
			return this;
		}
		
		/** set listener **/
		public ConnectActionListener setListener(MqttListener listener) {
			this.listener = listener;
			return this;
		}
		
		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
			MqttPlugin.debug(MqttServiceManager.class, "mqtt connect onSuccess.");
			try {
				final String token = userUuid + "/" + deviceUuid + "/#";
				
				client.subscribe(token,
						MqttPluginConstants.MQTT_QOS, null,
						new IMqttActionListener() {
							@Override
							public void onSuccess(
									IMqttToken asyncActionToken) {
								MqttPlugin
										.debug(MqttServiceManager.class, "mqtt subscribe success, topic: " + token);
								
								publishStatus(Status.CONNECTED);
								interceptConnectSuccedEvent();
								
								if (listener != null) {
									listener.onConnectSucc();
								}
							}

							@Override
							public void onFailure(
									IMqttToken asyncActionToken,
									Throwable exception) {
								MqttPlugin
										.debug(MqttServiceManager.class, "mqtt subscribe failure : "
												+ exception.toString());
								
								onConnectFailure(listener, exception);
							}
						});
			} catch (MqttException e) {
				e.printStackTrace();
				MqttPlugin.debug(MqttServiceManager.class, "mqtt subscribe exception : "
						+ e.toString());
				
				onConnectFailure(listener, e);
			}
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken,
				Throwable exception) {
			MqttPlugin.debug(MqttServiceManager.class, "mqtt connect onFailure : "
					+ exception.toString());
			onConnectFailure(listener, exception);
		}

	};
}
