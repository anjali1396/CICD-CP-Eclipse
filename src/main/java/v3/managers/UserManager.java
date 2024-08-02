package v3.managers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;

import dao.AdBannerRepository;
import dao.CommonRepository;
import dao.PromoBannersRepository;
import dao.RequestOutstandingRepository;
import dao.UserRepository;
import helper.AppContextProvider;
import models.AdBannerImage;
import models.LoginInfo;
import models.PromoBanners;
import models.RequestOutstanding;
import models.SecondaryInfo;
import models.User;
import models.UserCommunication;
import models.UserRequest;
import models.notification.CPNotification;
import services.Recaptcha;
import utils.BasicUtils;
import utils.ColumnsNFields;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.NotificationUtils;
import utils.OneResponse;
import utils.ProptertyUtils;
import utils.Constants.Actions;
import utils.Constants.Errors;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.NotificationUtils.AppScreens;
import utils.NotificationUtils.AudienceType;
import utils.NotificationUtils.NotificationKind;
import utils.NotificationUtils.NotificationPriority;
import utils.NotificationUtils.OnClickAction;
import utils.NotificationUtils.ScheduleType;
import v1.dto.NotificationUpdateDTO;
import v1.repository.NotificationRepository;
import v2.managers.AmazonClient;
import v2.managers.ContactManager;
import v2.managers.NotificationHelper;
import v2.managers.RequestManager;
import v2.managers.UserComHelper;
import v2.managers.AmazonClient.S3BucketPath;
import v2.managers.RequestManager.RequestType;
import v2.managers.UserComHelper.CommunicationType;

public class UserManager {

	public static final String UPLOAD_FILE_SERVER = "/var/www/images/profile_picture/";
	public static final String LOCAL_FILES_PATH = "/var/www/images/files/";
	public static final String UPLOAD_FILE_LOCAL_SERVER = "/Users/ranan/var/www/images/profile_picture/";

	private final UserRepository userRepo;
	private final SalesForceManager sfManager;
	private final Gson gson;
	private final CommonRepository commonRepo;
	private RequestOutstandingRepository _outstandingRepo = null;
	private NotificationRepository _notificationRepo = null;
	private AppContextProvider appContextProvider = new AppContextProvider();
	

	public UserManager() throws Exception {

		userRepo = new UserRepository();
		sfManager = new SalesForceManager();
		gson = new Gson();
		commonRepo = new CommonRepository();

	}

	private RequestOutstandingRepository outstandingRepo() {
		if (null == _outstandingRepo)
			_outstandingRepo = new RequestOutstandingRepository();
		return _outstandingRepo;
	}

	private NotificationRepository notificationRepo() throws Exception {
		if (null == _notificationRepo)
			_notificationRepo = new NotificationRepository();
		return _notificationRepo;
	}

	private void log(String value) {
		LoggerUtils.log("V3.UserManager." + value);
	}

	private void printLog(String value) {
		System.out.println("V3.UserManager." + value);
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

	public Response addServiceRequest(int userId, JSONObject bodyObject) throws Exception {

		JSONObject responseJson = new JSONObject();

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("User doesn't exist.").toJson());
		}

		String authType = bodyObject.optString("authType", Constants.NA);

		if (!authType.equalsIgnoreCase(Constants.PASSWORD) && !authType.equalsIgnoreCase(Constants.BIOMETRIC)) {

			return new OneResponse().getFailureResponse(
					new LocalResponse().setMessage("Invalid Auth type").setError(Errors.INVALID_DATA.value).toJson());

		}

		if (authType.equalsIgnoreCase(Constants.PASSWORD)) {

			String password = bodyObject.optString("password", Constants.NA);

			if (BasicUtils.isNotNullOrNA(password)) {
				LoggerUtils.log("addServiceRequest - Authenticatin using password");

				if (!fetchedUser.password.equals(BasicUtils.getTheSecureKey(password))) {
					return new OneResponse().getFailureResponse(
							new LocalResponse().setMessage("Wrong password. Please enter correct password.")
									.setError(Errors.INVALID_PASSWORD.value).toJson());
				}

			} else {
				return new OneResponse().getFailureResponse(new LocalResponse()
						.setMessage("Please enter a valid password.").setError(Errors.INVALID_PASSWORD.value).toJson());
			}

		}

		JSONObject serviceRequestJson = bodyObject.getJSONObject("serviceRequestParams");

		responseJson = new dao.UserManager().addServiceRequest(fetchedUser, serviceRequestJson);

		return new OneResponse().getSuccessResponse(responseJson);

	}
	
	public Response reCaptchaVerify(int userId, JSONObject bodyObject, String ipAddress) throws Exception {
		
		var token =  bodyObject.optString("captchaToken", Constants.NA);
		
		if(!Recaptcha.isValid(token)) {
			return new OneResponse().getFailureResponse(
					new LocalResponse().toJson());
		
		}
		
		return performLogin(userId, bodyObject, ipAddress);
		
	}

	public Response performLogin(int userId, JSONObject bodyObject, String ipAddress) throws Exception {
		
		User user = new User(bodyObject); 
	

		JSONObject responseJson = new JSONObject();

		User fetchedUser = userRepo.findUserByMobileNumber(user.mobileNumber);

		if (null == fetchedUser) {

			return new OneResponse()
					.getFailureResponse(new LocalResponse()
							.setMessage(Constants.INCORRECT_USERNAME_PASSWORD_ERROR)
							.toJson());

		} else {

			if (fetchedUser.isDeleted) {

				return new OneResponse()
						.getFailureResponse(new LocalResponse()
								.setMessage(Constants.INCORRECT_USERNAME_PASSWORD_ERROR)
								.toJson());

			} else if (fetchedUser.forcePasswordReset) {

				return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.RESET).toJson());

			} else if (fetchedUser.password.equals(Constants.NA)) {

				return new OneResponse().getFailureResponse(
						new LocalResponse().setMessage("Please complete the registration process.").toJson());

			} else if (fetchedUser.password.equals(Constants.RESET)) {

				return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.RESET).toJson());

			} else {
				
				var minuteDifference = 0;
				
				if(BasicUtils.isNotNullOrNA(fetchedUser.loginAttemptDateTime)) {
					 minuteDifference = DateTimeUtils.getDateDifferenceInMins(fetchedUser.loginAttemptDateTime);

				}
				
				if (BasicUtils.getTheSecureKey(user.password).equals(fetchedUser.password)) {
					
					var passwordDateTime = new DateTimeUtils().getDateTimeByAddingDays(Constants.PASSWORD_EXPIRE_DAY_COUNT, DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeZone.IST);
					
					if(fetchedUser.passwordDateTime == null || fetchedUser.passwordDateTime == "NA") {
						fetchedUser.passwordDateTime = passwordDateTime;
					}
					
					var daysDifference = new DateTimeUtils().getDateDifferenceInDays(DateTimeUtils.getCurrentDateTimeInIST(), fetchedUser.passwordDateTime);
					
					if(daysDifference <= 0) {
						LoggerUtils.log("login - password expired for user id :" + fetchedUser.userId);
						
						return new OneResponse().getFailureResponse(new LocalResponse()
								.setMessage("Your Password has expired, " +
	                            "you need to update your password by clicking on forgot password")
								.setError(Errors.OPERATION_FAILED.value)
								.setAction(Actions.DO_PASSWORD_RESET.value)
								.toJson());
						
					}
						
						if(minuteDifference < Constants.DEFAULT_LOGIN_ATTEMPT_MINUTES) {
							if(fetchedUser.loginAttempt <= 0) {
								LoggerUtils.log("login - account lock for user id :" + fetchedUser.userId);
								return new OneResponse().getFailureResponse(
										new LocalResponse().setMessage("Your account is locked due to invalid credentials. "
												+ "You can try login again after " + Constants.DEFAULT_LOGIN_ATTEMPT_MINUTES +" minutes.").toJson());
							}
						}else {
							fetchedUser.loginAttempt = Constants.DEFAULT_LOGIN_ATTEMPTS;
						}
					
					
					fetchedUser.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
					
					fetchedUser.loginAttempt = Constants.DEFAULT_LOGIN_ATTEMPTS;
					fetchedUser.loginAttemptDateTime = null;
					
					userRepo.saveUser(fetchedUser);
					
					LoginInfo lInfo = gson.fromJson(bodyObject.toString(), LoginInfo.class);
					lInfo.ipAddress = ipAddress;
					
					lInfo.userId = fetchedUser.userId;
					
					lInfo.loginDatetime = DateTimeUtils.getCurrentDateTimeInIST();

					commonRepo.insertLoginInfo(lInfo);
					

					 ArrayList<LoginInfo> loginInfoList = commonRepo.getUserIdOrderByLoginDatetimeDesc(fetchedUser.userId);
					
					 if(loginInfoList != null) {
					  if(loginInfoList.size() == 1)
						  fetchedUser.lastLoginDatetime = loginInfoList.get(0).loginDatetime;
				        else fetchedUser.lastLoginDatetime = loginInfoList.get(1).loginDatetime;
					 }
					
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
					
					
					var securityInfo = new JSONArray();
					
					System.out.println("===> fetchedUser.password " + fetchedUser.password);
					System.out.println("===> isBreachedPassword " + BasicUtils.isBreachedPassword(fetchedUser.password));

					
					if(BasicUtils.isBreachedPassword(user.password)) {
						
						securityInfo.put("Your password falls under vulnerable passwords and must be changed!");
					}
					
					if(daysDifference <= 10) {
						securityInfo.put("Your Password will be Expired in " + daysDifference + " days");
					}
					
					responseJson.put("securityInfo", securityInfo);
										
					return new OneResponse().getSuccessResponse(responseJson);

				} else {

					fetchedUser.loginAttempt = fetchedUser.loginAttempt - 1 ;
					fetchedUser.loginAttemptDateTime = DateTimeUtils.getCurrentDateTimeInIST();
					
					
					LoggerUtils.log("login - Failed : Invalid credentials entered for user id:" + fetchedUser.userId);
					
					var msg = Constants.NA;
					
				
				if(minuteDifference < Constants.DEFAULT_LOGIN_ATTEMPT_MINUTES && fetchedUser.loginAttempt <= 0) {
						msg = "Your account is locked due to invalid credentials. "
								+ "You can try login again after " + Constants.DEFAULT_LOGIN_ATTEMPT_MINUTES + " minutes.";
					
				} else if(minuteDifference > Constants.DEFAULT_LOGIN_ATTEMPT_MINUTES && fetchedUser.loginAttempt <= 0) { 
					
					fetchedUser.loginAttempt = Constants.DEFAULT_LOGIN_ATTEMPTS;
					fetchedUser.loginAttempt = fetchedUser.loginAttempt - 1 ;
					msg = "Invalid credentials entered. "
							+ "You have " + fetchedUser.loginAttempt + " attempts remaining to prevent your account "
									+ "from being suspended.";
					
				} else {
					
					if(fetchedUser.loginAttempt > 0) {
					msg = "Invalid credentials entered. "
							+ "You have " + fetchedUser.loginAttempt + " attempts remaining to prevent your account "
									+ "from being suspended.";
					
					}
					
				}
				
				
				userRepo.saveUser(fetchedUser);
					
					return new OneResponse().getFailureResponse(
							new LocalResponse().setMessage(msg).toJson()); 

				}

			}

		}

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
								LoggerUtils.log("Updating User in DB");
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

	public Response addPassword(int userId, String requestId, String token, JSONObject bodyObject, String ipAddress)
			throws Exception {

		String password = bodyObject.optString("password", Constants.NA);
		String requestTypeString = bodyObject.optString("requestType", Constants.NA);

		JSONObject responseJson = new JSONObject();

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("User doesn't exist.").toJson());
		}

		RequestType requestType = RequestType.get(requestTypeString);

		if (null == requestType) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE)
					.setError("Invalid request type.").toJson());

		}

		if (!(requestType == RequestType.REGISTERATION || requestType == RequestType.CHANGE_PASSWORD
				|| requestType == RequestType.FORGOT_PASSWORD || requestType == RequestType.FORCE_RESET_PASSWORD)) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE)
					.setError("Request type " + requestType.value + " is not allowed.").toJson());

		}

		UserRequest uRequest = fetchUserRequest(userId, requestId, requestType);

		if (null == uRequest) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE)
					.setError("Failed to verify request.").setAction(Constants.ActionType.RETRY.stringValue).toJson());
		}

		if (!uRequest.token.equals(token)) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE)
					.setError("Invalid token.").toJson());

		}

		if (null != fetchedUser.password && fetchedUser.password.equals(BasicUtils.getTheSecureKey(password))) {

			return new OneResponse().getFailureResponse(new LocalResponse()
					.setMessage("New password can't be your old password. Please choose different password!.")
					.toJson());
		}

		fetchedUser.password = BasicUtils.getTheSecureKey(password);
		//TODO: Confirm before PROD
		var passwordDateTime = new DateTimeUtils().getDateTimeByAddingDays(Constants.PASSWORD_EXPIRE_DAY_COUNT, DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeZone.IST);
		fetchedUser.passwordDateTime = passwordDateTime;
		fetchedUser.forcePasswordReset = false;
		
		System.out.println("addPassword password " + password);
		System.out.println("addPassword fetchedUser.password " + fetchedUser.password);
		System.out.println("addPassword BasicUtils.getTheSecureKey(password) " + BasicUtils.getTheSecureKey(password));

		SecondaryInfo sInfo = userRepo.findSecondaryInfoByUserId(fetchedUser.userId);
		sInfo.passwordChangeDatetime = DateTimeUtils.getCurrentDateTimeInIST();
		userRepo.saveSecondaryInfo(sInfo);

		if (null != userRepo.saveUser(fetchedUser)) {

			String action = Constants.NA;
			String message = Constants.DEFAULT_ERROR_MESSAGE;
			if (uRequest.type.equals(RequestType.REGISTERATION.value)) {
				updateRegisterInfoOnSF(fetchedUser);
				action = Constants.ActionType.CONTINUE.stringValue;
				message = "Your password has been set successfully. Let's take you home.";
			} else {
				action = Constants.ActionType.DO_LOGIN.stringValue;
				message = "Your password has been updated successfully. Please login with your new password";
			}

			RequestManager requestManager = new RequestManager();
			uRequest = requestManager.updateUserRequest(fetchedUser, requestId, Constants.CLOSED, false, ipAddress);

			if (null != uRequest) {
				fetchedUser.sessionPasscode = userRepo.updateAndGetSession(fetchedUser).sessionPasscode;
				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.ACTION, action);
				responseJson.put(Constants.MESSAGE, message);
				responseJson.put(Constants.USER, fetchedUser.toJson(true));
				responseJson.put(Constants.REQUEST, uRequest.toJson());

				return new OneResponse().getSuccessResponse(responseJson);
			} else {

				return new OneResponse()
						.getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE)
								.setError("Failed to update user request.").toJson());
			}

		} else {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE)
					.setError("Failed to add password.").toJson());
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

	public void updateRegisterInfoOnSF(User user) throws Exception {

		final Timer timer = new Timer(true);

		timer.schedule(new TimerTask() {
			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (sfManager.updateRegisteredInfo(user)) {
							user.isRegistrationMarkedOnSF = true;
							userRepo.saveUser(user);
							timer.cancel();

						} else
							count++;

					} catch (Exception e) {
						LoggerUtils.log("Error while updating Registered Info of applicant : " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Task rescheduled, Iteration: " + count);
					}

				} else {
					timer.cancel();
					LoggerUtils.log("Time's up! Failed to update Registered Info  of Applicant.");

				}

			}
		}, 10000);
	}

	public Response initiateMobileNumberChangeProcess(int userId, String requestId, JSONObject bodyObject,
			String ipAddress) throws Exception {

		JSONObject responseJson = new JSONObject();

		String oldMobileNumber = bodyObject.optString("oldMobileNumber", Constants.NA);

		User fetchedUser = userRepo.findUserByMobileNumber(oldMobileNumber);

		if (null == fetchedUser) {

			return new OneResponse().getFailureResponse(
					new LocalResponse().setMessage("No account is associated with given existing mobile number. "
							+ "Please check the current mobile number you entered.").toJson());

		}

		String newMobileNumber = bodyObject.optString("newMobileNumber", Constants.NA);

		String countryCode = bodyObject.optString("countryCode", "+91");

		String password = bodyObject.optString("password", Constants.NA);

		if (newMobileNumber.equals(oldMobileNumber)) {

			return new OneResponse().getFailureResponse(new LocalResponse()
					.setMessage("New mobile number cannot be same as current mobile number").toJson());
		}

		String authType = bodyObject.optString("authType", Constants.NA);
		if (!authType.equalsIgnoreCase(Constants.PASSWORD) && !authType.equalsIgnoreCase(Constants.BIOMETRIC)) {

			return new OneResponse().getFailureResponse(
					new LocalResponse().setMessage("Invalid Auth type").setError(Errors.INVALID_DATA.value).toJson());

		}
		if (authType.equalsIgnoreCase(Constants.PASSWORD)) {

			if (!password.equalsIgnoreCase(Constants.NA)) {
				LoggerUtils.log("initiateMobileNumberChangeProcess - Authenticatin using password");

				if (!fetchedUser.password.equals(BasicUtils.getTheSecureKey(password))) {
					return new OneResponse().getFailureResponse(
							new LocalResponse().setMessage("Wrong password. Please enter correct password.")
									.setError(Errors.INVALID_PASSWORD.value).toJson());
				}
			} else {
				return new OneResponse().getFailureResponse(new LocalResponse()
						.setMessage("Please enter a valid password.").setError(Errors.INVALID_PASSWORD.value).toJson());
			}
		}
		ContactManager contactManager = new ContactManager();

		UserRequest uRequest = fetchOrCreateUserRequest(fetchedUser, RequestType.CHANGE_MOBILE_NUMBER, requestId,
				ipAddress);

		if (null != uRequest) {	
			
//			var responseGenerateOTP = contactManager.generateOTP(newMobileNumber);
//			if (responseGenerateOTP.isSuccess) {

			JSONObject json = contactManager.sendOTP(newMobileNumber, countryCode);

			if (null != json && json.optString(Constants.STATUS).equals(Constants.SUCCESS)) {

				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.ACTION, Constants.ActionType.CONTINUE.stringValue);
				responseJson.put(Constants.MESSAGE, "OTP send successfully.");
				responseJson.put(Constants.USER, fetchedUser.toJson(true));
				responseJson.put(Constants.REQUEST, uRequest.toJson());
				return new OneResponse().getSuccessResponse(responseJson);

			} else
				return new OneResponse()
						.getFailureResponse(new LocalResponse().setMessage("Failed to sent OTP.").toJson());

		} else
			return new OneResponse()
					.getFailureResponse(new LocalResponse().setMessage("Failed to create user request.").toJson());

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

	private User getUserById(int userId) throws Exception {

		User user = appContextProvider.getUserById(userId);

		if (null != user)
			return user;
		else
			throw new Exception("No user found with given userId.");

	}

	public Response getDashboard(int userId) throws Exception {

		User user = getUserById(userId);

		JSONObject dashboardData = sfManager.getUserDashboard(user.crmAccountNumber);

		if (null != dashboardData) {

			dashboardData.put(Constants.STATUS, Constants.SUCCESS);
			dashboardData.put(Constants.MESSAGE, Constants.NA);
			dashboardData.put("userName", user.name);
			dashboardData.put("bounceCharge", 590);
			dashboardData.put("lastPosUpdateDatetime", "30th June 2024");

			ArrayList<AdBannerImage> activeAdBanner = new AdBannerRepository().getActiveAdBanner();
			Collections.shuffle(activeAdBanner);
			dashboardData.put("adBannerImage", new JSONArray(gson.toJson(activeAdBanner)));

			ArrayList<PromoBanners> activePromoBanners = new PromoBannersRepository().getActivePromoBanners();
			Collections.shuffle(activePromoBanners);
			dashboardData.put("promoBanners", new JSONArray(gson.toJson(activePromoBanners)));

			return new OneResponse().getSuccessResponse(dashboardData);

		} else
			return new OneResponse().operationFailedResponse();

	}

	public Response getLoanList(int userId) throws Exception {

		User user = getUserById(userId);

		var loanData = sfManager.getLoanList(user.crmAccountNumber);

		if (null != loanData && !loanData.isEmpty()) {
			var response = new JSONObject();
			response.put("loanData", new JSONArray(gson.toJson(loanData)));
			return new OneResponse().getSuccessResponse(response);
		} else
			return new OneResponse().getDefaultFailureResponse();

	}

	public Response getLoanDetails(int userId, JSONObject bodyObject) throws Exception {

		// var selectedLoan = gson.fromJson(bodyObject.optString("selectedLoan"),
		// Loan.class);

		var selectedLoanAccountNumber = bodyObject.optString("loanAccountNumber");

		var loanData = sfManager.getLoanDetail(selectedLoanAccountNumber);

		var outstandingStatus = outstandingRepo().findValidRequest(loanData.get(0).accountNumber, userId);
		if (null != outstandingStatus) {

			try {

				if (DateTimeUtils.getDateDifferenceInMinutes(outstandingStatus.createDatetime) < -1440)
					loanData.get(0).outstandingStatus = Constants.VALID;
				else
					loanData.get(0).outstandingStatus = Constants.IN_PROGRESS;

			} catch (Exception e) {
				e.printStackTrace();
//				return new OneResponse().getDefaultFailureResponse();
			}
		} else {
			loanData.get(0).outstandingStatus = Constants.INVALID;
		}

		if (null != loanData && !loanData.isEmpty()) {
			var response = new JSONObject();
			response.put("loanData", new JSONArray(gson.toJson(loanData)));
			return new OneResponse().getSuccessResponse(response);
		} else
			return new OneResponse().operationFailedResponse();

	}

	public Response requestOutstanding(int userId, JSONObject bodyObject) throws Exception {

		final var reqOutstanding = gson.fromJson(bodyObject.toString(), RequestOutstanding.class);

		if (null != outstandingRepo().findValidRequest(reqOutstanding.loanAccountNumber, reqOutstanding.userId)) {
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(
					"You already have a valid request, You'd be able to see your outstanding on MyLoans Screen.")
					.setError(Errors.DUPLICATE_RECORD.value).toJson());
		}
		
		reqOutstanding.validityDatetime = DateTimeUtils.getDateTimeAddingHours(26, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
				DateTimeZone.IST);

		if (null == outstandingRepo().saveRequestOutstanding(reqOutstanding)) {
			return new OneResponse().operationFailedResponse();
		} else {
			return new OneResponse().getSuccessResponse(new LocalResponse().setMessage(
					"Your request for outstanding amount has been successfully submitted.\nYou'll be able to see it in 24 hours.")
					.toJson());
		}

	}

	public Response getPromoBanners() throws Exception {

		try {

			ArrayList<PromoBanners> activeBlogs = new PromoBannersRepository().getActivePromoBanners();
			Collections.shuffle(activeBlogs);

			JSONObject responseJson = new JSONObject();
			responseJson.put("blogs", new JSONArray(gson.toJson(activeBlogs)));
			responseJson.put("baseUrl", new AmazonClient().getBaseUrl(S3BucketPath.RESOURCE_PROMOTION));

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response checkAndNotifyLoanOutstanding() throws Exception {

		final var outstandingData = outstandingRepo().findNonNotified();		

		if (outstandingData.isEmpty()) {
			
			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "No Data Present to notify");

			return new OneResponse().getSuccessResponse(responseJson);
			
		}
		
		for (var od : outstandingData) {
			sendOutstandingNotification(od);		
			Thread.sleep(5000);
		}

		final var responseJson = new JSONObject();
		responseJson.put(Constants.MESSAGE, "Notification Sent successfully.");

		return new OneResponse().getSuccessResponse(responseJson);

	}
		
	private void sendOutstandingNotification(RequestOutstanding reqOutstanding) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 15);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {


					try {
											
						final var userNotificationKeys = notificationRepo()
								.getUserNotificationTokensByUserId("" + reqOutstanding.userId + "");

						if (userNotificationKeys.size() == 0) {
							log("sendOutstandingNotification - No registration keys found while sending Outstanding notification.");
							// ndbHelper.close();
							timer.cancel();
							return;
						}

						CPNotification cpNotification = new CPNotification();
						// cpNotification.audienceGroup = new JSONObject();
						cpNotification.audienceType = AudienceType.PERSONALIZED.value;

						StringBuilder sb = new StringBuilder();
						sb.append("Your loan outstanding amount is now available");
						sb.append(" for loan account number ");
						sb.append(reqOutstanding.loanAccountNumber);
						sb.append("\nIt will be available upto 12 hours.");

						cpNotification.title = "Outstanding Amount Available";
						cpNotification.message = sb.toString();
						cpNotification.bigMessage = sb.toString();

						var jData = new JSONObject();
						jData.put("reqOutstanding", gson.toJson(reqOutstanding));

						cpNotification.data = jData.toString();
						cpNotification.kind = NotificationKind.TRANSACTIONAL.value;
						cpNotification.priority = NotificationPriority.HIGH.value;
						cpNotification.screenToOpen = AppScreens.LOAN.value;
						cpNotification.onClickAction = OnClickAction.IN_APP.value;
						cpNotification.shouldSchedule = false;

						String currentDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						cpNotification.datetime = currentDatetime;
						cpNotification.createDatetime = currentDatetime;

						cpNotification.platform = NotificationUtils.Platform.ALL.value;

						cpNotification.scheduleType = cpNotification.shouldSchedule ? ScheduleType.LATER.value
								: ScheduleType.NOW.value;

						NotificationHelper nHelper = new NotificationHelper(1);
						cpNotification.schedulerId = nHelper.aUser.id;
						cpNotification.schedulerName = nHelper.aUser.name;

						if (!notificationRepo().saveCpNotification(cpNotification)) {
							count++;
							log("sendOutstandingNotification - Failed to create Outstanding notification in the DB. Rescheduled, Iteration: "
									+ count);
							return;

						}

						log("sendOutstandingNotification - Outstanding notification created successfully in DB.");

						nHelper.schedulePersonalizedNotification(cpNotification, userNotificationKeys, false);

						log("sendOutstandingNotification -  completed. Iteration: " + count);

						UserCommunication userComm = new UserCommunication();
						userComm.isNotification = true;
						userComm.notificationDatetime = DateTimeUtils.getCurrentDateTimeInIST();

						userComm.recordType = ColumnsNFields.REQUEST_OUTSTANDING;

						// TODO: Need to change the parameter for recordId
						// userComm.recordId = reqOutstanding.id;
						userComm.userId = reqOutstanding.userId;

						new UserComHelper().updateUserCommunication(userComm, CommunicationType.NOTIFICATION);

						reqOutstanding.isNotified = true;
						outstandingRepo().saveRequestOutstanding(reqOutstanding);
						timer.cancel();

					} catch (Exception e) {

						log("sendOutstandingNotification - Error while sending Outstanding notification : "
								+ e.getMessage());
						e.printStackTrace();

						count++;
						log("sendOutstandingNotification - Outstanding notification Task rescheduled, Iteration: "
								+ count);

					}

				} else {

					log("sendOutstandingNotification - Time's up! Failed to send Outstanding  notification.");
					timer.cancel();

				}

			}

		}, time, 10000);

	}

	public JSONObject getDisbersementDetails(User user) throws Exception {

		JSONObject json = sfManager.getDisbursementDetail(user.loanAccountNumber);

		if (null != json) {
			json.put(Constants.STATUS, Constants.SUCCESS);
			json.put(Constants.MESSAGE, Constants.NA);
			return json;
		} else
			return (new LocalResponse()).toJson();

	}

	public Response getNotifications(int userId, JSONObject requestObject) throws Exception {
		
		int pageNumber = requestObject.optInt("pageNumber");
		
		final var userNotifications = notificationRepo().getUserNotificationByUserId(userId, pageNumber);

		final var responseJson = new JSONObject();
		responseJson.put("notifications", new JSONArray(gson.toJson(userNotifications)));

		return new OneResponse().getSuccessResponse(responseJson);

	}

	public Response updateNotificationStatus(int userId, String requestBody) throws Exception {

		final var notificationUpdate = gson.fromJson(requestBody, NotificationUpdateDTO.class);

		printLog("updateNotificationStatus - notificationUpdate : " + gson.toJson(notificationUpdate));

		final var allUserNotifications = notificationRepo().getAllUserNotificationByNotificationId(userId,
				notificationUpdate.notificationId);

		printLog("updateNotificationStatus - all notifications : " + allUserNotifications.size());

		if (allUserNotifications.isEmpty()) {

			printLog("updateNotificationStatus - No user notifications found for nId : "
					+ notificationUpdate.notificationId + " | userId: " + userId);

			return new OneResponse()
					.getSuccessResponse(new JSONObject().put(Constants.MESSAGE, "Notification updated."));

		}

		for (var userNotification : allUserNotifications) {

			userNotification.hasRead = notificationUpdate.hasRead == 1;
			userNotification.readDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			userNotification.updateDatetime = userNotification.readDatetime;

			if (userNotification.deviceId.equalsIgnoreCase(notificationUpdate.deviceId)) {

				userNotification.deviceId = notificationUpdate.deviceId;
				userNotification.deviceModel = notificationUpdate.deviceModel;
				userNotification.osVersion = notificationUpdate.osVersion;
				userNotification.appVersion = notificationUpdate.appVersion;
				userNotification.deviceType = notificationUpdate.deviceType;

			}

		}

		printLog("updateNotificationStatus - updating now..");

		if (!notificationRepo().saveAllUserNotifications(allUserNotifications)) {

			log("updateNotificationStatus - Failed to update user notifications.");

			return new OneResponse().errorResponse(null);

		}

		printLog("updateNotificationStatus - All user notificatons marked as read successfully.");

		return new OneResponse().getSuccessResponse(new JSONObject().put(Constants.MESSAGE, "Notification updated."));

	}

	public Response getUnreadNotificationCount(int userId) throws Exception {

		final var unreadCount = notificationRepo().getUnreadUserNotificationCount(userId);

		return new OneResponse().getSuccessResponse(new JSONObject().put("unreadCount", unreadCount));

	}
	
	public Response performRegisteration(String requestId, JSONObject bodyObject, String ipAddress) throws Exception {
		
		final var hasConsented = bodyObject.optBoolean("hasConsented", false);
		
		if (!hasConsented) {
		
			printLog("performRegisteration - Customer has not given consent to T&C and Privacy Policy.");
			return new OneResponse().getFailureResponse(					
					new LocalResponse()
					.setMessage("Please accept Terms & Conditions and Privacy Policy.")
					.setAction(Constants.ActionType.RETRY.stringValue)
					.toJson()
					);
			
		}
		
		final var v2UserManager = new v2.managers.UserManager();
		
		final var registrationResponse = v2UserManager.performRegisteration(requestId, bodyObject, ipAddress);
		
		if (registrationResponse.getString(Constants.STATUS).equalsIgnoreCase(Constants.SUCCESS))				
			return new OneResponse().getSuccessResponse(registrationResponse);
		else return new OneResponse().getFailureResponse(registrationResponse);
		
	}

}
