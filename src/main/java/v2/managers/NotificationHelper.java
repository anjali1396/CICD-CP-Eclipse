package v2.managers;

import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.json.JSONObject;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;

import models.SFContact;
import models.admin.AdminUser;
import models.notification.CPNotification;
import models.notification.NotificationCSV;
import models.notification.UserNotificationToken;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.NotificationUtils;
import utils.NotificationUtils.ScheduleType;
import v1.dto.UserNotificationTokenPair;
import v1.repository.NotificationRepository;
import utils.OneResponse;
import utils.Constants.Actions;
import utils.Constants.Errors;
import v2.managers.AmazonClient.S3BucketPath;

public class NotificationHelper {

	private final SalesForceNotificationManager sfnManager;
	private final AdminUserManager aManager;
	public final AdminUser aUser;
	private Gson gson = null;
	private AmazonClient _amazonS3Client = null;
	private NotificationRepository _notificationRepo = null;

	public NotificationHelper(int userId) throws Exception {

		new NotificationUtils().initFirebase();

		sfnManager = new SalesForceNotificationManager();
		aManager = new AdminUserManager();
		aUser = aManager.getUserByUserId(userId);
		gson = new Gson();
	}

	private AmazonClient s3Client() throws Exception {
		if (null == _amazonS3Client)
			_amazonS3Client = new AmazonClient();
		return _amazonS3Client;
	}

	private void log(String value) {
		LoggerUtils.log("NotificationHelper." + value);
	}

	private NotificationRepository notificationRepo() throws Exception {
		if (null == _notificationRepo)
			_notificationRepo = new NotificationRepository();
		return _notificationRepo;
	}

	// ----------------------------------------------------------------- //
	// -------------- START OF COMMON IMPLEMENTATION ------------------- //
	// ----------------------------------------------------------------- //

	private BatchResponse sendDynamicMulticastMessages(
			CPNotification cpNotification,
			ArrayList<UserNotificationTokenPair> userNotificationTokenPairs
			) throws FirebaseMessagingException {
		
		final var fbMessages = new ArrayList<Message>();
		
		for (var unt: userNotificationTokenPairs) {
			
			cpNotification.title = unt.customNotification.title;
			cpNotification.message = unt.customNotification.message;
			
			LoggerUtils.log("sendDynamicMulticastMessages - dynamic web url: " + unt.customNotification.webUrl);
			
			
			if (BasicUtils.isNotNullOrNA(unt.customNotification.webUrl))
				cpNotification.webUrl = unt.customNotification.webUrl;
			
			final var fbMessage = Message.builder()
					.putData("title", unt.customNotification.title)
					.putData("message", unt.customNotification.message)
					.putData("cpNotification", cpNotification.toJson().toString())
					.putData("imageUrl", cpNotification.imageUrl)
					.setNotification(
							Notification.builder()
							.setTitle(unt.customNotification.title)
							.setBody(unt.customNotification.message)
							.build()
					)
					.setApnsConfig(ApnsConfig.builder().putHeader("apns-priority", "10")
							.setAps(Aps.builder()
									.setAlert(ApsAlert.builder().setTitle(unt.customNotification.title)
											.setBody(unt.customNotification.message).build())
									.setMutableContent(true)
									.setBadge(1).build())
							.build())
					.setToken(unt.userNotificationToken.notificationKey)
					.build();
			
			fbMessages.add(fbMessage);
			
			
		}
		
		final var bResponse = FirebaseMessaging.getInstance()
				.sendAll(fbMessages, NotificationUtils.shouldDryRun());
		
		try {
			if (notificationRepo().saveDynamicUserNotification(cpNotification, userNotificationTokenPairs))
				log("sendMulticastMessages - Successfully insert new user notification after pushing");
			else
				log("sendMulticastMessages - Failed to insert new user notification after pushing.");
		} catch (Exception e1) {
			log("sendMulticastMessages - Error while inserting new user notifications after pushing: "
					+ e1.getMessage());
			e1.printStackTrace();
		}
		
		return bResponse;
		
	}
	
	private BatchResponse sendMulticastMessage(CPNotification cpNotification,
			ArrayList<UserNotificationToken> registrationKeys) throws FirebaseMessagingException {

		List<String> registrationTokens = new ArrayList<>();

//		for (RegistrationKey rKey : registrationKeys)
//			registrationTokens.add(rKey.key);
		
		for (var rKey : registrationKeys)
			registrationTokens.add(rKey.notificationKey);

		MulticastMessage message = MulticastMessage.builder()
				.putData("title", cpNotification.title)
				.putData("message", cpNotification.message)
				.putData("cpNotification", cpNotification.toJson().toString())
				.putData("imageUrl", cpNotification.imageUrl)

				.setApnsConfig(ApnsConfig.builder().putHeader("apns-priority", "10")
						.setAps(Aps.builder()
								.setAlert(ApsAlert.builder().setTitle(cpNotification.title)
										.setBody(cpNotification.message).build())
								.setMutableContent(true)
								.setBadge(1).build())
						.build())
				.addAllTokens(registrationTokens).build();
		

		BatchResponse bResponse = FirebaseMessaging.getInstance().sendMulticast(
				message,
				NotificationUtils.shouldDryRun()
			);

		try {
			if (notificationRepo().saveUserNotificationInfo(cpNotification, registrationKeys))
				log("sendMulticastMessage - Successfully insert new user notification after pushing");
			else
				log("sendMulticastMessage - Failed to insert new user notification after pushing.");
		} catch (Exception e1) {
			log("sendMulticastMessage - Error while inserting new user notifications after pushing: "
					+ e1.getMessage());
			e1.printStackTrace();
		}

		return bResponse;

	}

	public Response pushNotification(JSONObject requestObject) throws Exception {

		final var cpNotification = gson.fromJson(requestObject.optJSONObject("notification").toString(),
				CPNotification.class);		
		
		if (cpNotification.audienceType.equals(NotificationUtils.AudienceType.UNIVERSAL.value))
			return pushUniversalNotification(cpNotification);
		else if (cpNotification.audienceType.equals(NotificationUtils.AudienceType.PERSONALIZED.value))
			return pushPersonalizedNotification(cpNotification);
		else
			return new OneResponse()
					.getFailureResponse(new LocalResponse().setMessage("Invalid audience type.").toJson());

	}

	// ****************************************************************** //
	// ****************** END OF COMMON IMPLEMENATION ******************* //
	// ****************************************************************** //

	// ------------------------------------------------------------------ //
	// ---------- START OF UNIVERSAL NOTIFICATION IMPLMENTATION --------- //
	// ------------------------------------------------------------------ //

	public Response pushUniversalNotification(CPNotification cpNotification) throws Exception {

		//ArrayList<RegistrationKey> registrationKeys = new ArrayList<>();

		String currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();

		cpNotification.createDatetime = currentDateTime;

		if (!BasicUtils.isNotNullOrNA(cpNotification.datetime))
			cpNotification.datetime = currentDateTime;

		cpNotification.platform = NotificationUtils.Platform.ALL.value;
		cpNotification.scheduleType = cpNotification.shouldSchedule ? ScheduleType.LATER.value : ScheduleType.NOW.value;
		cpNotification.schedulerId = aUser.id;
		cpNotification.schedulerName = aUser.name;

		if (BasicUtils.isNotNullOrNA(cpNotification.imageFile)) {

			final var decoder = Base64.decodeBase64(cpNotification.imageFile);

			Tika tika = new Tika();
			String fileType = BasicUtils.MimeMap.mapMimetoExt(tika.detect(decoder));

			final var S3FileName = (DateTimeUtils.getCurrentDateTimeInIST() + "_" + aUser.id + "_" + "Notification_Image"
					+ fileType).replace(" ", "_");

			if (!s3Client().uploadImage(S3FileName, cpNotification.imageFile, S3BucketPath.RESOURCE_NOTIFICATION)) {

				log("pushUniversalNotification - Failed to upload Image file to S3");

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			cpNotification.imageUrl = (Constants.IS_PRODUCTION ? Constants.S3BUCKET_PROD : Constants.S3BUCKET_TEST)
					+ "notification/" + S3FileName;
		}

		if (BasicUtils.isNotNullOrNA(cpNotification.thumbnailFile)) {

			final var decoderTN = Base64.decodeBase64(cpNotification.thumbnailFile);

			Tika tika = new Tika();
			String fileType = BasicUtils.MimeMap.mapMimetoExt(tika.detect(decoderTN));

			final var S3TNFileName = (DateTimeUtils.getCurrentDateTimeInIST() + "_" + aUser.id + "_" + "Notification_Thumbnail"
					+ fileType).replace(" ", "_");

			if (!s3Client().uploadImage(S3TNFileName, cpNotification.thumbnailFile,
					S3BucketPath.RESOURCE_NOTIFICATION)) {

				log("pushUniversalNotification - Failed to upload Thumbnail file to S3");

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			cpNotification.thumbnailUrl = (Constants.IS_PRODUCTION ? Constants.S3BUCKET_PROD : Constants.S3BUCKET_TEST)
					+ "notification/" + S3TNFileName;
		}

		if (!notificationRepo().saveCpNotification(cpNotification)) {

			log("pushUniversalNotification - Failed to insert new notification in DB.");
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		//registrationKeys = notificationRepo().getAllRegistrationKeys();
		 
		
//		final var notificationTokens = notificationRepo().getAllUserNotificationToken();
		final var notificationTokens = notificationRepo().getAllUserNotificationTokens();		
		
			
		System.out.println("pushUniversalNotification  -- notificationTokensSize " + notificationTokens.size());

		if (notificationTokens.size() == 0)
			return new OneResponse().getFailureResponse(new LocalResponse()
					.setMessage("No notification registration keys found for the given condition.").toJson());

		
		// New service for background method calling
		
		ExecutorService service = Executors.newFixedThreadPool(4);
	    service.submit(new Runnable() {
	        public void run() {
	        	
	        	try {
					scheduleUniversalNotification(cpNotification, notificationTokens);
				} catch (ParseException e) {
					
					log("pushUniversalNotification - Failed to run scheduleUniversalNotification");
					e.printStackTrace();
				}
	         
	        }
	    });
		
		
		new NotificationUtils().sendNotificationSchedulingInitiationMail("Manual", cpNotification, aUser);

		return new OneResponse().getSuccessResponse(
				new LocalResponse().setStatus(true).setMessage("Notifications has been schedulled successfully. "
						+ "You'll get an email once all notifications has been pushed.").toJson());

	}

	private void scheduleUniversalNotification(CPNotification cpNotification,
			ArrayList<UserNotificationTokenPair> registrationKeys) throws ParseException {

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 10);
		Date time = calendar.getTime();

		if (cpNotification.shouldSchedule) {
			//LoggerUtils.log("--- time before converting: " + cpNotification.datetime);
			time = DateTimeUtils.getDateFromDateTimeString(
					DateTimeUtils.convertISTtoGMT(cpNotification.datetime, DateTimeFormat.yyyy_MM_dd_HH_mm_ss),
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss);
			//LoggerUtils.log("--- time after converting: " + time.toString());
		}

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						cpNotification.sentDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						// JSONArray messageSuccessIds = new JSONArray();
						// JSONArray messageFailedIds = new JSONArray();

						int successCount = 0;
						int failureCount = 0;

						//ArrayList<RegistrationKey> selectedRegistrationTokens = new ArrayList<>();
						ArrayList<UserNotificationToken> selectedRegistrationTokens = new ArrayList<>();
						boolean shouldPush = false;

						for (int i = 0; i < registrationKeys.size(); i++) {

							//RegistrationKey currentKey = registrationKeys.get(i);
							final var currentKey = registrationKeys.get(i);							
							selectedRegistrationTokens.add(currentKey.userNotificationToken);

							if (i == registrationKeys.size() - 1 && selectedRegistrationTokens
									.size() <= NotificationUtils.MAX_NOTIFICATION_THRESHOLD) {
								shouldPush = true;
							} else if (selectedRegistrationTokens
									.size() == NotificationUtils.MAX_NOTIFICATION_THRESHOLD) {
								shouldPush = true;
							}

							if (shouldPush) {

								LoggerUtils.log("Sending notification to: " + selectedRegistrationTokens.size());

								BatchResponse response = sendMulticastMessage(cpNotification,
										selectedRegistrationTokens);

								successCount += response.getSuccessCount();
								failureCount += response.getFailureCount();

								/*
								 * for (int n = 0; n < response.getResponses().size(); n++) {
								 * 
								 * SendResponse current = response.getResponses().get(n);
								 * 
								 * if (current.isSuccessful()) messageSuccessIds.put(current.getMessageId());
								 * else messageFailedIds.put(current.getMessageId());
								 * 
								 * }
								 */

								selectedRegistrationTokens.clear();
								shouldPush = false;
								
								// TODO: Do a lil delay here
								
								Thread.sleep(2000);

							}
						}

						LoggerUtils.log("==> Total messages status -  Success: " + successCount + " | Failure: "
								+ failureCount);

						cpNotification.totalCount = registrationKeys.size();
						cpNotification.successCount = successCount;
						cpNotification.failureCount = failureCount;
						cpNotification.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						cpNotification.isScheduled = true;
						
						if (notificationRepo().saveCpNotification(cpNotification))
							LoggerUtils.log("==> Notification updated in DB successfully after pushing.");
						else
							LoggerUtils.log("==> Failed to update Notification in DB after pushing.");

						LoggerUtils.log("Universal notification send successfully. Iteration : " + count);

						new NotificationUtils().sendNotificationConfirmationEmail(cpNotification,
								registrationKeys.size(), successCount, failureCount, aUser);

						toolkit.beep();
						timer.cancel();

					} catch (Exception e) {

						LoggerUtils.log("Error while while scheduling Universal notification : " + e.getMessage());
						e.printStackTrace();
						count++;
						toolkit.beep();
						LoggerUtils.log("Universal notification task rescheduled, Iteration : " + count);
					}

				} else {

					LoggerUtils.log("Retry count exhausted while scheduling Universal notification.");
					toolkit.beep();
					timer.cancel();
				}

			}
		}, time, 10000);

	}

	// ******************************************************************************
	// //
	// **************** END OF UNIVERSAL NOTIFICATION IMPLEMENTATION
	// **************** //
	// ******************************************************************************
	// //

	// ------------------------------------------------------------------------------
	// //
	// ----------- START OF PERSONALIZED NOTIFICATION IMPLEMENTATION
	// ---------------- //
	// ------------------------------------------------------------------------------
	// //

	public Response pushPersonalizedNotification(CPNotification cpNotification) throws Exception {

		final var currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();

		if (BasicUtils.isNotNullOrNA(cpNotification.imageFile)) {

			final var decoderPersonalised = Base64.decodeBase64(cpNotification.imageFile);

			Tika tika = new Tika();
			String fileType = BasicUtils.MimeMap.mapMimetoExt(tika.detect(decoderPersonalised));

			final var S3FileNamePersonalised = (DateTimeUtils.getCurrentDateTimeInIST() + "_" + aUser.id + "_"
					+ "Notification_Image" + fileType).replace(" ", "_");

			if (!s3Client().uploadImage(S3FileNamePersonalised, cpNotification.imageFile,
					S3BucketPath.RESOURCE_NOTIFICATION)) {

				log("pushPersonalizedNotification - Failed to upload Image file to S3");

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			cpNotification.imageUrl = (Constants.IS_PRODUCTION ? Constants.S3BUCKET_PROD : Constants.S3BUCKET_TEST)
					+ "notification/" + S3FileNamePersonalised;
		}

		if (BasicUtils.isNotNullOrNA(cpNotification.thumbnailFile)) {

			final var decoderPersonalisedTN = Base64.decodeBase64(cpNotification.thumbnailFile);

			Tika tika = new Tika();
			String fileType = BasicUtils.MimeMap.mapMimetoExt(tika.detect(decoderPersonalisedTN));

			final var S3FileNamePersonalisedTN = (DateTimeUtils.getCurrentDateTimeInIST() + "_" + aUser.id + "_"
					+ "Notification_Thumbnail" + fileType).replace(" ", "_");

			if (!s3Client().uploadImage(S3FileNamePersonalisedTN, cpNotification.thumbnailFile,
					S3BucketPath.RESOURCE_NOTIFICATION)) {

				log("pushPersonalizedNotification - Failed to upload Thumbnail file to S3");

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			cpNotification.thumbnailUrl = (Constants.IS_PRODUCTION ? Constants.S3BUCKET_PROD : Constants.S3BUCKET_TEST)
					+ "notification/" + S3FileNamePersonalisedTN;
		}

		/*
		 * Get the file csv file base64 from request, Store it in local and in S3 bucket
		 */

		final var decoder = Base64.decodeBase64(cpNotification.file);

		final var S3FileName = (DateTimeUtils.getCurrentDateTimeInIST() + "_" + aUser.id + "_" + "Notification.csv")
				.replace(" ", "_");

		final var tempFileName = Constants.UPLOAD_FILE_LOCAL_SERVER + S3FileName;
		final var file = new File(tempFileName);
		FileUtils.writeByteArrayToFile(file, decoder);

		final var reader = Files.newBufferedReader(Paths.get(tempFileName));

		/*
		 * Check whether all the header of the CSV file are in order
		 */
		final var csvReader = new CSVReader(reader);
		final var nextRecord = csvReader.readNext();

		if (!Arrays.equals(nextRecord, NotificationUtils.notificationCsvHeader)) {
			file.delete();
			csvReader.close();
			return new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
					.setMessage("Invalid CSV format").setError(Errors.INVALID_DATA.value).toJson());
		}

		if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

			if (!s3Client().uploadImage(S3FileName, cpNotification.file, S3BucketPath.NOTIFICATION)) {

				log("pushPersonalizedNotification - Failed to upload Notificaiton file to S3");
				file.delete();
				csvReader.close();
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

		}

		/*
		 * Get all the personalized notification from the CSV file
		 */
		final var csvNotifications = new CsvToBeanBuilder<NotificationCSV>(reader).withType(NotificationCSV.class)
				.withIgnoreLeadingWhiteSpace(true).withSkipLines(0).build();

		final var csvNotificationIterator = csvNotifications.iterator();

		final var dynamicNotification = new ArrayList<NotificationCSV>();

		while (csvNotificationIterator.hasNext()) {
			dynamicNotification.add(csvNotificationIterator.next());
		}

		csvReader.close();
		file.delete();

		if (dynamicNotification.isEmpty()) {
			log("pushPersonalizedNotification - Notification list in the csv is empty.");
			return new OneResponse()
					.getFailureResponse(new LocalResponse().setStatus(false).setMessage("No data found in the CSV")
							.setError(Errors.INVALID_DATA.value).setAction(Actions.FIX_RETRY.value).toJson());

		}

		/*
		 * Create a list of loan account numbers of customers and fetch notification keys for
		 * these customers only
		 */

		
		ArrayList<String> loanAccountNumbers = new ArrayList<>();
		for (var dn : dynamicNotification)
			loanAccountNumbers.add(dn.loanAccountNumber);

		final var userNotificationKeys = notificationRepo()
				.getUserNotificationTokensByLAI("'" + String.join("','", loanAccountNumbers) + "'");

		if (userNotificationKeys.size() == 0)
			return new OneResponse()
					.getFailureResponse(new LocalResponse().setMessage("No registration keys found.").toJson());

		/*
		 * Insert new notification in the DB
		 */

		cpNotification.createDatetime = currentDateTime;

		LoggerUtils.log("Notification: cpNotification.datetime " + cpNotification.datetime);
		if (!BasicUtils.isNotNullOrNA(cpNotification.datetime))
			cpNotification.datetime = currentDateTime;

		LoggerUtils.log("Notification: cpNotification.datetime " + cpNotification.datetime);
		cpNotification.platform = NotificationUtils.Platform.ALL.value;
		cpNotification.scheduleType = cpNotification.shouldSchedule ? ScheduleType.LATER.value : ScheduleType.NOW.value;
		cpNotification.schedulerId = aUser.id;
		cpNotification.schedulerName = aUser.name;

		if (!notificationRepo().saveCpNotification(cpNotification)) {

			log("pushPersonalizedNotification - Failed to insert new notification in DB.");
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		if (cpNotification.hasDynamicContent) {

			/*
			 * Notification has dynamic content Each customer will have personalized message
			 */
			

			for (var unk : userNotificationKeys) {

					
				final var dno = dynamicNotification.stream().filter(n -> n.loanAccountNumber.equals(unk.user.loanAccountNumber))
						.findFirst();

				
				/*
				 * Add the custom notification received in the CSV and add them for each item in
				 * userNotificationKeys
				 */

				if (dno.isPresent())
					unk.customNotification = dno.get();

			}

		}
		
		        // New service for background method calling
		
				ExecutorService service = Executors.newFixedThreadPool(4);
			    service.submit(new Runnable() {
			        public void run() {
			        	
			        	try {
							schedulePersonalizedNotification(cpNotification, userNotificationKeys, true);
						} catch (ParseException e) {
							log("pushPersonalizedNotification - Failed to run schedulePersonalizedNotification");
							e.printStackTrace();
						}
			         
			        }
			    });

		

		new NotificationUtils().sendNotificationSchedulingInitiationMail("Manual", cpNotification, aUser);

		return new OneResponse().getSuccessResponse(
				new LocalResponse().setStatus(true).setMessage("Notifications has been schedulled successfully. "
						+ "You'll get an email once all notifications has been pushed.").toJson());

	}

	public JSONObject scheduleCustomerBirthdayNotification(JSONObject requestJson) throws Exception {

		String currentDateTime = DateTimeUtils.getCurrentDateTimeInIST();

		String currentDate = DateTimeUtils.getStringFromDateTimeString(currentDateTime,
				DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.dd);

		String currentMonth = DateTimeUtils.getStringFromDateTimeString(currentDateTime,
				DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.MM);

		ArrayList<SFContact> contactsWithBD = sfnManager.getCustomersWithBirthday(currentDate, currentMonth);

		ArrayList<String> mobileNumbersWithBD = new ArrayList<>();
		for (SFContact sfContact : contactsWithBD)
			mobileNumbersWithBD.add(sfContact.mobilePhone);
		
		final var userNotificationKeys = notificationRepo()
				.getUserNotificationTokensByMobile("'" + String.join("','", mobileNumbersWithBD) + "'");

		if (userNotificationKeys.size() == 0)
			return new LocalResponse().setMessage("No registration keys found.").toJson();

		final var cpNotification = new CPNotification();

		cpNotification.audienceType = NotificationUtils.AudienceType.PERSONALIZED.value;

		String nTitle = "Happy Birthday!";
		String nMessage = "Hi " + NotificationUtils.DV_FIRST_NAME
				+ ", HomeFirst wishes you a day filled with happiness and a year filled with joy.";

		cpNotification.title = nTitle;
		cpNotification.message = nMessage;
		cpNotification.bigMessage = nMessage;
		cpNotification.kind = NotificationUtils.NotificationKind.TRANSACTIONAL.value;
		cpNotification.priority = NotificationUtils.NotificationPriority.HIGH.value;
		cpNotification.screenToOpen = Constants.DEFAULT;
		cpNotification.onClickAction = NotificationUtils.OnClickAction.IN_APP.value;
		cpNotification.shouldSchedule = true;
		cpNotification.platform = "all";
		cpNotification.scheduleType = "later";
		cpNotification.isScheduled = true;
		cpNotification.schedulerName = aUser.name;
		cpNotification.schedulerId = aUser.id;
		cpNotification.isValid = true;
		cpNotification.data = null;
		cpNotification.hasDynamicContent = true;
		
		String scheduleTime = requestJson.optString(Constants.SCHEDULE_TIME, "09:00:00");
		cpNotification.datetime = DateTimeUtils.getStringFromDateTimeString(currentDateTime,
				DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.yyyy_MM_dd) + " " + scheduleTime;

		cpNotification.createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

		if (!notificationRepo().saveCpNotification(cpNotification)) {

			log("scheduleCustomerBirthdayNotification - Failed to insert new notification in DB.");
			return new LocalResponse().setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson();

		}
		
		/*
		 * Loop through all the notification tokens, 
		 * and create customNotification for each of them, 
		 * for setting up the message dynamically for each customer.
		 * 
		 * # 
		 * This is required to support the new batch notification implementation,
		 * for sending dynamic messages.  
		 * #
		 * */
		for (var unk : userNotificationKeys) {
			
			if (null == unk.customNotification) {
				
				final var customNotification = new NotificationCSV();
				
				final var firstName = unk.user.name.split(" ")[0];
				customNotification.message = cpNotification.message.replace(NotificationUtils.DV_FIRST_NAME, firstName);
				customNotification.title = cpNotification.title;
								
				unk.customNotification = customNotification; 
				
				
			}
			
		}
		
		schedulePersonalizedNotification(cpNotification, userNotificationKeys, true);	
		
		new NotificationUtils().sendNotificationSchedulingInitiationMail("CronJob", cpNotification, aUser);

		return new LocalResponse().setStatus(true).setMessage("Notifications has been schedulled successfully. "
				+ "You'll get an email once all notifications has been pushed.").toJson();

	}

	public void schedulePersonalizedNotification(CPNotification cpNotification,
			ArrayList<UserNotificationTokenPair> userNotificationKeys, boolean shouldSendEmail) throws ParseException {

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 10);
		Date time = calendar.getTime();

		if (cpNotification.shouldSchedule) {
			time = DateTimeUtils.getDateFromDateTimeString(
					DateTimeUtils.convertISTtoGMT(cpNotification.datetime, DateTimeFormat.yyyy_MM_dd_HH_mm_ss),
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss);
			LoggerUtils.log("Notification: time" + gson.toJson(time));
		}

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						cpNotification.sentDatetime = DateTimeUtils.getCurrentDateTimeInIST();

						int totalCount = 0;
						int successCount = 0;
						int failureCount = 0;
						
						if (cpNotification.hasDynamicContent) {
						
							final var selectedUserNotificationKeys = new ArrayList<UserNotificationTokenPair>();
							
							for (var i = 0; i < userNotificationKeys.size(); i++) {
								
								final var unKey = userNotificationKeys.get(i);
								totalCount++;
								
								selectedUserNotificationKeys.add(unKey);
								
								var shouldPush = false;
								
								if (i == userNotificationKeys.size() - 1
										&& selectedUserNotificationKeys.size() <= NotificationUtils.MAX_NOTIFICATION_THRESHOLD) {
									shouldPush = true;
								} else if (selectedUserNotificationKeys.size() == NotificationUtils.MAX_NOTIFICATION_THRESHOLD) {
									shouldPush = true;
								}
								
								if (shouldPush) {
									
									final var cpn = cpNotification.asCopy();
									
									final var response = sendDynamicMulticastMessages(cpn, selectedUserNotificationKeys);
								
									successCount += response.getSuccessCount();
									failureCount += response.getFailureCount();
								
									shouldPush = false;
									selectedUserNotificationKeys.clear();
									Thread.sleep(2000);
									
									
								}
								
								
							}												
							
						} else {
						
							ArrayList<UserNotificationToken> selectedKeys = new ArrayList<>();
							
							for (var i = 0; i < userNotificationKeys.size(); i++) {

								final var unKey = userNotificationKeys.get(i);
								
								totalCount++;
								
								boolean shouldPush = false;

								selectedKeys.add(unKey.userNotificationToken);								

								if (i == userNotificationKeys.size() - 1
										&& selectedKeys.size() <= NotificationUtils.MAX_NOTIFICATION_THRESHOLD) {
									shouldPush = true;
								} else if (selectedKeys.size() == NotificationUtils.MAX_NOTIFICATION_THRESHOLD) {
									shouldPush = true;
								}

								if (shouldPush) {

									LoggerUtils.log("Sending personalized notification to: " + selectedKeys.size());

									final var cpn = cpNotification.asCopy();

									if (null != unKey.customNotification) {									

										cpn.title = unKey.customNotification.title;
										cpn.message = unKey.customNotification.message;
										cpn.bigMessage = unKey.customNotification.message;

										if (BasicUtils.isNotNullOrNA(unKey.customNotification.webUrl))
											cpn.webUrl = unKey.customNotification.webUrl;

									} else if (cpn.message.contains(NotificationUtils.DV_FIRST_NAME)
											|| cpn.bigMessage.contains(NotificationUtils.DV_FIRST_NAME)) {
										String firstName = unKey.user.name.split(" ")[0];
										cpn.message = cpn.message.replace(NotificationUtils.DV_FIRST_NAME, firstName);
										cpn.bigMessage = cpn.bigMessage.replace(NotificationUtils.DV_FIRST_NAME,
												firstName);
									}

									BatchResponse response = sendMulticastMessage(cpn, selectedKeys);
									successCount += response.getSuccessCount();
									failureCount += response.getFailureCount();

									shouldPush = false;
									selectedKeys.clear();
									
									Thread.sleep(2000);

								}

							}

							
						}
						
						log("schedulePersonalizedNotification - Total messages status -  Success: " + successCount
								+ " | Failure: " + failureCount);

						cpNotification.totalCount = totalCount;
						cpNotification.successCount = successCount;
						cpNotification.failureCount = failureCount;
						cpNotification.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						cpNotification.isScheduled = true;
						
						if (notificationRepo().saveCpNotification(cpNotification))
							log("schedulePersonalizedNotification - Personalized notification updated in DB successfully after pushing.");
						else
							LoggerUtils.log(
									"schedulePersonalizedNotification - Failed to update Personalized Notification in DB after pushing.");

						LoggerUtils.log("Personalized notification send successfully. Iteration : " + count);

						if (shouldSendEmail)
							new NotificationUtils().sendNotificationConfirmationEmail(cpNotification, totalCount,
									successCount, failureCount, aUser);

						timer.cancel();

					} catch (Exception e) {
						LoggerUtils.log("Error while while scheduling personalized notification : " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Personalized notification task rescheduled, Iteration : " + count);
					}

				} else {
					LoggerUtils.log("Retry count exhausted while scheduling personalized notification.");
					timer.cancel();
				}

			}
		}, time, 10000);

	}

	// ***************************************************************************
	// //
	// *********** END OF PERSONALIZED NOTIFICATION IMPLEMENTATION ***************
	// //
	// ***************************************************************************
	// //

}
