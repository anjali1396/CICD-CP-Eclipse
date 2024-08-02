package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.AdBannerImage;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class AdBannerRepository {

	public boolean addUpdateAdBanner(AdBannerImage adBannerImage) {

		Transaction transaction = null;
		Session session = null;
		
		try {

			session = HibernateUtil.getSessionFactory().openSession();
			
			transaction = session.beginTransaction();
			session.saveOrUpdate(adBannerImage);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("addUpdateAdBanner: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return false;

	}

	public ArrayList<AdBannerImage> getActiveAdBanner() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<AdBannerImage>) session
					.createQuery("from AdBannerImage where isActive = true order by createDatetime", AdBannerImage.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getActiveAdBanner: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<AdBannerImage>();

	}

	

}
