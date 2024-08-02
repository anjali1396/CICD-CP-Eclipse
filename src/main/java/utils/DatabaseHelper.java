package utils;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dao.DataProvider;
import models.SecondaryInfo;
import models.User;
import models.payment.Payment;

public class DatabaseHelper {
	
	private Connection connection = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	
	public DatabaseHelper() {}
	
	private void checkAndConnect() throws SQLException {
		if (null == connection || !connection.isValid(10))
			connection = DataProvider.getDataSource().getConnection();
	}
	
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
	
	public boolean registerUser(User user, String deviceType, String deviceId) throws SQLException, NoSuchAlgorithmException {
		
		checkAndConnect();
		
		String query = "INSERT INTO " + ColumnsNFields.USER_TABLE + " ("
				+ ColumnsNFields.UserColumn.MOBILE_NUMBER.stringValue 
				+ "," + ColumnsNFields.UserColumn.LOAN_ACCOUNT_NUMBER.stringValue
				+ "," + ColumnsNFields.UserColumn.CRM_ACCOUNT_NUMBER.stringValue
				+ "," + ColumnsNFields.UserColumn.EMAIL_ID.stringValue
				+ "," + ColumnsNFields.UserColumn.NAME.stringValue 
				+ "," + ColumnsNFields.UserColumn.REGISTRATION_DATETIME.stringValue
				+ "," + ColumnsNFields.UserColumn.COUNTRY_CODE.stringValue
				+ "," + ColumnsNFields.UserColumn.SESSION_PASSCODE.stringValue +") VALUES (?,?,?,?,?,?,?,?)";
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, user.mobileNumber);
		preparedStatement.setString(2, user.loanAccountNumber);
		preparedStatement.setString(3, user.crmAccountNumber);
		preparedStatement.setString(4, user.emailId);
		preparedStatement.setString(5, user.name);
		preparedStatement.setString(6, DateTimeUtils.getCurrentDateTimeInGMT());
		preparedStatement.setString(7, user.countryCode);
		preparedStatement.setString(8, getPasscodeHash(user));
		
		int result = preparedStatement.executeUpdate();
		int secondayResult = 0;
		
		if (result == 1) {
		
			user = getUserByMobileNumber(user.mobileNumber);

			if (null != user) {
				query = "INSERT INTO secondary_info (user_id,device_type,device_id) VALUES (?,?,?)";
				preparedStatement = connection.prepareStatement(query);
				preparedStatement.setInt(1, user.userId);
				preparedStatement.setString(2, deviceType);
				preparedStatement.setString(3, deviceId);
				
				secondayResult = preparedStatement.executeUpdate();
				
				if (secondayResult == 0) {
					query = "DELETE FROM user WHERE user_id = ?";
					preparedStatement = connection.prepareStatement(query);
					preparedStatement.setInt(1, user.userId);
					preparedStatement.executeUpdate();
				} 
				
			}
			
		}
		
		return secondayResult == 1;
	}
	
	/**
	 * @deprecated use the v2/dbhelpers/DatabaseHelper.addLoginInfo() instead
	 */
	@Deprecated
	public boolean addLoginInfo(int userId, String deviceId, String deviceType) throws Exception {
		
		checkAndConnect();
		
		SecondaryInfo sInfo = getSecondaryInfoById(userId);
		
		if (null != sInfo) {
			
			JSONArray liArray = new JSONArray();
			
			if (null != sInfo.loginInfo && !sInfo.loginInfo.equals(Constants.NA)) {			
				liArray = new JSONArray(sInfo.loginInfo);
				liArray.put(getLoginInfoJson(deviceId, deviceType));				
			}  else {				
				liArray.put(getLoginInfoJson(deviceId, deviceType));				
			}
		
			String query = "update secondary_info set login_info = ? where user_id = ?";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, liArray.toString());
			preparedStatement.setInt(2, userId);			
			int status = preparedStatement.executeUpdate();
			
			return status == 1;
			
		}	
		
		return false;
	}
	
	public boolean addApnsToken(int userId, JSONObject data) throws SQLException {
		
		checkAndConnect();
		
		SecondaryInfo sInfo = getSecondaryInfoById(userId);
		
		if (null != sInfo) {
			
			String deviceType = data.optString(Constants.DEVICE_TYPE, Constants.NA); 
			
			if (!deviceType.equalsIgnoreCase(Constants.NA)) {
				
				JSONArray niArray = new JSONArray();				
					
				if (null != sInfo.apnsKey && !sInfo.apnsKey.equals(Constants.NA)) 			
					niArray = new JSONArray(sInfo.apnsKey);					
				
				String query = "update secondary_info set apns_key = ? where user_id = ?";
				
				boolean isDataAlreadyUpdated = false;
				
				if (niArray.length() > 0) {
					for (int i = 0; i < niArray.length(); i++) {
						JSONObject current = niArray.getJSONObject(i);
						if (current.getString("device_id").equalsIgnoreCase(data.getString("deviceId"))) {
							current.put("key", data.getString("notificationKey"));
							current.put("device_type", deviceType);
							isDataAlreadyUpdated = true;
							break;
						}
					}
				}
				
				if (!isDataAlreadyUpdated)
					niArray.put(getNotificationInfoJson(data));
				
				preparedStatement = connection.prepareStatement(query);
				preparedStatement.setString(1, niArray.toString());
				preparedStatement.setInt(2, userId);			
				int status = preparedStatement.executeUpdate();
				
				return status == 1;
				
			} else {
				return false;
			}
			
		}
		
		return false;
		
	}
	
	public boolean addNotificationToken(int userId, JSONObject data) throws SQLException {
		
		checkAndConnect();
		
		SecondaryInfo sInfo = getSecondaryInfoById(userId);
		
		if (null != sInfo) {
			
			String deviceType = data.optString(Constants.DEVICE_TYPE, Constants.NA); 
			
			if (!deviceType.equalsIgnoreCase(Constants.NA)) {
				
				JSONArray niArray = new JSONArray();				
					
				if (null != sInfo.fcmKey && !sInfo.fcmKey.equals(Constants.NA)) 			
					niArray = new JSONArray(sInfo.fcmKey);					
				
				String query = "update secondary_info set fcm_key = ? where user_id = ?";
				
				boolean isDataAlreadyUpdated = false;
				
				if (niArray.length() > 0) {
					for (int i = 0; i < niArray.length(); i++) {
						JSONObject current = niArray.getJSONObject(i);
						if (current.getString("device_id").equalsIgnoreCase(data.getString("deviceId"))) {
							current.put("key", data.getString("notificationKey"));
							current.put("device_type", deviceType);
							isDataAlreadyUpdated = true;
							break;
						}
					}
				}
				
				if (!isDataAlreadyUpdated)
					niArray.put(getNotificationInfoJson(data));
				
				preparedStatement = connection.prepareStatement(query);
				preparedStatement.setString(1, niArray.toString());
				preparedStatement.setInt(2, userId);			
				int status = preparedStatement.executeUpdate();
				
				return status == 1;
				
			} else {
				return false;
			}
			
		}
		
		return false;
		
	}
	
	private JSONObject getLoginInfoJson(String deciceId, String deciceType) throws JSONException {
		JSONObject nliJson = new JSONObject();
		nliJson.put("device_id", deciceId);
		nliJson.put("device_type", deciceType);
		nliJson.put("login_datetime", DateTimeUtils.getCurrentDateTimeInGMT());
		return nliJson;
	}
	
	private JSONObject getNotificationInfoJson(JSONObject nData) throws JSONException {
		JSONObject niJson = new JSONObject();
		niJson.put("device_type", nData.getString(Constants.DEVICE_TYPE));
		niJson.put("device_id", nData.getString("deviceId"));
		niJson.put("key", nData.getString("notificationKey"));
		return niJson;
	}
	
	@Deprecated
	public Payment initiatePayment(Payment payment) throws SQLException, NoSuchAlgorithmException {
		
		checkAndConnect();
		
		String query = "INSERT INTO payment_info (" + 
				"user_id," + 
				"loan_account_number," + 
				"loan_account_type," + 
				"order_id," + 
				"amount," + 
				"initial_datetime," +  
				"status," + 
				"status_message," +  
				"payment_nature," + 
				"emi_reduction," + 
				"tenure_reduction," + 
				"device_type," + 
				"payment_method)" +
				"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
		String currentDateTime = DateTimeUtils.getCurrentDateTimeInGMT();
		
		String transactionId = "TXN" + System.currentTimeMillis();
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, payment.userId);
		preparedStatement.setString(2, payment.loanAccountNumber);
		preparedStatement.setString(3, payment.loanType);
		preparedStatement.setString(4, transactionId);
		preparedStatement.setDouble(5, payment.paymentAmount);
		preparedStatement.setString(6, currentDateTime);
		preparedStatement.setString(7, "N");
		preparedStatement.setString(8, "Payment Initiated");
		preparedStatement.setString(9, payment.paymentNature);
		if (payment.paymentNature.equals(PaymentUtils.PaymentType.PART_PAYMENT.value)) {
			preparedStatement.setInt(10, (payment.prePaymentType.equals("EMI Reduction")) ? 1 : 0);
			preparedStatement.setInt(11, (payment.prePaymentType.equals("Tenor Reduction")) ? 1 : 0);		
		} else {
			preparedStatement.setInt(10, 0);
			preparedStatement.setInt(11, 0);
		}
		
		preparedStatement.setString(12, payment.deviceType);
		preparedStatement.setString(13, payment.paymentMethod);
		
		int result = preparedStatement.executeUpdate();
		
		if (result == 1) {
			return getPaymentByTransactionId(transactionId);
		} else {
			return null;
		}
	
	}
	
	public Payment finalizePayment(Payment payment) throws SQLException, NoSuchAlgorithmException {
		
		checkAndConnect();
		
		String query = "UPDATE payment_info SET " +   
				"status = ?," + 
				"status_message = ?," +
				"completion_datetime = ?," +
				"payment_id = ?," +
				"payment_method = ?," +
				"receipt_data = ? where id = ?";
		
		String currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, payment.status);
		preparedStatement.setString(2, payment.statusMessage);
		preparedStatement.setString(3, currentDateTime);
		preparedStatement.setString(4, payment.paymentId);
		preparedStatement.setString(5, payment.paymentMethod);
		preparedStatement.setString(6, payment.receiptData);
		preparedStatement.setInt(7, payment.id);
		
		int result = preparedStatement.executeUpdate();
		
		if (result == 1) {
			return getPaymentById(payment.id);
		} else {
			return null;
		}
	
	}
	
 
	
	public boolean updateFailedPayment(Payment payment) throws SQLException {
		
		checkAndConnect();
		
		String query = "UPDATE payment_info SET status_message = ?, payment_method = ? where id = ?";
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, payment.statusMessage);
		preparedStatement.setString(2, payment.paymentMethod);
		preparedStatement.setInt(3, payment.id);
		
		int result = preparedStatement.executeUpdate();
		return result == 1;
	
	}
	
	public boolean addReceiptIdInPayment(Payment payment) throws SQLException {
		
		checkAndConnect();
		
		String query = "UPDATE payment_info SET receipt_number=?, receipt_id=? where id = ?";
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, payment.receiptNumber);
		preparedStatement.setString(2, payment.receiptId);
		preparedStatement.setInt(3, payment.id);
		
		int result = preparedStatement.executeUpdate();
		
		return result == 1;
	
	}

	public Payment getPaymentByTransactionId(String transactionId) throws SQLException {
		
		checkAndConnect();
		
		String query = "select * from payment_info where order_id = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, transactionId);
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
	
	public ArrayList<Payment> getPaymentByLoanAccountNumbers(ArrayList<String> loanAccountNumbers) throws SQLException {
		
		checkAndConnect();
		
		StringBuilder query = new StringBuilder("select * from payment_info where loan_account_number in (");
		for (int i = 0; i < loanAccountNumbers.size(); i++) {
			query.append("?");
			if (i != loanAccountNumbers.size() - 1)
				query.append(",");
			else if (i == loanAccountNumbers.size() - 1)
				query.append(")");
		}
		
		query.append(" and status_message = 'success' order by id desc");
		
		preparedStatement = connection.prepareStatement(query.toString());
		
		for (int i = 0; i < loanAccountNumbers.size(); i++)
			preparedStatement.setString(i + 1, loanAccountNumbers.get(i));
		
		resultSet = preparedStatement.executeQuery();
		
		if (null != resultSet && resultSet.first()) {
			
			ArrayList<Payment> payments = new ArrayList<>();
			
			do {				
				payments.add(PaymentUtils.getPaymentObjectFromResultSet(resultSet, false));				
			} while (resultSet.next());
			
			return payments;
			
		} else return null;
		
	}
	
	public Payment getPaymentById(int id) throws SQLException {
		
		checkAndConnect();
		
		String query = "select * from payment_info where id = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, id);
		resultSet = preparedStatement.executeQuery();
		
		if (null != resultSet && resultSet.first()) 
			return PaymentUtils.getPaymentObjectFromResultSet(resultSet, true);
		else return null;		
		
	}
	
	private String getPasscodeHash(User user) throws NoSuchAlgorithmException {
		Random random = new Random();
		double randomNumber = random.nextInt(99999);
		return (new BasicUtils()).getMD5Hash(user.mobileNumber + user.crmAccountNumber + (randomNumber));
	}
	
	public User getUserById(int userId) throws SQLException {
		
		checkAndConnect();
		
		String query = "select * from user where user_id = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, userId);
		resultSet = preparedStatement.executeQuery();
		
		if (null == resultSet) 
			return null;
		else {
			if (resultSet.first())
				return ColumnsNFields.getUserObjectFromResultSet(resultSet);
			else 
				return null;
		}
		
	}
	
	public SecondaryInfo getSecondaryInfoById(int userId) throws SQLException {
		
		checkAndConnect();
		
		String query = "select * from secondary_info where user_id = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, userId);
		resultSet = preparedStatement.executeQuery();
		
		if (null == resultSet) 
			return null;
		else {
			if (resultSet.first())
				return getSecondaryInfoObjectFromResultSet(resultSet);
			else 
				return null;
		}
		
	}
	
	public User getUserByMobileNumber(String mobileNumber) throws SQLException {
		
		checkAndConnect();
		
		String query = "select * from user where mobile_number = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, mobileNumber);
		resultSet = preparedStatement.executeQuery();
		
		if (null == resultSet) 
			return null;
		else {
			if (resultSet.first())
				return ColumnsNFields.getUserObjectFromResultSet(resultSet);
			else 
				return null;
		}
		
	}
	
	public boolean updateMobileNumber(User user, String newMobileNumber, String countryCode) throws SQLException {
		
		checkAndConnect();
		
		String query = "UPDATE user SET mobile_number = ?, country_code = ? WHERE mobile_number = ?";

		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, newMobileNumber);
		preparedStatement.setString(2, countryCode);
		preparedStatement.setString(3, user.mobileNumber);
		int result = preparedStatement.executeUpdate();
		
		if (result == 1) {
			
			query = "update secondary_info set mobile_number_change_datetime = ? where user_id = ?";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, DateTimeUtils.getCurrentDateTimeInGMT());
			preparedStatement.setInt(2, user.userId);			
			preparedStatement.executeUpdate();
			
		}
		
		return result == 1;
		
	}
	
	
	public boolean addPassword(User user) throws Exception {
		
		checkAndConnect();
		
		String query = "update user set password = ? , session_passcode = ? where user_id = ?";

		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, BasicUtils.getTheKey(user.password));		
		preparedStatement.setString(2, getPasscodeHash(user));
		preparedStatement.setInt(3, user.userId);
		
		int result = preparedStatement.executeUpdate();
		
		if (result == 1) {
			query = "update secondary_info set password_change_datetime = ? where user_id = ?";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, DateTimeUtils.getCurrentDateTimeInGMT());
			preparedStatement.setInt(2, user.userId);			
			preparedStatement.executeUpdate();
		}
		
		return result == 1;
		
	}
	
	public boolean verifyMobileNumber(int userId) throws SQLException {

		checkAndConnect();
		
		String query = "update user set mobile_verified = 1 where user_id = ?";

		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, userId);
		int result = preparedStatement.executeUpdate();
		
		return result == 1;
		
	}
	
	public User getUserIfExist(String mobileNumber, String accountNumber) throws Exception {
		
		checkAndConnect();
		
		String query = "SELECT * FROM " + ColumnsNFields.USER_TABLE + " WHERE " 
				+ ColumnsNFields.UserColumn.MOBILE_NUMBER.stringValue + " = ? ";
				
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, mobileNumber);
		resultSet = preparedStatement.executeQuery();
		
		if (null == resultSet) {
			return null;
		} else {

			if (resultSet.first()) return ColumnsNFields.getUserObjectFromResultSet(resultSet);
			else return null;
					
		}
		
	}
	
	public boolean addOrUpdateUserProfilePicture(int userId, String imageUrl) throws SQLException {
		
		checkAndConnect();
		
		String query = "update user set image_url = ? where user_id = ?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, imageUrl);
		preparedStatement.setInt(2, userId);			
		int status = preparedStatement.executeUpdate();
		
		return status == 1;
		
	}
	
	public String getUserSessionPasscode(int userId) throws Exception {
		
		checkAndConnect();
		
		String query = "SELECT session_passcode FROM " + ColumnsNFields.USER_TABLE + " WHERE user_id = ?";
		
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setInt(1, userId);			
		resultSet = preparedStatement.executeQuery();
		
		if (null == resultSet) {
			return Constants.NA;
		} else {
			if (resultSet.first()) {
				return resultSet.getString(ColumnsNFields.UserColumn.SESSION_PASSCODE.stringValue);
			}
			else return Constants.NA;
					
		}
		
	}
	
	private SecondaryInfo getSecondaryInfoObjectFromResultSet(ResultSet resultSet) throws SQLException {
		
		SecondaryInfo info = new SecondaryInfo();
		
		info.id = resultSet.getInt(ColumnsNFields.SecondaryInfoColumn.ID.value);
		info.userId = resultSet.getInt(ColumnsNFields.SecondaryInfoColumn.USER_ID.value);
		info.loginInfo = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.LOGIN_INFO.value);
	    info.deviceType = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.DEVICE_TYPE.value);
	    info.deviceId = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.DEVICE_ID.value);
	    info.apnsKey = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.APNS_KEY.value);
	    info.fcmKey = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.FCM_KEY.value);
	    info.passwordChangeDatetime = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.PASSWORD_CHANGE_DATETIME.value);
	    info.mobileNumberChangeDatetime = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.MOBILE_NUMBER_CHANGE_DATETIME.value);
		
		return info;
		
	}
	
	public int addAppsData(int userId, JSONObject appJson) throws SQLException {

		checkAndConnect();

		String appName = appJson.optString("appName", Constants.NA);
		String packageName = appJson.optString("packageName", Constants.NA);
		String versionName = appJson.optString("versionName", Constants.NA);
		String versionCode = appJson.optString("versionCode", Constants.NA);

		String query = "INSERT INTO " + ColumnsNFields.INSTALLED_APPS_TABLE + " ("
				+ ColumnsNFields.InstalledAppsColumn.USER_ID.value + ","
				+ ColumnsNFields.InstalledAppsColumn.APP_NAME.value + ","
				+ ColumnsNFields.InstalledAppsColumn.PACKAGE_NAME.value + ","
				+ ColumnsNFields.InstalledAppsColumn.VERSION_NAME.value + ","
				+ ColumnsNFields.InstalledAppsColumn.VERSION_CODE.value + ","
				+ ColumnsNFields.InstalledAppsColumn.DATETIME.value + ","
				+ ColumnsNFields.InstalledAppsColumn.RAW_DATA.value + ") VALUES(?,?,?,?,?,?,?)";

		preparedStatement = connection.prepareStatement(query);

		preparedStatement.setInt(1, userId);
		preparedStatement.setString(2, appName);
		preparedStatement.setString(3, packageName);
		preparedStatement.setString(4, versionName);
		preparedStatement.setString(5, versionCode);
		preparedStatement.setString(6, DateTimeUtils.getCurrentDateTimeInIST());
		preparedStatement.setString(7, appJson.toString());

		return preparedStatement.executeUpdate();

	}

}
