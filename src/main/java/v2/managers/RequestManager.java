package v2.managers;

import models.UserRequest;
import models.User;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import v2.dbhelpers.DatabaseHelper;

public class RequestManager {

	DatabaseHelper dbHelper;

	public RequestManager() {
		dbHelper = new DatabaseHelper();
	}

	public enum RequestType {
		REGISTERATION("REGISTERATION" , "Request for user registeration process."),
		CHANGE_PASSWORD("CHANGE_PASSWORD" ,"Request for change password process."),
		FORGOT_PASSWORD("FORGOT_PASSWORD","Request for forgot password process."),
		CHANGE_MOBILE_NUMBER("CHANGE_MOBILE_NUMBER","Request for change mobile number process."),
		FORCE_RESET_PASSWORD("FORCE_RESET_PASSWORD","Request for force reset password process.");

		public final String value, description;

		RequestType(String value, String description) {
			this.value = value;
			this.description = description;
		}
		
		public static RequestType get(String type) {
			for (RequestType requestType : RequestType.values()) {
				if (requestType.value.equals(type))
					return requestType;
			}
			return null;
		}
	}
	
	public UserRequest getUserRequest(
			int userId,
			String requestId,
			RequestType requestType
	) throws Exception {
		
		try {
			
			UserRequest request = dbHelper.getUserRequestById(requestId, userId , requestType);
			dbHelper.close();
			return request;
			
		} catch(Exception e) {
			dbHelper.close();
			throw e;
		}
		
	}
	
	public UserRequest createNewRequest(
			User user,
			RequestType type,
			String ipAddress
	) throws Exception {
		
		try {
			
			UserRequest requestDetails = new UserRequest();

			requestDetails.userId = user.userId;
			requestDetails.token = dbHelper.getPasscodeHash(user);
			requestDetails.type = type.value;
			requestDetails.description = type.description;
			requestDetails.isValid = true;
			requestDetails.status = Constants.OPEN;
			requestDetails.createdDateTime = DateTimeUtils.getCurrentDateTimeInIST();
			requestDetails.id = "req_N" + System.currentTimeMillis();
			requestDetails.validDatetime = DateTimeUtils.getDateTime(
					1, 
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
					DateTimeZone.IST
			);

			boolean status = dbHelper.addUserRequest(requestDetails, ipAddress);
			dbHelper.close();
			return status ? requestDetails : null;
			
		} catch(Exception e) {
			dbHelper.close();
			throw e;
		}
		
	}
	
	public UserRequest updateUserRequest(
			User user, 
			String requestId,  
			String status,
			boolean isValid,
			String ipAddress
			)
			throws Exception {

		try {

			UserRequest requestDetails = new UserRequest();

			requestDetails.userId = user.userId;
			requestDetails.status = status;
			requestDetails.isValid = isValid;
			if(requestDetails.status.equalsIgnoreCase(Constants.OPEN) && requestDetails.isValid)
				requestDetails.token = dbHelper.getPasscodeHash(user);
			requestDetails.id = requestId;
			requestDetails.updatedDateTime = DateTimeUtils.getCurrentDateTimeInIST();

			boolean statusSuccess = dbHelper.updateUserRequest(requestDetails, ipAddress);
			dbHelper.close();
			return statusSuccess ? requestDetails : null;

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}
	}

//	public UserRequest updateUserRequest(
//			User user, 
//			String requestId,  
//			String status,
//			boolean isValid,
//			String ipAddress,
//			String otpToken,
//			String otpTransactionId			
//			)
//			throws Exception {
//
//		try {
//
//			UserRequest requestDetails = new UserRequest();
//
//			requestDetails.userId = user.userId;
//			requestDetails.status = status;
//			requestDetails.isValid = isValid;
//			if(requestDetails.status.equalsIgnoreCase(Constants.OPEN) && requestDetails.isValid)
//				requestDetails.token = dbHelper.getPasscodeHash(user);
//			requestDetails.id = requestId;
//			requestDetails.updatedDateTime = DateTimeUtils.getCurrentDateTimeInIST();
//			requestDetails.otpTransactionId = otpTransactionId;
//			requestDetails.otpToken = otpToken;
//
//			
//			boolean statusSuccess = dbHelper.updateUserRequest(requestDetails, ipAddress);
//			
//			  
//			dbHelper.close();
//			return statusSuccess ? requestDetails : null;
//
//		} catch (Exception e) {
//			dbHelper.close();
//			throw e;
//		}
//	}
}
