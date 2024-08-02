package v3.managers;

import org.json.JSONObject;

import helper.AppContextProvider;
import models.User;
import utils.BasicUtils;
import utils.Constants;
import utils.Constants.Errors;

public class PaymentManager {
	
	private AppContextProvider appContextProvider = new AppContextProvider();

	public PaymentManager() {
	}

	public JSONObject initiatePayment(int userId, JSONObject bodyObject) throws Exception {

		JSONObject responseJson = null;

		try {

			User fetchedUser = appContextProvider.getUserById(userId);

			if (null == fetchedUser) {
				responseJson = new JSONObject();
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "User doesn't exist.");				
				return responseJson;
			}

			String password = bodyObject.optString("password", Constants.NA);

			if (!fetchedUser.password.equals(BasicUtils.getTheKey(password))) {
				responseJson = new JSONObject();
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "Wrong password. Please enter correct password.");
				responseJson.put(Constants.ERROR, Errors.INVALID_PASSWORD.value);
				return responseJson;
			}		
			
			return new v2.managers.PaymentManager().initiatePayment(userId, bodyObject);
			

		} catch (Exception e) {
			throw e;
		}

	}

}
