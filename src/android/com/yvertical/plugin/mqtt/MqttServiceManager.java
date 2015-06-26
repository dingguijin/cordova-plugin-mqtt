package com.yvertical.plugin.mqtt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.yvertical.plugin.mqtt.MqttClient.MqttListener;

public class MqttServiceManager {

	/** single instance **/
	private static MqttServiceManager mInstance;

	/** context **/
	private Context mContext;
	
	/** mapping from client to actual clients **/
	private Map<String, MqttClient> clients = new HashMap<String, MqttClient>();

	private MqttServiceManager(Context ctx) {
		mContext = ctx;
	}

	public static MqttServiceManager getInstance(Context context) {
		if (mInstance == null) {
			synchronized (MqttServiceManager.class) {
				mInstance = new MqttServiceManager(context);
			}
		}
		return mInstance;
	}
	
	/**
	 * connect
	 * 
	 * @param config
	 * @param listener
	 */
	public void connect(MqttConnectConfig config, MqttListener listener) {
		connect(true, config, listener);
	}
	
	/**
	 * 
	 * @param newConnect
	 * @param config
	 * @param listener
	 */
	public void connect(boolean newConnect, MqttConnectConfig config, MqttListener listener) {
		if (config != null) {
			synchronized (clients) {
				MqttClient client = null;
				if (clients.containsKey(config.getClientHandle())) {
					client = clients.get(config.getClientHandle());
				} else {
					client = new MqttClient(mContext, config, listener);
					clients.put(config.getClientHandle(), client);
				}
				client.connect(newConnect);
				
				if (clients.size() > 1) {
					findOtherClients(config.getClientHandle());
				}
			}
		}
	}
	
	/**
	 * disconnect
	 * 
	 * @param stopPushService
	 * @param fullyExit
	 */
	public void disconnect(boolean stopPushService, boolean fullyExit) {
		disconnectOnBatch(clients.values(), true, true);
	}
	
	/**
	 * disconnect clients onbatch
	 * 
	 * @param batchClients
	 * @param stopPushService
	 * @param fullyExit
	 */
	private void disconnectOnBatch(Collection<MqttClient> batchClients,
			boolean stopPushService, boolean fullyExit) {
		if (batchClients != null) {
			for (MqttClient client : batchClients) {
				client.disconnect(stopPushService, fullyExit);
			}
			for (MqttClient client : batchClients) {
				clients.remove(client.getConnectConfig().getClientHandle());
			}
		}
	}
	
	/**
	 * Find other MqttClient which clientHandle are different from the param clientHandle.
	 * 
	 * @param clientHandle
	 * @return
	 */
	private Collection<MqttClient> findOtherClients(String clientHandle) {
		Collection<MqttClient> otherClients = new ArrayList<MqttClient>();
		synchronized (clients) {
			for (MqttClient client : otherClients) {
				if (!client.getConnectConfig().getClientHandle()
						.equals(clientHandle)) {
					otherClients.add(client);
				}
			}
		}
		return otherClients;
	}
}
