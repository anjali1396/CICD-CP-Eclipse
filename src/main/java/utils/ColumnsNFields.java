package utils;

import java.sql.ResultSet;
import java.sql.SQLException;

import models.User;

public class ColumnsNFields {
	
	public static String getTableName(String tableName) {
		return "HomeFirstCustomerPortal." + tableName;
	}
	
	public static final String USER_TABLE = "user";
	public static final String SECONDARY_INFO_TABLE = "secondary_info";
	public static final String LOGIN_INFO_TABLE = "login_info";
	public static final String PAYMENT_INFO_TABLE = "payment_info";
	public static final String COMMON_KEY_USER_ID = "user_id";
	public static final String COMMON_KEY_ID = "id";
	public static final String COMMON_KEY_CREATE_DATETIME = "create_datetime";	
	public static final String REFER_TABLE = "referrals";
	public static final String CF_DATA = "contact_info";
	
	public static final String ADMIN_USER_TABLE = "admin_user";
	public static final String ADMIN_SECONDARY_TABLE = "admin_secondary_info";
	public static final String ADMIN_LOGIN_INFO_TABLE = "admin_login_info";
	
	public static final String NOTIFICATION_TABLE = "notification";
	public static final String USDR_NOTIFICATION_INFO_TABLE = "user_notification_info";
	public static final String USER_REQUEST = "user_request";
	public static final String ADMIN_LOG_TABLE = "admin_log";
	public static final String USER_COMMUNICATION = "user_communication";
	
	public static final String INSTALLED_APPS_TABLE = "installed_apps_info";
	
	public static final String TABLE_PARTNER = "Partner";
	public static final String TABLE_PROSPECT = "Prospect";
	public static final String COMMON_KEY_ORG_ID = "orgId";
	public static final String COMMON_KEY_SESSION_PASSCODE = "sessionPasscode";
	public static final String COMMON_KEY_SESSION_VALID_DATETIME = "sessionValidDatetime";
	public static final String COMMON_KEY_SESSION_UPDATE_DATETIME = "sessionUpdateDatetime";		
	public static final String COMMON_KEY_IS_ENABLED = "isEnabled";
	public static final String COMMON_KEY_IP_ADDRESS = "ipAddress";
	public static final String COMMON_KEY_UPDATE_DATETIME = "updateDatetime";
	public static final String COMMON_KEY_CREATEDATETIME = "createDatetime";	
	
	public static final String COMMON_KEY_IS_ACTIVE = "is_active";
	public static final String TABLE_WHITELISTED_IP = "whitelisted_ip";
	public static final String REQUEST_OUTSTANDING = "RequestOutstanding";


	public enum UserColumn {
		USER_ID("user_id"),
		NAME("name"),
		PASSWORD("password"),
		EMAIL_ID("email_id"),
		IMAGE_URL("image_url"),
		MOBILE_NUMBER("mobile_number"),
		COUNTRY_CODE("country_code"),
		REGISTRATION_DATETIME("registeration_datetime"),
		MOBILE_VERIFIED("mobile_verified"),
		CRM_ACCOUNT_NUMBER("crm_account_number"),
		SESSION_PASSCODE("session_passcode"),
		IS_REGISTRATION_MARKED_ON_SF("is_registration_marked_on_sf"),
		IS_DELETED("is_deleted"),
		LOAN_ACCOUNT_NUMBER("loan_account_number");
		
		public final String stringValue;
		UserColumn(String stringValue) {
			this.stringValue = stringValue;
		}
	}
	
	public enum CfColumn {
		ID("id"),
		USER_ID("user_id"),
		NAME("name"),
		EMAIL_ID("email"),
		MOBILE_NUMBER("number"),
		REGISTRATION_DATETIME("datetime"),
		RAW_DATA("raw_data");
		
		public final String stringValue;
		CfColumn(String stringValue) {
			this.stringValue = stringValue;
		}
	}
	
	public enum PaymentInfoColumn {	
		ID("id"),
		USER_ID("user_id"),
		LOAN_ACCOUNT_NUMBER("loan_account_number"),
		LOAN_TYPE("loan_account_type"),
		ORDER_ID("order_id"),
		PG_ORDER_ID("pg_order_id"),
		PAYMENT_ID("payment_id"),
		CURRENCTY("currency"),
		AMOUNT("amount"),
		INITIAL_DATETIME("initial_datetime"),
		COMPLETION_DATETIME("completion_datetime"),
		STATUS("status"),
		STATUS_MESSAGE("status_message"),
		RECEIPT_ID("receipt_id"),
		RECEIPT_NUMBER("receipt_number"),
		RECEIPT_DATA("receipt_data"),
		PAYMENT_NATURE("payment_nature"),
		PAYMENT_SUB_TYPE("payment_sub_type"),
		EMI_REDUCTION("emi_reduction"),
		TENURE_REDUCTION("tenure_reduction"),
		PAYMENT_METHOD("payment_method"),
		DEVICE_TYPE("device_type");

		public final String value;
		PaymentInfoColumn(String value) {
			this.value = value;
		}
	}
	
	public enum SecondaryInfoColumn {
		
		ID("id"),
		USER_ID("user_id"),
		LOGIN_INFO("login_info"),
		DEVICE_TYPE("device_type"),
		DEVICE_ID("device_id"),
		APNS_KEY("apns_key"),
		FCM_KEY("fcm_key"),
		PASSWORD_CHANGE_DATETIME("password_change_datetime"),
		MOBILE_NUMBER_CHANGE_DATETIME("mobile_number_change_datetime");
		
		public final String value;
		SecondaryInfoColumn(String value) {
			this.value = value;
		}
	}
	
	public enum LoginInfoColumn {
		
		USER_ID("user_id"),
	    LOGIN_DATETIME("login_datetime"),
	    IP_ADDRESS("ip_address"),
	    DEVICE_ID("device_id"),
	    DEVICE_TYPE("device_type"),
	    DEVICE_MODEL("device_model"),
	    APP_VERSION("app_version"),
	    OS_VERSION("os_version");
	    
	    public final String value;
		LoginInfoColumn(String value) {
			this.value = value;
		}
		
	}
	
	public enum ReferralColumn {
		
		USER_ID("user_id"),
	    MOBILE_NUMBER("mobile_number"),
	    EMAIL_ID("email_id"),
	    ADDRESS("address"),
	    SALUTATION("salutation"),
	    FIRST_NAME("first_name"),
	    MIDDLE_NAME("middle_name"),
	    LAST_NAME("last_name"),
	    SF_LEAD_ID("sf_lead_id"),
	    POINTS("points"),
	    STATUS("status"),	    
	    DATETIME("datetime");
	
	    public final String value;
	    ReferralColumn(String value) {
			this.value = value;
		}
	}
	
	public static User getUserObjectFromResultSet(ResultSet resultSet) throws SQLException {
		
		User fetchedUser = new User();
		
		fetchedUser.userId = resultSet.getInt(ColumnsNFields.UserColumn.USER_ID.stringValue);
		fetchedUser.name = resultSet.getString(ColumnsNFields.UserColumn.NAME.stringValue);
		fetchedUser.password = resultSet.getString(ColumnsNFields.UserColumn.PASSWORD.stringValue);
		fetchedUser.emailId = resultSet.getString(ColumnsNFields.UserColumn.EMAIL_ID.stringValue);
		fetchedUser.imageUrl = resultSet.getString(ColumnsNFields.UserColumn.IMAGE_URL.stringValue);
		fetchedUser.mobileNumber = resultSet.getString(ColumnsNFields.UserColumn.MOBILE_NUMBER.stringValue);
		fetchedUser.countryCode = resultSet.getString(ColumnsNFields.UserColumn.COUNTRY_CODE.stringValue);
		fetchedUser.registrationDateTime = resultSet.getString(ColumnsNFields.UserColumn.REGISTRATION_DATETIME.stringValue);
		fetchedUser.isMobileNumberVerified = resultSet.getBoolean(ColumnsNFields.UserColumn.MOBILE_VERIFIED.stringValue);
		fetchedUser.loanAccountNumber = resultSet.getString(ColumnsNFields.UserColumn.LOAN_ACCOUNT_NUMBER.stringValue);
		fetchedUser.crmAccountNumber = resultSet.getString(ColumnsNFields.UserColumn.CRM_ACCOUNT_NUMBER.stringValue);
		fetchedUser.sessionPasscode = resultSet.getString(ColumnsNFields.UserColumn.SESSION_PASSCODE.stringValue);
		fetchedUser.isRegistrationMarkedOnSF = resultSet.getBoolean(ColumnsNFields.UserColumn.IS_REGISTRATION_MARKED_ON_SF.stringValue);
		fetchedUser.isDeleted = resultSet.getBoolean(ColumnsNFields.UserColumn.IS_DELETED.stringValue);

		
		return fetchedUser;
		
	}
	
	// =============== ADMIN TABLES ABD COLUMNS ======================= //
	// ================================================================ //
	
	public enum AdminUserColumn {
		
		NAME("name"),
		EMAIL("email"),
		SF_USER_ID("sf_user_id"),
		IMAGE_URL("image_url"),
		PASSWORD("password"),		
		PASSCODE("passcode"),
		COUNTRY_CODE("country_code"),
		MOBILE_NUMBER("mobile_number"),
		MOBILE_VERIFIED("mobile_verified"),
		REGISTRATION_DATETIME("registeration_datetime"),
		NOTIFICATOIN_ALLOWED("notification_allowed"),
		ROLE("role");
		
		public final String stringValue;
		AdminUserColumn(String stringValue) {
			this.stringValue = stringValue;
		}
	}
	
	public enum AdminSecondaryInfoColumn {
		
		ID("id"),
		USER_ID("user_id"),
		LOGIN_INFO("login_info"),
		DEVICE_TYPE("device_type"),
		DEVICE_ID("device_id"),
	    DEVICE_MODEL("device_model"),
	    APP_VERSION("app_version"),
	    OS_VERSION("os_version"),
		APNS_KEY("apns_key"),
		FCM_KEY("fcm_key"),
		PASSWORD_CHANGE_DATETIME("password_change_datetime"),
		MOBILE_NUMBER_CHANGE_DATETIME("mobile_number_change_datetime");
		
		public final String value;
		AdminSecondaryInfoColumn(String value) {
			this.value = value;
		}
	}
	
	public enum AdminLoginInfoColumn {
		
		USER_ID("user_id"),
	    LOGIN_DATETIME("login_datetime"),
	    IP_ADDRESS("ip_address"),
	    DEVICE_ID("device_id"),
	    DEVICE_TYPE("device_type"),
	    DEVICE_MODEL("device_model"),
	    APP_VERSION("app_version"),
	    OS_VERSION("os_version");
	    
	    public final String value;
	    AdminLoginInfoColumn(String value) {
			this.value = value;
		}
		
	}
	
	// *************** END OF ADMIN TABLES AND COLUMNS **************** //
	// **************************************************************** //
	
	// =============== NOTIFICATION TABLES ABD COLUMNS ================ //
	// ================================================================ //
    
	public enum NotificationColumn {
		
		TITLE("title"),
	    MESSAGE("message"),
	    BIG_MESSAGE("big_message"),
	    IMAGE_URL("image_url"),
	    WEB_URL("web_url"),
	    DATA("data"),
	    AUDIENCE_TYPE("audience_type"),
	    ADIENCE_GROUP("audience_group"),
	    PRIORITY("priority"),
	    PLATFORM("platform"),
	    KIND("kind"),
	    ON_CLICK_ACTION("on_click_action"),
	    SCREEN_TO_OPEN("screen_to_open"),
	    SCHEDULE_TYPE("schedule_type"),
	    SCHEDULE_DATETIME("schedule_datetime"),
	    SENT_DATETIME("sent_datetime"),
	    CREATE_DATETIME("create_datetime"),
	    IS_SCHEDULED("is_scheduled"),
	    TOTAL_COUNT("total_count"),
	    SUCCESS_COUNT("success_count"),
	    FAILURE_COUNT("failure_count"),
	    SCHEDULER_ID("scheduler_id"),
	    SCHEDULER_NAME("scheduler_name");
		
		public final String stringValue;
		NotificationColumn(String stringValue) {
			this.stringValue = stringValue;
		}
	}
	
	public enum UserNotificationInfoColumn {
		
		NOTIFICATION_ID("notification_id"),
		DYNAMIC_MESSAGE("dynamic_message"),
		USER_ID("user_id"),
		HAS_READ("has_read"),
	    READ_DATETIME("read_datetime"),
	    DEVICE_ID("device_id"),
	    DEVICE_TYPE("device_type"),
	    DEVICE_MODEL("device_model"),
	    APP_VERSION("app_version"),
	    OS_VERSION("os_version");
	    
	    public final String value;
	    UserNotificationInfoColumn(String value) {
			this.value = value;
		}
		
	}

	// ************ END OF NOTIFICATION TABLES AND COLUMNS ************ //
	// **************************************************************** //
    
	
	public enum UserRequestColumn
	{
		USER_ID("user_id"),
		ID("id"),
		TOKEN("token"),
		IS_VALID("is_valid"),
		TYPE("type"),
		DESCRIPTION("description"),
		STATUS("status"),
		CREATED_DATETIME("created_datetime"),
		UPDATED_DATETIME("updated_datetime"),
		VALID_DATETIME("valid_datetime"),
		IP_ADDRESS("ip_address");
		
		public final String value;
		UserRequestColumn(String value) {
           this.value = value;
		}
	}
	
	public enum AdminLogInfoColumn {

		RECORD_TYPE("record_type"), RECORD_ID("record_id"), ACTION("action"),
		DESCRIPTION("description"), DATETIME("datetime"), STATUS("status");

		public final String value;

		AdminLogInfoColumn(String value) {
			this.value = value;
		}

	}
	
	
	public enum InstalledAppsColumn {

		USER_ID("user_id"), APP_NAME("app_name"), PACKAGE_NAME("package_name"), VERSION_NAME("version_name"),
		VERSION_CODE("version_code"), DATETIME("datetime"), RAW_DATA("raw_data");

		public final String value;

		InstalledAppsColumn(String value) {
			this.value = value;
		}

	}
	
	
	public enum UserCommunicationColumn {
		
		ID("id"),
		RECORD_TYPE("record_type"),
		RECORD_ID("record_id"),
		HFO_ID("hfo_id"),
		IS_EMAIL("is_email_sent"),
		EMAIL_DATETIME("email_sent_datetime"),
		IS_SMS("is_sms_sent"),
		SMS_DATETIME("sms_sent_datetime"),
		IS_NOTIFICATION("is_notification_sent"),
		NOTIFICATION_DATETIME("notification_sent_datetime"),
		CREATE_DATETIME("create_datetime");
		
		public final String stringValue;
		UserCommunicationColumn(String stringValue) {
			this.stringValue = stringValue;
		}
	}
	
	public enum PartnerColumn {	
		ORG_NAME("orgName"),	
		CLIENT_ID("clientId"),
		CLIENT_SECRET("clientSecret"),
		IP_RESTRICTED("ipRestricted"),
		SERVICES_ALLOWED("servicesAllowed"),
		DESTINATION("destination"),
		LEAD_SOURCE("leadSource");

		public final String value;
		PartnerColumn(String value) {
			this.value = value;
		}
	}
	
	public enum WhiteListColumn {	
		ORG_ID("org_id"),	
		NAME("name"),
		IP_ADDRESS("ip_address"),
		IS_ACTIVE("is_active");
		

		public final String value;
		WhiteListColumn(String value) {
			this.value = value;
		}
	}
	
}


