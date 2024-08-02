package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;
import models.PromoBanners;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class PromoBannersRepository {

	public boolean addUpdatePromo(PromoBanners promoBanners) {

		Transaction transaction = null;
		Session session = null;
		
		try {

			session = HibernateUtil.getSessionFactory().openSession();
			
			transaction = session.beginTransaction();
			session.saveOrUpdate(promoBanners);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("addUpdatePromoBanners: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return false;

	}

	public ArrayList<PromoBanners> getActivePromoBanners() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<PromoBanners>) session
					.createQuery("from PromoBanners where isActive = true order by position", PromoBanners.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getActivePromoBanners: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<PromoBanners>();

	}

	

}
