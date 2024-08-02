package v2.managers;

import models.UserCommunication;
import utils.Constants;
import utils.Constants.Errors;
import utils.LocalResponse;
import utils.LoggerUtils;
import v2.dbhelpers.DatabaseHelper;

public class UserComHelper {

	private final DatabaseHelper dbHelper;
	
	public UserComHelper() {
		dbHelper = new DatabaseHelper();
	}
	
	public enum CommunicationType {
		
		EMAIL("Email"),
		SMS("SMS"),
		NOTIFICATION("Notification");
		
		public final String value;
		CommunicationType(String value) {
			this.value = value;
		}
	}
	
	private LocalResponse closeResourceAndReturn(LocalResponse lResponse) {
		dbHelper.close();
		return lResponse;
	}
	
	public LocalResponse insertUserCommunitcation(UserCommunication userCom) {
		
		try {
			
			boolean isSuccess = dbHelper.inserUserCommunication(userCom);
			
			return closeResourceAndReturn(
					new LocalResponse()
					.setStatus(isSuccess)
					.setMessage(isSuccess ? Constants.NA : Errors.UNKNOWN.value)
			);
			
		} catch (Exception e) {
			
			LoggerUtils
					.log("Failed to insert user communication entry for " + userCom.recordType + " | Error: "
							+ e.getMessage());
			e.printStackTrace();
			
			return closeResourceAndReturn(
					new LocalResponse()
					.setStatus(false)
					.setError(e.getMessage())
			);
			
		}
		
	}
	
	public LocalResponse updateUserCommunication(UserCommunication userCom, CommunicationType cType) {
		
		try {
			
			boolean isSuccess = dbHelper.updateUserCommunication(userCom, cType);
			
			return closeResourceAndReturn(
					new LocalResponse()
					.setStatus(isSuccess)
					.setMessage(isSuccess ? Constants.NA : Errors.UNKNOWN.value)
			);
			
		} catch (Exception e) {
			
			LoggerUtils
					.log("Failed to update user communication entry for " + userCom.recordType + " | Error: "
							+ e.getMessage());
			e.printStackTrace();
			
			return closeResourceAndReturn(
					new LocalResponse()
					.setStatus(false)
					.setError(e.getMessage())
			);
			
		}
		
	}
	
}
