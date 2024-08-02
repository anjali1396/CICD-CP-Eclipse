package v2.dbhelpers;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;


import org.json.JSONArray;
import org.json.JSONObject;

import dao.DataProvider;
import models.admin.AdminDashboard;
import models.admin.AdminLog;
import models.admin.AdminUser;
import models.payment.Payment;
import utils.BasicUtils;
import utils.ColumnsNFields;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LoggerUtils;
import utils.PaymentUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import v2.managers.AdminUserManager.AnalyticsRange;
import v2.managers.AdminUserManager.SearchType;

public class AdminDatabaseHelper {
	
	private Connection connection = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	
	public AdminDatabaseHelper() {}
	
	public void close() {
		if (resultSet != null) {
			try {
				resultSet.close();
			}catch (SQLException se) {
		    	  se.printStackTrace();
			}
			resultSet = null;
		}
		if (preparedStatement != null) {
			try { 
				preparedStatement.close(); 
			} catch (SQLException se) {
		    	  se.printStackTrace();
			}
			preparedStatement = null;
		}	
		if(connection != null) {
			try {
				connection.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
			connection = null;
		}
	}
	
	private void checkAndConnect() throws SQLException {
		if (null == connection || !connection.isValid(10))
			connection = DataProvider.getDataSource().getConnection();
	}
	
	public String getUserSessionPasscode(int userId) throws Exception {
		
		AdminUser user = getAdminUserByUserId(userId);
		if (null == user) return Constants.NA;
		else return user.passcode;
		
	}
	
	public String updateAdminPasscode(AdminUser user) throws SQLException, NoSuchAlgorithmException {
		
		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("update ");
		sb.append(ColumnsNFields.ADMIN_USER_TABLE);
		sb.append(" set ");
		sb.append(ColumnsNFields.AdminUserColumn.PASSCODE.stringValue);
		sb.append("=?");
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_ID);
		sb.append("=?");
		
		String newPasscode = getPasscodeHash(user);
		
		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setString(1, newPasscode);
		preparedStatement.setInt(2, user.id);
		
		boolean success = preparedStatement.executeUpdate() == 1;
		
		if (success) return newPasscode;
		
		return null;
		
	}
	
	public boolean createSecondaryInfo(int userId) throws SQLException {
		
		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(ColumnsNFields.ADMIN_SECONDARY_TABLE);
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_USER_ID);
		sb.append("=?");
		
		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setInt(1, userId);
		resultSet = preparedStatement.executeQuery();
		
		if (null == resultSet || !resultSet.first()) {
			
			sb = new StringBuilder();
			sb.append("insert into ");
			sb.append(ColumnsNFields.ADMIN_SECONDARY_TABLE);
			sb.append(" (");
			sb.append(ColumnsNFields.COMMON_KEY_USER_ID);
			sb.append(")");
			sb.append(" values (?)");
			
			preparedStatement = connection.prepareStatement(sb.toString());
			preparedStatement.setInt(1, userId);
			
			return preparedStatement.executeUpdate() == 1;
			
		}
		
		return true;
		
	}
	
	public boolean addAdminLoginInfo(int userId, JSONObject requestObject, String ipAddress) throws SQLException {
		
		checkAndConnect();
		
		String query = "INSERT INTO " + ColumnsNFields.ADMIN_LOGIN_INFO_TABLE + 
				" (" +
				ColumnsNFields.AdminLoginInfoColumn.USER_ID.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.LOGIN_DATETIME.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.IP_ADDRESS.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.DEVICE_ID.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.DEVICE_TYPE.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.DEVICE_MODEL.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.APP_VERSION.value + "," +
				ColumnsNFields.AdminLoginInfoColumn.OS_VERSION.value +
				") VALUES(?,?,?,?,?,?,?,?)";
		
		preparedStatement = connection.prepareStatement(query);
	
		preparedStatement.setInt(1, userId);
		preparedStatement.setString(2, DateTimeUtils.getCurrentDateTimeInIST());
		preparedStatement.setString(3, ipAddress);
		preparedStatement.setString(4, requestObject.optString("deviceId", Constants.NA));
		preparedStatement.setString(5, requestObject.optString("deviceType", Constants.NA));
	
		String modelName = requestObject.optString("deviceModel", Constants.NA);
		if (modelName.length() > 128)
			modelName = modelName.substring(0, 128);
		
		preparedStatement.setString(6, modelName);
		preparedStatement.setString(7, requestObject.optString("appVersion", Constants.NA));
		preparedStatement.setString(8, requestObject.optString("osVersion", Constants.NA));
		
		int status = preparedStatement.executeUpdate();
		
		return status == 1;
		
	}
	
	public AdminUser getAdminUserByEmailId(String emailId) throws Exception {
		
		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(ColumnsNFields.ADMIN_USER_TABLE);
		sb.append(" where ");
		sb.append(ColumnsNFields.AdminUserColumn.EMAIL.stringValue);
		sb.append("=?");
		
		preparedStatement = connection.prepareStatement(sb.toString());
		
		preparedStatement.setString(1, emailId);
		
		resultSet = preparedStatement.executeQuery();
		
		if (null != resultSet && resultSet.first()) 
			return getAdminUserFromRS(resultSet);
		
		return null;
		
	}
	
	public AdminUser getAdminUserByUserId(int userId) throws Exception {
		
		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(ColumnsNFields.ADMIN_USER_TABLE);
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_ID);
		sb.append("=?");
		
		preparedStatement = connection.prepareStatement(sb.toString());
		
		preparedStatement.setInt(1, userId);
		
		resultSet = preparedStatement.executeQuery();
		
		if (null != resultSet && resultSet.first()) 
			return getAdminUserFromRS(resultSet);
		
		return null;
		
	}
	
	private AdminUser getAdminUserFromRS(ResultSet rs) throws SQLException {
		
		AdminUser user = new AdminUser();
		
		user.id = rs.getInt(ColumnsNFields.COMMON_KEY_ID);
		user.name = rs.getString(ColumnsNFields.AdminUserColumn.NAME.stringValue);
		user.email = rs.getString(ColumnsNFields.AdminUserColumn.EMAIL.stringValue);
		user.imageUrl = rs.getString(ColumnsNFields.AdminUserColumn.IMAGE_URL.stringValue);
		user.countryCode = rs.getString(ColumnsNFields.AdminUserColumn.COUNTRY_CODE.stringValue);
		user.mobileNumber = rs.getString(ColumnsNFields.AdminUserColumn.MOBILE_NUMBER.stringValue);
		user.sfUserId = rs.getString(ColumnsNFields.AdminUserColumn.SF_USER_ID.stringValue);
		user.passcode = rs.getString(ColumnsNFields.AdminUserColumn.PASSCODE.stringValue);
		user.password = rs.getString(ColumnsNFields.AdminUserColumn.PASSWORD.stringValue);
		user.registeredDatetime = rs.getString(ColumnsNFields.AdminUserColumn.REGISTRATION_DATETIME.stringValue);
		user.allowedNotification = rs.getInt(ColumnsNFields.AdminUserColumn.NOTIFICATOIN_ALLOWED.stringValue) == 1;
		user.role = rs.getInt(ColumnsNFields.AdminUserColumn.ROLE.stringValue);
		
		return user;
		
	}
	
	private String getPasscodeHash(AdminUser user) throws NoSuchAlgorithmException {
		Random random = new Random();
		double randomNumber = random.nextInt(99999);
		return (new BasicUtils()).getMD5Hash(user.mobileNumber + user.sfUserId + (randomNumber));
	}
	
	public AdminDashboard getDashboard(AnalyticsRange aRange) throws SQLException {
		
		checkAndConnect();
		
		String startDatetime = DateTimeUtils.getDateTime(aRange.dayCode, DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST) + " 00:00:01";
		String endDatetime = DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST) + " 23:59:59";
		
		StringBuilder sb = new StringBuilder();
		sb.append("select count(u.user_id) registeredUsers, li.activeUsers, pi.totalPaymentAmount, pi.totalPaymentCount, ri.totalReferrers, ri.totalReferrals");
		sb.append(" from HomeFirstCustomerPortal.user u ");
		sb.append(" left join (select count(distinct(user_id)) activeUsers FROM HomeFirstCustomerPortal.login_info  where login_datetime between '" + startDatetime + "' and '" + endDatetime + "') li on li.activeUsers"); 
		sb.append(" left join (select sum(amount) totalPaymentAmount, count(id) totalPaymentCount FROM HomeFirstCustomerPortal.payment_info where status_message = 'success' and user_id != 8832 and completion_datetime between '" + startDatetime + "' and '" + endDatetime + "') pi on pi.totalPaymentAmount");
		sb.append(" left join (select count(distinct(user_id)) totalReferrers, count(id) totalReferrals  FROM HomeFirstCustomerPortal.referrals  where status = 'success' and user_id != 8832 and datetime between '" + startDatetime + "' and '" + endDatetime + "') ri on ri.totalReferrers"); 
		sb.append(" where registeration_datetime between '" + startDatetime + "' and '" + endDatetime + "'");
		
		preparedStatement = connection.prepareStatement(sb.toString());	
		
		resultSet = preparedStatement.executeQuery();
		
		if (null != resultSet && resultSet.first()) {
			
			AdminDashboard dashboard = new AdminDashboard();
			
			dashboard.startDatetime = startDatetime;
			dashboard.endDatetime = endDatetime;
			dashboard.activeUser = resultSet.getInt("activeUsers");
			dashboard.registeredUser = resultSet.getInt("registeredUsers");
			dashboard.totalPaymentAmount = resultSet.getDouble("totalPaymentAmount");
			dashboard.totalPaymentCount = resultSet.getInt("totalPaymentCount");
			dashboard.totalLeads = resultSet.getInt("totalReferrals");
			dashboard.totalReferrers = resultSet.getInt("totalReferrers");
			
			return dashboard;
			
		}
		
		return null;
		
	}
	
	public JSONArray searchPayments(
			SearchType searchType, 
			String searchId, 
			double amount
	) throws SQLException {
		
		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("select * from " + ColumnsNFields.PAYMENT_INFO_TABLE + " where ");
		sb.append(searchType.key + "=? ");
		
		if (amount > 0)
			sb.append(" and " + ColumnsNFields.PaymentInfoColumn.AMOUNT.value + " =?");
		
		sb.append(" order by " + ColumnsNFields.PaymentInfoColumn.INITIAL_DATETIME.value + " desc");

		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setString(1, searchId);
		if (amount > 0) preparedStatement.setDouble(2, amount);

		resultSet = preparedStatement.executeQuery();
		
		JSONArray paymentArray = new JSONArray();

		if (null != resultSet && resultSet.first()) {
			do {
				paymentArray.put(PaymentUtils.getPaymentObjectFromResultSet(resultSet, true).toJson());
			} while (resultSet.next());
		}
		
		return paymentArray;

	}
	
	
	public AdminLog addAdminLog(AdminLog adminLog) throws SQLException {

		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + ColumnsNFields.ADMIN_LOG_TABLE + " (");
		sb.append(ColumnsNFields.COMMON_KEY_USER_ID + ",");
		sb.append(ColumnsNFields.AdminLogInfoColumn.RECORD_TYPE.value + ",");
		sb.append(ColumnsNFields.AdminLogInfoColumn.RECORD_ID.value + ",");
		sb.append(ColumnsNFields.AdminLogInfoColumn.ACTION.value + ",");
		sb.append(ColumnsNFields.AdminLogInfoColumn.STATUS.value + ",");
		sb.append(ColumnsNFields.AdminLogInfoColumn.DESCRIPTION.value + ",");
		sb.append(ColumnsNFields.AdminLogInfoColumn.DATETIME.value + ") ");
		sb.append(" VALUES (?,?,?,?,?,?,?)");

		preparedStatement = connection.prepareStatement(sb.toString(),new String[]{ColumnsNFields.COMMON_KEY_ID});

		preparedStatement.setInt(1, adminLog.userId);
		preparedStatement.setString(2, adminLog.recordType);
		preparedStatement.setInt(3, adminLog.recordId);
		preparedStatement.setString(4, adminLog.action);
		preparedStatement.setString(5, adminLog.status);
		preparedStatement.setString(6, adminLog.description);
		preparedStatement.setString(7, DateTimeUtils.getCurrentDateTimeInIST());

		boolean status = preparedStatement.executeUpdate() == 1;
		int id = 0;
		if (status) {
			LoggerUtils.log("Admin log has been added successfully");
			resultSet = preparedStatement.getGeneratedKeys();
			if (null != resultSet && resultSet.first()) 
				id = resultSet.getInt(1);
			adminLog.id= id;
			return adminLog;
		} else {
			LoggerUtils.log("Error: Failed to add admin log");
			return null;
		}		

	}

	
	public boolean updateAdminLog(AdminLog aLog) throws SQLException {
		
		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("update ");
		sb.append(ColumnsNFields.ADMIN_LOG_TABLE);
		sb.append(" set ");
		sb.append(ColumnsNFields.AdminLogInfoColumn.DATETIME.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.AdminLogInfoColumn.STATUS.value);
		sb.append("=?");
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_ID);
		sb.append("=?");
		sb.append(" and ");
		sb.append(ColumnsNFields.COMMON_KEY_USER_ID);
		sb.append("=?");

		
		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setString(1, DateTimeUtils.getCurrentDateTimeInIST());
		preparedStatement.setString(2, aLog.status);
		preparedStatement.setInt(3, aLog.id);
		preparedStatement.setInt(4, aLog.userId);

		return preparedStatement.executeUpdate() > 0;
		
	}

	public Payment getPaymentById(int id) throws SQLException {

		checkAndConnect();

		String query = "select * from payment_info where id = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, id);
		resultSet = preparedStatement.executeQuery();

		if (null == resultSet)
			return null;
		else {
			if (resultSet.first())
				return PaymentUtils.getPaymentObjectFromResultSet(resultSet, true);
			else
				return null;
		}
	}

	public Payment updateManunalPayment(Payment payment) throws SQLException, NoSuchAlgorithmException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("update ");
		sb.append(ColumnsNFields.PAYMENT_INFO_TABLE);
		sb.append(" set ");
		sb.append(ColumnsNFields.PaymentInfoColumn.COMPLETION_DATETIME.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.PaymentInfoColumn.PAYMENT_ID.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.PaymentInfoColumn.PAYMENT_METHOD.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.PaymentInfoColumn.STATUS_MESSAGE.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.PaymentInfoColumn.STATUS.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.PaymentInfoColumn.RECEIPT_DATA.value);
		sb.append("=?,");
		sb.append(ColumnsNFields.PaymentInfoColumn.RECEIPT_NUMBER.value);
		sb.append("=?");
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_ID);
		sb.append("=?");

		String currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setString(1, currentDateTime);
		preparedStatement.setString(2, payment.paymentId);
		preparedStatement.setString(3, payment.paymentMethod);
		preparedStatement.setString(4, payment.statusMessage);
		preparedStatement.setString(5, payment.status);
		preparedStatement.setString(6, payment.receiptData);
		preparedStatement.setString(7, payment.receiptNumber);
		preparedStatement.setInt(8, payment.id);

		int result = preparedStatement.executeUpdate();

		if (result == 1) {
			return getPaymentById(payment.id);
		} else {
			return null;
		}

	}
	
	public boolean getCronWhiteListIP(String ipAddress) throws SQLException {

		checkAndConnect();

		String query = "Select * from whitelisted_ip where ip_address = ? and is_active";

		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, ipAddress);

		resultSet = preparedStatement.executeQuery();

		if (null != resultSet && resultSet.first())
			return resultSet.getBoolean("is_active");

		return false;

	}
	
}
