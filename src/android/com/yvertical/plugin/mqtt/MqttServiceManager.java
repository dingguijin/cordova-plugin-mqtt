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
	
	/** current config **/
	private MqttConnectConfig mConfig;
	
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
	public synchronized void connect(final MqttConnectConfig config, final MqttListener listener) {
		
		if (mStatus == Status.DISCONNECTING) {
			if (listener != null) {
				listener.onConnectFailure(new Throwable("disconnecting..."));
			}
			return;
		}

		// assert != null
		if (config == null) {
			if (listener != null) {
				listener.onConnectFailure(new Throwable(
						"host or deviceUuid or username or password can not be empty."));
			}
			return;
		}

		// already connect
		if (isConnectedOrConnecting(config)) {
			MqttPlugin.debug(
					this.getClass(),
					String.format("mqtt client already connected %s.",
							config.toString()));
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
					safeConnect(config, listener);
				}
			});
		} else {
			safeConnect(config, listener);
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
	private void safeConnect(final MqttConnectConfig config,
			MqttListener listener) {
		MqttPlugin.debug(this.getClass(),
				String.format("try to connect %s", config.toString()));

		storeClient(config);
		
		startPushService();

		MqttConnectOptions conOpt = new MqttConnectOptions();
		conOpt.setCleanSession(true);
		conOpt.setConnectionTimeout(config.getTimeout());
		conOpt.setKeepAliveInterval(config.getKeepAliveInterval());
		conOpt.setUserName(config.getUserName());
		conOpt.setPassword(config.getPassword().toCharArray());

		client = newClient(config, listener);
		connect(client, conOpt, config.getDeviceUuid(), listener);
	}
	
	/**
	 * is connected or connecting
	 * 
	 * @param config
	 * @return
	 */
	private boolean isConnectedOrConnecting(MqttConnectConfig config) {
		MqttPlugin.debug(this.getClass(), "mStatus: " + mStatus);
		return this.client != null
				&& config.equals(this.mConfig)
				&& (mStatus == Status.CONNECTED || mStatus == Status.CONNECTING);
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
				"disconnect %s.", (mConfig != null ? mConfig.toString() : "unknown host")));
		
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
		this.mConfig = null;
	}
	
	/**
	 * store
	 * @param host
	 * @param deviceUuid
	 */
	private void storeClient(MqttConnectConfig config) {
		this.mConfig = config;
		
		SharedPreferences sharedPref = context.getSharedPreferences(MqttPluginConstants.MQTT_PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		
		editor.putString(MqttPluginConstants.MQTT_CONFIG_HOST, config.getHost());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_DEVICE_UUID, config.getDeviceUuid());
		editor.putInt(MqttPluginConstants.MQTT_CONFIG_TIMEOUT, config.getTimeout());
		editor.putInt(MqttPluginConstants.MQTT_CONFIG_KEEP_ALIVE_INTERVAL, config.getKeepAliveInterval());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_USER_NAME, config.getUserName());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_PASSWORD, config.getPassword());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_NOTIFICATION_TITLE, config.getNotificationTitle());
		
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
	private MqttAndroidClient newClient(final MqttConnectConfig config, final MqttListener listener) {
		client = new MqttAndroidClient(context, config.getHost(), config.getDeviceUuid());
		client.setCallback(new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				MqttPlugin.debug(MqttServiceManager.class, "connectionLost:" + (cause != null ? cause.toString() : "unknown reason"));
			}

			@Override
			public void messageArrived(String topic, MqttMessage message)
					throws Exception {
				MqttPlugin.debug(MqttServiceManager.class, "messageArrived:" + message.toString());
				
				if (!interceptMessage(config)) {
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
	private boolean interceptMessage(MqttConnectConfig config) {
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
				MqttPluginUtils.showNotification(context, config.getNotificationTitle(),
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
