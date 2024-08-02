package v2.dbhelpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;


import dao.DataProvider;
import models.User;
import models.notification.CPNotification;
import models.notification.RegistrationKey;
import models.notification.UserNotificationKey;
import utils.ColumnsNFields;
import utils.Constants;
import utils.DateTimeUtils;
import utils.NotificationUtils.NotificationFetchType;

public class NotificationDatabaseHelper {

	private Connection connection = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public NotificationDatabaseHelper() {
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

//	public ArrayList<RegistrationKey> getRegistrationKeyByUserId(int userId) throws SQLException {
//
//		checkAndConnect();
//
//		String query = "SELECT " + ColumnsNFields.SecondaryInfoColumn.FCM_KEY.value + " FROM "
//				+ ColumnsNFields.SECONDARY_INFO_TABLE + " WHERE " + ColumnsNFields.COMMON_KEY_USER_ID + "=?";
//		preparedStatement = connection.prepareStatement(query);
//		preparedStatement.setInt(1, userId);
//
//		resultSet = preparedStatement.executeQuery();
//
//		ArrayList<RegistrationKey> keys = new ArrayList<>();
//
//		if (null != resultSet && resultSet.first()) {
//
//			String tokenJsonString = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.FCM_KEY.value);
//
//			if (null != tokenJsonString) {
//				JSONArray tokenJsonArray = new JSONArray(tokenJsonString);
//				for (int i = 0; i < tokenJsonArray.length(); i++) {
//					keys.add(new RegistrationKey(userId, tokenJsonArray.getJSONObject(i)));
//				}
//			}
//
//		}
//
//		return keys;
//
//	}

//	public ArrayList<RegistrationKey> getAllRegistrationKeys() throws SQLException {
//
//		checkAndConnect();
//
//		String query = "SELECT " + ColumnsNFields.SecondaryInfoColumn.FCM_KEY.value + ","
//				+ ColumnsNFields.COMMON_KEY_USER_ID + " FROM " + ColumnsNFields.SECONDARY_INFO_TABLE;
//		preparedStatement = connection.prepareStatement(query);
//
//		resultSet = preparedStatement.executeQuery();
//
//		ArrayList<RegistrationKey> keys = new ArrayList<>();
//
//		if (null != resultSet && resultSet.first()) {
//
//			do {
//
//				int userId = resultSet.getInt(ColumnsNFields.COMMON_KEY_USER_ID);
//				String tokenJsonString = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.FCM_KEY.value);
//
//				if (null != tokenJsonString) {
//					JSONArray tokenJsonArray = new JSONArray(tokenJsonString);
//					for (int i = 0; i < tokenJsonArray.length(); i++) {
//						keys.add(new RegistrationKey(userId, tokenJsonArray.getJSONObject(i)));
//					}
//				}
//
//			} while (resultSet.next());
//
//		}
//
//		return keys;
//
//	}

//	public ArrayList<UserNotificationKey> getUserNotificationKeysByMobile(String mobileNumbers) throws SQLException {
//
//		checkAndConnect();
//
//		String query = "select u.user_id, u.name, u.mobile_number, si.fcm_key "
//				+ " from HomeFirstCustomerPortal.user u "
//				+ " left join (select user_id, fcm_key from HomeFirstCustomerPortal.secondary_info) si on si.user_id = u.user_id "
//				+ " where mobile_number in ('" + mobileNumbers + "')";
//
//		preparedStatement = connection.prepareStatement(query);
//		// preparedStatement.setString(1, mobileNumbers);
//
//		resultSet = preparedStatement.executeQuery();
//
//		ArrayList<UserNotificationKey> userNotificationKeys = new ArrayList<>();
//
//		if (null != resultSet && resultSet.first()) {
//			do {
//
//				UserNotificationKey unKey = new UserNotificationKey();
//
//				String fcmKeyArrayString = resultSet.getString(ColumnsNFields.SecondaryInfoColumn.FCM_KEY.value);
//				if (null != fcmKeyArrayString && fcmKeyArrayString.startsWith("[")) {
//
//					User user = new User();
//
//					user.userId = resultSet.getInt(ColumnsNFields.UserColumn.USER_ID.stringValue);
//					user.name = resultSet.getString(ColumnsNFields.UserColumn.NAME.stringValue);
//					user.mobileNumber = resultSet.getString(ColumnsNFields.UserColumn.MOBILE_NUMBER.stringValue);
//
//					unKey.user = user;
//
//					ArrayList<RegistrationKey> keys = new ArrayList<>();
//
//					JSONArray tokenJsonArray = new JSONArray(fcmKeyArrayString);
//					for (int i = 0; i < tokenJsonArray.length(); i++) {
//						keys.add(new RegistrationKey(user.userId, tokenJsonArray.getJSONObject(i)));
//					}
//
//					unKey.registerationKeys = keys;
//
//					userNotificationKeys.add(unKey);
//
//				}
//
//			} while (resultSet.next());
//		}
//
//		return userNotificationKeys;
//
//	}

//	public boolean updateNotification(CPNotification cpNotification, int totalCount, int successCount, int failedCount)
//			throws SQLException {
//
//		checkAndConnect();
//
//		StringBuilder sb = new StringBuilder();
//		sb.append("update ");
//		sb.append(ColumnsNFields.NOTIFICATION_TABLE);
//		sb.append(" set ");
//		sb.append(ColumnsNFields.NotificationColumn.IS_SCHEDULED.stringValue + "=?,");
//		sb.append(ColumnsNFields.NotificationColumn.TOTAL_COUNT.stringValue + "=?,");
//		sb.append(ColumnsNFields.NotificationColumn.SUCCESS_COUNT.stringValue + "=?,");
//		sb.append(ColumnsNFields.NotificationColumn.FAILURE_COUNT.stringValue + "=?,");
//		sb.append(ColumnsNFields.NotificationColumn.SENT_DATETIME.stringValue + "=?");
//		sb.append(" where ");
//		sb.append(ColumnsNFields.COMMON_KEY_ID + "=?");
//
//		preparedStatement = connection.prepareStatement(sb.toString());
//
//		preparedStatement.setInt(1, 1);
//		preparedStatement.setInt(2, totalCount);
//		preparedStatement.setInt(3, successCount);
//		preparedStatement.setInt(4, failedCount);
//		preparedStatement.setString(5, DateTimeUtils.getCurrentDateTimeInIST());
//		preparedStatement.setInt(6, cpNotification.id);
//
//		return preparedStatement.executeUpdate() == 1;
//
//	}

//	public boolean updateUserNotificationStatus(User user, JSONObject requestObject, boolean shouldIncludeDeviceId)
//			throws SQLException {
//
//		checkAndConnect();
//
//		String deviceId = requestObject.optString("deviceId", Constants.NA);
//
//		StringBuilder sb = new StringBuilder();
//		sb.append("update ");
//		sb.append(ColumnsNFields.USDR_NOTIFICATION_INFO_TABLE);
//		sb.append(" set ");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.HAS_READ.value + "=?,");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.READ_DATETIME.value + "=?,");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.DEVICE_TYPE.value + "=?,");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.DEVICE_MODEL.value + "=?,");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.APP_VERSION.value + "=?,");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.OS_VERSION.value + "=?");
//		sb.append(" where ");
//		sb.append(ColumnsNFields.COMMON_KEY_USER_ID + "=?");
//		sb.append(" and ");
//		sb.append(ColumnsNFields.UserNotificationInfoColumn.NOTIFICATION_ID.value + "=?");
//
//		if (shouldIncludeDeviceId) {
//			sb.append(" and ");
//			sb.append(ColumnsNFields.UserNotificationInfoColumn.DEVICE_ID.value + "=?");
//		}
//
//		preparedStatement = connection.prepareStatement(sb.toString());
//
//		preparedStatement.setInt(1, requestObject.optInt("hasRead", 0));
//		preparedStatement.setString(2, DateTimeUtils.getCurrentDateTimeInIST());
//		preparedStatement.setString(3, requestObject.optString("deviceType", Constants.NA));
//
//		String modelName = requestObject.optString("deviceModel", Constants.NA);
//		if (modelName.length() > 128)
//			modelName = modelName.substring(0, 128);
//		preparedStatement.setString(4, modelName);
//
//		preparedStatement.setString(5, requestObject.optString("appVersion", Constants.NA));
//		preparedStatement.setString(6, requestObject.optString("osVersion", Constants.NA));
//		preparedStatement.setInt(7, user.userId);
//		preparedStatement.setInt(8, requestObject.optInt("notificationId", -1));
//
//		if (shouldIncludeDeviceId)
//			preparedStatement.setString(9, deviceId);
//
//		return preparedStatement.executeUpdate() > 0;
//
//	}

	public ArrayList<CPNotification> getUserNotifications(User user, NotificationFetchType fetchType, String datetime,
			boolean getReadOnes) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();

		sb.append(
				"select n.`id`,  n.`is_valid`,  n.`title`,  n.`message`, n.`big_message`, n.`image_url`, n.`web_url`, ");
		sb.append(
				" n.`data`, n.`kind`, n.`on_click_action`, n.`screen_to_open`, n.`schedule_type`, n.`schedule_datetime` ,");
		sb.append(" un.user_id, un.has_read, un.dynamic_title, un.dynamic_message, un.read_datetime");
		sb.append(" FROM HomeFirstCustomerPortal.notification n");
		sb.append(" right join (");
		sb.append(
				"	select distinct(notification_id) notification_id, user_id, has_read, dynamic_title, dynamic_message, read_datetime ");
		sb.append("    FROM HomeFirstCustomerPortal.user_notification_info un");
		sb.append("	where user_id = ? and has_read = ? ");
		sb.append("    group by notification_id");
		sb.append(" ) un on un.notification_id = n.id");
		sb.append(" where n.schedule_datetime ");
		sb.append(fetchType == NotificationFetchType.TOP ? ">" : "<");
		sb.append(" ? and n.is_valid = true ");
		sb.append(" order by n.schedule_datetime desc limit 25");

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setInt(1, user.userId);
		preparedStatement.setBoolean(2, getReadOnes);
		preparedStatement.setString(3, datetime);


		ArrayList<CPNotification> notifications = new ArrayList<>();

		resultSet = preparedStatement.executeQuery();

		if (null != resultSet && resultSet.first()) {
			do {

				CPNotification notification = new CPNotification();

				notification.id = resultSet.getInt("id");
				notification.title = resultSet.getString("title");
				notification.message = resultSet.getString("message");
				notification.bigMessage = resultSet.getString("big_message");

				String dynamicMessage = resultSet.getString("dynamic_message");
				if (null != dynamicMessage) {
					notification.message = dynamicMessage;
					notification.bigMessage = dynamicMessage;
				}

				String dynamicTitle = resultSet.getString("dynamic_title");
				if (null != dynamicTitle)
					notification.title = dynamicTitle;

				notification.imageUrl = resultSet.getString("image_url");
				notification.datetime = resultSet.getString("schedule_datetime");
				notification.webUrl = resultSet.getString("web_url");
				notification.onClickAction = resultSet.getString("on_click_action");
				notification.screenToOpen = resultSet.getString("screen_to_open");

				String dataJson = resultSet.getString("data");
				if (null != dataJson) {

					notification.data = dataJson;
					//notification._data = dataJson;
				
				}


			
				notification.kind = resultSet.getString("kind");
				notification.hasRead = resultSet.getBoolean("has_read");

				notifications.add(notification);

			} while (resultSet.next());
		}

		return notifications;

	}

//	public int getUnreadNotificationCount(int userId) throws SQLException {
//
//		checkAndConnect();
//
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("SELECT distinct(un.notification_id), max(un.read_datetime) read_datetime, un.user_id, n.id");
//		sb.append(" FROM HomeFirstCustomerPortal.user_notification_info un");
//		sb.append(
//				" left join (SELECT id, is_valid FROM HomeFirstCustomerPortal.notification where is_scheduled = 1) n on n.id = un.notification_id");
//		sb.append(" where user_id = ? and n.is_valid = true group by notification_id");
//
//		preparedStatement = connection.prepareStatement(sb.toString());
//		preparedStatement.setInt(1, userId);
//
//		resultSet = preparedStatement.executeQuery();
//
//		int unreadCount = 0;
//
//		if (null != resultSet && resultSet.first()) {
//			do {
//
//				String readDatetime = resultSet.getString("read_datetime");
//				if (null == readDatetime)
//					unreadCount++;
//
//			} while (resultSet.next());
//		}
//
//		return unreadCount;
//
//	}

}

//SELECT distinct(un.notification_id), max(un.read_datetime) read_datetime, un.user_id, un.dynamic_message, n.title, n.message, n.big_message,
//n.image_url, n.web_url, n.data, n.audience_type, n.audience_group, n.priority, n.platform,
//n.kind, n.on_click_action,  n.screen_to_open, n.schedule_type, n.schedule_datetime
//FROM HomeFirstCustomerPortal.user_notification_info un
//left join (SELECT * FROM HomeFirstCustomerPortal.notification where is_scheduled = 1
//and schedule_datetime < '2020-09-25 20:30:00') n on n.id = un.notification_id
//where user_id = 8832
//and n.schedule_datetime < '2020-09-25 20:30:00' and n.is_valid = true group by notification_id order by n.schedule_datetime desc limit 25;
//
//
//
//sb.append(
//		" SELECT distinct(un.notification_id), max(un.read_datetime) read_datetime, un.user_id, un.dynamic_message, n.title, n.message, n.big_message,");
//sb.append(" n.image_url, n.web_url, n.data, n.audience_type, n.audience_group, n.priority, n.platform,");
//sb.append(" n.kind, n.on_click_action,  n.screen_to_open, n.schedule_type, n.schedule_datetime");
//sb.append(" FROM HomeFirstCustomerPortal.user_notification_info un");
//sb.append(" left join (SELECT * FROM HomeFirstCustomerPortal.notification where is_scheduled = 1");
//sb.append(" and schedule_datetime ");
//sb.append(fetchType == NotificationFetchType.TOP ? ">" : "<");
//sb.append(" ?) n on n.id = un.notification_id");
//
//sb.append(" where user_id = ?");
//sb.append(" and n.schedule_datetime ");
//sb.append(fetchType == NotificationFetchType.TOP ? ">" : "<");
//sb.append(" ? and n.is_valid = true group by notification_id order by n.schedule_datetime desc limit 25");