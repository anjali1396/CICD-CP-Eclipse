package models.admin;

import org.json.JSONObject;

import utils.Constants;

public class AdminUser {
	
    public int id = -1;
    public String name = Constants.NA;
    public String email = Constants.NA;
    public String imageUrl = Constants.NA;
    public String countryCode = Constants.NA;
    public String mobileNumber = Constants.NA;
    public String sfUserId = Constants.NA;
    public String passcode = Constants.NA;
    public String password = Constants.NA;
    public String registeredDatetime = Constants.NA;
    public boolean allowedNotification = false;
    public int role = -1;

    public AdminUser() {}
    
    public AdminUser(JSONObject json) {

        id = json.optInt("id", id);
        name = json.optString("name", name);
        email = json.optString("email", email);
        imageUrl = json.optString("imageUrl", imageUrl);
        countryCode = json.optString("countryCode", countryCode);
        mobileNumber = json.optString("mobileNumber", mobileNumber);
        sfUserId = json.optString("sfUserId", sfUserId);
        registeredDatetime = json.optString("registeredDatetime", registeredDatetime);
        allowedNotification = json.optBoolean("allowedNotification", allowedNotification);

    }
    
    public JSONObject toJson() {
    	
    	JSONObject json = new JSONObject();
    	
    	json.put("id", id);
        json.put("name", name);
        json.put("email", email);
        json.put("imageUrl", imageUrl);
        json.put("countryCode", countryCode);
        json.put("mobileNumber", mobileNumber);
        json.put("sfUserId", sfUserId);
        json.put("registeredDatetime", registeredDatetime);
        json.put("allowedNotification", allowedNotification);

    	return json;
    	
    }
    
    public JSONObject toJsonWithPasscode() {
    	
    	JSONObject json = new JSONObject();
    	
    	json.put("id", id);
        json.put("name", name);
        json.put("email", email);
        json.put("imageUrl", imageUrl);
        json.put("countryCode", countryCode);
        json.put("mobileNumber", mobileNumber);
        json.put("sfUserId", sfUserId);
        json.put("passcode", passcode);
        json.put("registeredDatetime", registeredDatetime);
        json.put("allowedNotification", allowedNotification);
    	
    	return json;
    	
    }

}
