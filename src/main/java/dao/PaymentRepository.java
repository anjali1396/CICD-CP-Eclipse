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

public class PaymentRepository {

	public Payment addPayment(Payment payment) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.save(payment);
			transaction.commit();
			session.close();
			LoggerUtils.log("addPayment: Success : " + payment.id);
			return payment;

		} catch (Exception e) {

			LoggerUtils.log("addPayment: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return null;
	}

	public Payment savePayment(Payment payment) {

		Transaction transaction = null;
		Session session = null;

		try {

			session = HibernateUtil.getSessionFactory().openSession();

			transaction = session.beginTransaction();
			session.saveOrUpdate(payment);
			transaction.commit();
			session.close();
			LoggerUtils.log("savePayment: Success : " + payment.id);
			return payment;

		} catch (Exception e) {

			LoggerUtils.log("savePayment: Error : " + e.getMessage());
			e.printStackTrace();
			if (transaction != null)
				transaction.rollback();
			if (session != null)
				session.close();
		}

		return null;
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

	public ArrayList<Payment> getFailedSFPaymentList() {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			StringBuilder sb = new StringBuilder();
			sb.append("from Payment");
			sb.append(" where statusMessage = 'success'");
			sb.append(" and (paymentId is not null or paymentId != 'NA')");
			sb.append(" and (receiptId is null or receiptId = 'NA' or receiptId = '')");

			return (ArrayList<Payment>) session.createQuery(sb.toString(), Payment.class).list();

		} catch (Exception e) {

			LoggerUtils.log("getFailedSFPaymentList: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<Payment>();
	}

	public ArrayList<Payment> getRepricePayment(ROIReprice reprice) {

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {

			StringBuilder sb = new StringBuilder();
			sb.append("from Payment");
			sb.append(" where statusMessage = 'success'");
			sb.append(" and (paymentId is not null or paymentId != 'NA')");
			sb.append(" and payment_nature = 'Repricing Fees'");
			sb.append(" and amount = :amount");
			sb.append(" and userId = :userId");
			sb.append(" and initialDateTime > :initialDateTime");

			return (ArrayList<Payment>) session.createQuery(sb.toString(), Payment.class)
					.setParameter("amount", reprice.paymentAmount)
					.setParameter("initialDateTime", reprice.createDatetime).setParameter("userId", reprice.userId)
					.list();

		} catch (Exception e) {

			LoggerUtils.log("getRepricePayment: Error : " + e.getMessage());
			e.printStackTrace();

		}

		return new ArrayList<Payment>();
	}

}