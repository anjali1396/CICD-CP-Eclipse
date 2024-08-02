package v2.dbhelpers;

import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import org.json.JSONObject;

import dao.DataProvider;
import models.User;
import models.UserCommunication;
import models.UserRequest;
import models.payment.Payment;
import utils.BasicUtils;
import utils.ColumnsNFields;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LoggerUtils;
import utils.PaymentUtils;
import v2.managers.RequestManager.RequestType;
import v2.managers.UserComHelper.CommunicationType;

public class DatabaseHelper {

	private Connection connection = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public DatabaseHelper() {
	}

	public void close() {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException se) {
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
		if (connection != null) {
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

	public String getPasscodeHash(User user) throws NoSuchAlgorithmException {
		Random random = new Random();
		double randomNumber = random.nextInt(99999);
		return (new BasicUtils()).getMD5Hash(user.mobileNumber + (randomNumber));
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
			} else
				return Constants.NA;

		}

	}

	
	public boolean addLoginInfo(int userId, JSONObject requestObject, String ipAddress)
			throws SQLException, UnknownHostException {

		checkAndConnect();

		String query = "INSERT INTO " + ColumnsNFields.LOGIN_INFO_TABLE + " ("
				+ ColumnsNFields.LoginInfoColumn.USER_ID.value + ","
				+ ColumnsNFields.LoginInfoColumn.LOGIN_DATETIME.value + ","
				+ ColumnsNFields.LoginInfoColumn.IP_ADDRESS.value + "," + ColumnsNFields.LoginInfoColumn.DEVICE_ID.value
				+ "," + ColumnsNFields.LoginInfoColumn.DEVICE_TYPE.value + ","
				+ ColumnsNFields.LoginInfoColumn.DEVICE_MODEL.value + ","
				+ ColumnsNFields.LoginInfoColumn.APP_VERSION.value + ","
				+ ColumnsNFields.LoginInfoColumn.OS_VERSION.value + ") VALUES(?,?,?,?,?,?,?,?)";

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

//	public User getUserByMobileNumber(String mobileNumber) throws SQLException {
//
//		utils.DatabaseHelper v1DbHelper = new utils.DatabaseHelper();
//		try {
//			User user = v1DbHelper.getUserByMobileNumber(mobileNumber);
//			v1DbHelper.close();
//			return user;
//		} catch (SQLException e) {
//			v1DbHelper.close();
//			throw e;
//		}
//
//	}

//	public User getUserById(int userId) throws SQLException {
//
//		utils.DatabaseHelper dbHelper = new utils.DatabaseHelper();
//
//		try {
//			User user = dbHelper.getUserById(userId);
//			dbHelper.close();
//			return user;
//
//		} catch (SQLException e) {
//			dbHelper.close();
//			throw e;
//		}
//	}

	public Payment initiatePayment(Payment payment) throws SQLException, NoSuchAlgorithmException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + ColumnsNFields.PAYMENT_INFO_TABLE + " (");
		sb.append(ColumnsNFields.COMMON_KEY_USER_ID + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.LOAN_ACCOUNT_NUMBER.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.LOAN_TYPE.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.ORDER_ID.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.AMOUNT.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.INITIAL_DATETIME.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.STATUS.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.STATUS_MESSAGE.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.PAYMENT_NATURE.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.EMI_REDUCTION.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.TENURE_REDUCTION.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.DEVICE_TYPE.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.PAYMENT_METHOD.value + ",");
		sb.append(ColumnsNFields.PaymentInfoColumn.PAYMENT_SUB_TYPE.value + ") ");
		sb.append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setInt(1, payment.userId);
		preparedStatement.setString(2, payment.loanAccountNumber);
		preparedStatement.setString(3, payment.loanType);
		preparedStatement.setString(4, payment.orderId);
		preparedStatement.setDouble(5, payment.paymentAmount);
		preparedStatement.setString(6, payment.initialDateTime);
		preparedStatement.setString(7, "N");
		preparedStatement.setString(8, "Payment Initiated");
		preparedStatement.setString(9, payment.paymentNature);

		if (payment.paymentNature.equals(PaymentUtils.PaymentType.PART_PAYMENT.value)
				|| payment.paymentNature.equals(PaymentUtils.PaymentType.AUTO_PREPAY.value)) {
			preparedStatement.setInt(10, (payment.prePaymentType.equals("EMI Reduction")) ? 1 : 0);
			preparedStatement.setInt(11, (payment.prePaymentType.equals("Tenor Reduction")) ? 1 : 0);
		} else {
			preparedStatement.setInt(10, 0);
			preparedStatement.setInt(11, 0);
		}

		preparedStatement.setString(12, payment.deviceType);
		preparedStatement.setString(13, payment.paymentMethod);
		preparedStatement.setString(14, payment.paymentSubType);

		int result = preparedStatement.executeUpdate();

		if (result == 1) {
			utils.DatabaseHelper v1DbHelper = new utils.DatabaseHelper();
			Payment storedPayment = v1DbHelper.getPaymentByTransactionId(payment.orderId);
			v1DbHelper.close();
			return storedPayment;
		} else {
			return null;
		}

	}

	public boolean addRazorpayOrderId(Payment payment) throws SQLException {

		checkAndConnect();

		String query = "UPDATE " + ColumnsNFields.PAYMENT_INFO_TABLE + " SET "
				+ ColumnsNFields.PaymentInfoColumn.PG_ORDER_ID.value + "=? " + ","
				+ ColumnsNFields.PaymentInfoColumn.PAYMENT_METHOD.value + "=? " + " WHERE "
				+ ColumnsNFields.PaymentInfoColumn.ID.value + "=?";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, payment.pGatewayOrderId);
		preparedStatement.setString(2, payment.paymentMethod);
		preparedStatement.setInt(3, payment.id);

		return preparedStatement.executeUpdate() == 1;

	}

	// ================= ADD / UPDATE REFERRALS INFO METHODS
	// =========================== //


//	public int insertReferralInfo(int userId, Lead lead) throws SQLException {
//
//		checkAndConnect();
//
//		String query = "INSERT INTO " + ColumnsNFields.REFER_TABLE + " (" + ColumnsNFields.ReferralColumn.USER_ID.value
//				+ "," + ColumnsNFields.ReferralColumn.MOBILE_NUMBER.value + ","
//				+ ColumnsNFields.ReferralColumn.EMAIL_ID.value + "," + ColumnsNFields.ReferralColumn.SALUTATION.value
//				+ "," + ColumnsNFields.ReferralColumn.FIRST_NAME.value + ","
//				+ ColumnsNFields.ReferralColumn.MIDDLE_NAME.value + "," + ColumnsNFields.ReferralColumn.LAST_NAME.value
//				+ "," + ColumnsNFields.ReferralColumn.ADDRESS.value + "," + ColumnsNFields.ReferralColumn.STATUS.value
//				+ "," + ColumnsNFields.ReferralColumn.DATETIME.value + ") VALUES(?,?,?,?,?,?,?,?,?,?)";
//
//		preparedStatement = connection.prepareStatement(query, new String[] { ColumnsNFields.COMMON_KEY_ID });
//
//		preparedStatement.setInt(1, userId);
//		preparedStatement.setString(2, lead.mobileNumber);
//		preparedStatement.setString(3, lead.email);
//		preparedStatement.setString(4, lead.salutation);
//		preparedStatement.setString(5, lead.firstName);
//		preparedStatement.setString(6, lead.middleName);
//		preparedStatement.setString(7, lead.lastName);
//		preparedStatement.setString(8, lead.address.toJson().toString());
//		preparedStatement.setString(9, "INITIATED");
//		preparedStatement.setString(10, DateTimeUtils.getCurrentDateTimeInIST());
//
//		boolean isSuccess = preparedStatement.executeUpdate() == 1;
//		if (isSuccess) {
//
//			resultSet = preparedStatement.getGeneratedKeys();
//			if (null != resultSet && resultSet.first())
//				return resultSet.getInt(1);
//			else
//				return -1;
//
//		} else
//			return -1;
//
//	}
//
//	public boolean updateReferralInfo(Lead lead) throws SQLException {
//
//		checkAndConnect();
//
//		String query = "UPDATE " + ColumnsNFields.REFER_TABLE + " SET " + ColumnsNFields.ReferralColumn.SF_LEAD_ID.value
//				+ "=?," + ColumnsNFields.ReferralColumn.STATUS.value + "=?" + " WHERE " + ColumnsNFields.COMMON_KEY_ID
//				+ "=?";
//
//		preparedStatement = connection.prepareStatement(query);
//		preparedStatement.setString(1, lead.sfLeadId);
//		preparedStatement.setString(2, lead.status);
//		preparedStatement.setInt(3, lead.id);
//
//		return preparedStatement.executeUpdate() == 1;
//
//	}
//
//	public boolean updateReferralInfo(int userId, Lead lead) throws SQLException {
//
//		checkAndConnect();
//
//		String query = "UPDATE " + ColumnsNFields.REFER_TABLE + " SET " + ColumnsNFields.COMMON_KEY_USER_ID + "=?,"
//				+ ColumnsNFields.ReferralColumn.SF_LEAD_ID.value + "=?," + ColumnsNFields.ReferralColumn.STATUS.value
//				+ "=?" + " WHERE " + ColumnsNFields.COMMON_KEY_ID + "=?";
//
//		preparedStatement = connection.prepareStatement(query);
//		preparedStatement.setInt(1, userId);
//		preparedStatement.setString(2, lead.sfLeadId);
//		preparedStatement.setString(3, lead.status);
//		preparedStatement.setInt(4, lead.id);
//
//		return preparedStatement.executeUpdate() == 1;
//
//	}

	// ****************** END OF ADD / UPDATE REFERRALS INFO METHODS
	// ************************* //

	public int addContactData(int userId, String mobile, String email, String name, JSONObject raw_data)
			throws SQLException {

		checkAndConnect();

		String query = "INSERT INTO " + ColumnsNFields.CF_DATA + " (" + ColumnsNFields.CfColumn.USER_ID.stringValue
				+ "," + ColumnsNFields.CfColumn.MOBILE_NUMBER.stringValue + ","
				+ ColumnsNFields.CfColumn.EMAIL_ID.stringValue + "," + ColumnsNFields.CfColumn.NAME.stringValue + ","
				+ ColumnsNFields.CfColumn.REGISTRATION_DATETIME.stringValue + ","
				+ ColumnsNFields.CfColumn.RAW_DATA.stringValue + ") VALUES(?,?,?,?,?,?)";

		preparedStatement = connection.prepareStatement(query);

		preparedStatement.setInt(1, userId);
		preparedStatement.setString(2, mobile);
		preparedStatement.setString(3, email);
		preparedStatement.setString(4, name);
		preparedStatement.setString(5, DateTimeUtils.getCurrentDateTimeInIST());
		preparedStatement.setString(6, raw_data.toString());

		return preparedStatement.executeUpdate();

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

	public String getUserSessionPasscode(User user) throws SQLException, NoSuchAlgorithmException {

		checkAndConnect();

		String query = " UPDATE " + ColumnsNFields.USER_TABLE + " SET "
				+ ColumnsNFields.UserColumn.SESSION_PASSCODE.stringValue + " =?" + " WHERE user_id = ?";

		preparedStatement = connection.prepareStatement(query);

		String newPasscode = getPasscodeHash(user);
		preparedStatement.setString(1, newPasscode);
		preparedStatement.setInt(2, user.userId);

		if (preparedStatement.executeUpdate() > 0)
			return newPasscode;

		return null;

	}

	public boolean verifyMobileNumber(User user) throws SQLException, NoSuchAlgorithmException {

		checkAndConnect();

		String query = " Update user set mobile_verified = 1 " + " Where "
				+ ColumnsNFields.UserColumn.USER_ID.stringValue + " =?";

		preparedStatement = connection.prepareStatement(query);

		preparedStatement.setInt(1, user.userId);

		boolean status = preparedStatement.executeUpdate() == 1;

		if (status)
			LoggerUtils.log("Verify OTP successfully");
		else
			LoggerUtils.log("Failed to verify OTP");

		return status;

	}

	public boolean addUserRequest(UserRequest userRequest, String ipAddress) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + ColumnsNFields.USER_REQUEST + " (");
		sb.append(ColumnsNFields.UserRequestColumn.ID.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.USER_ID.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.TOKEN.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.IS_VALID.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.TYPE.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.DESCRIPTION.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.STATUS.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.CREATED_DATETIME.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.VALID_DATETIME.value + ",");
		sb.append(ColumnsNFields.UserRequestColumn.IP_ADDRESS.value + ") ");
		sb.append(" VALUES (?,?,?,?,?,?,?,?,?,?)");

		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setString(1, userRequest.id);
		preparedStatement.setInt(2, userRequest.userId);
		preparedStatement.setString(3, userRequest.token);
		preparedStatement.setBoolean(4, userRequest.isValid);
		preparedStatement.setString(5, userRequest.type);
		preparedStatement.setString(6, userRequest.description);
		preparedStatement.setString(7, userRequest.status);
		preparedStatement.setString(8, userRequest.createdDateTime);
		preparedStatement.setString(9, userRequest.validDatetime);
		preparedStatement.setString(10, ipAddress);

		boolean status = preparedStatement.executeUpdate() == 1;

		if (status) {
			LoggerUtils.log("Request has been inserted successfully");
		} else {
			LoggerUtils.log("Failed to request in DB");

		}
		return status;

	}

	public UserRequest getUserRequestById(String requestId, int userId, RequestType requestType) throws Exception {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(ColumnsNFields.USER_REQUEST);
		sb.append(" where ");
		sb.append(ColumnsNFields.UserRequestColumn.ID.value);
		sb.append("=?");
		sb.append(" and ");
		sb.append(ColumnsNFields.UserRequestColumn.USER_ID.value);
		sb.append("=?");
		sb.append(" and ");
		sb.append(ColumnsNFields.UserRequestColumn.TYPE.value);
		sb.append("=?");

		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setString(1, requestId);
		preparedStatement.setInt(2, userId);
		preparedStatement.setString(3, requestType.value);

		resultSet = preparedStatement.executeQuery();

		if (null != resultSet && resultSet.first())
			return getRequestDetailFromRS(resultSet);

		return null;

	}

	public boolean updateUserRequest(UserRequest userRequest, String ipAddress) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();

		sb.append(" UPDATE " + ColumnsNFields.USER_REQUEST + " SET ");
		sb.append(ColumnsNFields.UserRequestColumn.STATUS.value);
		sb.append(" =?,");
		sb.append(ColumnsNFields.UserRequestColumn.IS_VALID.value);
		sb.append(" =?,");
		sb.append(ColumnsNFields.UserRequestColumn.TOKEN.value);
		sb.append(" =?,");
		sb.append(ColumnsNFields.UserRequestColumn.UPDATED_DATETIME.value);
		sb.append(" =?,");
		sb.append(ColumnsNFields.UserRequestColumn.IP_ADDRESS.value);
		sb.append(" =?,");
		sb.append(" otp_transaction_id");
		sb.append(" =?,");
		sb.append(" otp_token");
		sb.append(" =?");
		sb.append(" WHERE ");
		sb.append(ColumnsNFields.UserRequestColumn.ID.value);
		sb.append(" =?");
		sb.append(" and ");
		sb.append(ColumnsNFields.COMMON_KEY_USER_ID);
		sb.append(" =?");

		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setString(1, userRequest.status);
		preparedStatement.setBoolean(2, userRequest.isValid);
		preparedStatement.setString(3, userRequest.token);
		preparedStatement.setString(4, userRequest.updatedDateTime);
		preparedStatement.setString(5, ipAddress);
		
		preparedStatement.setString(6, userRequest.otpTransactionId);		
		preparedStatement.setString(7, userRequest.otpToken);
		
		preparedStatement.setString(8, userRequest.id);
		preparedStatement.setInt(9, userRequest.userId);

		return preparedStatement.executeUpdate() > 0;

	}

	private UserRequest getRequestDetailFromRS(ResultSet resultSet) throws SQLException {

		UserRequest userRequest = new UserRequest();

		userRequest.userId = resultSet.getInt(ColumnsNFields.UserRequestColumn.USER_ID.value);
		userRequest.id = resultSet.getString(ColumnsNFields.UserRequestColumn.ID.value);
		userRequest.token = resultSet.getString(ColumnsNFields.UserRequestColumn.TOKEN.value);
		userRequest.isValid = resultSet.getBoolean(ColumnsNFields.UserRequestColumn.IS_VALID.value);
		userRequest.type = resultSet.getString(ColumnsNFields.UserRequestColumn.TYPE.value);
		userRequest.description = resultSet.getString(ColumnsNFields.UserRequestColumn.DESCRIPTION.value);
		userRequest.status = resultSet.getString(ColumnsNFields.UserRequestColumn.STATUS.value);
		userRequest.createdDateTime = resultSet.getString(ColumnsNFields.UserRequestColumn.CREATED_DATETIME.value);
		userRequest.updatedDateTime = resultSet.getString(ColumnsNFields.UserRequestColumn.UPDATED_DATETIME.value);
		userRequest.validDatetime = resultSet.getString(ColumnsNFields.UserRequestColumn.VALID_DATETIME.value);
		
		userRequest.otpToken = resultSet.getString("otp_token");
		userRequest.otpTransactionId = resultSet.getString("otp_transaction_id");

		return userRequest;
	}

	public boolean inserUserCommunication(UserCommunication userComm) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();

		// check if the record already exists
		sb.append("select * from " + ColumnsNFields.USER_COMMUNICATION + " where ");
		sb.append(ColumnsNFields.UserCommunicationColumn.RECORD_TYPE.stringValue + "=? and ");
		sb.append(ColumnsNFields.UserCommunicationColumn.RECORD_ID.stringValue + "=?");

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setString(1, userComm.recordType);
		preparedStatement.setInt(2, userComm.recordId);

		resultSet = preparedStatement.executeQuery();

		if (null != resultSet && resultSet.first()) {
			LoggerUtils.log("User communication record already exists in DB.");
			return false;
		}

		// record doesn't exists, create new

		sb = new StringBuilder();

		sb.append("INSERT INTO " + ColumnsNFields.USER_COMMUNICATION + " (");
		sb.append(ColumnsNFields.COMMON_KEY_USER_ID + ",");
		sb.append(ColumnsNFields.UserCommunicationColumn.RECORD_TYPE.stringValue + ",");
		sb.append(ColumnsNFields.UserCommunicationColumn.RECORD_ID.stringValue + ",");
		sb.append(ColumnsNFields.UserCommunicationColumn.CREATE_DATETIME.stringValue + ") ");
		sb.append("VALUES (?,?,?,?)");

		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setInt(1, userComm.userId);
		preparedStatement.setString(2, userComm.recordType);
		preparedStatement.setInt(3, userComm.recordId);
		preparedStatement.setString(4, userComm.createDatetime);

		boolean isSuccess = preparedStatement.executeUpdate() == 1;

		if (isSuccess)
			LoggerUtils.log("User communication entry inserted successfully in DB.");
		else
			LoggerUtils.log("Failed to insert User communication entry in DB");

		return isSuccess;

	}

	public boolean updateUserCommunication(UserCommunication userComm, CommunicationType cType) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();

		sb.append(" UPDATE " + ColumnsNFields.USER_COMMUNICATION + " SET ");

		if (cType == CommunicationType.EMAIL) {
			sb.append(ColumnsNFields.UserCommunicationColumn.IS_EMAIL.stringValue);
			sb.append("=?,");
			sb.append(ColumnsNFields.UserCommunicationColumn.EMAIL_DATETIME.stringValue);
			sb.append("=?");
		} else if (cType == CommunicationType.SMS) {
			sb.append(ColumnsNFields.UserCommunicationColumn.IS_SMS.stringValue);
			sb.append("=?,");
			sb.append(ColumnsNFields.UserCommunicationColumn.HFO_ID.stringValue);
			sb.append("=?,");
			sb.append(ColumnsNFields.UserCommunicationColumn.SMS_DATETIME.stringValue);
			sb.append("=?");
		} else if (cType == CommunicationType.NOTIFICATION) {
			sb.append(ColumnsNFields.UserCommunicationColumn.IS_NOTIFICATION.stringValue);
			sb.append("=?,");
			sb.append(ColumnsNFields.UserCommunicationColumn.NOTIFICATION_DATETIME.stringValue);
			sb.append("=?");
		} else {
			LoggerUtils.log("Invalid user communication type: " + cType.value);
			return false;
		}

		sb.append(" WHERE ");
		sb.append(ColumnsNFields.UserCommunicationColumn.RECORD_TYPE.stringValue);
		sb.append("=?");
		sb.append(" and ");
		sb.append(ColumnsNFields.UserCommunicationColumn.RECORD_ID.stringValue);
		sb.append("=?");

		preparedStatement = connection.prepareStatement(sb.toString());

		if (cType == CommunicationType.EMAIL) {
			preparedStatement.setBoolean(1, userComm.isEmail);
			preparedStatement.setString(2, userComm.emailDatetime);
			preparedStatement.setString(3, userComm.recordType);
			preparedStatement.setInt(4, userComm.recordId);
		} else if (cType == CommunicationType.SMS) {
			preparedStatement.setBoolean(1, userComm.isSMS);
			preparedStatement.setString(2, userComm.hfoId);
			preparedStatement.setString(3, userComm.smsDatetime);
			preparedStatement.setString(4, userComm.recordType);
			preparedStatement.setInt(5, userComm.recordId);
		} else if (cType == CommunicationType.NOTIFICATION) {
			preparedStatement.setBoolean(1, userComm.isNotification);
			preparedStatement.setString(2, userComm.notificationDatetime);
			preparedStatement.setString(3, userComm.recordType);
			preparedStatement.setInt(4, userComm.recordId);
		}

		

		boolean status = preparedStatement.executeUpdate() == 1;

		if (status)
			LoggerUtils.log("User communication updated successfully.");
		else
			LoggerUtils.log("Failed to update user communication in db");

		return status;

	}

	public static int getColumnCount(ResultSet res) throws SQLException {
		return res.getMetaData().getColumnCount();
	}

	public void getDetailsToCSVFile(String filePath) throws SQLException, IOException {

		checkAndConnect();

		FileWriter fw;

		String query = "SELECT `user`.`user_id`,`user`.`name`,`user`.`email_id`,`user`.`mobile_number`,`user`.`registeration_datetime`,`user`.`mobile_verified`,`user`.`loan_account_number`,`user`.`crm_account_number` FROM `HomeFirstCustomerPortal`.`user` where `password` != 'NA' and `password` != 'RESET' order by user_id desc";
		preparedStatement = connection.prepareStatement(query);

		resultSet = preparedStatement.executeQuery();

		int columnCount = getColumnCount(resultSet);

		try {

			fw = new FileWriter(filePath);

			for (int i = 1; i <= columnCount; i++) {
				fw.append(resultSet.getMetaData().getColumnName(i));
				fw.append(",");

			}

			fw.append(System.getProperty("line.separator"));

			while (resultSet.next()) {
				for (int i = 1; i <= columnCount; i++) {
					String data = "null";
					if (null != resultSet.getObject(i)) {
						data = resultSet.getObject(i).toString();
					}
					fw.append(data);
					fw.append(",");

				}
				fw.append(System.getProperty("line.separator"));
			}

			fw.flush();
			fw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	public boolean updateUserIsRegistered(User user) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();

		sb.append(" UPDATE " + ColumnsNFields.USER_TABLE + " SET ");
		sb.append(ColumnsNFields.UserColumn.IS_REGISTRATION_MARKED_ON_SF.stringValue);
		sb.append("= true");

		sb.append(" WHERE ");
		sb.append(ColumnsNFields.UserColumn.USER_ID.stringValue);
		sb.append("=?");

		preparedStatement = connection.prepareStatement(sb.toString());

		preparedStatement.setInt(1, user.userId);

		LoggerUtils.log(sb.toString());

		boolean status = preparedStatement.executeUpdate() == 1;

		if (status)
			LoggerUtils.log("User isRegistered updated successfully.");
		else
			LoggerUtils.log("Failed to update user isRegistered in db");

		return status;

	}

}
