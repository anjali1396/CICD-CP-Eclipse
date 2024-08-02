package managers;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.json.JSONObject;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import dao.PaymentRepository;
import dao.UserRepository;
import models.User;
import models.payment.Payment;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LoggerUtils;
import utils.MailUtils;
import utils.MailUtils.ContentType;
import utils.OneResponse;
import utils.ProptertyUtils;
import utils.ProptertyUtils.Keys;
import v2.managers.SalesForceManager;

public class CrownServiceManager {

	private final PaymentRepository paymentRepo;
	private final SalesForceManager sfManger;
	private final UserRepository userRepo;
	private RazorpayClient _razorpayClient = null;

	public CrownServiceManager() {
		paymentRepo = new PaymentRepository();
		sfManger = new SalesForceManager();
		userRepo = new UserRepository();
	}

	private void log(String value) {
		LoggerUtils.log("V1 CrownServiceManager." + value);
	}

	private RazorpayClient razorpayClient() throws Exception {

		if (null == _razorpayClient) {

			String razorpayKey = Constants.NA;
			String razorpaySecret = Constants.NA;

			if (Constants.IS_PAYMENT_LIVE) {
				razorpayKey = ProptertyUtils.getValurForKey(Keys.RAZORPAY_PROD_KEY);
				razorpaySecret = ProptertyUtils.getValurForKey(Keys.RAZORPAY_PROD_SECRET);
			} else {
				razorpayKey = ProptertyUtils.getValurForKey(Keys.RAZORPAY_TEST_KEY);
				razorpaySecret = ProptertyUtils.getValurForKey(Keys.RAZORPAY_TEST_SECRET);
			}

			_razorpayClient = new RazorpayClient(razorpayKey, razorpaySecret);

		}

		return _razorpayClient;

	}

	public Response syncRazorPaymentStatus() throws Exception {

		ArrayList<Payment> paymentList = paymentRepo.getFailedPaymentList();

		log("syncRazorPaymentStatus - Total razorpay payment with not success status :" + paymentList.size());

		int totalCount = paymentList.size();
		int receiptCreated = 0;

		for (int i = 0; i < paymentList.size(); i++) {

			Payment payment = paymentList.get(i);

			com.razorpay.Payment razorPayment = getRazorpayOrderInfo(payment.pGatewayOrderId);

			if (null == razorPayment || razorPayment.toJson().isEmpty()) {
				log("syncRazorPaymentStatus - Failed to fetch razorpay order status for PG Order ID :"
						+ payment.pGatewayOrderId);

				payment.razarpayStatus = Constants.RZP_NOT_FOUND;
				payment.updateDateTime = DateTimeUtils.getCurrentDateTimeInIST();

				paymentRepo.savePayment(payment);

				continue;
			}

			payment.razarpayStatus = razorPayment.get("status");
			boolean rzpStatus = false;

			if (payment.razarpayStatus.equalsIgnoreCase(Constants.RZP_CAPTURED)
					|| payment.razarpayStatus.equalsIgnoreCase(Constants.CAPTURED)) {

				payment.statusMessage = Constants.SUCCESS;
				payment.paymentId = razorPayment.get("id");
				rzpStatus = true;
				
				
				JSONObject pReceiptData = new JSONObject();
				pReceiptData.put("transactionId", payment.orderId);
				pReceiptData.put("paymentAmount", payment.paymentAmount);
				pReceiptData.put("paymentId", payment.paymentId);
				pReceiptData.put("loanAccountNumber", payment.loanAccountNumber);
				pReceiptData.put("paymentMethod", payment.paymentMethod);
				payment.receiptData = pReceiptData.toString();

			}

			payment.updateDateTime = DateTimeUtils.getCurrentDateTimeInIST();

			if (paymentRepo.savePayment(payment) == null) {
				log("syncRazorPaymentStatus - Failed to Update Payment Details in DB after fetching status from RZP, PG Order ID : "
						+ payment.pGatewayOrderId);
				continue;
			}

			if (!rzpStatus || !BasicUtils.isNotNullOrNA(payment.paymentId)) {
				log("syncRazorPaymentStatus - Payment status is not captured on RZP, PG Order ID : "
						+ payment.pGatewayOrderId + " | Skiping to next..");
				continue;
			}

			if (getOrAddPaymentReceipt(payment))
				receiptCreated++;
		}

		JSONObject responseJson = new JSONObject();
		responseJson.put(Constants.MESSAGE, "RazorPay status sychronized successfully.");
		responseJson.put("totalCount", totalCount);
		responseJson.put("receiptCreated", receiptCreated);

		if (receiptCreated > 0) {
			MailUtils.getInstance().sendDefaultMail(ContentType.TEXT_PLAIN, "RazorPay status sychronized successfully.",
					responseJson.toString(), "sanjay.jaiswar@homefirstindia.com", "ranan.rodrigues@homefirstindia.com");
		}

		return new OneResponse().getSuccessResponse(responseJson);

	}

	private com.razorpay.Payment getRazorpayOrderInfo(String orderId) {

		try {

			List<com.razorpay.Payment> payments = razorpayClient().Orders.fetchPayments(orderId);

			log("getRazorpayOrderInfo - Order ID: " + orderId + " | Payments: " + payments);
			
			for (var i = 0; i < payments.size(); i++) {

				final var cPaymentObject = payments.get(i);
				
				String razarpayStatus = cPaymentObject.get("status");

				if (razarpayStatus.equalsIgnoreCase(Constants.RZP_CAPTURED)
						|| razarpayStatus.equalsIgnoreCase(Constants.CAPTURED)) {
					return cPaymentObject;
				} else {
					
					if (i == payments.size() - 1) {
						return cPaymentObject;
					}
					
				}
											
			}

		} catch (RazorpayException e) {
			log("getRazorpayOrderInfo - RazorpayException: " + e.getMessage());
		} catch (Exception e) {
			log("getRazorpayOrderInfo - Exception: " + e.getMessage());
		}

		return null;

	}

	public Response createFailedReceiptOnSF() throws Exception {

		ArrayList<Payment> paymentList = paymentRepo.getFailedSFPaymentList();

		log("createFailedReceiptOnSF - total payments with success status but failed sf receipt : "
				+ paymentList.size());

		for (Payment payment : paymentList) {
			getOrAddPaymentReceipt(payment);
		}

		return new OneResponse().getSuccessResponse(new JSONObject().put(Constants.MESSAGE, "Receipts created on SF."));

	}

	public boolean getOrAddPaymentReceipt(Payment payment) throws Exception {

		String existingReceiptInfo = sfManger.getReceiptInfoByIdAndLAI(payment.paymentId, payment.loanAccountNumber);

		if (BasicUtils.isNotNullOrNA(existingReceiptInfo)) {

			String[] receiptInfoArray = existingReceiptInfo.split(";");
			payment.receiptId = receiptInfoArray[0];
			payment.receiptNumber = receiptInfoArray[1];

			log("syncRazorPaymentStatus - Receipt info is already prsent on SF, using existing for PG Order ID : "
					+ payment.pGatewayOrderId);

		} else {

			User user = userRepo.findUserByUserId(payment.userId);

			if (payment.emiReduction == 1) {
				payment.prePaymentType = Constants.PART_PAYMENT_TYPE_EMI_REDUCTION;
			} else if (payment.tenurReduction == 1) {
				payment.prePaymentType = Constants.PART_PAYMENT_TYPE_TENOR_REDUCTION;
			}


			payment = sfManger.addPaymentReceipt(user, payment);

			if (!BasicUtils.isNotNullOrNA(payment.receiptId)) {
				log("syncRazorPaymentStatus - Failed to create payment receipt on SF after fetching status from RZP, PG Order ID : "
						+ payment.pGatewayOrderId);
				return false;
			}
		}

		payment.completionDateTime = DateTimeUtils.getCurrentDateTimeInIST();

		if (paymentRepo.savePayment(payment) == null) {
			log("createFailedReceiptOnSF - Failed to Update Payment Details in DB after creteing receipt on SF, Order ID : "
					+ payment.orderId);
			return false;
		} else
			return true;

	}

}
