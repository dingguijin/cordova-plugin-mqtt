package com.yvertical.plugin.mqtt;

public class MqttConnectConfig {

	private String host;
	private String deviceUuid;
	private String userName;
	private String password;
	private int timeout;
	private int keepAliveInterval;
	private String notificationTitle;
	
	public MqttConnectConfig(){
		
	}
	
	public MqttConnectConfig(String host, String deviceUuid, String userName,
			String password, int timeout, int keepAliveInterval,
			String notificationTitle) {
		super();
		this.host = host;
		this.deviceUuid = deviceUuid;
		this.userName = userName;
		this.password = password;
		this.timeout = timeout;
		this.keepAliveInterval = keepAliveInterval;
		this.notificationTitle = notificationTitle;
	}

	public String getNotificationTitle() {
		return notificationTitle;
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
	
	public MqttConnectConfig setNotificationTitle(String notificationTitle) {
		this.notificationTitle = notificationTitle;
		return this;
	}
	
	public MqttConnectConfig setHost(String host){
		this.host = host;
		return this;
	}
	
	public MqttConnectConfig setDeviceUuid(String deviceUuid){
		this.deviceUuid = deviceUuid;
		return this;
	}
	
	public MqttConnectConfig setUserName(String userName){
		this.userName = userName;
		return this;
	}
	
	public MqttConnectConfig setPassword(String password){
		this.password = password;
		return this;
	}
	
	public MqttConnectConfig setTimeout(int timeout){
		this.timeout = timeout;
		return this;
	}
	
	public MqttConnectConfig setKeepAliveInterval(int keepAliveInterval){
		this.keepAliveInterval = keepAliveInterval;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((deviceUuid == null) ? 0 : deviceUuid.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MqttConnectConfig other = (MqttConnectConfig) obj;
		if (deviceUuid == null) {
			if (other.deviceUuid != null)
				return false;
		} else if (!deviceUuid.equals(other.deviceUuid))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MqttConnectConfig [host=" + host + ", deviceUuid=" + deviceUuid
				+ ", userName=" + userName + ", password=" + password
				+ ", timeout=" + timeout + ", keepAliveInterval="
				+ keepAliveInterval + ", notificationTitle="
				+ notificationTitle + "]";
	}
	
	
}
