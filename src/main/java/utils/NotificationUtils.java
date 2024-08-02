package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import models.admin.AdminUser;
import models.notification.CPNotification;
import utils.MailUtils.ContentType;

public class NotificationUtils {

	public static final int MAX_NOTIFICATION_THRESHOLD = 500;  
	public static final String DV_FIRST_NAME = "$$FIRST_NAME$$";

	public void initFirebase() throws IOException {
		
		boolean hasBeenInitialized = false;
		List<FirebaseApp> firebaseApps = FirebaseApp.getApps();
		for (FirebaseApp app : firebaseApps) {
			if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
				hasBeenInitialized = true;
			}
		}

		if (!hasBeenInitialized) {

			FileInputStream serviceAccount = new FileInputStream(
					new File(getClass().getClassLoader().getResource("hffc_customer_portal_firebase.json").getFile()));

					
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccount))
					.setDatabaseUrl("https://hffc-customer-portal.firebaseio.com").build();
		
			FirebaseApp.initializeApp(options);
		}
		
	}
	
	public enum AppScreens {
		DEFAULT("default"), SERVICE_REQUEST("service_Request"), ADD_SERVICE_REQUEST("add_service_request"),
		SPECIFIC_SERVICE_RESPONSE("specific_service_response"), USER_PROFILE("user_profile"),
		ACCOUNT_STATEMENT("account_statement"), PAYMENT("payment"), PAYMENT_SUMMARY("payment_summary"), LOAN("loan"),
		BOUNCE_CHARGE_PAYMENT("bounce_charge_payment"), SPECIFIC_AMOUNT_PAYMENT("specific_amount_payment"),
		WISHES("wishes"), DOCUMENT_CENTER("document_center"), DISBURSEMENT("disbursement"),
		DISBURSEMENT_CONSENT("disbursement_consent"), INTEREST_RATE_CHANGE("interest_rate_change"),
		FEEDBACK("feedback"), BRANCH_LOCATOR("branch_locator"), APPLY_TOP_UP_LOAN("apply_top_up_loan"),
		APPLY_INSURANCE_LOAN("apply_insurance_loan"), REFER_A_FRIEND("refer_a_friend"), AUTO_PREPAY("auto_prepay"),
		PAYMENT_RECEIPT("payment_receipt");

		public final String value;

		AppScreens(String value) {
			this.value = value;
		}

	}
	
	public enum OnClickAction {
		IN_APP("inApp"), WEB("web");

		public final String value;

		OnClickAction(String value) {
			this.value = value;
		}

	}

	public enum ScheduleType {
		NOW("now"), LATER("later");

		public final String value;

		ScheduleType(String value) {
			this.value = value;
		}

	}

	public enum NotificationPriority {
		HIGH("H"), MEDIUM("M"), LOW("L");

		public final String value;

		NotificationPriority(String value) {
			this.value = value;
		}
	}

	public enum NotificationKind {
		PROMOTIONAL("promotional"), TRANSACTIONAL("transactional");

		public final String value;

		NotificationKind(String value) {
			this.value = value;
		}
	}

	public enum AudienceType {
		UNIVERSAL("universal"), PERSONALIZED("personalized"), SINGLE_USER("singleUser");

		public final String value;

		AudienceType(String value) {
			this.value = value;
		}
	}

	public enum Platform {
		ANDROID("android"), iOS("iOS"), APP("app"), WEB("web"), ALL("all");

		public final String value;

		Platform(String value) {
			this.value = value;
		}
	}

	public enum NotificationFetchType {
		TOP("TOP"), BOTTOM("BOTTOM"), FIRST("FIRST");

		public final String value;

		NotificationFetchType(String value) {
			this.value = value;
		}

		public static NotificationFetchType get(String value) {
			for (NotificationFetchType nft : NotificationFetchType.values()) {
				if (nft.value.equals(value))
					return nft;
			}
			return null;
		}

	}
	
	public enum NotificationServiceType {
		APNS("APNS"), FIREBASE("FIREBASE");

		public final String value;

		NotificationServiceType(String value) {
			this.value = value;
		}
	}

	public static boolean shouldDryRun() {
		return !Constants.IS_NOTIFICATION_LIVE;
	}
	
	public static String[] notificationCsvHeader = new String[] { "loanAccountNumber", "title",
			"message", "webUrl" };

	public void sendNotificationConfirmationEmail(CPNotification cpNotification, int totalCount, int successCount,
			int failedCount, AdminUser aUser) {

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 10);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}

						StringBuilder sb = new StringBuilder();

						sb.append("Voila! All notifications have been successfully pushed.");

						sb.append("\n\n\n============== Notification Info ================");
						sb.append("\n\n ID: " + cpNotification.id);
						sb.append("\n Title: " + cpNotification.title);
						sb.append("\n Message : " + cpNotification.message);
						sb.append("\n Big message : " + cpNotification.bigMessage);
						sb.append("\n Create datetime : " + cpNotification.createDatetime);
						sb.append("\n Schedule datetime : " + cpNotification.datetime);
						sb.append("\n Sent datetime : " + cpNotification.sentDatetime);
						sb.append("\n Total count: " + totalCount);
						sb.append("\n Success count: " + successCount);
						sb.append("\n Failure count: " + failedCount);

						sb.append("\n\n\n============== Admin Info ================");
						sb.append("\n\n Name: " + aUser.name);
						sb.append("\n Email: " + aUser.email);

						sb.append("\n\n\nThis is an automatic email generated by HomeFirst Customer Portal.");
						sb.append("\nPlease do not reply to this email.");

						MailUtils.getInstance().sendDefaultMail(ContentType.TEXT_PLAIN,
								"Notification Scheduled | Customer Portal", sb.toString(),
								"ranan.rodrigues@homefirstindia.com", aUser.email);

						LoggerUtils.log("Notifcation email has been sent successfully.");
						timer.cancel();

					} catch (Exception e) {
						LoggerUtils.log("Error while while notification schedule email : " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Notifcatin email task rescheduled, Iteration : " + count);
					}

				} else {
					LoggerUtils.log("Retry count exhausted while sending notifcatin email.");
					timer.cancel();
				}

			}
		}, time, 10000);

	}

	public void sendNotificationSchedulingInitiationMail(String source, CPNotification cpNotification,
			AdminUser aUser) {

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 5);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}

						StringBuilder sb = new StringBuilder();

						sb.append(
								"Notification scheduling process has been initiated. You'll receive another Email once all notifications have been pushed.");

						sb.append("\n\n\n============== Notification Info ================");
						sb.append("\n\n ID: " + cpNotification.id);
						sb.append("\n Title: " + cpNotification.title);
						sb.append("\n Message : " + cpNotification.message);
						sb.append("\n Big message : " + cpNotification.bigMessage);
						sb.append("\n Create datetime : " + cpNotification.createDatetime);
						sb.append("\n Schedule datetime : " + cpNotification.datetime);
						sb.append("\n Initiation Source: " + source);

						sb.append("\n\n\n============== Admin Info ================");
						sb.append("\n\n Name: " + aUser.name);
						sb.append("\n Email: " + aUser.email);

						sb.append("\n\n\nThis is an automatic email generated by HomeFirst Customer Portal.");
						sb.append("\nPlease do not reply to this email.");

						MailUtils.getInstance().sendDefaultMail(ContentType.TEXT_PLAIN,
								"Notification Scheduling Initiated | Customer Portal", sb.toString(),
								"ranan.rodrigues@homefirstindia.com", aUser.email);

						LoggerUtils.log("Notification initiation email has been sent successfully.");
						timer.cancel();

					} catch (Exception e) {
						LoggerUtils.log("Error while while Notification initiation email : " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Notification initiation email task rescheduled, Iteration : " + count);
					}

				} else {
					LoggerUtils.log("Retry count exhausted while sending Notification initiation email.");
					timer.cancel();
				}

			}
		}, time, 5000);

	}

}
