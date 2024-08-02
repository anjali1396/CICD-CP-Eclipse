package v1.repository;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;

import models.SecondaryInfo;
import models.User;
import models.notification.CPNotification;
import models.notification.RegistrationKey;
import models.notification.UserNotificationInfo;
import models.notification.UserNotificationKey;
import models.notification.UserNotificationToken;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.HibernateUtil;
import utils.LoggerUtils;
import utils.NotificationUtils.NotificationServiceType;
import v1.dto.NotificationDTO;
import v1.dto.UserNotificationTokenPair;

public class NotificationRepository {

	private void log(String value) {
		LoggerUtils.log("NotificationRepository." + value);
	}

//	private void printLog(String value) {
//		System.out.println("NotificationRepository." + value);
//	}

	public boolean saveCpNotification(CPNotification cpNotification) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(cpNotification);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveCpNotification - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}

		return false;

	}

	public CPNotification findUserNotificationById(int id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from CPNotification where id = :id", CPNotification.class)
					.setParameter("id", id).getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findUserNotificationById - Error : " + e.getMessage());
		}

		return null;

	}

	public boolean saveUserNotificationInfo(UserNotificationInfo userNotificationInfo) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(userNotificationInfo);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveUserNotificationInfo - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}

		return false;

	}
	
	public boolean saveDynamicUserNotification(			
			CPNotification cpNotification,
			ArrayList<UserNotificationTokenPair> userNotificationTokenPairs
	) {
		
		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			
			for (var i = 0; i < userNotificationTokenPairs.size(); i++) {

				final var rk = userNotificationTokenPairs.get(i);

				final var uni = new UserNotificationInfo();			

				// uni.notificationId = cpNotification.id;
				uni.notificationId = cpNotification;
				uni.userId = rk.user.userId;
				uni.hasRead = false;
				uni.deviceId = rk.userNotificationToken.deviceId;
				uni.deviceType = rk.userNotificationToken.deviceType;
				uni.dynamicTitle = rk.customNotification.title;
				uni.dynamicMessage = rk.customNotification.message;
				
				
				if (BasicUtils.isNotNullOrNA(rk.customNotification.webUrl))
					uni.dynamicWebUrl = rk.customNotification.webUrl;

				session.saveOrUpdate(uni);

				if (i % Constants.HITBERNATE_BATCH_SIZE == 0) {
					session.flush();
					session.clear();
				}

			}
			
			
			transaction.commit();
			session.close();			

			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveDynamicUserNotification - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}
		
		return false;
		
	}

	public boolean saveUserNotificationInfo(CPNotification cpNotification,
			ArrayList<UserNotificationToken> registrationKeys) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();

			String message = null;
			String title = null;

			message = cpNotification.message;

//				if (BasicUtils.isNotNullOrNA(cpNotification.bigMessage))
//					message = cpNotification.bigMessage;

			if (BasicUtils.isNotNullOrNA(cpNotification.title))
				title = cpNotification.title;

			for (var i = 0; i < registrationKeys.size(); i++) {

				final var rk = registrationKeys.get(i);

				final var uni = new UserNotificationInfo();

				// uni.notificationId = cpNotification.id;
				uni.notificationId = cpNotification;
				uni.userId = rk.userId;
				uni.hasRead = false;
				uni.deviceId = rk.deviceId;
				uni.deviceType = rk.deviceType;
				uni.dynamicTitle = title;
				uni.dynamicMessage = message;
				
				
				if (BasicUtils.isNotNullOrNA(cpNotification.webUrl))
					uni.dynamicWebUrl = cpNotification.webUrl;

				session.saveOrUpdate(uni);

				if (i % Constants.HITBERNATE_BATCH_SIZE == 0) {
					session.flush();
					session.clear();
				}

			}

			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveUserNotificationInfo - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}

		return false;

	}

//	@SuppressWarnings("unchecked")
//	public ArrayList<UserNotificationKey> getUserNotificationKeysByMobile(String mobileNumbers) {
//
//		final var userNotificationKeys = new ArrayList<UserNotificationKey>();
//
//		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//
////			final var query = "select u.user_id, u.mobile_number, si.fcm_key "
////					+ " from HomeFirstCustomerPortal.user u "
////					+ " left join (select user_id, fcm_key from HomeFirstCustomerPortal.secondary_info) si on si.user_id = u.user_id "
////					+ " where mobile_number in (" + mobileNumbers + ")";
//
//			final var query = "select u.user_id, u.name, u.mobile_number, si.fcm_key "
//					+ " from HomeFirstCustomerPortal.user u "
//					+ " left join (select user_id, fcm_key from HomeFirstCustomerPortal.secondary_info) si on si.user_id = u.user_id "
//					+ " where mobile_number in (" + mobileNumbers + ")";
//			
//		
//			final var hql = session.createNativeQuery(query);
//			// hql.setParameter("mobileNumbers", mobileNumbers);
//
//			List<Object[]> unkObjects = hql.list();
//
//			for (Object[] unkObject : unkObjects) {
//
//				final var unKey = new UserNotificationKey();
//
//				final var fcmKeyArrayString = (String) unkObject[3];
//
//				if (null != fcmKeyArrayString && fcmKeyArrayString.startsWith("[")) {
//
//					final var user = new User();
//
//					user.userId = (Integer) unkObject[0];
//					user.name = (String) unkObject[1];
//					user.mobileNumber = (String) unkObject[2];
//
//					unKey.user = user;
//
//					final var keys = new ArrayList<RegistrationKey>();
//
//					final var tokenJsonArray = new JSONArray(fcmKeyArrayString);
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
//			}
//
//		} catch (Exception e) {
//			log("getUserNotificationKeysByMobile - Error : " + e.getMessage());
//		}
//
//		return userNotificationKeys;
//
//	}

	public ArrayList<RegistrationKey> getAllRegistrationKeys() {

		final var rKeys = new ArrayList<RegistrationKey>();

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			final var secondaryInfos = session.createQuery("from SecondaryInfo", SecondaryInfo.class).getResultList();

			for (var secondaryInfo : secondaryInfos) {

				if (null != secondaryInfo.fcmKey) {

					JSONArray tokenJsonArray = new JSONArray(secondaryInfo.fcmKey);

					for (int i = 0; i < tokenJsonArray.length(); i++) {
						rKeys.add(new RegistrationKey(secondaryInfo.userId, tokenJsonArray.getJSONObject(i)));
					}

				}

			}

		} catch (Exception e) {
			log("getAllRegistrationKeys - Error : " + e.getMessage());
			// e.printStackTrace();
		}

		return rKeys;

	}

	public ArrayList<NotificationDTO> getUserNotificationByUserId(int userId, int pageNumber) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			final var entityManager = session.getEntityManagerFactory().createEntityManager();
			final var builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<NotificationDTO> criteria = builder.createQuery(NotificationDTO.class);

			Root<UserNotificationInfo> userNotificationInfo = criteria.from(UserNotificationInfo.class);
			Join<UserNotificationInfo, CPNotification> notificationJoin = userNotificationInfo.join("notificationId",
					JoinType.LEFT);
			criteria.groupBy(userNotificationInfo.get("notificationId"));

			criteria.multiselect(notificationJoin.get("id").alias("id"),
					userNotificationInfo.get("userId").alias("userId"),
					userNotificationInfo.get("dynamicTitle").alias("title"),
					userNotificationInfo.get("dynamicMessage").alias("message"),					
					notificationJoin.get("imageUrl").alias("imageUrl"),
					notificationJoin.get("thumbnailUrl").alias("thumbnailUrl"),
										
					userNotificationInfo.get("dynamicWebUrl").alias("webUrl"),
//					notificationJoin.get("webUrl").alias("webUrl"),
					notificationJoin.get("onClickAction").alias("onClickAction"),
					notificationJoin.get("screenToOpen").alias("screenToOpen"),
					notificationJoin.get("deeplink").alias("deeplink"), notificationJoin.get("data").alias("data"),
					notificationJoin.get("priority").alias("priority"), notificationJoin.get("kind").alias("kind"),
					userNotificationInfo.get("hasRead").alias("hasRead"),
					userNotificationInfo.get("createDatetime").alias("createDatetime"),
					notificationJoin.get("datetime").alias("datetime"));

			criteria.where(builder.equal(userNotificationInfo.get("userId"), userId));
			criteria.orderBy(builder.desc(userNotificationInfo.get("createDatetime")));

			final var pageSize = 25;

			final var countQuery = session.createQuery(
					"select count(distinct un.notificationId) from UserNotificationInfo un where userId = :userId")
					.setParameter("userId", userId);
			final var countResults = (Long) countQuery.uniqueResult();
			
			System.out.println("NotificationRepository. -- getCount " + countResults);
			
			 final var selectQuery = session.createQuery(criteria);
			 selectQuery.setFirstResult((pageNumber - 1) * pageSize);
			 selectQuery.setMaxResults(pageSize);

			return (ArrayList<NotificationDTO>) selectQuery.getResultList();

			
		} catch (Exception e) {
			log("getUserNotificationByUserId - Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<NotificationDTO>();

	}

	public boolean saveUserNotificationToken(UserNotificationToken userNotificationToken) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(userNotificationToken);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			log("saveUserNotificationToken - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}

		return false;

	}

	public UserNotificationToken findUserNotificationTokenByNotificationKey(String notificationKey, int userId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session
					.createQuery("from UserNotificationToken " + " where notificationKey = :notificationKey"
							+ " and userId = :userId", UserNotificationToken.class)
					.setParameter("notificationKey", notificationKey).setParameter("userId", userId).setMaxResults(1)
					.getSingleResult();

		} catch (Exception e) {
			log("findUserNotificationTokenByNotificationKey - Error : " + e.getMessage());
		}

		return null;

	}

	public ArrayList<UserNotificationInfo> getAllUserNotificationByNotificationId(int userId, int notificationId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<UserNotificationInfo>) session
					.createQuery("from UserNotificationInfo " + " where notificationId.id = :notificationId"
							+ " and userId = :userId", UserNotificationInfo.class)
					.setParameter("notificationId", notificationId).setParameter("userId", userId).getResultList();

		} catch (Exception e) {
			log("findUserNotificationTokenByNotificationKey - Error : " + e.getMessage());
		}

		return new ArrayList<UserNotificationInfo>();

	}

	public boolean saveAllUserNotifications(ArrayList<UserNotificationInfo> userNotifications) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();

			for (var i = 0; i < userNotifications.size(); i++) {

				final var userNotification = userNotifications.get(i);

				session.saveOrUpdate(userNotification);

				if (i % Constants.HITBERNATE_BATCH_SIZE == 0) {
					session.flush();
					session.clear();
				}

			}

			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveAllUserNotifications - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}

		return false;

	}

	public long getUnreadUserNotificationCount(int userId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			final var entityManager = session.getEntityManagerFactory().createEntityManager();
			final var builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Long> criteria = builder.createQuery(Long.class);

			Root<UserNotificationInfo> userNotificationInfo = criteria.from(UserNotificationInfo.class);

			criteria.select(builder.countDistinct(userNotificationInfo.get("notificationId")));
			var predicate = builder.conjunction();
			predicate = builder.and(predicate, builder.equal(userNotificationInfo.get("userId"), userId));
			predicate = builder.and(predicate, builder.equal(userNotificationInfo.get("hasRead"), false));
			criteria.where(predicate);

			return session.createQuery(criteria).uniqueResult();

		} catch (Exception e) {
			log("getUnreadUserNotificationCount - Error : " + e.getMessage());
		}

		return 0;

	}
	
	
	
	public ArrayList<UserNotificationToken> getAllUserNotificationToken() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			final var updateDatetime = DateTimeUtils.getDateTimeByAddingMonths(-6, DateTimeFormat.yyyy_MM_dd,
					DateTimeZone.IST) + " 00:00:00";

			return (ArrayList<UserNotificationToken>) session
					.createQuery("from UserNotificationToken " + " where updateDatetime > :updateDatetime"
							+ " and notificationService = :notificationService" + " and isValid = true order by userId",
							UserNotificationToken.class)
					.setParameter("updateDatetime", updateDatetime)
					.setParameter("notificationService", NotificationServiceType.FIREBASE.value).getResultList();

		} catch (Exception e) {
			log("getAllUserNotificationToken - Error : " + e.getMessage());
		}

		return new ArrayList<UserNotificationToken>();

	}

	public ArrayList<UserNotificationTokenPair> getUserNotificationTokensByLAI(String loanAccountNumbers) {		
		return fetchUserNotificationTokens(loanAccountNumbers, true, false); 				
	}
	
	public ArrayList<UserNotificationTokenPair> getUserNotificationTokensByMobile(String mobileNumbers) {		
		return fetchUserNotificationTokens(mobileNumbers, false, true); 				
	}
	
	public ArrayList<UserNotificationTokenPair> getUserNotificationTokensByUserId(String userId) {		
		return fetchUserNotificationTokens(userId, false, false); 			
	}
	
	public ArrayList<UserNotificationTokenPair> getAllUserNotificationTokens() {		
		return fetchUserNotificationTokens(null, false, false); 			
	}
	
	private ArrayList<UserNotificationTokenPair> fetchUserNotificationTokens(			
			@Nullable String identifiers, 
			boolean byLAI,
			boolean byMobile
	) {
		
		final var userNotificationKeys = new ArrayList<UserNotificationTokenPair>();

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			
			final var updateDatetime = DateTimeUtils.getDateTimeByAddingMonths(-6, DateTimeFormat.yyyy_MM_dd,
					DateTimeZone.IST) + " 00:00:00";

			final var query = new StringBuilder(  
					"SELECT u.user_id userId, u.name, u.mobile_number mobileNumber, "
					+ " unt.notificationKey, unt.id userNotificationTokenId, unt.deviceId, u.loan_account_number loanAccountNumber"
					+ " FROM HomeFirstCustomerPortal.user u"
					+ " right join (select * from HomeFirstCustomerPortal.UserNotificationToken where updateDatetime > :updateDatetime"
					+ " and isValid = true)"
					+ " unt on unt.userId = u.user_id "
					+ " where u.is_loan_active = true");  
						
			if (null != identifiers) {		
				
				if (byLAI) query.append(" and u.loan_account_number ");
				else if (byMobile) query.append(" and u.mobile_number ");
				else query.append(" and u.user_id ");
				
				query.append(" in (" + identifiers + ")");
			}
			
						
			final var hql = session.createNativeQuery(query.toString())
					.setParameter("updateDatetime", updateDatetime);			

			List<Object[]> unkObjects = hql.list();
			

			for (Object[] unkObject : unkObjects) {
				userNotificationKeys.add(new UserNotificationTokenPair(unkObject));
				
				//System.out.println("====> here userNotificationKeys 1" + userNotificationKeys.get(0));
				//System.out.println("====> here userNotificationKeys 2" + unkObject);
			}
			
			//System.out.println("====> here userNotificationKeys 3" + userNotificationKeys.get(0));

		} catch (Exception e) {
			
			log("fetchUserNotificationTokens - Error --> : " + e.getMessage());
		}

		return userNotificationKeys;
		
	}
	
	
	public ArrayList<User> getNonNotifiedUsersforOutstandingNotifications() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<User>) session
					.createQuery("from PromoBanners where isActive = true order by position", User.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getActivePromoBanners: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<User>();

	}

}
