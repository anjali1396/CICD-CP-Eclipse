package dao;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.PromoBanners;
import models.SecondaryInfo;
import models.ServiceRequest;
import models.User;
import models.UserRequest;
import utils.BasicUtils;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class UserRepository {

	public User saveUser(User user) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.saveOrUpdate(user);
			transaction.commit();
			session.close();

			return user;

		} catch (Exception e) {

			LoggerUtils.log("saveUser: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return null;
	}
	
	public boolean saveServiceRequest(ServiceRequest serviceReq) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.saveOrUpdate(serviceReq);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {

			LoggerUtils.log("saveServiceRequest: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return false;
	}


	public User findUserByUserId(int userId) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from User where userId = :userId", User.class).setParameter("userId", userId)
					.getSingleResult();
		} catch (Exception e) {
			LoggerUtils.log("findUserByUserId: Error : " + e.getMessage());
		}
		return null;
	}

	public User findUserByMobileNumber(String mobileNumber) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from User where mobileNumber = :mobileNumber", User.class)
					.setParameter("mobileNumber", mobileNumber).getSingleResult();
		} catch (Exception e) {
			LoggerUtils.log("findUserByMobileNumber: Error : " + e.getMessage());
		}
		return null;
	}

	public User updateAndGetSession(User user) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			user.sessionPasscode = getPasscodeHash(user);
			transaction = session.beginTransaction();
			session.saveOrUpdate(user);
			transaction.commit();
			session.close();

			return user;

		} catch (Exception e) {
			LoggerUtils.log("updateAndGetSession - Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (null != session)
				session.close();
		} finally {
			if (null != session)
				session.close();
		}

		return null;

	}

	public String getPasscodeHash(User user) throws NoSuchAlgorithmException {
		Random random = new Random();
		double randomNumber = random.nextInt(99999);
		return (new BasicUtils()).getMD5Hash(user.mobileNumber + (randomNumber));
	}

	public boolean saveSecondaryInfo(SecondaryInfo sInfo) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(sInfo);
			transaction.commit();
			session.close();
			LoggerUtils.log("saveSecondaryInfo - Success : ");
			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveSecondaryInfo - Error : " + e.getMessage());
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

	public SecondaryInfo findSecondaryInfoByUserId(int userId) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from SecondaryInfo where user_id = :userId", SecondaryInfo.class)
					.setParameter("userId", userId).getSingleResult();
		} catch (Exception e) {
			LoggerUtils.log("findSecondaryInfoByUserId: Error : " + e.getMessage());
		}
		return null;
	}

	public int getOpenUserRequestCount(int userId) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			String dateTime = DateTimeUtils.getDateTimeAddingHours(-3, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
					DateTimeZone.IST);
			return (int) session
					.createQuery("select count(id)from user_request where user_id = :userId and status = 'open' "
							+ "and created_datetime > :dateTime")
					.setParameter("userId", userId).setParameter("dateTime", dateTime).uniqueResult();
		} catch (Exception e) {
			LoggerUtils.log("getOpenUserRequestCount: Error : " + e.getMessage());
		}
		return 0;
	}

	public ArrayList<UserRequest> getOpenRequests(int userId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			String dateTime = DateTimeUtils.getDateTimeAddingHours(-1, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
					DateTimeZone.IST);
			return (ArrayList<UserRequest>) session
					.createQuery("from UserRequest where user_id = :userId and status = 'open'"
							+ "and created_datetime > :dateTime", UserRequest.class)
					.setParameter("userId", userId).setParameter("dateTime", dateTime).list();

		} catch (Exception e) {
			LoggerUtils.log("getOpenRequests: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<UserRequest>();

	}

	public User findUserByCrmAcc(String crmAcc) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from User where crmAccountNumber = :crmAccountNumber order by registeration_datetime desc", User.class)
					.setParameter("crmAccountNumber", crmAcc).setMaxResults(1).getSingleResult();
		} catch (Exception e) {
			LoggerUtils.log("findUserByCrmAcc: Error : " + e.getMessage());
		}
		return null;
	}
	
	

}