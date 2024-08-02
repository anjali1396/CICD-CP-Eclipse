package jugaad;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;

import dao.BreachedPasswordRepository;
import dao.UserRepository;
import models.BreachedPassword;
import models.ROIReprice;
import models.SecondaryInfo;
import models.User;
import models.notification.RegistrationKey;
import models.notification.UserNotificationToken;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.HibernateUtil;
import utils.LoggerUtils;
import utils.OneResponse;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.NotificationUtils.NotificationServiceType;
import v1.repository.NotificationRepository;

public class MyJugaadStuff {

	private static NotificationRepository nRepo = new NotificationRepository();
	private UserRepository uRepo = new UserRepository();
	private static BreachedPasswordRepository bRepo = new BreachedPasswordRepository();

	private static void printLog(String value) {
		System.out.println("MyJugaadStuff." + value);
	}

	public static void main(String[] args) {

		/*
		 * 1. Get all users 2. Get all secondary info 3. parse fcmKeys and Apns Keys
		 * from SI 4. Get only the last 5 from the array list of tokens for each users
		 * 5. Uses token data to create a UerNoticationToken for each token. 6. Save in
		 * bulk, with batches and time detay like 1-2 seconds,
		 * 
		 * note: - run on local, as you already have prod db on your local machine -
		 * 
		 * 
		 * 
		 */

		try {

			updateEncryptedBreachedPasswords();

			// migrateAllNotificationKeys();

//			User fetchedUser = new UserRepository().findUserByUserId(76220);
//			
//			utils.DatabaseHelper v1dbHelper = new utils.DatabaseHelper();
//
//			boolean dbStatus = v1dbHelper.updateMobileNumber(fetchedUser, "9821800821", "+91");
//			v1dbHelper.close();

			// calculateRemainingTenure();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static boolean updateEncryptedBreachedPasswords() throws Exception {

		final var ePwds = new ArrayList<BreachedPassword>();

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			Transaction tx = session.beginTransaction();

			var unencryptedPasswords = bRepo.findUnencryptedPasswords();

			for (var i = 0; i < unencryptedPasswords.size(); i++) {

				final var currentPassword = unencryptedPasswords.get(i);

				currentPassword.encryptedPassword = BasicUtils.getTheSecureKeyTemp(currentPassword.password);
				ePwds.add(currentPassword);

			}

			for (var i = 0; i < ePwds.size(); i++) {

				final var ePwd = ePwds.get(i);
				session.update(ePwd);

				if (i % Constants.HITBERNATE_BATCH_SIZE == 0 || i == ePwds.size() - 1) {
					session.flush();
					session.clear();
				}

			}

			System.out.println("updateEncryptedBreachedPasswords - Success!!!!!!");

			tx.commit();
			session.close();
			return true;

		} catch (Exception e) {
			System.out.println("updateEncryptedBreachedPasswords - Error : " + e.getMessage());
			e.printStackTrace();
		}

		return false;

	}

	

	public static ArrayList<UserNotificationToken> migrateAllNotificationKeys() throws Exception {

		final var rKeys = new ArrayList<UserNotificationToken>();

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			final var secondaryInfos = session.createQuery("from SecondaryInfo", SecondaryInfo.class).getResultList();

			printLog("migrateAllNotificationKeys - total secondaryInfos: " + secondaryInfos.size());

			var processedUserIds = new ArrayList<Integer>();

			var totalFirebaseKeys = 0;
			var totalApnsKeys = 0;

			for (var secondaryInfo : secondaryInfos) {

				if (processedUserIds.contains(secondaryInfo.userId))
					continue;

				if (null != secondaryInfo.fcmKey) {

					JSONArray tokenJsonArray = new JSONArray(secondaryInfo.fcmKey);

					final var currentTotalFCMKeys = tokenJsonArray.length();

					// printLog("migrateAllNotificationKeys - currentTotalFCMKeys : " +
					// currentTotalFCMKeys);

					// for (int i = currentTotalFCMKeys - 1)

					for (int i = 0; i < currentTotalFCMKeys; i++) {

						totalFirebaseKeys++;

						// printLog("migrateAllNotificationKeys - i : " + i + " | currentTotalFCMKeys -
						// 1 : " + ((currentTotalFCMKeys - 1) - i));

						final var currentTokenObject = tokenJsonArray.getJSONObject((currentTotalFCMKeys - 1) - i);

						rKeys.add(new UserNotificationToken(secondaryInfo.userId,
								currentTokenObject.optString("key", null),
								currentTokenObject.optString("device_id", null),
								currentTokenObject.optString("device_type", null), null,
								NotificationServiceType.FIREBASE.value, true));

						if (i == 3 || i == currentTotalFCMKeys - 1) {
							// printLog("migrateAllNotificationKeys - Breaking fcm for userId : " +
							// secondaryInfo.userId);
							break;
						}

					}
				}

				// System.out.println("\n");

				if (null != secondaryInfo.apnsKey) {
					JSONArray tokenJsonArray = new JSONArray(secondaryInfo.apnsKey);

					final var currentTotalAPNSKeys = tokenJsonArray.length();

					for (int i = 0; i < currentTotalAPNSKeys; i++) {

						totalApnsKeys++;

						final var currentTokenObject = tokenJsonArray.getJSONObject((currentTotalAPNSKeys - 1) - i);

						rKeys.add(new UserNotificationToken(secondaryInfo.userId,
								currentTokenObject.get("key").toString(),
								currentTokenObject.get("device_id").toString(),
								currentTokenObject.get("device_type").toString(), null,
								NotificationServiceType.APNS.value, true));

						if (i == 2 || i == currentTotalAPNSKeys - 1)
							break;

					}

				}

				processedUserIds.add(secondaryInfo.userId);

			}

			printLog("migrateAllNotificationKeys - total rKeys: " + rKeys.size());
			printLog("migrateAllNotificationKeys - total totalFirebaseKeys: " + totalFirebaseKeys);
			printLog("migrateAllNotificationKeys - total totalApnsKeys: " + totalApnsKeys);

		} catch (Exception e) {
			System.out.println("getAllSecondaryInfosAndKeys - Error : " + e.getMessage());
			e.printStackTrace();
		}

//		var batchCount = 0;
//		var currentBatch = new ArrayList<UserNotificationToken>();
//		for (var key: rKeys) {
//			
//			currentBatch.add(key);
//			
//			if 
//			
//			
//		}

		if (!batchUpdateUNT(rKeys)) {
			System.out.println("getAllSecondaryInfosAndKeys - Failure : Batch Update Failed");

		} else
			System.out.println("getAllSecondaryInfosAndKeys - Success");

		return rKeys;

	}

	public static boolean batchUpdateUNT(ArrayList<UserNotificationToken> unts) throws Exception {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			Transaction tx = session.beginTransaction();

			for (var i = 0; i < unts.size(); i++) {

				final var userNotification = unts.get(i);

				session.saveOrUpdate(userNotification);

				if (i % Constants.HITBERNATE_BATCH_SIZE == 0) {
					session.flush();
					session.clear();
				}

			}

//			unts.forEach((unt) -> {
//				session.saveOrUpdate(unt);
//			});

			tx.commit();
			session.close();
			return true;

		} catch (Exception e) {

			LoggerUtils.log("updateUNT: Error : " + e.getMessage());
			e.printStackTrace();

		}
		return false;
	}

	public static int calculateRemainingTenure() throws Exception {

		var monthlyroi = 12.7500 / 1200;

		final var calculatedTenure = ((Math.log(7590.55) - Math.log(7590.55 - (462003.98 * monthlyroi)))
				/ Math.log(1 + monthlyroi));

		// int roundOffIntTenure = (int) calculatedTenure;

		int roundOffIntTenure = (int) Math.round(calculatedTenure);

		LoggerUtils.log("roundOffIntTenure " + roundOffIntTenure);

		return roundOffIntTenure;
	}

}
