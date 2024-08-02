package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.AdBannerImage;
import models.Creds;
import models.DeleteAccountRequest;
import models.LoginInfo;
import models.Referrals;
import models.UserRequest;
import utils.Constants.CredType;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class CommonRepository {

	public boolean insertLoginInfo(LoginInfo lInfo) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(lInfo);
			transaction.commit();
			session.close();
			LoggerUtils.log("insertLoginInfo - Success : ");
			return true;

		} catch (Exception e) {
			LoggerUtils.log("insertLoginInfo - Error : " + e.getMessage());
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

	public ArrayList<LoginInfo> getUserIdOrderByLoginDatetimeDesc(int userId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return (ArrayList<LoginInfo>) session
					.createQuery("from LoginInfo where user_id = :userId order by login_datetime desc", LoginInfo.class)
					.setParameter("userId", userId)
					.setMaxResults(2)
					.list();
			
					
					 

		} catch (Exception e) {
			LoggerUtils.log("getUserIdOrderByLoginDatetimeDesc: Error : " + e.getMessage());
			e.printStackTrace();
		}

		return new ArrayList<LoginInfo>();

	}


	public DeleteAccountRequest saveDeleteAccountRequest(DeleteAccountRequest req) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.saveOrUpdate(req);
			transaction.commit();
			session.close();
			return req;

		} catch (Exception e) {

			LoggerUtils.log("saveDeleteAccountRequest: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return null;
	}
	
	public Creds findCredsByPartnerName(String partnerName, CredType type) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session
                    .createQuery(
                            "from Creds where partnerName = :partnerName and credType = :credType and isValid = true",
                            Creds.class)
                    .setParameter("partnerName", partnerName)
                    .setParameter("credType", type.value)
                    .getSingleResult();
        } catch (Exception e) {
            LoggerUtils.log("findCredsByPartnerName - Error : " + e.getMessage());
            return null;
        }
    }
	
	
	public Referrals findLeadByMobileNumber(String mobileNumber) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			return session.createQuery("from Referrals where mobileNumber = :mobileNumber", Referrals.class)
					.setParameter("mobileNumber", mobileNumber).setMaxResults(1).getSingleResult();
		} catch (Exception e) {
			LoggerUtils.log("findByMobileNumber - Error : " + e.getMessage());
			return null;
		}
	}
	
	public boolean saveRefferal(Referrals rInfo) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(rInfo);
			transaction.commit();
			session.close();
			return true;

		} catch (Exception e) {
			LoggerUtils.log("saveRefferal - Error : " + e.getMessage());
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

}
