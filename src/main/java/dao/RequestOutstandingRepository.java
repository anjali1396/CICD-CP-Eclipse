package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;
import models.RequestOutstanding;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class RequestOutstandingRepository {

	public RequestOutstanding saveRequestOutstanding(RequestOutstanding reqOutstanding) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.saveOrUpdate(reqOutstanding);
			transaction.commit();
			session.close();
			LoggerUtils.log("saveRequestOutstanding: Success : " + reqOutstanding.id);
			return reqOutstanding;

		} catch (Exception e) {

			LoggerUtils.log("saveRequestOutstanding: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return null;
	}

	public RequestOutstanding findValidRequest(String loanAccountNumber, int userId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			var currentDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			
			LoggerUtils.log("findValidRequest: DateTime : " + currentDatetime);

			return session.createQuery(
					"from RequestOutstanding where loanAccountNumber = :loanAccountNumber "
					+ "and userId = :userId and validityDatetime > :validityDatetime "
					+ "order by createDatetime desc",
					RequestOutstanding.class)
					.setParameter("loanAccountNumber", loanAccountNumber)
					.setParameter("userId", userId)
					.setParameter("validityDatetime", currentDatetime)
					.setMaxResults(1)
					.getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findValidRequest: Error : " + e.getMessage());
		}

		return null;

	}

	public ArrayList<RequestOutstanding> findNonNotified() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			final var currentDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			final var createDatetime = DateTimeUtils.getDateTimeAddingHours(-2, DateTimeFormat.yyyy_MM_dd_HH_mm,
					DateTimeZone.IST);

			return (ArrayList<RequestOutstanding>) session.createQuery(
					"from RequestOutstanding where isNotified = false and createDatetime < :createDatetime "
					+ "and validityDatetime > :validityDatetime",
					RequestOutstanding.class)
					.setParameter("createDatetime", createDatetime)
					.setParameter("validityDatetime", currentDatetime)
					.list();

		} catch (Exception e) {
			LoggerUtils.log("findNonNotified: Error : " + e.getMessage());
		}

		return new ArrayList<RequestOutstanding>();

	}

}
