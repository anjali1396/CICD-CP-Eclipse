package v2.managers;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.collections4.ListUtils;
import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import dao.BlogRepository;
import dao.CommonRepository;
import dao.LendingProductsRepository;
import dao.PaymentRepository;
import dao.PromoRepository;
import dao.ROIRepository;
import dao.UserRepository;
import helper.AppContextProvider;
import managers.HFOManager;
import managers.LMSManager;
import models.Address;
import models.Blog;
import models.DeleteAccountRequest;
import models.LendingProducts;
import models.LoginInfo;
import models.ROIReprice;
import models.Referrals;
import models.SecondaryInfo;
import models.User;
import models.UserRequest;
import models.notification.CPNotification;
import models.notification.UserNotificationToken;
import models.payment.SFAccountStatement;
import models.sob.Promo;
import services.Recaptcha;
import utils.BasicUtils;
import utils.Constants;
import utils.Constants.Actions;
import utils.Constants.Errors;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.DateTimeUtils;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.MailUtils;
import utils.MailUtils.ContentType;
import utils.NotificationUtils.NotificationFetchType;
import utils.NotificationUtils.NotificationServiceType;
import v1.repository.NotificationRepository;
import utils.OneResponse;
import utils.ProptertyUtils;
import utils.BasicUtils.LoanListRequestType;
import utils.BasicUtils.MimeMap;
import v2.dbhelpers.DatabaseHelper;
import v2.dbhelpers.NotificationDatabaseHelper;
import v2.managers.AmazonClient.S3BucketPath;
import v2.managers.RequestManager.RequestType;

public class UserManager {

	public static final String UPLOAD_FILE_SERVER = Constants.UPLOAD_FILE_SERVER;
	public static final String LOCAL_FILES_PATH = Constants.UPLOAD_FILE_LOCAL_SERVER;
	public static final String UPLOAD_FILE_LOCAL_SERVER = Constants.UPLOAD_FILE_LOCAL_SERVER;;

	private final SalesForceManager sfManager;
	private final LocalResponse lResponse;
	private final DatabaseHelper dbHelper;
	private final Gson gson;
	private final UserRepository userRepo;
	private final PaymentRepository paymentRepo;
	private final ROIRepository roiRepo;
	private final CommonRepository commonRepo;
	private final LendingProductsRepository lendingProdRepo;
	private final HFOManager hfoManager;
	private final LMSManager lmsManager;
	private NotificationRepository _notificationRepo = null;
	private AppContextProvider appContextProvider = new AppContextProvider();
	ArrayList<String> validImageExtension = new ArrayList<>(Arrays.asList("image/png","image/jpeg","image/jpg"));

	private NotificationRepository notificationRepo() throws Exception {
		if (null == _notificationRepo)
			_notificationRepo = new NotificationRepository();
		return _notificationRepo;
	}

	private void log(String value) {
		LoggerUtils.log("V2.UserManager." + value);
	}

	private void printLog(String value) {
		System.out.println("V2.UserManager." + value);
	}

	public UserManager() {
		sfManager = new SalesForceManager();
		lResponse = new LocalResponse();
		dbHelper = new DatabaseHelper();
		gson = new Gson();
		userRepo = new UserRepository();
		roiRepo = new ROIRepository();
		lendingProdRepo = new LendingProductsRepository();
		commonRepo = new CommonRepository();
		hfoManager = new HFOManager();
		lmsManager = new LMSManager();
		paymentRepo = new PaymentRepository();
	}

	public boolean verifySource(String sourceCode) {

		try {

			if (BasicUtils.getTheKey(sourceCode)
					.equals(ProptertyUtils.getValurForKey(ProptertyUtils.Keys.KEY_TO_THE_SOUCE)))
				return true;

		} catch (Exception e) {
			System.out.println("Error while verifying source code: " + e.toString());
			e.printStackTrace();
		}

		return false;
	}

	public JSONObject performLogin(int userId, JSONObject bodyObject, String ipAddress) throws Exception {

		User user = new User(bodyObject);

		JSONObject responseJson = new JSONObject();

		try {

			User fetchedUser = appContextProvider.getUserByMobileNumber(user.mobileNumber);

			if (null == fetchedUser) {

				responseJson = new JSONObject();
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "No account associated with this mobile number. "
						+ "Please do the registration process or re-check entered mobile number.");

			} else {

				if (fetchedUser.isDeleted) {

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, "No account associated with this mobile number. "
							+ "Please do the registration process or re-check entered mobile number.");

				} else if (fetchedUser.password.equals(Constants.NA)) {

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, "Please complete the registration process.");

				} else if (fetchedUser.password.equals(Constants.RESET)) {

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, Constants.RESET);

				} else {

					if (BasicUtils.getTheKey(user.password).equals(fetchedUser.password)) {

						try {
//							dbHelper.addLoginInfo(fetchedUser.userId, bodyObject, ipAddress);
							appContextProvider.addLoginInfo(fetchedUser.userId, bodyObject, ipAddress);
						} catch (Exception e) {
							System.out.println("Error while adding login info: " + e.toString());
						}

						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, Constants.NA);
						updateLoginInfoOnSF(fetchedUser);
						boolean requireSessionPass = bodyObject.optBoolean("requireSessionPass", true);
						if (!requireSessionPass) {

							responseJson.put(Constants.SESSION_PASSCODE,
									userRepo.updateAndGetSession(fetchedUser).sessionPasscode);
							responseJson.put("user", fetchedUser.toJson(true));

						} else {
							responseJson.put("userId", fetchedUser.userId);
						}

					} else {

						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, "Incorrect username or password");

					}
				}

			}

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseJson;

	}

	public JSONObject getDashboard(int userId) throws Exception {

		User user = getUserById(userId);

		JSONObject dashboardData = sfManager.getUserDashboard(user.crmAccountNumber);

		if (null != dashboardData) {

			dashboardData.put(Constants.STATUS, Constants.SUCCESS);
			dashboardData.put(Constants.MESSAGE, Constants.NA);
			dashboardData.put("bounceCharge", 590);

			return dashboardData;

		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getLoanDetails(int userId) throws Exception {

		User user = getUserById(userId);

		JSONObject loanData = sfManager.getLoanDetail(user.crmAccountNumber);

		if (null != loanData) {
			loanData.put(Constants.STATUS, Constants.SUCCESS);
			loanData.put(Constants.MESSAGE, Constants.NA);

			return loanData;
		} else
			return (new LocalResponse()).toJson();

	}

	private User getUserById(int userId) throws Exception {

		User user = appContextProvider.getUserById(userId);

		if (null != user)
			return user;
		else
			throw new Exception("No user found with given userId.");

	}

	public JSONObject getPaymentDetailForLoan(JSONObject requestJson) throws Exception {

		JSONArray paymentArray = sfManager.getPaymentDetailForLoan(requestJson);

		if (null != paymentArray) {
			JSONObject responseJson = BasicUtils.getSuccessTemplateObject();
			responseJson.put("payments", paymentArray);
			return responseJson;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getUserPaymentDetails(int userId) throws Exception {

		User user = getUserById(userId);

		JSONObject json = sfManager.getPaymentDetails(user.crmAccountNumber);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getLoanAccountList(int userId, JSONObject requestJson) throws Exception {

		User user = getUserById(userId);

		if (null != user) {

			String condition = requestJson.optString("condition", Constants.NA);
			BasicUtils.LoanListRequestType requestCondition = BasicUtils.LoanListRequestType.get(condition);

			if (null == requestCondition) {
				LocalResponse lResponse = new LocalResponse();
				lResponse.message = "Invalid request condition";
				return lResponse.toJson();
			}

			String HlProductList = Constants.NA;
			if (requestCondition == LoanListRequestType.PROPERTY_IMAGES) {
				ArrayList<LendingProducts> lendingProds = lendingProdRepo.getHLProducts();

				Iterator<LendingProducts> iter = lendingProds.iterator();
				ArrayList<String> sfIdList = new ArrayList<String>();
				while (iter.hasNext()) {
					LendingProducts yp = iter.next();
					sfIdList.add(yp.sfId);
				}
				if (!sfIdList.isEmpty()) {
					HlProductList = String.join(",",
							sfIdList.stream().map(name -> ("'" + name + "'")).collect(Collectors.toList()));
				}

			}

			JSONObject response = sfManager.getLoanAccountList(user.crmAccountNumber, requestCondition, HlProductList);

			if (null != response) {
				response.put(Constants.STATUS, Constants.SUCCESS);
				response.put(Constants.MESSAGE, Constants.NA);
				return response;
			} else
				return (new LocalResponse()).toJson();

		} else {
			return (new LocalResponse()).toJson();
		}

	}

	public JSONObject getRecentMonthlyPayments(int userId, String body) throws Exception {

		User user = getUserById(userId);

		JSONObject requestObject = new JSONObject(body);
		String paymentType = requestObject.getString("paymentType");
		String loanAccountNumber = requestObject.getString("loanAccountNumber");

		JSONObject json = sfManager.getRecentMonthlyPayments(user.crmAccountNumber, loanAccountNumber, paymentType);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject addReferral(int userId, JSONObject bodyObject) throws Exception {

		Referrals referral = gson.fromJson(bodyObject.toString(), Referrals.class);
		referral.userId = userId;

		var addressJson = bodyObject.optJSONObject("address");

		if (null != addressJson) {
			Address address = gson.fromJson(addressJson.toString(), Address.class);

			if (address.isValid())
				referral.addressJson = address.toJson().toString();
			else
				referral.address = null;

		}

		referral.sfLeadSource = Constants.REFFERAL_LEAD_SOURCE; // Replacing lead source by default

		JSONObject responseJson = null;

		try {

			var existingLead = commonRepo.findLeadByMobileNumber(referral.mobileNumber);

			var user = userRepo.findUserByUserId(userId);

			LocalResponse eResponse = new LocalResponse();

			if (null != existingLead) {

				if (null != existingLead.sfLeadId && !existingLead.sfLeadId.equalsIgnoreCase(Constants.NA)) {

					if (existingLead.userId == userId)
						eResponse.message = "You've already referred this Customer previously. Please use a different mobile number.";
					else
						eResponse.message = "This customer has already been referred by someone else. Please use a different mobile number.";

					return eResponse.toJson();

				} else {

					if (existingLead.status.equalsIgnoreCase("DUPLICATE_RECORD")) {

						eResponse.message = "This customer has already been referred by someone else. Please use a different mobile number.";
						return eResponse.toJson();

					} else {

						responseJson = this.saveReferralLead(existingLead, user);

					}

				}

			} else {

				boolean isSuccess = commonRepo.saveRefferal(referral);

				if (!isSuccess) {
					LoggerUtils.log("Failed to insert referral data in the DB ");
					eResponse.message = Constants.DEFAULT_ERROR_MESSAGE;
					return eResponse.toJson();

				}

				LoggerUtils.log("New lead data: " + gson.toJson(referral));

				responseJson = this.saveReferralLead(referral, user);

			}

		} catch (Exception e) {

			LoggerUtils.log("Error: " + e.toString());

		}

		if (null == responseJson) {
			return new LocalResponse().toJson();

		}

		return responseJson;

	}

	private JSONObject saveReferralLead(Referrals referral, User user) throws Exception {

		JSONObject responseJson = null;
		LocalResponse fResponse = new LocalResponse();

		try {

			salesforce.SalesForceManager v1SFManager = new salesforce.SalesForceManager();

			var accountResp = v1SFManager.getAccountIDFromCRM(user.crmAccountNumber);

			if (null == accountResp) {
				fResponse.message = Constants.DEFAULT_ERROR_MESSAGE;
				responseJson = fResponse.toJson();
				return responseJson;

			}

			referral.referredById = accountResp.optString("accountId");
			referral.referredByName = accountResp.optString("accountName");

			final var lmsResponse = lmsManager.createLead(referral.getLMSRequestJson());

			if (lmsResponse.isSuccess) {

				var lmsLeadId = new JSONObject(lmsResponse.message).optString("leadId");
				referral.sfLeadId = lmsLeadId;
				referral.status = "success";
				referral.userId = user.userId;

				boolean isSuccess = commonRepo.saveRefferal(referral);

				if (isSuccess) {
					LoggerUtils.log("Lead has been created and updated in the DB");

				} else {
					LoggerUtils.log("Lead has been created and but failed to update in DB");

				}

				responseJson = BasicUtils.getSuccessTemplateObject();
				responseJson.put("referralInfo", gson.toJson(referral));

				new SendEmailTask(user, referral).run();

			} else {

				referral.sfLeadId = Constants.NA;
				referral.status = lmsResponse.error;

				boolean isSuccess = commonRepo.saveRefferal(referral);

				if (isSuccess) {
					LoggerUtils.log("Failed to create LEAD on Kaysis and status updated in the DB");

				} else {
					LoggerUtils.log("Failed to create LEAD on Kaysis and Failed to updated status in the DB");

				}

				if (referral.status.equalsIgnoreCase("DUPLICATE_RECORD")) {
					fResponse.message = "This customer has already been referred by someone else. Please use a different mobile number.";

				}

				responseJson = fResponse.toJson();

			}

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());

		}

		return responseJson;

	}

	public class SendEmailTask implements Runnable {

		private Referrals lead;
		private User user;

		public SendEmailTask(User user, Referrals lead) {
			this.lead = lead;
			this.user = user;
		}

		@Override
		public void run() {
			try {

				if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE)
					return;

				StringBuilder sb = new StringBuilder();

				sb.append("A new lead has been created via Customer Poral App Referral: ");

				sb.append("\n\n========== LEAD DETAILS ===========");

				sb.append("\n\n Lead ID : " + lead.id);
				sb.append("\n Name: " + lead.getFullName());
				sb.append("\n Mobile Number : " + lead.mobileNumber);
				sb.append("\n Email ID " + lead.emailId);
				sb.append("\n Address : " + lead.address);
				sb.append("\n Date : " + DateTimeUtils.getCurrentDateTimeInIST());
				sb.append("\n Salesforce Lead ID: " + lead.sfLeadId);
				sb.append("\n Salesforce Lead URL: https://hffc.lightning.force.com/lightning/r/Lead/" + lead.sfLeadId
						+ "/view");
				sb.append("\n Lead Source:  " + lead.sfLeadSource);

				sb.append("\n\n========== REFERRER DETAILS ===========");

				sb.append("\n\n Name : " + user.name);
				sb.append("\n Source ID : " + user.userId);
				sb.append("\n Mobile Number : " + user.mobileNumber);
				sb.append("\n Email ID : " + user.emailId);

				sb.append(
						"\n\n\nThis is an automatic email generated by \"Artificial Intelligence\" designed by Almighty AYUSH MAURYA himself.");
				sb.append("\nPlease do not reply to this email. Bas dua mein yaad rakhna!");

				if (!Constants.IS_PRODUCTION) {

					MailUtils.getInstance().sendDefaultMail(ContentType.TEXT_PLAIN, "New Lead | Customer Portal App",
							sb.toString(), "ranan.rodrigues@homefirstindia.com");

				} else {

					MailUtils.getInstance().sendDefaultMail(ContentType.TEXT_PLAIN, "New Lead | Customer Portal App",
							sb.toString(), "anuj.bhelkar@homefirstindia.com", "ranan.rodrigues@homefirstindia.com");

				}
			} catch (Exception e) {
				LoggerUtils.log("Error while sending email after lead genration: " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	public JSONObject addCFInfo(int userId, JSONObject requestObject) throws Exception {

		JSONArray cfArray = new JSONArray(requestObject.optString("data", "[]"));

		DatabaseHelper dbHelper = new DatabaseHelper();

		try {

			int storedCFCount = 0;

			for (int i = 0; i < cfArray.length(); i++) {

				JSONObject currentContact = cfArray.getJSONObject(i);

				JSONArray phoneNumbers = currentContact.getJSONArray("PHONE_NUMBERS");
				JSONArray emailList = currentContact.getJSONArray("EMAIL_LIST");

				String emailString = Constants.NA;

				if (!emailList.isEmpty()) {

					emailString = "";

					for (int j = 0; j < emailList.length(); j++) {
						emailString += emailList.get(j).toString();
						if (emailList.length() > 1 && j < emailList.length() - 1) {
							emailString += ",";
						}
					}

				}

				String userName = currentContact.optString("NAME");

				for (int j = 0; j < phoneNumbers.length(); j++) {

					try {
						storedCFCount += dbHelper.addContactData(userId, phoneNumbers.get(j).toString(), emailString,
								userName, currentContact);
					} catch (SQLException sqle) {
						// LoggerUtils.log("DB Error while adding CFInfo; Contact level: " +
						// sqle.getMessage());
					}

				}

			}

			dbHelper.close();

			LoggerUtils.log("Total CF Added: " + storedCFCount);

			return BasicUtils.getSuccessTemplateObject();

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

	}

//	public JSONObject updateNotificationStatus(int userId, JSONObject requestObject) throws Exception {
//
//		User user = getUserById(userId);
////		NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper();
//
//		try {
//
//			notificationRepo().
//			boolean status = dbHelper.updateUserNotificationStatus(user, requestObject, true);
//			if (!status)
//				status = dbHelper.updateUserNotificationStatus(user, requestObject, false);
//			dbHelper.close();
//
//			LocalResponse lResponse = new LocalResponse();
//
//			if (status) {
//				lResponse.isSuccess = true;
//				lResponse.message = "Successfully updated user notification status";
//				LoggerUtils.log("Successfully updated user notification status");
//			} else
//				LoggerUtils.log("Failed to update user notification status");
//
//			return lResponse.toJson();
//
//		} catch (Exception e) {
//			dbHelper.close();
//			throw e;
//		}
//
//	}

	public JSONObject getNotifications(int userId, JSONObject requestObject) throws Exception {

		User user = getUserById(userId);
		NotificationDatabaseHelper ndbHelper = new NotificationDatabaseHelper();

		try {

			NotificationFetchType fetchType = NotificationFetchType
					.get(requestObject.optString(Constants.NOTIFICATION_FETCH_TYPE, NotificationFetchType.FIRST.value));

			String currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();

			String topNotificationDatetime = requestObject.optString(Constants.TOP_NOTIFICATION_DATE_TIME,
					currentDateTime);

			String bottomNotificationDatetime = requestObject.optString(Constants.BOTTOM_NOTIFICATION_DATE_TIME,
					currentDateTime);

			if (fetchType != NotificationFetchType.FIRST) {
				if (topNotificationDatetime.equals(Constants.NA))
					return new LocalResponse().setError("Invalid topNotificationDatetime.").toJson();
				if (bottomNotificationDatetime.equals(Constants.NA))
					return new LocalResponse().setError("Invalid bottomNotificationDatetime.").toJson();
			}

			ArrayList<CPNotification> readNotifications = ndbHelper.getUserNotifications(user, fetchType,
					(fetchType == NotificationFetchType.TOP ? topNotificationDatetime
							: (fetchType == NotificationFetchType.BOTTOM ? bottomNotificationDatetime
									: currentDateTime)),
					true);

			ArrayList<CPNotification> unreadNotifications = ndbHelper.getUserNotifications(user, fetchType,
					(fetchType == NotificationFetchType.TOP ? topNotificationDatetime
							: (fetchType == NotificationFetchType.BOTTOM ? bottomNotificationDatetime
									: currentDateTime)),
					false);

			ndbHelper.close();

			LoggerUtils.log("This is how much it took to do the DB Query..");

			ArrayList<CPNotification> notifications = readNotifications;

			ArrayList<Integer> readIds = new ArrayList<>();
			for (CPNotification rn : readNotifications)
				readIds.add(rn.id);

			unreadNotifications = (ArrayList<CPNotification>) unreadNotifications.stream()
					.filter(n -> !readIds.contains(n.id)).collect(Collectors.toList());

			notifications.addAll(unreadNotifications);

			Collections.sort(notifications);

			int unreadNotificationCount = 0;
			JSONArray notificationArray = new JSONArray();

			for (int i = 0; i < notifications.size(); i++) {

				if (i < 25) {
					CPNotification item = notifications.get(i);
					notificationArray.put(item.toJson());
					if (!item.hasRead)
						unreadNotificationCount++;
				} else
					break;
			}

			LoggerUtils.log("This is how much it took to process all notifications.");

			JSONObject response = BasicUtils.getSuccessTemplateObject();
			response.put(Constants.NOTIFICATIONS, notificationArray);
			response.put(Constants.UNREAD_COUNT, unreadNotificationCount);

			return response;

		} catch (

		Exception e) {
			ndbHelper.close();
			throw e;
		}

	}

	public JSONObject getUnreadNotificationCount(int userId) throws Exception {

		final var unreadCount = notificationRepo().getUnreadUserNotificationCount(userId);
		JSONObject responseJson = BasicUtils.getSuccessTemplateObject();
		responseJson.put("unreadCount", unreadCount);

		return responseJson;

	}

	public JSONObject setProfileImage(int userId, String body) throws Exception {

		JSONObject bodyObject = new JSONObject(body);
	
		JSONObject json = new JSONObject();
		
		String requestFileName =  bodyObject.optString("fileName", null);
		String fileData = bodyObject.optString("fileData", null);
		
		Tika tika = new Tika();
		var ext = tika.detect(requestFileName); 
		
		if(!BasicUtils.isNotNullOrNA(fileData)|| !BasicUtils.isNotNullOrNA(requestFileName)|| !this.validImageExtension.contains(ext) ) {
			json.put(Constants.STATUS, Constants.FAILURE);
			json.put(Constants.MESSAGE, "Please provide valid image data.");
			return json;
		}
		
		String fileName = userId + "-" + requestFileName;
		
		AmazonClient amazonClient = new AmazonClient();

		DatabaseHelper dbHelper = new DatabaseHelper();

		boolean status = amazonClient.uploadImage(fileName, fileData, S3BucketPath.PROFILE_IMAGES);

		if (status) {


			try {

				boolean success = dbHelper.addOrUpdateUserProfilePicture(userId, fileName);
				dbHelper.close();

				if (success) {
					json.put(Constants.STATUS, Constants.SUCCESS);
					json.put("imageUrl", fileName);
				} else {
					json.put(Constants.STATUS, Constants.FAILURE);
					json.put(Constants.MESSAGE, "Failed to add your profile picture. Please try again.");
					json.put("imageUrl", fileName);
				}

			} catch (Exception e) {
				dbHelper.close();
				LoggerUtils.log("Error: " + e.toString());
				e.printStackTrace();
				throw e;
			}

			return json;

		} else
			return BasicUtils.getFailureTemplateObject();

	}

	public JSONObject getAccountStatement(int userId, String body) throws Exception {

		User user = getUserById(userId);
		JSONObject requestJson = new JSONObject(body);

		String loanAccountNumber = requestJson.optString(Constants.LOAN_ACCOUNT_NUMBER, Constants.NA);
		String startDate = requestJson.optString(Constants.START_DATE, Constants.NA);
		String endDate = requestJson.optString(Constants.END_DATE, Constants.NA);

		if (loanAccountNumber.equals(Constants.NA)) {
			lResponse.message = "Invalid loan account number.";
			return lResponse.toJson();
		}

		if (startDate.equals(Constants.NA)) {
			lResponse.message = "Invalid start date.";
			return lResponse.toJson();
		}

		if (endDate.equals(Constants.NA)) {
			lResponse.message = "Invalid end date.";
			return lResponse.toJson();
		}

		ArrayList<SFAccountStatement> accountStatements = sfManager.getAccountStatement(loanAccountNumber, startDate,
				endDate);

		JSONArray asArray = new JSONArray();
		for (SFAccountStatement item : accountStatements) {
			asArray.put(item.toJson());
		}

		JSONObject rJson = BasicUtils.getSuccessTemplateObject();
		rJson.put("transactions", asArray);
		rJson.put(Constants.LOAN_ACCOUNT_NUMBER, loanAccountNumber);
		rJson.put(Constants.CRM_ACCOUNT_NUMBER, user.crmAccountNumber);

		return rJson;

	}

	public JSONObject performRegisteration(String requestId, JSONObject bodyObject, String ipAddress) throws Exception {

		User user = new User(bodyObject);

		String deviceType = bodyObject.optString("deviceType", Constants.NA);
		String deviceId = bodyObject.optString("deviceId", Constants.NA);

		var hasConsented = bodyObject.optBoolean("hasConsented", false);

		salesforce.SalesForceManager v1sfManager = new salesforce.SalesForceManager();
		ContactManager contactManager = new ContactManager();

		try {

			JSONObject responseJson = new JSONObject();

			User sfUser = v1sfManager.getSalesforceUser(user.loanAccountNumber);

			if (null == sfUser) {

				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE,
						"Wrong mobile number. Please enter the mobile number that was registered with your loan account.");

				return responseJson;

			}

			User existingUser = userRepo.findUserByMobileNumber(user.mobileNumber);
			User deletedUser = null;

			if (null != existingUser) {

				if (existingUser.isMobileNumberVerified && !existingUser.password.equalsIgnoreCase(Constants.NA)
						&& !existingUser.password.equalsIgnoreCase(Constants.RESET) && !existingUser.isDeleted) {

					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.ACTION, Constants.ActionType.DO_LOGIN.stringValue);
					responseJson.put(Constants.MESSAGE,
							"Account already exist. Please login with your existing credentials.");
					responseJson.put(Constants.USER, existingUser.toJson(true));

					return responseJson;

				} else if (existingUser.isMobileNumberVerified
						&& existingUser.password.equalsIgnoreCase(Constants.RESET)) {

					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.ACTION, Constants.ActionType.RESET.stringValue);
					responseJson.put(Constants.MESSAGE,
							"Account already exist. Password reset required, Please continue to reset");
					responseJson.put(Constants.USER, existingUser.toJson(true));

					return responseJson;

				} else if (existingUser.isDeleted) {
					deletedUser = existingUser;
				} else {

					UserRequest uRequest = fetchOrCreateUserRequest(existingUser, RequestType.REGISTERATION, requestId,
							ipAddress);

					if (null != uRequest) {

						// var responseGenerateOTP =
						// contactManager.generateOTP(existingUser.mobileNumber);

						JSONObject otpJson = contactManager.sendOTP(existingUser.mobileNumber,
								existingUser.countryCode);

						if (existingUser.sessionPasscode.equalsIgnoreCase(Constants.NA)) {

							existingUser.name = sfUser.name;
							existingUser.emailId = sfUser.emailId;
							existingUser.crmAccountNumber = sfUser.crmAccountNumber;

							existingUser.sessionPasscode = BasicUtils.getPasscodeHash(existingUser);

							existingUser.isDeleted = false;
							existingUser.registrationDateTime = DateTimeUtils.getCurrentDateTimeInIST();

							existingUser.hasConsented = hasConsented;

							if (hasConsented)
								existingUser.consentDatetime = DateTimeUtils.getCurrentDateTimeInIST();

							userRepo.saveUser(existingUser);

						}

						// if (responseGenerateOTP.isSuccess) {
						if (null != otpJson && otpJson.getString(Constants.STATUS).equals(Constants.SUCCESS)) {

							responseJson.put(Constants.STATUS, Constants.SUCCESS);
							responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
							responseJson.put(Constants.MESSAGE,
									"Account already exist. But mobile number is not verified or password is not set."
											+ " Continue to complete registration process..");
							responseJson.put(Constants.USER, existingUser.toJson(true));
							responseJson.put(Constants.REQUEST, uRequest.toJson());

						} else
							responseJson = new LocalResponse("Failed to sent OTP.").toJson();

					} else
						responseJson = new LocalResponse("Failed to create user request.").toJson();

					return responseJson;

				}

			}

			/*
			 * User doesn't exist in our DB, Fetch user info from SF and continue
			 */

			SecondaryInfo sInfo = new SecondaryInfo();

			if (null != deletedUser) {
				user = deletedUser;
				sInfo = userRepo.findSecondaryInfoByUserId(user.userId);
				user.password = Constants.RESET;

			} else
				sInfo = new SecondaryInfo();

			if (null != sfUser) {

				if (!sfUser.mobileNumber.equalsIgnoreCase(user.mobileNumber)) {
					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE,
							"Wrong mobile number. Please enter the mobile number that was registered with your loan account.");

				} else {

					user.name = sfUser.name;
					user.emailId = sfUser.emailId;
					user.crmAccountNumber = sfUser.crmAccountNumber;

					user.sessionPasscode = BasicUtils.getPasscodeHash(user);

					user.isDeleted = false;
					user.registrationDateTime = DateTimeUtils.getCurrentDateTimeInIST();

					user.hasConsented = hasConsented;

					if (hasConsented)
						user.consentDatetime = DateTimeUtils.getCurrentDateTimeInIST();

					var isRegistered = userRepo.saveUser(user);

					sInfo.userId = user.userId;
					sInfo.deviceId = deviceId;
					sInfo.deviceType = deviceType;
					sInfo.apnsKey = null;
					sInfo.fcmKey = null;
					sInfo.loginInfo = null;
					sInfo.mobileNumberChangeDatetime = null;
					sInfo.passwordChangeDatetime = null;

					userRepo.saveSecondaryInfo(sInfo);

					if (null == isRegistered) {

						responseJson = new LocalResponse().setError(Errors.OPERATION_FAILED.value).toJson();

					} else {

						// get updated user data from DB
						existingUser = userRepo.findUserByMobileNumber(user.mobileNumber);

						UserRequest uRequest = fetchOrCreateUserRequest(existingUser, RequestType.REGISTERATION,
								requestId, ipAddress);

						if (null != uRequest) {

//							var responseGenerateOTP = contactManager.generateOTP(existingUser.mobileNumber);				
//						      if (responseGenerateOTP.isSuccess) {

							JSONObject otpJson = contactManager.sendOTP(existingUser.mobileNumber,
									existingUser.countryCode);

							if (null != otpJson && otpJson.getString(Constants.STATUS).equals(Constants.SUCCESS)) {

								responseJson.put(Constants.STATUS, Constants.SUCCESS);
								responseJson.put(Constants.MESSAGE, "User was successfully registered.");
								responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
								responseJson.put(Constants.USER, existingUser.toJson(true));
								responseJson.put(Constants.REQUEST, uRequest.toJson());

							} else
								responseJson = new LocalResponse("Failed to sent OTP.").toJson();

						} else
							responseJson = new LocalResponse("Failed to create user request.").toJson();

					}

				}
			} else {

				responseJson = new JSONObject();
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "Loan account number doesn't exist.");

			}

			return responseJson;

		} catch (Exception e) {
			throw e;
		}

	}

	private UserRequest fetchUserRequest(int userId, String requestId, RequestType requestType) throws Exception {

		RequestManager rManager = new RequestManager();
		UserRequest uRequest = null;

		if (BasicUtils.isNotNullOrNA(requestId)) {

			uRequest = rManager.getUserRequest(userId, requestId, requestType);

			if (null != uRequest && uRequest.isOpen() && uRequest.isValid && uRequest.isDateValid()) {

				return uRequest;

			} else
				uRequest = null;

		}

		return uRequest;

	}

	private UserRequest fetchOrCreateUserRequest(User user, RequestType requestType, String requestId, String ipAddress)
			throws Exception {

		UserRequest uRequest = fetchUserRequest(user.userId, requestId, requestType);

		if (null != uRequest) {
			return uRequest;
		} else {
			RequestManager rManager = new RequestManager();

			uRequest = rManager.createNewRequest(user, requestType, ipAddress);

			if (null != uRequest)
				return uRequest;
			else
				throw new Exception("Failed to create new request for: " + requestType);

		}

	}

	public JSONObject verifyMobileNumber(String requestId, int userId, String token, JSONObject bodyObject,
			String ipAddress) throws Exception {

		JSONObject responseJson = new JSONObject();

		try {

			User fetchedUser = appContextProvider.getUserById(userId);

			if (null == fetchedUser) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "User doesn't exist.");
				dbHelper.close();
				return responseJson;
			}

			String mobileNumber = bodyObject.optString("mobileNumber", Constants.NA);
			String countryCode = bodyObject.optString("countryCode", "+91");
			String OTP = bodyObject.optString("OTP", Constants.NA);
			String requestTypeString = bodyObject.optString("requestType", Constants.NA);

			ContactManager contactManager = new ContactManager();

			RequestType requestType = RequestType.get(requestTypeString);

			if (null == requestType) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid request type.");
				return responseJson;
			}

			if (!(requestType == RequestType.REGISTERATION || requestType == RequestType.CHANGE_PASSWORD
					|| requestType == RequestType.FORGOT_PASSWORD || requestType == RequestType.FORCE_RESET_PASSWORD)) {

				return new LocalResponse(false, Constants.DEFAULT_ERROR_MESSAGE,
						"Request type " + requestType.value + " is not allowed.").toJson();

			}

			UserRequest uRequest = fetchUserRequest(userId, requestId, requestType);

			if (null == uRequest) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.ACTION, Constants.ActionType.RETRY.stringValue);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Failed to verify request.");
				responseJson.put(Constants.USER, fetchedUser.toJson(false));
				return responseJson;
			}

			if (!uRequest.token.equals(token)) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid token.");
				return responseJson;

			}

//			var lResponse = contactManager.validateOTP(mobileNumber, OTP);
//			if (lResponse.isSuccess) {
//			if (dbHelper.verifyMobileNumber(fetchedUser)) {

			JSONObject otpJson = contactManager.verifyOTP(mobileNumber, countryCode, OTP);

			if (null != otpJson && otpJson.optString(Constants.STATUS).equals(Constants.SUCCESS)) {

				boolean isSuccess = dbHelper.verifyMobileNumber(fetchedUser);

				if (isSuccess) {

					RequestManager requestManager = new RequestManager();

					uRequest = requestManager.updateUserRequest(fetchedUser, requestId, Constants.OPEN, true,
							ipAddress);

					if (null != uRequest) {
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, Constants.NA);
						responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
						responseJson.put(Constants.USER, fetchedUser.toJson(true));
						responseJson.put(Constants.REQUEST, uRequest.toJson());
					} else {
						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
						responseJson.put(Constants.ERROR, "Failed to update request.");
					}

				} else {
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
					responseJson.put(Constants.ERROR, "Failed to update mobile number verify status in DB.");
				}
			} else {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE,
						otpJson.optString(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE));
				responseJson.put(Constants.ERROR, "Failed to verify OTP.");
			}

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

		dbHelper.close();
		return responseJson;

	}

	public JSONObject addPassword(int userId, String requestId, String token, JSONObject bodyObject, String ipAddress)
			throws Exception {

		String password = bodyObject.optString("password", Constants.NA);
		String requestTypeString = bodyObject.optString("requestType", Constants.NA);

		JSONObject responseJson = new JSONObject();

		try {

			User user = appContextProvider.getUserById(userId);

			if (null == user) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "User doesn't exist.");
				return responseJson;
			}

			RequestType requestType = RequestType.get(requestTypeString);

			if (null == requestType) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid request type.");
				return responseJson;
			}

			if (!(requestType == RequestType.REGISTERATION || requestType == RequestType.CHANGE_PASSWORD
					|| requestType == RequestType.FORGOT_PASSWORD || requestType == RequestType.FORCE_RESET_PASSWORD)) {

				return new LocalResponse(false, Constants.DEFAULT_ERROR_MESSAGE,
						"Request type " + requestType.value + " is not allowed.").toJson();

			}

			UserRequest uRequest = fetchUserRequest(userId, requestId, requestType);

			if (null == uRequest) {
				responseJson.put(Constants.ACTION, Constants.ActionType.RETRY.stringValue);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Failed to verify request.");
				responseJson.put(Constants.USER, user.toJson(false));
				return responseJson;
			}

			if (!uRequest.token.equals(token)) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid token.");
				return responseJson;

			}

			if (user.password.equals(BasicUtils.getTheKey(password))) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE,
						"New password can't be your old password. Please choose different password!.");

				return responseJson;
			}

			user.password = BasicUtils.getTheKey(password);

			if (null != userRepo.saveUser(user)) {

				String action = Constants.NA;
				String message = Constants.DEFAULT_ERROR_MESSAGE;
				if (uRequest.type.equals(RequestType.REGISTERATION.value)) {
					updateRegisterInfoOnSF(user);
					action = Constants.ActionType.CONTINUE.stringValue;
					message = "Your password has been set successfully. Let's take you home.";
				} else {
					action = Constants.ActionType.DO_LOGIN.stringValue;
					message = "Your password has been updated successfully. Please login with your new password";
				}

				RequestManager requestManager = new RequestManager();
				uRequest = requestManager.updateUserRequest(user, requestId, Constants.CLOSED, false, ipAddress);

				if (null != uRequest) {
//					user.sessionPasscode = dbHelper.getUserSessionPasscode(userId);
					user.sessionPasscode = userRepo.updateAndGetSession(user).sessionPasscode;
					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.ACTION, action);
					responseJson.put(Constants.MESSAGE, message);
					responseJson.put(Constants.USER, user.toJson(true));
					responseJson.put(Constants.REQUEST, uRequest.toJson());
				} else {
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
					responseJson.put(Constants.ERROR, "Failed to update user request.");
				}

			} else {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Failed to add password.");
			}

		} catch (Exception e) {

			throw e;
		}

		return responseJson;

	}

	public JSONObject initiateMobileNumberChangeProcess(int userId, String requestId, JSONObject bodyObject,
			String ipAddress) throws Exception {

		JSONObject responseJson = new JSONObject();

		try {

			String oldMobileNumber = bodyObject.optString("oldMobileNumber", Constants.NA);

			User fetchedUser = appContextProvider.getUserByMobileNumber(oldMobileNumber);
			dbHelper.close();

			if (null == fetchedUser) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "No account is associated with given existing mobile number. "
						+ "Please check the current mobile number you entered.");

				return responseJson;
			}

			String newMobileNumber = bodyObject.optString("newMobileNumber", Constants.NA);

			String countryCode = bodyObject.optString("countryCode", "+91");

			String password = bodyObject.optString("password", Constants.NA);

			if (newMobileNumber.equals(oldMobileNumber)) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "New mobile number cannot be same as current mobile number");

				return responseJson;
			}

			if (!fetchedUser.password.equals(BasicUtils.getTheKey(password))) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "Wrong password. Please enter correct password.");
				responseJson.put(Constants.ERROR, Errors.INVALID_PASSWORD.value);

				return responseJson;
			}

			ContactManager contactManager = new ContactManager();

			UserRequest uRequest = fetchOrCreateUserRequest(fetchedUser, RequestType.CHANGE_MOBILE_NUMBER, requestId,
					ipAddress);

			if (null != uRequest) {

//				var responseGenerateOTP = contactManager.generateOTP(newMobileNumber);
//				   if (responseGenerateOTP.isSuccess) {

				JSONObject json = contactManager.sendOTP(newMobileNumber, countryCode);

				if (null != json && json.optString(Constants.STATUS).equals(Constants.SUCCESS)) {

					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
					responseJson.put(Constants.MESSAGE, "OTP send successfully.");
					responseJson.put(Constants.USER, fetchedUser.toJson(true));
					responseJson.put(Constants.REQUEST, uRequest.toJson());

				} else
					responseJson = new LocalResponse("Failed to sent OTP.").toJson();

			} else
				responseJson = new LocalResponse("Failed to create user request.").toJson();

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

		return responseJson;

	}

	public JSONObject verifyAndUpdateNewMobileNumber(int userId, String requestId, String token, JSONObject bodyObject,
			String ipAddress) throws Exception {

		JSONObject responseJson = new JSONObject();

		try {
			User fetchedUser = appContextProvider.getUserById(userId);

			if (null == fetchedUser) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "User doesn't exist.");
				return responseJson;
			}

			String newMobileNumber = bodyObject.optString("newMobileNumber", Constants.NA);
			String countryCode = bodyObject.optString("countryCode", "+91");
			String OTP = bodyObject.optString("OTP", Constants.NA);
			String requestTypeString = bodyObject.optString("requestType", Constants.NA);

			ContactManager contactManager = new ContactManager();

			RequestType requestType = RequestType.get(requestTypeString);

			if (null == requestType) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid request type.");
				return responseJson;
			}

			if (!(requestType == RequestType.CHANGE_MOBILE_NUMBER)) {

				return new LocalResponse(false, Constants.DEFAULT_ERROR_MESSAGE,
						"Request type " + requestType.value + " is not allowed.").toJson();

			}
			UserRequest uRequest = fetchUserRequest(userId, requestId, requestType);

			if (null == uRequest) {

				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.ACTION, Constants.ActionType.RETRY.stringValue);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Failed to verify request.");
				responseJson.put(Constants.USER, fetchedUser.toJson(false));
				return responseJson;

			}

			if (!uRequest.token.equals(token)) {

				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid token.");
				return responseJson;

			}

//			var lResponse = contactManager.validateOTP(newMobileNumber, OTP);
//			if (lResponse.isSuccess) {

			responseJson = contactManager.verifyOTP(newMobileNumber, countryCode, OTP);

			if (null != responseJson && responseJson.getString(Constants.STATUS).equals(Constants.SUCCESS)) {

				salesforce.SalesForceManager sfManager = new salesforce.SalesForceManager();

				boolean updateStatus = sfManager.updateContactMobile(fetchedUser, newMobileNumber);

				LoggerUtils.log("verifyAndUpdateNewMobileNumber - UpdatedSF:" + updateStatus);
				if (updateStatus) {

					fetchedUser.mobileNumber = newMobileNumber;
					fetchedUser.countryCode = countryCode;

					if (null == userRepo.saveUser(fetchedUser)) {

						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
						responseJson.put(Constants.ERROR, "Failed to update mobile number.");

						return responseJson;
					}

					SecondaryInfo sInfo = userRepo.findSecondaryInfoByUserId(fetchedUser.userId);
					sInfo.mobileNumberChangeDatetime = DateTimeUtils.getCurrentDateTimeInGMT();
					var secondaryResp = userRepo.saveSecondaryInfo(sInfo);

					if (!secondaryResp) {

						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
						responseJson.put(Constants.ERROR, "Failed to update mobile number.");

						return responseJson;
					}

					RequestManager requestManager = new RequestManager();
					uRequest = requestManager.updateUserRequest(fetchedUser, requestId, Constants.CLOSED, false,
							ipAddress);

					if (null != uRequest) {

						fetchedUser.sessionPasscode = userRepo.updateAndGetSession(fetchedUser).sessionPasscode;
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, "Mobile number has been successfully updated.");
						responseJson.put(Constants.USER, fetchedUser.toJson(true));
						responseJson.put(Constants.REQUEST, uRequest.toJson());

					} else {
						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
						responseJson.put(Constants.ERROR, "Failed to update request.");
					}

				} else {
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
					responseJson.put(Constants.MESSAGE, "Failed to update mobile number.");
				}

			} else {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
//				responseJson.put(Constants.MESSAGE, lResponse.message);
				responseJson.put(Constants.ERROR, "Failed to verify OTP.");
			}

		} catch (Exception e) {
			throw e;
		}

		return responseJson;

	}

	public JSONObject initiateForgot(int userId, String requestId, JSONObject bodyObject, String ipAddress)
			throws Exception {

		var token = bodyObject.optString("captchaToken", Constants.NA);

		if (!Recaptcha.isValid(token)) {
			return new LocalResponse().toJson();

		}

		return initiateForgotOrChangePassword(userId, requestId, bodyObject, ipAddress);

	}

	public JSONObject initiateForgotOrChangePassword(int userId, String requestId, JSONObject bodyObject,
			String ipAddress) throws Exception {

		String mobileNumber = bodyObject.optString("mobileNumber", Constants.NA);
		String countryCode = bodyObject.optString("countryCode", Constants.NA);
		String requestTypeString = bodyObject.optString("requestType", Constants.NA);
		JSONObject responseJson = new JSONObject();

		try {

			User fetchedUser = appContextProvider.getUserByMobileNumber(mobileNumber);

			if (null == fetchedUser) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "Invalid credential.");
				return responseJson;
			}

			else {

				if (fetchedUser.isDeleted) {

					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, "Invalid credential.");
					return responseJson;

				}

				ArrayList<UserRequest> requestCount = userRepo.getOpenRequests(fetchedUser.userId);

				if (requestCount.size() >= 3) {
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE,
							"You have already requested 3 OTPs, please wait an hour before trying again");
					return responseJson;

				}

				RequestType requestType = RequestType.get(requestTypeString);

				if (null == requestType) {
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
					responseJson.put(Constants.ERROR, "Invalid request type.");
					return responseJson;
				}

				if (!(requestType == RequestType.CHANGE_PASSWORD || requestType == RequestType.FORGOT_PASSWORD
						|| requestType == RequestType.FORCE_RESET_PASSWORD)) {

					return new LocalResponse(false, Constants.DEFAULT_ERROR_MESSAGE,
							"Request type " + requestType.value + " is not allowed.").toJson();

				}

				UserRequest uRequest = fetchOrCreateUserRequest(fetchedUser, requestType, requestId, ipAddress);

				if (null != uRequest) {

					ContactManager contactManager = new ContactManager();

//					var responseGenerateOTP = contactManager.generateOTP(mobileNumber);
//					if (responseGenerateOTP.isSuccess) {

					JSONObject json = contactManager.sendOTP(mobileNumber, countryCode);

					if (null != json && json.optString(Constants.STATUS).equals(Constants.SUCCESS)) {
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
						responseJson.put(Constants.MESSAGE, "OTP send successfully.");
						responseJson.put(Constants.USER, fetchedUser.toJson(true));
						responseJson.put(Constants.REQUEST, uRequest.toJson());

					} else
						responseJson = new LocalResponse("Failed to sent OTP.").toJson();

				} else
					responseJson = new LocalResponse("Failed to create user request.").toJson();

			}
		} catch (Exception e) {
			throw e;
		}

		return responseJson;
	}

	public JSONObject resendOTP(int userId, String requestId, String token, JSONObject bodyObject) throws Exception {

		JSONObject responseJson = new JSONObject();

		try {

			User fetchedUser = appContextProvider.getUserById(userId);

			if (null == fetchedUser) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "User doesn't exist.");
				return responseJson;
			}

			ArrayList<UserRequest> requestCount = userRepo.getOpenRequests(fetchedUser.userId);

			if (requestCount.size() >= 3) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE,
						"You have already requested 3 OTPs, please wait an hour before trying again");
				return responseJson;

			}

			String mobileNumber = bodyObject.optString("mobileNumber", Constants.NA);
			String countryCode = bodyObject.optString("countryCode", "+91");
			String requestTypeString = bodyObject.optString("requestType", Constants.NA);

			RequestType requestType = RequestType.get(requestTypeString);

			if (null == requestType) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid request type.");
				return responseJson;
			}

			UserRequest uRequest = fetchUserRequest(userId, requestId, requestType);

			if (null == uRequest) {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.ACTION, Constants.ActionType.RETRY.stringValue);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Failed to verify request.");
				responseJson.put(Constants.USER, fetchedUser.toJson(false));
				return responseJson;

			}

			if (!uRequest.token.equals(token)) {

				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				responseJson.put(Constants.ERROR, "Invalid token.");
				return responseJson;

			}

			ContactManager contactManager = new ContactManager();
								
//			var responseGenerateOTP = contactManager.generateOTP(mobileNumber);
//			if (responseGenerateOTP.isSuccess) {

			responseJson = contactManager.resendOTP(mobileNumber, countryCode);

			if (null != responseJson && responseJson.optString(Constants.STATUS).equals(Constants.SUCCESS)) {

				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.MESSAGE, "OTP send successfully.");
				responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
				responseJson.put(Constants.USER, fetchedUser.toJson(true));
				responseJson.put(Constants.REQUEST, uRequest.toJson());

			} else
				responseJson.put(Constants.ERROR, "Failed to sent OTP");

			// responseJson = new LocalResponse("Failed to sent OTP.").toJson();

		} catch (Exception e) {
			throw e;
		}

		return responseJson;
	}

	public JSONObject addServiceRequest(int userId, JSONObject bodyObject) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser) {
			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.STATUS, Constants.FAILURE);
			responseJson.put(Constants.MESSAGE, "User doesn't exist.");
			return responseJson;
		}

		String password = bodyObject.optString("password", Constants.NA);

		if (!fetchedUser.password.equals(BasicUtils.getTheKey(password))) {
			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.STATUS, Constants.FAILURE);
			responseJson.put(Constants.MESSAGE, "Wrong password. Please enter correct password.");
			responseJson.put(Constants.ERROR, Errors.INVALID_PASSWORD.value);

			return responseJson;
		}

		JSONObject serviceRequestJson = bodyObject.getJSONObject("serviceRequestParams");

		return new dao.UserManager().addServiceRequest(fetchedUser, serviceRequestJson);

	}

	public Response getPromo() throws Exception {

		try {

			ArrayList<Promo> activePromos = new PromoRepository().getActivePromos();

			ArrayList<Blog> activeBlogs = new BlogRepository().getActiveBlogs();
			Collections.shuffle(activeBlogs);

			JSONObject responseJson = new JSONObject();
			responseJson.put("promos", new JSONArray(gson.toJson(activePromos)));
			responseJson.put("blogs", new JSONArray(gson.toJson(activeBlogs)));
			responseJson.put("baseUrl", new AmazonClient().getBaseUrl(S3BucketPath.RESOURCE_PROMOTION));

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response getUserDetailsToCSV() throws Exception {

		try {

			String fileName = "RegisteredUsers" + DateTimeUtils.getCurrentDateTimeInIST() + ".csv";
			String fileLocation = Constants.IS_PRODUCTION ? UPLOAD_FILE_SERVER : UPLOAD_FILE_LOCAL_SERVER;
			String filePath = fileLocation + fileName;

			File file = new File(filePath);

			dbHelper.getDetailsToCSVFile(filePath);
			dbHelper.close();

			String bodyMessage = "Hi Team,\n" + "Please find the below attached file with the registered user data\n\n"
					+ "This is an automatic email generated.\n" + "Please do not reply to this email.";

			MailUtils.getInstance().sendDefaultMailAttach(ContentType.TEXT_CSV, "Registered Users Data", fileName,

					filePath, bodyMessage, "ranan.rodrigues@homefirstindia.com", "sanjay.jaiswar@homefirstindia.com",
					"sandya.ramakrishna@homefirstindia.com", "service.ninjas@homefirstindia.com",
					"seerthi.nadar@homefirstindia.com", "neelima.verma@homefirstindia.com");

			file.delete();

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Customer information successfully.");

			return new OneResponse().getSuccessResponse(responseJson);
		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

	}

	public void updateRegisterInfoOnSF(User user) throws Exception {

		final Timer timer = new Timer(true);

		timer.schedule(new TimerTask() {
			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (sfManager.updateRegisteredInfo(user)) {
							dbHelper.updateUserIsRegistered(user);
							dbHelper.close();
							timer.cancel();

						} else
							count++;

					} catch (Exception e) {
						dbHelper.close();
						LoggerUtils.log("Error while updating Registered Info of applicant : " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Task rescheduled, Iteration: " + count);
					}

				} else {

					dbHelper.close();
					timer.cancel();
					LoggerUtils.log("Time's up! Failed to update Registered Info  of Applicant.");

				}

			}
		}, 10000);
	}

	public void updateLoginInfoOnSF(User user) throws Exception {

		final Timer timer = new Timer(true);

		timer.schedule(new TimerTask() {
			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (sfManager.updateLoginInfo(user)) {
							if (!user.isRegistrationMarkedOnSF) {
//								LoggerUtils.log("Updating User in DB");

								user.isRegistrationMarkedOnSF = true;
								userRepo.saveUser(user);

							}

							timer.cancel();
						} else
							count++;

					} catch (Exception e) {
						LoggerUtils.log("Error while updating Login Info of applicant : " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Task rescheduled, Iteration: " + count);
					}

				} else {
					timer.cancel();
					LoggerUtils.log("Time's up! Failed to update Login Info of Applicant.");

				}

			}
		}, 10000);
	}

	public Response performAutoLogin(int userId, JSONObject bodyObject, String ipAddress, String sessionPasscode)
			throws Exception {

		JSONObject responseJson = new JSONObject();

//		if (!BasicUtils.isSignMatch(bodyObject)) {
//			return new OneResponse()
//					.getFailureResponse(new LocalResponse().setMessage("Invalid Request, Please Try Again")
//							.setError(Errors.INVALID_DATA.value).setAction(Actions.FIX_RETRY.value).toJson());
//		}

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser) {
			return new OneResponse().getFailureResponse(new LocalResponse()
					.setMessage("No account associated with this mobile number. "
							+ "Please do the registration process or re-check entered mobile number.")
					.setAction(Actions.RETRY.value).toJson());

		} else {

			if (fetchedUser.isDeleted) {

				return new OneResponse().getFailureResponse(new LocalResponse()
						.setMessage("No account associated with this mobile number. "
								+ "Please do the registration process or re-check entered mobile number.")
						.setAction(Actions.RETRY.value).toJson());

			} else if (fetchedUser.password.equals(Constants.RESET)) {
				return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.RESET).toJson());

			} else {

				// LoggerUtils.log(sessionPasscode + "|" + fetchedUser.sessionPasscode);
				if (fetchedUser.sessionPasscode.equalsIgnoreCase(sessionPasscode)) {

					LoginInfo lInfo = gson.fromJson(bodyObject.toString(), LoginInfo.class);
					lInfo.ipAddress = ipAddress;
					lInfo.userId = userId;
					lInfo.loginDatetime = DateTimeUtils.getCurrentDateTimeInIST();

					commonRepo.insertLoginInfo(lInfo);

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.MESSAGE, Constants.NA);

					updateLoginInfoOnSF(fetchedUser);

					boolean requireSessionPass = bodyObject.optBoolean("requireSessionPass", true);
					if (!requireSessionPass) {

						User fUser = userRepo.updateAndGetSession(fetchedUser);
						responseJson.put(Constants.SESSION_PASSCODE, fUser.sessionPasscode);
						responseJson.put("user", fetchedUser.toJson(true));

					} else {
						responseJson.put("userId", fetchedUser.userId);
					}

					return new OneResponse().getSuccessResponse(responseJson);

				} else {

					return new OneResponse()
							.getFailureResponse(new LocalResponse().setMessage(Actions.DO_LOGIN.value).toJson());

				}

			}

		}

	}

	public Response addSitePhotograph(int userId, JSONObject bodyObject) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser)
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("No user found for this id")
					.setAction(Actions.RETRY.value).toJson());

		LocalResponse spResponse = hfoManager.addSitePhotograph(bodyObject);

		if (!spResponse.isSuccess) {

			LoggerUtils.log("addSitePhotograph - Failed to add Site Photograph for userId " + userId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		LoggerUtils.log("addSitePhotograph - Successfully to added Site Photograph for userId " + userId);

		JSONObject hfoJson = new JSONObject(spResponse.message);

		return new OneResponse().getSuccessResponse(hfoJson);

	}

	public Response getSitePhotographList(int userId) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser)
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("No user found for this id")
					.setAction(Actions.RETRY.value).toJson());

		JSONObject requestObject = new JSONObject();
		requestObject.put(Constants.USER_ID, fetchedUser.userId);
		requestObject.put("source", Constants.SITE_PHOTO_SOURCE);
		LocalResponse spResponse = hfoManager.getSitePhotographList(requestObject);

		if (!spResponse.isSuccess) {

			LoggerUtils.log("getSitePhotographList - Failed to get Site Photograph for userId " + userId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		LoggerUtils.log("getSitePhotographList - Success for get Site Photograph for userId " + userId);

		JSONObject hfoJson = new JSONObject(spResponse.message);

		return new OneResponse().getSuccessResponse(hfoJson);

	}

	public Response getSitePhotographUrl(int userId, JSONObject bodyObject) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser)
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("No user found for this id")
					.setAction(Actions.RETRY.value).toJson());

		LocalResponse spResponse = hfoManager.getSitePhotographUrl(bodyObject);

		if (!spResponse.isSuccess) {

			LoggerUtils.log("getSitePhotographUrl - Failed to get Site Photograph for userId " + userId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		LoggerUtils.log("getSitePhotographUrl - Success for get Site Photograph for userId " + userId);

		JSONObject hfoJson = new JSONObject(spResponse.message);

		return new OneResponse().getSuccessResponse(hfoJson);

	}

	public Response checkRepriceEligibility(int userId) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		JSONObject hfoJson = new JSONObject();

		if (null == fetchedUser) {
			hfoJson.put("isEligible", false);
			return new OneResponse().getSuccessResponse(hfoJson);
		}

		var roi = roiRepo.getCustomerDetailsFromUserId(fetchedUser.userId);

		if (roi.isEmpty()) {
			hfoJson.put("isEligible", false);
			LoggerUtils.log("checkRepriceEligibility - User not found for ROI Reprice " + userId);
			return new OneResponse().getSuccessResponse(hfoJson);

		} else {

			LoggerUtils.log("checkRepriceEligibility - User eligible for reprice with userId " + userId);

			hfoJson.put("isEligible", true);
			hfoJson.put("repriceData", new JSONArray(gson.toJson(roi)));

			return new OneResponse().getSuccessResponse(hfoJson);
		}

	}

	public Response processRepriceEligibility() throws Exception {

		var totalCount = 0;

		var currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();
		LoggerUtils.log("processRepriceEligibility - Start of Reprice Data Processing :" + currentDateTime);
		var roiData = roiRepo.getAllUnProccessedRepriceData();

		if (roiData.isEmpty()) {
			LoggerUtils.log("processRepriceEligibility - No data to process" + roiData.toString());
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.RESOURCE_NOT_FOUND.value)
					.setMessage("No Data to process").setAction(Actions.CANCEL.value).toJson());

		}

		List<List<ROIReprice>> subSets = ListUtils.partition(roiData, 100);

		for (List<ROIReprice> roi : subSets) {

			// LoggerUtils.log("Size:" +roi.size());

			var laiList = roi.stream().map(a -> String.valueOf(a.loanAccountNumber))
					.collect(Collectors.joining("','", "'", "'"));

			var crmAccount = sfManager.getAllUserCrmAcc(laiList);
			totalCount += roi.size();
			if (crmAccount.isEmpty()) {
				LoggerUtils.log("processRepriceEligibility - No data found on SF");
				return new OneResponse()
						.getFailureResponse(new LocalResponse().setError(Errors.RESOURCE_NOT_FOUND.value)
								.setMessage("No Data to process").setAction(Actions.CANCEL.value).toJson());
			}

			var roiList = new ArrayList<ROIReprice>();
			crmAccount.forEach(clContract -> {

				var fetchedUser = userRepo.findUserByCrmAcc(clContract.crmAccountNumber);

				if (null != fetchedUser) {

					var matchROI = roi.stream().filter(p -> p.loanAccountNumber.equals((clContract.name))).findFirst();

					if (!matchROI.isEmpty()) {

						matchROI.get().isEligible = true;
						matchROI.get().updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						matchROI.get().userId = fetchedUser.userId;
						if (!BasicUtils.isNotNullOrNA(matchROI.get().loanType)) {
							var lp = lendingProdRepo.findProductById(clContract.product);
							matchROI.get().loanType = lp.name;
						}
						matchROI.get().paymentAmount = ((Double.parseDouble(clContract.principalRemaining))) / 100;

						if (!BasicUtils.isNotNullOrNA(matchROI.get().eligibleDatetime))
							matchROI.get().eligibleDatetime = DateTimeUtils.getDateTimeAddingMinutes(20160,
									DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeZone.IST);

						roiList.add(matchROI.get());

					}
				}

			});

			if (!roiRepo.batchUpdateROI(roiList)) {
				return new OneResponse().getDefaultFailureResponse();

			}

		}
		currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();
		LoggerUtils.log("processRepriceEligibility - End of Reprice Data Processing :" + currentDateTime);
		return new OneResponse().getSuccessResponse(new LocalResponse()
				.setMessage("ROI Repricing Data Processing Completed. Total = " + totalCount).toJson());

	}

	public Response finalizeReprice(int userId, JSONObject bodyObject) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser)
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("No user found for this id")
					.setAction(Actions.RETRY.value).toJson());

		var paymentId = bodyObject.optInt("paymentId", -1);
		var repriceId = bodyObject.optString("repriceId", Constants.NA);

		var repriceData = roiRepo.findRepricetById(repriceId);

		if (null == repriceData || paymentId == -1) {

			LoggerUtils.log("finalizeReprice - Invalid data while updating " + repriceId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		repriceData.paymentId = paymentId;
		repriceData.isPaid = true;
		repriceData.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

		if (null == roiRepo.saveROIReprice(repriceData)) {

			LoggerUtils.log("finalizeReprice - Failed to save RepriceData for " + userId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		LoggerUtils.log("finalizeReprice - Successfully to Saved RepriceData " + userId);

		return new OneResponse().getSuccessResponse(
				new LocalResponse().setMessage("Reprice Data successfully updated").setStatus(true).toJson());

	}

	public Response processFailedReprice() {

		var roi = roiRepo.getAllPendingRepriceData();

		if (roi.isEmpty()) {
			LoggerUtils.log("processFailedReprice - No data to process" + roi.toString());
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.RESOURCE_NOT_FOUND.value)
					.setMessage("No Data to process").setAction(Actions.CANCEL.value).toJson());

		}

		var updatedList = new ArrayList<ROIReprice>();
		roi.forEach(r -> {

			var payment = paymentRepo.getRepricePayment(r);
			if (!payment.isEmpty()) {
				r.paymentId = payment.get(0).id;
				r.isPaid = true;
				r.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
				updatedList.add(r);

			} else {
				LoggerUtils.log("processFailedReprice - No payment found" + r.loanAccountNumber);

			}

		});

		if (!roiRepo.batchUpdateROI(updatedList)) {
			return new OneResponse().getDefaultFailureResponse();

		} else {

			var currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();
			LoggerUtils.log("processFailedReprice - End of Failed Reprice Data Processing :" + currentDateTime);
			return new OneResponse().getSuccessResponse(
					new LocalResponse().setMessage("ROI Repricing Failed Data Processing Completed.").toJson());

		}

	}

	public Response sendRepriceCSV() throws Exception {

		var roi = roiRepo.getAllPaidRepriceData();

		if (roi.isEmpty()) {
			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "No Data Present to email");

			return new OneResponse().getSuccessResponse(responseJson);
		}

		var fileName = "RepriceROI" + DateTimeUtils.getCurrentDateTimeInIST() + ".csv";
		var fileLocation = Constants.IS_PRODUCTION ? UPLOAD_FILE_SERVER : UPLOAD_FILE_LOCAL_SERVER;
		var filePath = fileLocation + fileName;

		var file = new File(filePath);
		var writer = new FileWriter(file);

		var sbc = new StatefulBeanToCsvBuilder<ROIReprice>(writer).withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();

		sbc.write(roi);
		writer.flush();

		String bodyMessage = "Hi Team,\n" + "Please find the below attached file with the Reprice ROI Payment data\n\n"
				+ "This is an automatic email generated.\n" + "Please do not reply to this email.";

		if (Constants.IS_PRODUCTION) {
			MailUtils.getInstance().sendDefaultMailAttach(ContentType.TEXT_CSV, "Reprice ROI Payment Data", fileName,
					filePath, bodyMessage, "ranan.rodrigues@homefirstindia.com", "sanjay.jaiswar@homefirstindia.com",
					"muthumari.m@homefirstindia.com", "neelima.verma@homefirstindia.com");
		} else {
			MailUtils.getInstance().sendDefaultMailAttach(ContentType.TEXT_CSV, "Reprice ROI Payment Data", fileName,
					filePath, bodyMessage, "ranan.rodrigues@homefirstindia.com");
		}

		file.delete();

		JSONObject responseJson = new JSONObject();
		responseJson.put(Constants.MESSAGE, "RepriceRoiData Sent successfully.");

		return new OneResponse().getSuccessResponse(responseJson);

	}

	public Response deleteAccount(int userId, JSONObject bodyObject) throws Exception {

		final var fetchedUser = userRepo.findUserByUserId(userId);

		var delReq = gson.fromJson(bodyObject.toString(), DeleteAccountRequest.class);

		delReq.userId = userId;

		if (null == commonRepo.saveDeleteAccountRequest(delReq)) {

			LoggerUtils.log("deleteAccount - Failed to deleteAccount for " + userId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		fetchedUser.isDeleted = true;
		fetchedUser.deleteDatetime = DateTimeUtils.getCurrentDateTimeInIST();
		fetchedUser.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

		if (null == userRepo.saveUser(fetchedUser)) {

			LoggerUtils.log("deleteAccount - Failed to save user for " + userId);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		return new OneResponse().getSuccessResponse(
				new LocalResponse().setMessage("Account successfully deleted").setStatus(true).toJson());

	}

	public Response addNotificationToken(int userId, String body) throws Exception {

		final var nToken = gson.fromJson(body, UserNotificationToken.class);
		nToken.notificationService = NotificationServiceType.FIREBASE.value;

		return addUpdatedNotificationToken(userId, nToken);

	}

	public Response addApnsToken(int userId, String body) throws Exception {

		final var nToken = gson.fromJson(body, UserNotificationToken.class);
		nToken.notificationService = NotificationServiceType.APNS.value;

		return addUpdatedNotificationToken(userId, nToken);

	}

	private Response addUpdatedNotificationToken(int userId, UserNotificationToken nToken) throws Exception {

		final var user = userRepo.findUserByUserId(userId);
		nToken.userId = user.userId;

		final var eToken = notificationRepo().findUserNotificationTokenByNotificationKey(nToken.notificationKey,
				user.userId);

		if (null != eToken) {

			printLog("addNotificationToken - Token already exists. Updating..");
			eToken.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

			if (notificationRepo().saveUserNotificationToken(eToken))
				printLog("addNotificationToken - Existing token updated successfully.");
			else
				log("addNotificationToken - Failed to update existing token.");

			return new OneResponse().getSuccessResponse(
					new JSONObject().put(Constants.MESSAGE, "Token updated.").put(Constants.STATUS, Constants.SUCCESS));

		}

		printLog("addNotificationToken - Adding new token..");

		if (!notificationRepo().saveUserNotificationToken(nToken)) {

			log("addNotificationToken - Failed to add new token.");
			return new OneResponse().errorResponse(null);

		}

		printLog("addNotificationToken - New token added successfully.");

		return new OneResponse().getSuccessResponse(
				new JSONObject().put(Constants.MESSAGE, "Token added.").put(Constants.STATUS, Constants.SUCCESS));

	}

}
