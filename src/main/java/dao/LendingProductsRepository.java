package dao;

import java.util.ArrayList;

import org.hibernate.Session;

import models.LendingProducts;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class LendingProductsRepository {

	public LendingProducts findProductById(String id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from LendingProducts where sfId = :id and isActive = 1", LendingProducts.class)
					.setParameter("id", id).getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findProductById: Error : " + e.getMessage());
			// e.printStackTrace();
		}

		return null;

	}

	public LendingProducts findTopupProductById(String id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from LendingProducts where sfId = :id and isActive = true and isTopup = true",
					LendingProducts.class).setParameter("id", id).getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findTopupProductById: Error : " + e.getMessage());
			// e.printStackTrace();
		}

		return null;

	}

	public ArrayList<LendingProducts> getHLProducts() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<LendingProducts>) session.createQuery("from LendingProducts where isActive = true and isTopup = false",
					LendingProducts.class).getResultList();
			

		} catch (Exception e) {
			LoggerUtils.log("getHLProducts: Error : " + e.getMessage());
			// e.printStackTrace();
		}

		return null;

	}

}
