package dao;

import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Transaction;

import models.ROIReprice;
import models.payment.Payment;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.HibernateUtil;
import utils.LoggerUtils;

public class ROIRepository {

	public ROIReprice saveROIReprice(ROIReprice roi) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.saveOrUpdate(roi);
			transaction.commit();
			session.close();
			LoggerUtils.log("saveROIReprice: Success : " + roi.id);
			return roi;

		} catch (Exception e) {

			LoggerUtils.log("saveROIReprice: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return null;
	}

	public boolean batchUpdateROI(ArrayList<ROIReprice> rois) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			Transaction tx = session.beginTransaction();

			rois.forEach((r) -> {
				session.update(r);
			});

			tx.commit();
			session.close();
			return true;

		} catch (Exception e) {

			LoggerUtils.log("updateROI: Error : " + e.getMessage());
			e.printStackTrace();

		}
		return false;
	}

	public ArrayList<Payment> getFailedPaymentList() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			StringBuilder sb = new StringBuilder();
			sb.append("from Payment");
			sb.append(" where (pGatewayOrderId != 'NA' and pGatewayOrderId is not null)");
			sb.append(" and (paymentMethod = 'RazorPay' or paymentMethod = 'Razorpay')");
			sb.append(" and (statusMessage != 'success' or statusMessage is null )");
			sb.append(" and (razarpayStatus is null or razarpayStatus NOT IN");
			sb.append("('" + Constants.RZP_CAPTURED + "','" + Constants.RZP_FAILED + "','" + Constants.RZP_REFUNDED
					+ "',");
			sb.append("'" + Constants.CAPTURED + "','" + Constants.REFUNDED + "','" + Constants.FAILED + "','"
					+ Constants.RZP_NOT_FOUND + "'))");
			sb.append(" and (initialDateTime between '2021-06-24 00:00:00'");

			String upperDatetime = DateTimeUtils.getDateTimeAddingMinutes(-30, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
					DateTimeZone.IST);

			LoggerUtils.log(
					"PaymentRepository.getFailedPaymentList - Upper initiate datetime to consider : " + upperDatetime);

			sb.append(" and '" + upperDatetime + "')");

			return (ArrayList<Payment>) session.createQuery(sb.toString(), Payment.class).list();

		} catch (Exception e) {

			LoggerUtils.log("getFaildPaymentList: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<Payment>();
	}

	public ArrayList<ROIReprice> getCustomerDetailsFromUserId(int userId) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			var eligibleDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			return (ArrayList<ROIReprice>) session
					.createQuery("from ROIReprice where userId = :userId "
							+ "and isPaid is false and isEligible is true and eligibleDatetime > :eligibleDatetime",
							ROIReprice.class)
					.setParameter("eligibleDatetime", eligibleDatetime).setParameter("userId", userId).list();

		} catch (Exception e) {

			LoggerUtils.log("getCustomerDetailsFromLAI: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<ROIReprice>();
	}

	public ArrayList<ROIReprice> getAllUnProccessedRepriceData() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			var eligibleDatetime = DateTimeUtils.getCurrentDateTimeInIST();

			return (ArrayList<ROIReprice>) session.createQuery(
					"from ROIReprice where isPaid is false and (eligibleDatetime is null or eligibleDatetime > :eligibleDatetime)",
					ROIReprice.class).setParameter("eligibleDatetime", eligibleDatetime).list();

		} catch (Exception e) {

			LoggerUtils.log("getAllRepriceData: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<ROIReprice>();

	}

	public ArrayList<ROIReprice> getAllPaidRepriceData() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			var eligibleDatetime = DateTimeUtils.getDateTimeAddingMinutes(-1440, DateTimeFormat.yyyy_MM_dd,
					DateTimeZone.IST);

			return (ArrayList<ROIReprice>) session.createQuery(
					"from ROIReprice where isPaid is true and updateDatetime like :updateDatetime",
					ROIReprice.class).setParameter("updateDatetime", eligibleDatetime + "%").list();

		} catch (Exception e) {

			LoggerUtils.log("getAllRepriceData: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<ROIReprice>();

	}

	public ArrayList<ROIReprice> getAllPendingRepriceData() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			var eligibleDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			return (ArrayList<ROIReprice>) session
					.createQuery("from ROIReprice where isEligible is true and isPaid is false"
							+ " and eligibleDatetime > :eligibleDatetime ", ROIReprice.class)
					.setParameter("eligibleDatetime", eligibleDatetime).list();

		} catch (Exception e) {

			LoggerUtils.log("getAllPendingRepriceData: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<ROIReprice>();

	}

	public ROIReprice findRepricetById(String id) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			return session.createQuery("from ROIReprice where id = :id", ROIReprice.class).setParameter("id", id)
					.getSingleResult();

		} catch (Exception e) {
			LoggerUtils.log("findRepricetById: Error : " + e.getMessage());
		}

		return null;

	}

}