package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.Blog;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class BlogRepository {

	public boolean addUpdatePromo(Blog blog) {

		Transaction transaction = null;
		Session session = null;
		
		try {

			session = HibernateUtil.getSessionFactory().openSession();
			
			transaction = session.beginTransaction();
			session.saveOrUpdate(blog);
			transaction.commit();
			session.close();

			return true;

		} catch (Exception e) {
			LoggerUtils.log("addUpdateBlog: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return false;

	}

	public ArrayList<Blog> getBlogs() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<Blog>) session.createQuery("from Blog", Blog.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getBlogs: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<Blog>();

	}

	public ArrayList<Blog> getActiveBlogs() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<Blog>) session
					.createQuery("from Blog where isActive = true order by position", Blog.class).list();

		} catch (Exception e) {
			LoggerUtils.log("getActiveBlogs: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<Blog>();

	}

	public Blog findBlogById(String id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from Blog where id = :id", Blog.class).setParameter("id", id)
					.getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findBlogById: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return null;

	}

}
