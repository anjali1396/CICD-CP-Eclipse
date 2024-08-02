package utils;

public class Constants {

	// ---------------------<< TRUE FOR PRODUCTION >>--------------------------- //
	public static final boolean IS_PRODUCTION = false; // TODO: TRUE for production
	public static final boolean IS_SF_LIVE = false; // TODO: TRUE for production
	public static final boolean IS_DB_LIVE = false; // TODO: TRUE for production
	public static final boolean IS_PAYMENT_LIVE = false; // TODO: TRUE for production
	public static final boolean IS_NOTIFICATION_LIVE = false; // TODO: TRUE for production
	public static final boolean IS_STRICT_PROD_PROCESS_ACTIVE = false; // TODO: TRUE for production
	// ---------------------------------------------------------false--------------- //

	// ---------------------<< FALSE FOR PRODUCTION >>-------------------------- //
	public static final boolean IS_STAGING = true; // TODO: FALSE for production
	// ------------------------------------------------------------------------- //

	public static final String UPLOAD_FILE_SERVER = "/var/www/images/document_picture/"; // TODO For Production
	public static final String UPLOAD_FILE_LOCAL_SERVER = "/var/www/images/document_picture/"; // TODO: FOR STAGING
//	public static final String UPLOAD_FILE_SERVER = "/Users/appledeveloper/var/www/images/document_picture/";
//	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/appledeveloper/var/www/images/document_picture/";
//	public static final String UPLOAD_FILE_SERVER = "/Users/sanjay/var/www/files/";
//	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/sanjay/var/www/files/";
//	public static final String UPLOAD_FILE_SERVER = "/Users/rananrodrigues/var/www/images/document_picture/";
//	public static final String UPLOAD_FILE_SERVER = "/Users/ho-anjali/var/www/files/";
//	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/ho-anjali/var/www/files/";
//	public static final String UPLOAD_FILE_SERVER = "/Users/anujbhelkar/Documents/Files/";
//	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/anujbhelkar/Documents/Files/";
		
//	public static final String UPLOAD_FILE_SERVER = "/Users/harshitchauhan/var/www/images/document_picture/"; //Local
//	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/harshitchauhan/var/www/images/document_picture/"; //Local


	public static final int HITBERNATE_BATCH_SIZE = 25; // TODO: original was 25

	public static final String NA = "NA";
	public static final String RESET = "RESET";
	public static final String NONE = "NONE";
	public static final String STATUS = "status";
	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";
	public static final String MESSAGE = "message";
	public static final String ERROR = "error";
	public static final String FAILURE_JSON_STRING = "{\"status\": \"failure\"}";
	public static final String SUCCESS_JSON_STRING = "{\"status\": \"success\"}";
	public static final String SESSION_PASSCODE = "sessionPasscode";
	public static final String SOURCE_PASSCODE = "sourcePasscode";
	public static final String CROWN_PASSCODE = "crownPasscode";
	public static final String USER_ID = "userId";
	public static final String DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again!";
	public static final String PAYMENT_ERROR_MESSAGE = "We've received your payment,"
			+ " but there seem to be a problem while generating your payment receipt."
			+ " Please retry, or contact our customer support regarding this payment.";
	public static final String INCORRECT_USERNAME_PASSWORD_ERROR = "Incorrect username or password. ";

	public static final String LOAN_TYPE_CLOSED = "Closed - Obligations met";
	public static final String LOAN_TYPE_CANCELED = "Canceled";
	public static final String LOAN_TYPE_CANCELLED = "Cancelled";

	public static final String PART_PAYMENT_TYPE_EMI_REDUCTION = "EMI Reduction";
	public static final String PART_PAYMENT_TYPE_TENOR_REDUCTION = "Tenor Reduction";

	public static final String DEVICE_TYPE = "deviceType";
	public static final String ANDROID = "android";
	public static final String iOS = "iOS";
	public static final String KEY_ACCOUNT_STATEMENT = "Account Statement";

//	public static final String BANK_ACCOUNT_FOR_PAYNIMO = "Collection A/c - Axis Bank A/c No. 912020036268117";
//	public static final String BANK_ACCOUNT_FOR_RAZORPAY = "E-Payments HDFC 57500000275310";

	public static final String BANK_ACCOUNT_FOR_PAYNIMO = "E-Payment A/c HDFC Bank Ac. ****5310";

	public static final String BANK_ACCOUNT_FOR_RAZORPAY = "E-Payment A/c HDFC Bank Ac. ****5310";

	public static final String RECORD_TYPE_HOME_LAON = "01290000000TAL9";
	public static final String RECORD_TYPE_TOPUP_LOAN = "01290000001Akms";

	public static final String BIOMETRIC = "biometric";
	public static final String EMAIL_ID = "emailId";
	public static final String PASSWORD = "password";
	public static final String SHOULD_AUTHORIZE = "shouldAuthorize";
	public static final String ADMIN_USER = "adminUser";
	public static final String ADMIN_ROLE = "adminRole";
	public static final String ADMIN = "ADMIN";
	public static final String ADMIN_SERVICES = "ADMIN_SERVICES";
	public static final String CP_USER = "CP_USER";
	public static final String PROSPECT_SERVICES = "PROSPECT_SERVICES";
	public static final String LAST_NOTIFICATION_DATE_TIME = "lastNotificationDatetime";
	public static final String NOTIFICATION_FETCH_TYPE = "notificationFetchType";
	public static final String TOP_NOTIFICATION_DATE_TIME = "topNotificationDatetime";
	public static final String BOTTOM_NOTIFICATION_DATE_TIME = "bottomNotificationDatetime";
	public static final String NOTIFICATIONS = "notifications";
	public static final String UNREAD_COUNT = "unreadCount";
	public static final String LOAN_ACCOUNT_NUMBER = "loanAccountNumber";
	public static final String CRM_ACCOUNT_NUMBER = "crmAccountNumber";
	public static final String START_DATETIME = "startDatetime";
	public static final String END_DATETIME = "endDatetime";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	public static final String REQUEST_ID = "requestId";
	public static final String TOKEN = "token";
	public static final String OPEN = "open";
	public static final String CLOSED = "closed";
	public static final String USER = "user";
	public static final String REQUEST = "request";
	public static final String ACTION = "action";
	public static final String CRON = "cron";
	public static final String SCHEDULE_DATETIME = "scheduleDateTime";
	public static final String SCHEDULE_TIME = "scheduleTime";
	public static final String DEFAULT = "default";
	public static final String EPAY_EMAIL_ID = "epay@homefirstindia.com";
	public static final int TEST_LOAN_ACCOUNT_ID = 8832;
	public static final String AUTH_TWO = "AUTH_TWO";

	// public static final String EXTERNAL_PARTNER = "externalPartner";
	public static final String PARTNER_HOMEFIRST_LMS = "HomefirstLMS";
	public static final String PARTNER_AMAZON = "AWS-RABIT";
	public static final String PARTNER_HOMEFIRSTONE_SPRING = "HomefirstOneSpring";



	public static final String AUTHORIZATION = "Authorization";
	public static final String ORG_ID = "orgId";
	public static final String ROLE_ALL = "ALL";
	public static final String UTF_8 = "UTF-8";
	public static final String VALID_UPTO = "validUpto";

	public static final String LEAD = "lead";
	public static final String ID = "id";
	public static final String LEAD_ID = "leadId";
	public static final String DEFAULT_LEAD_SOURCE = "Organic";
	public static final String MOBILE_NUMBER = "mobileNumber";
	public static final String REFFERAL_LEAD_SOURCE = "Customer Referral App";
	public static final String RZP_CAPTURED = "Captured";
	public static final String RZP_NOT_FOUND = "NOT_FOUND";
	public static final String RZP_AUTHORIZED = "Authorized";
	public static final String RZP_REFUNDED = "Refunded";
	public static final String RZP_CREATED = "Created";
	public static final String RZP_FAILED = "Failed";

	public static final String CAPTURED = "captured";
	public static final String REFUNDED = "refunded";
	public static final String FAILED = "failed";

	public static final String VALID = "valid";
	public static final String INVALID = "inValid";
	public static final String IN_PROGRESS = "inProgress";

	public static final String PRIMARY = "Primary";
	
	public static final String FULLY_DISBURSED = "Fully Disbursed";

	public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	
	public static final int PASSWORD_EXPIRE_DAY_COUNT = 60;
	public static final int DEFAULT_LOGIN_ATTEMPTS = 4;
	public static final int DEFAULT_LOGIN_ATTEMPT_MINUTES = 30;

	// Header key for Repay Attack
	public static final String CYPHER = "cypher";
	public static final String CRYPT = "crypt";
	public static final String SITE_PHOTO_SOURCE = "Customer Portal";

	public static final String S3BUCKET_TEST = "https://hffc-teststaging-s3.s3.ap-south-1.amazonaws.com/external/";

	public static final String S3BUCKET_PROD = "https://homefirstindia-s3bucket.s3.ap-south-1.amazonaws.com/external/";
	
	public static final String SOURCE_CUSTOMER_PORTAL = "CUSTOMER_PORTAL";
	
	public static final String CUSTOMER_NAME = "customerName";
	public static final String SOURCE = "source";

	public enum ActionType {

		DO_LOGIN("DO_LOGIN"), DO_REGISTERATION("DO_REGISTERATION"), CONTINUE("CONTINUE"), RESET("RESET"),
		RETRY("RETRY"),

		AUTHENTICATE_AGAIN("AUTHENTICATE_AGAIN"), CONTACT_ADMIN("CONTACT_ADMIN");

		public final String stringValue;

		ActionType(String stringValue) {
			this.stringValue = stringValue;
		}
	}

	public enum Actions {
		AUTHENTICATE_AGAIN("AUTHENTICATE_AGAIN"), RETRY("RETRY"), FIX_RETRY("FIX_RETRY"), CANCEL("CANCEL"),
		CONTACT_ADMIN("CONTACT_ADMIN"), DO_REGISTRATION("DO_REGISTRATION"), DO_VERIFICATION("DO_VERIFICATION"),
		GO_BACK("GO_BACK"), DO_LOGIN("DO_LOGIN"), CONTINUE("CONTINUE"), DO_PASSWORD_RESET("DO_PASSWORD_RESET");

		public final String value;

		Actions(String value) {
			this.value = value;
		}

	}

	public enum Errors {

		UNKNOWN("UNKNOWN"), FAILED("FAILED"), INVALID_PASSWORD("INVALID_PASSWORD"),
		INVALID_CREDENTIALS("INVALID_CREDENTIALS"), RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
		ACCESS_DENIED("ACCESS_DENIED"), UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS"),
		DUPLICATE_RECORD("DUPLICATE_RECORD"), STRING_TOO_LONG("STRING_TOO_LONG"),
		JSON_PARSER_ERROR("JSON_PARSER_ERROR"), OPERATION_FAILED("OPERATION_FAILED"), INVALID_DATA("INVALID_DATA");

		public final String value;

		Errors(String value) {
			this.value = value;
		}
	}

//	public enum ProductTypeMap {
//		HOME_LOAN("Home Loan", "Home Loan"), 
//		HOME_LOAN_BALANCE_TRANSFER("Home Loan Balance Transfer", "Home Loan (BT)"),
//		LOAN_AGAINST_PROPERTY("Loan Against Property", "Loan Against Property (LAP)"),
//		HOME_CONSTRUCTION_LOAN("Home Construction Loan", "Own Plot + HL (SeCo)");
//
//		public final String displayName;
//		public final String sfName;
//
//		ProductTypeMap(String displayName, String sfName) {
//			this.sfName = sfName;
//			this.displayName = displayName;
//		}
//
//		public static String getSfNameFromType(String displayName) throws Exception {
//			for (ProductTypeMap item : ProductTypeMap.values()) {
//				if (item.displayName.equals(displayName))
//					return item.sfName;
//			}
//			throw new Exception("Invalid display name for : " + displayName);
//		}
//
//		public static String getDisplayName(String sfName) throws Exception {
//			for (ProductTypeMap item : ProductTypeMap.values()) {
//				if (item.sfName.equals(sfName))
//					return item.displayName;
//			}
//			throw new Exception("Invalid sfName for : " + sfName);
//		}
//
//	}

	public enum ProductTypeMap {

		HOME_LOAN("Home Loan", "Home Loan", true), 
		HOME_LOAN_RESALE("Home Loan", "Home Loan (Resale)", false),
		SHOP_LOAN_RESALE("Shop Loan", "Shop Loan (Resale)", true),
		TOP_UP_LOAN("Home Loan", "Top Up Loan", false),
		LOAN_FOR_REPAIR_RENOVATION_EXTENSION("Home Loan", "Loan for Repair / Renovation / Extension", false),
		LTV_ENHANCEMENT("Home Loan", "LTV Enhancement", false),

		HOME_LOAN_BT("Home Loan Balance Transfer", "Home Loan (BT)", true),
		BT_TOP_UP("Home Loan Balance Transfer", "Home Loan (BT) + Top Up", false),
		HL_BT_RENOVATION_LOAN("Home Loan Balance Transfer", "HL-BT + Renovation Loan", false),
		HL_BT_SECO("Home Loan Balance Transfer", "HL-BT + SECO", false),

		LOAN_AGAINST_PROPERTY("Loan Against Property", "LAP-BT", false),
		LAP_BT_RENOVATION_LOAN("Loan Against Property", "LAP-BT + Renovation Loan", false),
		LAP_BT_SECO("Loan Against Property", "LAP-BT + SECO", false),
		LAP_BT_TOP_UP("Loan Against Property", "LAP-BT + Top-Up", false),
		LOAN_AGAINST_PROPERTY_LAP("Loan Against Property", "Loan Against Property (LAP)", true),
		LAP_AGAINST_COMMERCIAL_PROPERTY("Loan Against Property", "Loan Against Property against commercial property",
				false),
		LAP_RENOVATION("Loan Against Property", "LAP + Renovation", false),
		LAP_SECO("Loan Against Property", "LAP + SECO", false),

		PLOT_LOAN_BT_SECO("Home Construction Loan", "Plot Loan (BT) + SECO", false),
		OWN_PLOT_HL_SECO("Home Construction Loan", "Own Plot + HL (SeCo)", true),
		PLOT_LOAN("Home Construction Loan", "Plot Loan", false),
		PLOT_LOAN_HL_SECO("Home Construction Loan", "Plot Loan + HL (SeCo)", false);

		public final String displayName;
		public final String sfName;
		public final boolean isDefault;

		ProductTypeMap(String displayName, String sfName, boolean isDefault) {
			this.sfName = sfName;
			this.displayName = displayName;
			this.isDefault = isDefault;
		}


		public static boolean checkSfNameFromType(String displayName) throws Exception {

			for (ProductTypeMap item : ProductTypeMap.values()) {
				
				if (item.sfName.equals(displayName)) {
					LoggerUtils.log("ProductTypeMap - item.sfName=== : " + item.sfName);
					return true;

				}
			}
			//LoggerUtils.log("ProductTypeMap - Error no SF Name found for displayName : " + displayName);
			return false;
		}
		
		public static String getSfNameFromType(String displayName) throws Exception {
			for (ProductTypeMap item : ProductTypeMap.values()) {
				if (item.displayName.equals(displayName) && item.isDefault)
					return item.sfName;
			}
			//throw new Exception("Invalid display name for : " + displayName);
			LoggerUtils.log("ProductTypeMap - Error no SF Name found for displayName : " + displayName);
			return HOME_LOAN.sfName;
		}

		public static String getDisplayName(String sfName) throws Exception {
			for (ProductTypeMap item : ProductTypeMap.values()) {
				if (item.sfName.equals(sfName))
					return item.displayName;
			}
			//throw new Exception("Invalid sfName for : " + sfName);
			LoggerUtils.log("ProductTypeMap - Error no Display name found for sfName : " + sfName);
			return HOME_LOAN.displayName;
		}

	}

	public enum CredType {
		PRODUCTION("PRODUCTION"), UAT("UAT");

		public final String value;

		CredType(String value) {
			this.value = value;
		}
	}

}
