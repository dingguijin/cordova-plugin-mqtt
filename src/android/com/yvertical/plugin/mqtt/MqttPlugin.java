package com.yvertical.plugin.mqtt;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.yvertical.plugin.mqtt.MqttServiceManager.MqttListener;

public class MqttPlugin extends CordovaPlugin {
	
	private static CallbackContext mConnectCallbackContext;
    private static CallbackContext mMessageArrivedCallbackContext;
    
    /** matt manager **/
    private MqttServiceManager mqttManager;
    
    /** default mqtt listener **/
    static MqttListener DEFAULT = new MqttListener() {

		@Override
		public void onMessageArrived(MqttMessage message) {
			if (mMessageArrivedCallbackContext != null) {
                debug(MqttPlugin.class, "java->onMessageArrived->js");
				PluginResult result = new PluginResult(
						Status.OK, message.toString());
				result.setKeepCallback(true);
				mMessageArrivedCallbackContext.sendPluginResult(result);
			}
			
		}

		@Override
		public void onConnectSucc() {
			if (mConnectCallbackContext != null) {
				mConnectCallbackContext.success();
			}
		}

		@Override
		public void onConnectFailure(Throwable exception) {
			if (mConnectCallbackContext != null) {
				mConnectCallbackContext.error("connect failed.");
			}
		}
		
	};

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		mqttManager = MqttServiceManager.getInstance(webView.getContext());
	}

	@Override
	public boolean execute(String action, CordovaArgs args,
                           final CallbackContext callbackContext) throws JSONException {

        debug(this.getClass(), String.format("action:%s, callbackContext.callbackId:%s", action, callbackContext.getCallbackId()));
        
        try{
        	if (action.equals(MqttPluginConstants.CONNECT_ACTION)) {

                //THREAD WARNING: exec() call to Toast.connect blocked the main thread for 17ms.
                //Plugin should use CordovaInterface.getThreadPool().
                
            	mConnectCallbackContext = callbackContext;
            	
                String url = args.getString(0); // url
                String clientHandle = args.getString(1); // clientHandle

                JSONObject options = args.getJSONObject(2); // options
                int timeout = options.getInt("timeout"); // timeout
                int keepAliveInterval = options.getInt("keepAliveInterval"); // keepAliveInterval
                String userName = options.getString("userName"); // username
                String password = options.getString("password"); // password
                
				mqttManager.connect(url, clientHandle, timeout,
						keepAliveInterval, userName, password, DEFAULT);
            } else if (action
					.equals(MqttPluginConstants.SET_ON_MESSAGE_ARRIVED_CALLBACK_ACTION)) {
                mMessageArrivedCallbackContext = callbackContext;
				PluginResult result = new PluginResult(Status.OK);
				result.setKeepCallback(true);
				callbackContext.sendPluginResult(result);
				debug(this.getClass(), "set on message arrived callback action successful.");
			} else if (action.equals(MqttPluginConstants.DISCONNECT_ACTION)) {
				if (mqttManager != null) {
					mqttManager.disconnect(true, true);
				}
				callbackContext.success();
			}
        } catch (JSONException e){
            callbackContext.error(e.toString());
            debug(this.getClass(), e.toString());
        } catch (IllegalArgumentException e){
            callbackContext.error(e.toString());
            debug(this.getClass(), e.toString());
        }

		return true;
	}
    
    public static void debug(Class<?> c, String message) {
    	Log.d("MqttPlugin", String.format("[%s]:%s", (c != null ? c.getSimpleName() : "Unknown class"), message));
    }
}
