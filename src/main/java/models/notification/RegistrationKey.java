package models.notification;

import org.json.JSONObject;

import utils.Constants;

public class RegistrationKey {
	
	public int userId = -1;
	public String key = Constants.NA;
	public String deviceId = Constants.NA;
	public String deviceType = Constants.NA;
	
	public RegistrationKey() {}
	
	public RegistrationKey(int userId, JSONObject json) {
		this.userId = userId;
		key = json.optString("key", key);
		deviceId = json.optString("device_id", deviceId);
		deviceType = json.optString("device_type", deviceType);
	}

}
