package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.sob.Prospect;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class ProspectRepository {

	public boolean addUpdateProspect(Prospect prospect) {

		Transaction transaction = null;
		Session session = null;

		try {
			
			prospect.setSFLoanType();
			
			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			System.out.println("prospect is ==> "+ prospect);
			session.saveOrUpdate(prospect);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {			
			LoggerUtils.log("addUpdateProspect: Error : " + e.getMessage());
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

	public ArrayList<Prospect> getProspects() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<Prospect>) session.createQuery("from Prospect", Prospect.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getProspects: Error : " + e.getMessage());
		}

		return new ArrayList<Prospect>();

	}

	public Prospect findProspectById(String id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from Prospect where id = :id", Prospect.class).setParameter("id", id)
					.getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findProspectById: Error : " + e.getMessage());
		}

		return null;

	}

	public Prospect findProspectByMobileNumber(String mobileNumber) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from Prospect where mobileNumber = :mobileNumber", Prospect.class)
					.setParameter("mobileNumber", mobileNumber).getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findProspectByMobileNumber: Error : " + e.getMessage());
		}

		return null;

	}

}
