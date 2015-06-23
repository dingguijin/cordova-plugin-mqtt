package com.yvertical.plugin.mqtt;

public class MqttConnectConfig {

	private String host;
	private String deviceUuid;
	private String userName;
	private String password;
	private int timeout;
	private int keepAliveInterval;
	
	public MqttConnectConfig(String host, String deviceUuid, String userName,
			String password, int timeout, int keepAliveInterval) {
		super();
		this.host = host;
		this.deviceUuid = deviceUuid;
		this.userName = userName;
		this.password = password;
		this.timeout = timeout;
		this.keepAliveInterval = keepAliveInterval;
	}
	
	public String getHost() {
		return host;
	}
	public String getDeviceUuid() {
		return deviceUuid;
	}
	public String getUserName() {
		return userName;
	}
	public String getPassword() {
		return password;
	}
	public int getTimeout() {
		return timeout;
	}
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}
	
}
