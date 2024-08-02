package utils;

import java.sql.SQLException;

import models.admin.AdminLog;
import v2.dbhelpers.AdminDatabaseHelper;


public class AdminUtils {
	
	private final AdminDatabaseHelper adbHelper;
	
	public AdminUtils() {
		adbHelper = new AdminDatabaseHelper();	
	}
	
	public enum AdminAction {
		ADD("ADD"),
		UPDATE("UPDATE"),
		DELETE("DELETE");
		
		public final String value;
		AdminAction(String value) {
			this.value = value;
		}
	}
	
	public enum LogStatus {
		INITIATED("INITIATED"),
		SUCCESS("SUCCESS"),
		FAILED("FAILED");
		
		public final String value;
		LogStatus(String value) {
			this.value = value;
		}
	}
	
	public enum RecordType {
		PAYMENT("PaymentInfo");
		
		public final String value;
		RecordType(String value) {
			this.value = value;
		}
	}
	
	public AdminLog addAdminLog(
			int userId,
			RecordType recordType,
			int recordId,
			AdminAction action,
			String description
	) throws SQLException {
		
		if (userId == -1) {
			LoggerUtils.log("Invalid admin's id while adding admin log: " + userId);
			return null;
		}
		
		AdminLog aLog = new AdminLog();
		aLog.userId = userId;
		aLog.recordType = recordType.value;
		aLog.recordId = recordId;
		aLog.action = action.value;
		aLog.status = LogStatus.INITIATED.value;
		aLog.description = description;
		
		try {
			aLog = adbHelper.addAdminLog(aLog);
			adbHelper.close();
			return aLog;
		} catch (SQLException e) {
			adbHelper.close();
			throw e;
		}
			
	}
	
	public boolean updateAdminLog(
			int userId,
			int logId,
			LogStatus logStatus
	) throws SQLException {
		
		if (userId == -1) {
			LoggerUtils.log("Invalid admin's id while updatng admin log: " + userId);
			return false;
		}
		
		AdminLog aLog = new AdminLog();
		aLog.userId = userId;
		aLog.id = logId;
		aLog.status = logStatus.value;
		
		try {
			
			boolean status = adbHelper.updateAdminLog(aLog);
			adbHelper.close();
			return status;
		} catch (SQLException e) {
			adbHelper.close();
			throw e;
		}
			
	}

}
