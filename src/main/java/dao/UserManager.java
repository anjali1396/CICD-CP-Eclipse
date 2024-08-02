package dao;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import helper.AppContextProvider;
import models.ServiceRequest;
import models.User;
import models.payment.Payment;
import salesforce.SalesForceManager;
import utils.BasicUtils;
import utils.Constants;
import utils.DatabaseHelper;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.ProptertyUtils;
import v2.managers.AmazonClient;
import v2.managers.AmazonClient.S3BucketPath;
import v2.managers.ContactManager;

public class UserManager {

	public static final String UPLOAD_FILE_SERVER = "/var/www/images/profile_picture/";
	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/appledeveloper/var/www/images/profile_picture/";
	// public static final String UPLOAD_FILE_LOCAL_SERVER =
	// "/home/sarika/Documents/repo/HFFCCustomerPortal_Images/";

	private SalesForceManager sfManager = new SalesForceManager();
	private AppContextProvider appContextProvider = new AppContextProvider();

	private final UserRepository userRepo;
	private AmazonClient _amazonS3Client = null;

	public UserManager() {
		userRepo = new UserRepository();
	}

	private AmazonClient s3Client() throws Exception {
		if (null == _amazonS3Client)
			_amazonS3Client = new AmazonClient();
		return _amazonS3Client;
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

	public boolean verifyUser(int userId, String passcode) {

		if (!passcode.equals(Constants.NA)) {
			DatabaseHelper dbHelper = new DatabaseHelper();

			String dPasscode = "";
			try {
				dPasscode = dbHelper.getUserSessionPasscode(userId);
				dbHelper.close();
			} catch (Exception e) {
				dbHelper.close();
				System.out.println("Error while verifying user: " + e.toString());
				e.printStackTrace();
			}
			if (dPasscode.equals(passcode))
				return true;
		}

		return false;
	}

	/**
	 * @deprecated use the v2/managers/UserManager.performLogin()
	 */
	@Deprecated
	public JSONObject performLogin(int userId, JSONObject bodyObject) throws Exception {

		User user = new User(bodyObject);
		String deviceType = bodyObject.optString("deviceType", Constants.NA);
		String deviceId = bodyObject.optString("deviceId", Constants.NA);

		JSONObject responseJson = new JSONObject();

		try {

			User fetchedUser = appContextProvider.getUserByMobileNumber(user.mobileNumber);

			if (null == fetchedUser) {

				responseJson = new JSONObject();
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "No account associated with this mobile number. "
						+ "Please do the registration process or re-check entered mobile number.");

			} else {

				if (fetchedUser.password.equals(Constants.NA)) {

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
							appContextProvider.addLoginInfo(fetchedUser.userId, deviceId, deviceType);
						} catch (Exception e) {
							System.out.println("Error while adding login info: " + e.toString());
						}

						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, Constants.NA);

						boolean requireSessionPass = bodyObject.optBoolean("requireSessionPass", true);						
						
						if (!requireSessionPass) {
//							responseJson.put(Constants.SESSION_PASSCODE,							
//									dbHelper.getUserSessionPasscode(fetchedUser.userId));
							responseJson.put(Constants.SESSION_PASSCODE, userRepo.updateAndGetSession(fetchedUser));							
							responseJson.put("user", fetchedUser.toJson(true));
						} else {
							responseJson.put("userId", fetchedUser.userId);
						}

					} else {

						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, "Incorrect password!");

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

	/**
	 * @deprecated use the v2/managers/UserManager.performRegisteration()
	 */
	@Deprecated
	public JSONObject performRegisteration(JSONObject bodyObject) throws Exception {

		User user = new User(bodyObject);
		String deviceType = bodyObject.optString("deviceType", Constants.NA);
		String deviceId = bodyObject.optString("deviceId", Constants.NA);

		JSONObject responseJson = new JSONObject();
		DatabaseHelper databaseHelper = new DatabaseHelper();

		try {

			User existingUser = databaseHelper.getUserIfExist(user.mobileNumber, user.loanAccountNumber);

			if (null != existingUser) {

				if (existingUser.isMobileNumberVerified && !existingUser.password.equalsIgnoreCase(Constants.NA)
						&& !existingUser.password.equals(Constants.RESET)) {

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.FAILURE);
					responseJson.put(Constants.MESSAGE,
							"Account already exist. Please login with your existing credentials.");
					responseJson.put("user", existingUser.toJson(true));

					databaseHelper.close();
					return responseJson;

				} else if (existingUser.isMobileNumberVerified && existingUser.password.equals(Constants.RESET)) {

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.MESSAGE, "Account already exist. Please continue to set password.");
					responseJson.put(Constants.SESSION_PASSCODE,
							databaseHelper.getUserSessionPasscode(existingUser.userId));
					responseJson.put("user", existingUser.toJson(true));

					databaseHelper.close();
					return responseJson;

				} else {

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.MESSAGE,
							"Account already exist. But mobile number is not verified or password is not set."
									+ " Continue to complete registration process.");
					responseJson.put(Constants.SESSION_PASSCODE,
							databaseHelper.getUserSessionPasscode(existingUser.userId));
					responseJson.put("user", existingUser.toJson(true));

					databaseHelper.close();
					return responseJson;

				}

			}

			User sfUser = sfManager.getSalesforceUser(user.loanAccountNumber);

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

					boolean isRegistered = databaseHelper.registerUser(user, deviceType, deviceId);

					if (!isRegistered) {
						responseJson = new LocalResponse().toJson();
					} else {

						existingUser = databaseHelper.getUserIfExist(user.mobileNumber, user.loanAccountNumber);
						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, "User was successfully registered.");
						responseJson.put(Constants.SESSION_PASSCODE,
								databaseHelper.getUserSessionPasscode(existingUser.userId));
						responseJson.put("user", existingUser.toJson(true));

					}

				}

			} else {

				responseJson = new JSONObject();
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "Loan account number doesn't exist.");

			}

			databaseHelper.close();

		} catch (Exception e) {
			databaseHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseJson;
	}

	@Deprecated
	public JSONObject checkAccount(JSONObject bodyObject) throws Exception {

		String mobileNumber = bodyObject.getString("mobileNumber");

		JSONObject responseObject = new JSONObject();

		try {

			User fetchedUser = appContextProvider.getUserByMobileNumber(mobileNumber);

			if (null != fetchedUser) {
				responseObject.put(Constants.STATUS, Constants.SUCCESS);
				responseObject.put(Constants.MESSAGE, Constants.NA);
				responseObject.put("user", fetchedUser.toJson(true));
			} else {
				responseObject.put(Constants.STATUS, Constants.FAILURE);
				responseObject.put(Constants.MESSAGE, "No account is associated with this mobile number.");
			}

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseObject;

	}

	/**
	 * @deprecated use the v2/managers/UserManager.verifyMobileNumber()
	 */
	public JSONObject varifyMobileNumber(int userId, JSONObject bodyObject) throws Exception {

		String OTP = bodyObject.getString("OTP");
		String mobileNumber = bodyObject.getString("mobileNumber");
		String countryCode = bodyObject.optString("countryCode", "+91");

		ContactManager cManager = new ContactManager();
		JSONObject otpJson = cManager.verifyOTP(mobileNumber, countryCode, OTP);

		if (otpJson.getString(Constants.STATUS).equals(Constants.SUCCESS)) {

			DatabaseHelper databaseHelper = new DatabaseHelper();
			try {

				boolean isSuccess = databaseHelper.verifyMobileNumber(userId);
				databaseHelper.close();

				if (!isSuccess) {
					otpJson = new JSONObject();
					otpJson.put(Constants.STATUS, Constants.FAILURE);
					otpJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
				}

			} catch (Exception e) {
				databaseHelper.close();
				LoggerUtils.log("Error: " + e.toString());
				e.printStackTrace();
				throw e;
			}
		}

		return otpJson;

	}

	/**
	 * @deprecated use the v2/managers/UserManager.resendOTP()
	 */
	@Deprecated
	public JSONObject resendOTP(String mobileNumber, String countryCode) throws Exception {

		JSONObject responseObject = new JSONObject();

		ContactManager cManager = new ContactManager();
		JSONObject optJson = cManager.resendOTP(mobileNumber, countryCode);

		if (null != optJson)
			responseObject = optJson;
		else
			responseObject = new LocalResponse().toJson();

		return responseObject;

	}

	@Deprecated
	public JSONObject generateOTP(String mobileNumber, String countryCode) throws Exception {

		JSONObject responseObject = new JSONObject();

		try {

			User user = appContextProvider.getUserByMobileNumber(mobileNumber);

			if (null != user) {

				ContactManager cManager = new ContactManager();
				JSONObject optJson = cManager.sendOTP(mobileNumber, countryCode);

				if (null != optJson) {

					responseObject = optJson;
					responseObject.put(Constants.STATUS, Constants.SUCCESS);
					responseObject.put(Constants.MESSAGE, Constants.NA);
					responseObject.put("mobileNumber", mobileNumber);
					responseObject.put("countryCode", countryCode);
					responseObject.put("userId", user.userId);

				} else {
					responseObject = new LocalResponse().toJson();
				}

			} else {
				responseObject.put(Constants.STATUS, Constants.FAILURE);
				responseObject.put(Constants.MESSAGE, "No account associated with this mobile number.");
			}

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseObject;

	}

	/**
	 * @deprecated use the
	 *             v2/managers/UserManager.initiateMobileNumberChangeProcess()
	 */
	@Deprecated
	public JSONObject initiateMobileNumberChangeProccess(User user, String newMobileNumber, String countryCode)
			throws Exception {

		JSONObject responseJson = new JSONObject();

		User fetchedUser = null;

		try {

			fetchedUser = appContextProvider.getUserByMobileNumber(user.mobileNumber);

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		if (null != fetchedUser) {

			ContactManager cManager = new ContactManager();
			JSONObject cJson = cManager.sendOTP(newMobileNumber, countryCode);

			if (null != cJson) {
				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.MESSAGE, "OTP sent successfully to: " + countryCode + newMobileNumber);
			} else {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE,
						"Something went wrong. Please check the new mobile number you entered.");
			}

		} else {
			responseJson.put(Constants.STATUS, Constants.FAILURE);
			responseJson.put(Constants.MESSAGE, "No account is associated with given existing mobile number. "
					+ "Please check the current mobile number you entered.");
		}

		return responseJson;

	}

	/**
	 * @deprecated use the v2/managers/UserManager.verifyAndUpdateNewMobileNumber()
	 */
	@Deprecated
	public JSONObject verifyAndUpdateNewMobileNumber(User user, String newMobileNumber, String countryCode, String OTP)
			throws Exception {

		JSONObject responseJson = new JSONObject();

		ContactManager cManager = new ContactManager();
		JSONObject cJson = cManager.verifyOTP(newMobileNumber, countryCode, OTP);

		if (cJson.getString(Constants.STATUS).equals(Constants.SUCCESS)) {

			LoggerUtils.log("OTP for new mobile number was verified successfully");

			SalesForceManager sfManager = new SalesForceManager();
			boolean updateStatus = sfManager.updateContactInformation(user, newMobileNumber);
			if (updateStatus) {
				DatabaseHelper dbHelper = new DatabaseHelper();
				try {

					Boolean dbSuccess = dbHelper.updateMobileNumber(user, newMobileNumber, countryCode);
					dbHelper.close();

					if (dbSuccess) {
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, "Mobile number updated successfully.");
					} else {
						// revert back the mobile number change on SF
						sfManager.updateContactInformation(user, user.mobileNumber);

						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
					}

				} catch (Exception e) {
					dbHelper.close();
					LoggerUtils.log("Error: " + e.toString());
					e.printStackTrace();
					throw e;
				}

			} else {
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
			}

		} else {
			responseJson = cJson;
		}

		return responseJson;

	}

	/**
	 * @deprecated use the v2/managers/UserManager.initiateForgotOrChangePassword()
	 */
	@Deprecated
	public JSONObject updatePassword(int userId, String password) throws Exception {

		LocalResponse localResponse = new LocalResponse();

		try {

			User existingUser = appContextProvider.getUserById(userId);

			if (existingUser.password.equals(BasicUtils.getTheKey(password))) {

				localResponse.isSuccess = false;
				localResponse.message = "New password can't be your old password. Please choose a different password!";

			} else {

				existingUser.password = BasicUtils.getTheKey(password);				

				if (null != userRepo.saveUser(existingUser)) {
					localResponse.isSuccess = true;
					localResponse.message = Constants.NA;
				}

			}

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return localResponse.toJson();

	}

	/**
	 * @deprecated use the v2/managers/UserManager.addPassword()
	 */
	@Deprecated
	public JSONObject addPassword(int userId, String password) throws Exception {

		JSONObject responseJson = (new LocalResponse()).toJson();

		try {

			User existingUser = appContextProvider.getUserById(userId);
			existingUser.password = BasicUtils.getTheKey(password);
//			boolean hasPasswordAdded = databaseHelper.addPassword(existingUser);

			if (null != userRepo.saveUser(existingUser)) {
				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.MESSAGE, Constants.NA);
				responseJson.put(Constants.SESSION_PASSCODE, userRepo.updateAndGetSession(existingUser));
			}

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseJson;

	}

	@Deprecated
	public JSONObject getLoanAccountList(int userId) throws Exception {

		User user = null;

		try {

			user = appContextProvider.getUserById(userId);

		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		if (null != user) {

			JSONObject response = sfManager.getLoanAccountList(user.crmAccountNumber);

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

	/**
	 * @deprecated use the v2/managers/UserManager.getDashboard()
	 */
	@Deprecated
	public JSONObject getDashboard(User user) throws Exception {

		JSONObject dashboardData = sfManager.getUserDashboard(user.crmAccountNumber);

		if (null != dashboardData) {
			dashboardData.put(Constants.STATUS, Constants.SUCCESS);
			dashboardData.put(Constants.MESSAGE, Constants.NA);
			return dashboardData;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getBranch() throws Exception {

		JSONObject branchData = sfManager.getUBranch();

		if (null != branchData) {
			branchData.put(Constants.STATUS, Constants.SUCCESS);
			branchData.put(Constants.MESSAGE, Constants.NA);
			return branchData;
		} else
			return (new LocalResponse()).toJson();

	}

	@Deprecated
	public JSONObject getLoanDetails(User user) throws Exception {

		JSONObject loanData = sfManager.getLoanDetail(user.crmAccountNumber);

		if (null != loanData) {
			loanData.put(Constants.STATUS, Constants.SUCCESS);
			loanData.put(Constants.MESSAGE, Constants.NA);
			return loanData;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getDisbersementDetails(User user) throws Exception {

		JSONObject json = sfManager.getDisbursementDetail(user.crmAccountNumber);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getUserProfileDetails(int userId) throws Exception {

		User user = null;

		try {
			user = appContextProvider.getUserById(userId);
		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		if (null != user) {

			JSONObject json = sfManager.getUserProfile(user);

			if (null != json) {
				json.put(Constants.STATUS, Constants.SUCCESS);
				json.put(Constants.MESSAGE, Constants.NA);
				return json;
			} else
				return (new LocalResponse()).toJson();

		} else {
			LocalResponse lResponse = new LocalResponse();
			lResponse.message = "No User found";
			return lResponse.toJson();
		}

	}

	@Deprecated
	public JSONObject getUserPaymentDetails(User user) throws Exception {

		JSONObject json = sfManager.getPaymentDetails(user.crmAccountNumber);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	@Deprecated
	public JSONObject getRecentMonthlyPayments(User user, String paymentType) throws Exception {

		JSONObject json = sfManager.getRecentMonthlyPayments(user.crmAccountNumber, paymentType);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getServiceRequests(User user) throws Exception {

		JSONObject json = sfManager.getServiceRequests(user.crmAccountNumber);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject addServiceRequest(User user, JSONObject bodyObject) throws Exception {

		var serviceReq = new ServiceRequest().initForSR(bodyObject);

		if (!userRepo.saveServiceRequest(serviceReq)) {
			return (new LocalResponse()).toJson();
		}

		if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

			if (!s3Client().uploadImage(serviceReq.attachmentName, serviceReq.attachmentFile,
					S3BucketPath.SERVICE_REQUEST)) {

				LoggerUtils.log("addServiceRequest - Failed to upload Service Request Attachment file to S3");

				return (new LocalResponse()).toJson();

			}

		}

		JSONObject json = sfManager.addServiceRequest(user, bodyObject);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			serviceReq.caseId = json.getString("caseId");

			if (!userRepo.saveServiceRequest(serviceReq)) {
				return (new LocalResponse()).toJson();
			}
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getServiceRequestAttachment(String parentId) throws Exception {

		JSONObject json = sfManager.getServiceRequestAttachment(parentId);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getAttachmentData(String documentId) throws Exception {

		JSONObject json = sfManager.getAttachmentData(documentId);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getCLAttachmentList(int userId) throws Exception {

		User user = null;

		try {
			user = appContextProvider.getUserById(userId);
		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		if (null != user) {

			JSONObject json = sfManager.getCLAttachmentList(user.crmAccountNumber);

			if (null != json) {
				json.put(Constants.STATUS, Constants.SUCCESS);
				json.put(Constants.MESSAGE, Constants.NA);
				return json;
			} else
				return (new LocalResponse()).toJson();

		} else {
			LocalResponse lResponse = new LocalResponse();
			lResponse.message = "No User found";
			return lResponse.toJson();
		}

	}

	public JSONObject addApnsToken(int userId, String body) throws Exception {

		DatabaseHelper dbHelper = new DatabaseHelper();
		LocalResponse response = new LocalResponse();

		try {

			JSONObject data = new JSONObject(body);
			boolean status = dbHelper.addApnsToken(userId, data);
			dbHelper.close();

			if (status) {
				response.isSuccess = true;
				response.message = "APNS token updated successfully.";
			}

		} catch (Exception e) {
			dbHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return response.toJson();
	}

	public JSONObject addNotificationToken(int userId, String body) throws Exception {

		DatabaseHelper dbHelper = new DatabaseHelper();
		LocalResponse response = new LocalResponse();

		try {

			JSONObject data = new JSONObject(body);
			boolean status = dbHelper.addNotificationToken(userId, data);
			dbHelper.close();

			if (status) {
				response.isSuccess = true;
				response.message = "Notification token updated successfully.";
			}

		} catch (Exception e) {
			dbHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return response.toJson();
	}

	@Deprecated
	public JSONObject setProfileImage(int userId, String body) throws Exception {

		AmazonClient amazonClient = new AmazonClient();
		JSONObject bodyObject = new JSONObject(body);
		String fileName = bodyObject.getString("fileName");
		String base64String = bodyObject.getString("fileData");

		InputStream inputStream = new ByteArrayInputStream(Base64.decodeBase64(base64String.getBytes()));

		OutputStream outputStream = null;
		String qualifiedUploadFileName = userId + "-" + fileName;
		String qualifiedUploadFilePath = (Constants.IS_DB_LIVE ? UPLOAD_FILE_SERVER : UPLOAD_FILE_LOCAL_SERVER)
				+ qualifiedUploadFileName;

		boolean status = false;

		status = amazonClient.uploadImage(qualifiedUploadFileName, base64String, S3BucketPath.PROFILE_IMAGES);

		try {
			outputStream = new FileOutputStream(new File(qualifiedUploadFilePath));
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			outputStream.flush();

			status = true;

			LoggerUtils.log("=========== file saved successfully at: " + qualifiedUploadFilePath);

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			outputStream.close();
		}

		if (status) {
			JSONObject json = new JSONObject();
			DatabaseHelper dbHelper = new DatabaseHelper();
			try {

				boolean success = dbHelper.addOrUpdateUserProfilePicture(userId, qualifiedUploadFileName);
				dbHelper.close();

				if (success) {
					json.put(Constants.STATUS, Constants.SUCCESS);
					json.put("imageUrl", qualifiedUploadFileName);
				} else {
					json.put(Constants.STATUS, Constants.FAILURE);
					json.put(Constants.MESSAGE, "Failed to add your profile picture. Please try again.");
					json.put("imageUrl", qualifiedUploadFileName);
				}

			} catch (Exception e) {
				dbHelper.close();
				LoggerUtils.log("Error: " + e.toString());
				e.printStackTrace();
				throw e;
			}

			return json;

		} else
			return (new LocalResponse()).toJson();

	}

	public JSONObject getAppPayments(int userId) throws Exception {

		DatabaseHelper dbHelper = new DatabaseHelper();

		try {

			User user = appContextProvider.getUserById(userId);

			JSONObject loanAccJson = sfManager.getLoanAccountList(user.crmAccountNumber);
			if (null != loanAccJson) {

				JSONArray loans = loanAccJson.getJSONArray("loans");
				ArrayList<String> loanAccountIds = new ArrayList<>();
				for (int i = 0; i < loans.length(); i++)
					loanAccountIds.add(loans.getJSONObject(i).getString("accountNumber"));

				ArrayList<Payment> payments = dbHelper.getPaymentByLoanAccountNumbers(loanAccountIds);
				dbHelper.close();
				JSONArray paymentArray = new JSONArray();
				if (null != payments) {
					for (Payment pItem : payments) {
						paymentArray.put(pItem.toJson());
					}
				}

				JSONObject responseObject = new JSONObject();
				responseObject.put(Constants.STATUS, Constants.SUCCESS);
				responseObject.put(Constants.MESSAGE, Constants.NA);
				responseObject.put("payments", paymentArray);
				return responseObject;

			} else {
				dbHelper.close();
				LocalResponse errorResponse = new LocalResponse();
				errorResponse.message = "No Loan Account is associated with given CRM Account Number.";
				return errorResponse.toJson();
			}

		} catch (Exception e) {
			dbHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

	}

	public JSONObject addInstalledAppInfo(int userId, JSONObject requestObject) throws Exception {

		JSONArray appsArray = new JSONArray(requestObject.optString("data", "[]"));

		DatabaseHelper dbHelper = new DatabaseHelper();

		try {

			int storedAppCount = 0;

			for (int i = 0; i < appsArray.length(); i++) {

				JSONObject currentApp = appsArray.getJSONObject(i);

				try {
					storedAppCount += dbHelper.addAppsData(userId, currentApp);
				} catch (SQLException sqle) {
				}

			}

			dbHelper.close();

			LoggerUtils.log("Total Apps Added: " + storedAppCount);

			return BasicUtils.getSuccessTemplateObject();

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

	}

}
