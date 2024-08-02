package helper;

import org.json.JSONObject;

import dao.CommonRepository;
import dao.UserRepository;
import models.LoginInfo;
import models.User;
import utils.Constants;

public class AppContextProvider {

	private UserRepository _userRepo = null;
	private CommonRepository _commonRepo = null;
	
	private UserRepository userRepo() {
		if (null == _userRepo)
			_userRepo = new UserRepository();
		return _userRepo;
	}
	
	private CommonRepository commonRepo() {
		if (null == _commonRepo)
			_commonRepo = new CommonRepository();
		return _commonRepo;
	}
	
	public User getUserById(int userId) {
		return userRepo().findUserByUserId(userId);
	}
	
	public User getUserByMobileNumber(String mobileNumber) {
		return userRepo().findUserByMobileNumber(mobileNumber);
	}
	
	public boolean addLoginInfo(int userId, String deviceId, String deviceType) {
		
		final var loginInfo = new LoginInfo();
		loginInfo.userId = userId;
		loginInfo.deviceId = deviceId;
		loginInfo.deviceType = deviceType;
				
		return commonRepo().insertLoginInfo(loginInfo);
	}
	
	public boolean addLoginInfo(LoginInfo loginInfo) {				
		return commonRepo().insertLoginInfo(loginInfo);
	}
	
	public boolean addLoginInfo(int userId, JSONObject requestObject, String ipAddress) {
		
		final var loginInfo = new LoginInfo();
		loginInfo.userId = userId;
		loginInfo.deviceId = requestObject.optString("deviceId", Constants.NA);
		loginInfo.deviceType = requestObject.optString("deviceType", Constants.NA);
		loginInfo.ipAddress = ipAddress;
		
		String modelName = requestObject.optString("deviceModel", Constants.NA);
		if (modelName.length() > 128)
			modelName = modelName.substring(0, 128);
		loginInfo.deviceModel = modelName;
		
		loginInfo.appVersion = requestObject.optString("appVersion", Constants.NA);
		loginInfo.osVersion = requestObject.optString("osVersion", Constants.NA);
				
		return commonRepo().insertLoginInfo(loginInfo);
	}
	
	public boolean verifyUser(int userId, String passcode) {

		if (!passcode.equals(Constants.NA)) {
			
			final var user = userRepo().findUserByUserId(userId);

			if (user.sessionPasscode.equals(passcode))
				return true;
		}

		return false;
	}
	
}
