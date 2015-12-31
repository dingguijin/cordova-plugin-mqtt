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
import android.os.Handler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MqttClient {
	private final Context context;

	/** mqtt client **/
	private MqttAndroidClient client;
	/** listen for connect or disconnect state **/
	private MqttListener mListener;

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

	/** handler to exeute task to re-connect server **/
	private Handler mHandler;

	/** reconnect-task **/
	private ReconnectTask mReconnectTask;
	
	/** record connect times for debug **/
	private static int connectTimes;

	/**
	 * mqtt event listener
	 * 
	 * @author zhaokun
	 *
	 */
	interface MqttListener {

		/**
		 * on message arrived
		 * 
		 * @param message
		 */
		void onMessageArrived(MqttMessage message);

		/**
		 * connect successful
		 */
		void onConnectSucc();

		/**
		 * on connect failure
		 * 
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

	public MqttClient(Context ctx, MqttConnectConfig config,
                      MqttListener listener) {
		context = ctx;
		mStatus = Status.DISCONNECTED;
		this.mConfig = config;
		this.mListener = listener;
	}
	
	/**
	 * 
	 * @param newConnect
	 */
	public synchronized void connect(boolean newConnect) {
		if (!newConnect) {
			if (isQuit(context)) {
				return;
			}
		}
		
		MqttPlugin.debug(this.getClass(), "----->" + mStatus.name());
		
		if (isConnected()) {
			if (mListener != null) {
				mListener.onConnectSucc();
			}
			return;
		}
		
		if (isConnecting() || isDisconnecting()) {
			return;
		}

		safeConnect(mConfig, mListener);
	}
	
	/**
	 * get config on this client
	 * 
	 * @return
	 */
	public MqttConnectConfig getConnectConfig() {
		return mConfig;
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
		storeClient();
		startPushService();

		MqttConnectOptions conOpt = new MqttConnectOptions();
		conOpt.setCleanSession(true);
		conOpt.setConnectionTimeout(config.getTimeout());
		conOpt.setKeepAliveInterval(config.getKeepAliveInterval());
		conOpt.setUserName(config.getUserName());
		conOpt.setPassword(config.getPassword().toCharArray());

		client = findClient();
		connect(client, conOpt);
	}
	
	/**
	 * is connected
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return this.client != null && mStatus == Status.CONNECTED && this.client.isConnected();
	}
	
	/**
	 * connecting
	 * @return
	 */
	private boolean isConnecting() {
		return mStatus == Status.CONNECTING;
	}
	
	/**
	 * is disconnecting
	 * @return
	 */
	private boolean isDisconnecting() {
		return mStatus == Status.DISCONNECTING;
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
        //		MqttPlugin.debug(this.getClass(), "user fully leave our app : " + exit);
		return exit;
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
	public void disconnect(boolean stopPushService, boolean fullyExit,
                           final DisconnectListener listener) {
        //		MqttPlugin.debug(this.getClass(), String.format("disconnect %s.",
        //				(mConfig != null ? mConfig.toString() : "unknown host")));

		publishStatus(Status.DISCONNECTING);

		if (client != null) {
			try {
				client.disconnect(null, new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            publishStatus(Status.DISCONNECTED);
                            if (listener != null) {
                                listener.onDisconnectFinish();
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken,
                                              Throwable exception) {
                            publishStatus(Status.DISCONNECTED);
                            if (listener != null) {
                                listener.onDisconnectFinish();
                            }
                        }

                    });
			} catch (MqttException e) {
				e.printStackTrace();
				MqttPlugin.debug(this.getClass(), "disconnect mqtt exception: "
                                 + e.toString());
			}
		}

		if (fullyExit) {
			fullyExit();
		}

		if (stopPushService) {
			stopPushService();
		}
	}

	public void quickDisconnect(){
		if (this.client != null) {
			try {
				this.client.disconnect(null, null);
			} catch (MqttException e) {
				MqttPlugin.debug(this.getClass(), e.toString());
			}
		}
	}
	
	/**
	 * publish new status
	 * 
	 * @param newStatus
	 */
	private void publishStatus(Status newStatus) {
		MqttPlugin.debug(this.getClass(), "current status: " + newStatus.name());
		mStatus = newStatus;
	}

	/**
	 * store
	 * 
	 * @param host
	 * @param deviceUuid
	 */
	private void storeClient() {
		SharedPreferences sharedPref = context.getSharedPreferences(
                                                                    MqttPluginConstants.MQTT_PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();

		editor.putString(MqttPluginConstants.MQTT_CONFIG_HOST,
                         mConfig.getHost());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_DEVICE_UUID,
                         mConfig.getDeviceUuid());
		editor.putInt(MqttPluginConstants.MQTT_CONFIG_TIMEOUT,
                      mConfig.getTimeout());
		editor.putInt(MqttPluginConstants.MQTT_CONFIG_KEEP_ALIVE_INTERVAL,
                      mConfig.getKeepAliveInterval());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_USER_NAME,
                         mConfig.getUserName());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_PASSWORD,
                         mConfig.getPassword());
		editor.putString(MqttPluginConstants.MQTT_CONFIG_NOTIFICATION_TITLE,
                         mConfig.getNotificationTitle());

		editor.commit();

		context.getSharedPreferences(MqttPluginConstants.CLIENT_PREF_NAME,
                                     Context.MODE_PRIVATE).edit()
            .putBoolean(MqttPluginConstants.CLIENT_CONFIG_EXIT, false)
            .commit();
	}

	/**
	 * clear client when disconnect
	 * 
	 */
	private void fullyExit() {
		SharedPreferences sharedPref = context.getSharedPreferences(MqttPluginConstants.CLIENT_PREF_NAME, Context.MODE_PRIVATE);
		sharedPref.edit()
            .putBoolean(MqttPluginConstants.CLIENT_CONFIG_EXIT, true)
            .commit();
	}

	/**
	 * find a new client with host and deviceUuid
	 * 
	 * @param host
	 * @param deviceUuid
	 * @param mListener
	 * @return
	 */
	private MqttAndroidClient findClient() {
		if (client == null) {
			client = new MqttAndroidClient(context, mConfig.getHost(), mConfig.getDeviceUuid());

			client.setCallback(new MqttCallback() {
                    
                    @Override
                    public void connectionLost(Throwable cause) {
                        MqttPlugin.debug(MqttServiceManager.class,
                                         "connectionLost:"
                                         + (cause != null ? cause.toString()
                                            : "unknown reason"));
                        if (cause != null) {
                            onConnectFailure("connectionLost", cause, true);
                        }
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message)
                        throws Exception {
                        MqttPlugin.debug(MqttServiceManager.class,
                                         "messageArrived:" + message.toString());

                        if (!interceptMessage(mConfig, message)) {
                            if (mListener != null) {
                                mListener.onMessageArrived(message);
                            }
                        }

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        MqttPlugin.debug(MqttClient.this.getClass(),
                                         "deliveryComplete");
                    }

                });
		}

		return client;
	}

	/**
	 * connnect to speceified host
	 * 
	 * @param client
	 * @param conOpt
	 * @param deviceUuid
	 * @param mListener
	 */
	private void connect(final MqttAndroidClient client,
                         final MqttConnectOptions conOpt) {
		try {
			publishStatus(Status.CONNECTING);
            
			MqttPlugin.debug(this.getClass(),
                             String.format("client connect %s, times:%d.", mConfig.toString(), ++connectTimes));
			
			client.connect(conOpt, null,
                           ((ConnectActionListener)getConnectActionListener(mConfig, mListener)).setDebugId("debugId:" + connectTimes));
		} catch (MqttException e) {
			e.printStackTrace();
			MqttPlugin.debug(this.getClass(),
                             "mqtt connect exception : " + e.toString());
			onConnectFailure("A", e, false);
		}
	}

	/**
	 * @param mListener
	 * @param e
	 */
	private void onConnectFailure(String callFrom, final Throwable e, final boolean disconnect) {
		MqttPlugin.debug(this.getClass(),
                         "onConnectFailure----->callFrom----->" + callFrom);
		if (disconnect) {
			disconnect(false, false, new DisconnectListener() {
                    @Override
                    public void onDisconnectFinish() {
                        if (mListener != null) {
                            mListener.onConnectFailure(e);
                        }
                        tryToReconnect();
                    }
                });
		} else {
			publishStatus(Status.DISCONNECTED);
			if (mListener != null) {
				mListener.onConnectFailure(e);
			}
			tryToReconnect();
		}
	}

	/**
	 * try to reconnect to server when connect failure
	 * 
	 * @param mConfig
	 * @param listener
	 */
	private void tryToReconnect() {
		if (!MqttPluginUtils.isNetworkAvaliable(context)) {
			MqttPlugin.debug(this.getClass(), "Network is not avaliable.");
			cancelReconnect();
			return;
		}

		if (reconnecting()) {
			return;
		}

		if (mHandler == null) {
			mHandler = new Handler();
		}

		if (mReconnectTask == null) {
			mReconnectTask = new ReconnectTask();
		}

		if (mHandler != null && mReconnectTask != null) {
			mHandler.postDelayed(mReconnectTask,
                                 mReconnectTask.getNextReconnectSleepTime());
		}
	}

	/**
	 * reconnecting?
	 * 
	 * @return
	 */
	private boolean reconnecting() {
		return mHandler != null && mReconnectTask != null;
	}

	/**
	 * cancel reconnect
	 * 
	 * @return
	 */
	private void cancelReconnect() {
		if (mHandler != null) {
			if (mReconnectTask != null) {
				MqttPlugin.debug(this.getClass(), "Cancel reconnect task");
				mHandler.removeCallbacks(mReconnectTask);
				mReconnectTask = null;
			}
			mHandler = null;
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
	
	private void onDisconnected() {
		if (client != null) {
			client.setCallback(null);
		}
	}

	/**
	 * intercept connect event
	 * 
	 */
	private void interceptConnectSuccedEvent() {
		cancelReconnect();
	}

	/**
	 * intercept message
	 */
	private boolean interceptMessage(MqttConnectConfig config, MqttMessage message) {
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
				// MqttPluginUtils.showNotification(context,
                //                                  config.getNotificationTitle(),                                                 
                //                                  "You have unread messages.", notificationSmallIcon,
                //                                  notificationOpenActivity);
                String msg = message1.toString();
                String title = null;
                try {
                    JSONObject json = new JSONObject(msg);
                    title = json.getString("title");
                } catch (JSONException e) {
                    
                }
                MqttPluginUtils.showNotification(context,
                                                 config.getNotificationTitle(),
                                                 title != null ? title : "",
                                                 notificationSmallIcon,
                                                 notificationOpenActivity);
			} else {
				MqttPlugin
                    .debug(this.getClass(),
                           "get notificationOpenActivity error or get notificationSmallIcon error.");
			}

			return true;
		}

		MqttPlugin.debug(this.getClass(),
                         "Our app is not running in background.");
		return false;
	}

	/** get the single connect action listener **/
	private IMqttActionListener getConnectActionListener(MqttConnectConfig config, MqttListener listener) {
		if (mConnectActionListener == null) {
			mConnectActionListener = new ConnectActionListener(config, listener);
		} else {
			mConnectActionListener.setConnectConfig(config).setListener(listener);
		}

		return mConnectActionListener;
	}

	/** connect action listener **/
	private final class ConnectActionListener implements IMqttActionListener {

		private MqttConnectConfig config;
		private MqttListener listener;
		private String debugId;

		public ConnectActionListener(MqttConnectConfig config,
                                     MqttListener listener) {
			this.config = config;
			this.listener = listener;
		}

		/** set connect config **/
		public ConnectActionListener setConnectConfig(MqttConnectConfig config) {
			this.config = config;
			return this;
		}

		/** set listener **/
		public ConnectActionListener setListener(MqttListener listener) {
			this.listener = listener;
			return this;
		}
		
		public ConnectActionListener setDebugId(String debugId){
			this.debugId = debugId;
			return this;
		}

		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
			MqttPlugin.debug(MqttServiceManager.class,
                             "mqtt connect onSuccess : " + debugId);
			try {
				final String token = config.getUserName() + "/"
                    + config.getDeviceUuid() + "/#";

				client.subscribe(token, MqttPluginConstants.MQTT_QOS, null,
                                 new IMqttActionListener() {
                                     @Override
                                     public void onSuccess(IMqttToken asyncActionToken) {
                                         MqttPlugin.debug(MqttServiceManager.class,
                                                          "mqtt subscribe success, topic: "
                                                          + token);

                                         publishStatus(Status.CONNECTED);
                                         interceptConnectSuccedEvent();

                                         if (listener != null) {
                                             listener.onConnectSucc();
                                         }
                                     }

                                     @Override
                                     public void onFailure(IMqttToken asyncActionToken,
                                                           Throwable exception) {
                                         //								MqttPlugin.debug(
                                         //										MqttServiceManager.class,
                                         //										"mqtt subscribe failure : "
                                         //												+ exception.toString());

                                         onConnectFailure("subscribe onFailure", exception, true);
                                     }
                                 });
			} catch (MqttException e) {
				e.printStackTrace();
				MqttPlugin.debug(MqttServiceManager.class,
                                 "mqtt subscribe exception : " + e.toString());
				onConnectFailure("subscribe Exception", e, true);
			}
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			MqttPlugin.debug(MqttServiceManager.class,
                             "mqtt connect onFailure : " + exception.toString());
			onConnectFailure("connect onFailure", exception, false);
		}

	};

	/** a Runnable which provide function to reconnect **/
	private final class ReconnectTask implements Runnable {

		/** pointer **/
		private int mNextIndex;
		/** next time re-connect sleep time **/
		private int mNextReconnectSleepTime;
		/** try to re-connect sleep time **/
		final int MQTT_RETRY_CONNECT_SLEEP_TIME[] = { 8 * 1000, 16 * 1000,
                                                      32 * 1000, 64 * 1000, 128 * 1000, 256 * 1000, 512 * 1000,
                                                      1024 * 1000, 2048 * 1000, 4096 * 1000, };

		public ReconnectTask() {
			mNextReconnectSleepTime = MQTT_RETRY_CONNECT_SLEEP_TIME[mNextIndex];
		}

		@Override
		public void run() {
			MqttPlugin.debug(this.getClass(), String.format(
                                                            "Try to reconnect , the last sleep time is %d.",
                                                            mNextReconnectSleepTime));
			connect(false);
			setNextReconnectSleepTime();

			if (mHandler != null) {
				mHandler.postDelayed(this, mNextReconnectSleepTime);
			}
		}

		private void setNextReconnectSleepTime() {
			mNextReconnectSleepTime = MQTT_RETRY_CONNECT_SLEEP_TIME[mNextIndex++
                                                                    % MQTT_RETRY_CONNECT_SLEEP_TIME.length];
		}

		public int getNextReconnectSleepTime() {
			return mNextReconnectSleepTime;
		}
	}
}
