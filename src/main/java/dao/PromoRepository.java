package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.sob.Promo;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class PromoRepository {
	
	public boolean addUpdatePromo(Promo promo) {

		Transaction transaction = null;
		Session session = null;

		try {
			
			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(promo);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {			
			LoggerUtils.log("addUpdatePromo: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return false;

	}

	public ArrayList<Promo> getPromos() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<Promo>) session.createQuery("from Promo", Promo.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getPromos: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<Promo>();

	}
	
	public ArrayList<Promo> getActivePromos() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<Promo>) session.createQuery("from Promo where isActive = true order by position", Promo.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getActivePromos: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<Promo>();

	}

	public Promo findPromoById(String id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from Promo where id = :id", Promo.class).setParameter("id", id)
					.getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findPromoById: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return null;

	}

}
