package v2.managers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import helper.AppContextProvider;
import models.User;
import models.UserCommunication;
import models.notification.CPNotification;
import models.payment.Payment;
import utils.BasicUtils;
import utils.ColumnsNFields;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.MailUtils;
import utils.MailUtils.ContentType;
import utils.NotificationUtils;
import utils.NotificationUtils.AppScreens;
import utils.NotificationUtils.AudienceType;
import utils.NotificationUtils.NotificationKind;
import utils.NotificationUtils.NotificationPriority;
import utils.NotificationUtils.OnClickAction;
import utils.NotificationUtils.ScheduleType;
import utils.PaymentUtils;
import utils.PaymentUtils.ErrorType;
import utils.PaymentUtils.PaymentType;
import utils.ProptertyUtils;
import utils.ProptertyUtils.Keys;
import v1.repository.NotificationRepository;
import v2.dbhelpers.DatabaseHelper;
import v2.managers.UserComHelper.CommunicationType;

public class PaymentManager {

	private final DatabaseHelper dbHelper;

	private NotificationRepository _notificationRepo = null;
	private AppContextProvider appContextProvider = new AppContextProvider();

	public PaymentManager() throws Exception {
		dbHelper = new DatabaseHelper();
	}

//	private void log(String value) {
//		LoggerUtils.log("V2 PaymentManager." + value);
//	}

	private NotificationRepository notificationRepo() throws Exception {
		if (null == _notificationRepo)
			_notificationRepo = new NotificationRepository();
		return _notificationRepo;
	}

	public JSONObject initiatePayment(int userId, JSONObject bodyObject) throws Exception {

		JSONObject responseJson = null;

		try {

			User user = appContextProvider.getUserById(userId);

			if (null != user) {

				Payment payment = new Payment(bodyObject.optJSONObject("paymentRequest"));
				payment.userId = userId;
				payment.initialDateTime = DateTimeUtils.getCurrentDateTimeInIST();
				payment.orderId = "TXN" + System.currentTimeMillis();

				Payment storedPaymentValues = dbHelper.initiatePayment(payment);

				if (null != storedPaymentValues) {

					payment.id = storedPaymentValues.id;

					responseJson = new JSONObject();
					responseJson.put(Constants.STATUS, Constants.SUCCESS);
					responseJson.put(Constants.MESSAGE, "Payment initiated successfully");
					responseJson.put("paymentDetail", payment.toJson());

				} else {
					LocalResponse localResponse = new LocalResponse();
					localResponse.message = Constants.DEFAULT_ERROR_MESSAGE;
					responseJson = localResponse.toJson();
				}

			} else {
				LocalResponse localResponse = new LocalResponse();
				localResponse.message = "No user associated with this ID";
				responseJson = localResponse.toJson();
			}

			dbHelper.close();

		} catch (Exception e) {
			dbHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseJson;

	}

	@Deprecated
	public JSONObject createOrder(int userId, String body) throws Exception {
		dao.PaymentManager pManager = new dao.PaymentManager();
		return pManager.createOrder(userId, body);
	}

	public JSONObject createPaymentOrderForRazorpay(int userId, String body) throws Exception {

		JSONObject responseJson = null;

		try {

			User user = appContextProvider.getUserById(userId);

			if (null != user) {

				JSONObject requestJson = new JSONObject(body);

				Payment payment = new Payment(requestJson.optJSONObject("paymentRequest"));

				try {

					JSONObject orderRequest = new JSONObject();
					orderRequest.put("amount", (int)Math.round(payment.paymentAmount * 100.0)); // amount in the Paise
					orderRequest.put("currency", "INR");
					orderRequest.put("receipt", payment.orderId);
					orderRequest.put("payment_capture", true);

					String razorpayKey = Constants.NA;
					String razorpaySecret = Constants.NA;

					if (Constants.IS_PAYMENT_LIVE) {
						razorpayKey = ProptertyUtils.getValurForKey(Keys.RAZORPAY_PROD_KEY);
						razorpaySecret = ProptertyUtils.getValurForKey(Keys.RAZORPAY_PROD_SECRET);
					} else {
						razorpayKey = ProptertyUtils.getValurForKey(Keys.RAZORPAY_TEST_KEY);
						razorpaySecret = ProptertyUtils.getValurForKey(Keys.RAZORPAY_TEST_SECRET);
					}

					RazorpayClient razorpay = new RazorpayClient(razorpayKey, razorpaySecret);
					Order order = razorpay.Orders.create(orderRequest);
					payment.pGatewayOrderId = order.get("id").toString();

					if (dbHelper.addRazorpayOrderId(payment))
						LoggerUtils.log("Razorpay orderId added successfully in DB");
					else
						LoggerUtils.log("Failed to add razorpay orderId in DB");

					responseJson = BasicUtils.getSuccessTemplateObject();
					responseJson.put("order", order.toJson());
					responseJson.put("paymentDetail", payment.toJson());

				} catch (RazorpayException e) {
					LoggerUtils.log("Error while generating razorpay order id: " + e.getMessage());
					LocalResponse localResponse = new LocalResponse();
					localResponse.message = "Unable to create order Id for the payment. Error: " + e.getMessage();
					responseJson = localResponse.toJson();
				}

			} else {
				LocalResponse localResponse = new LocalResponse();
				localResponse.message = "No user associated with this ID";
				responseJson = localResponse.toJson();
			}

			dbHelper.close();

		} catch (Exception e) {
			dbHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}

		return responseJson;
	}

	public JSONObject finalizePayment(int userId, JSONObject bodyObject) throws Exception {

		JSONObject responseJson = new JSONObject();

		DatabaseHelper dbHelper = new DatabaseHelper();
		utils.DatabaseHelper v1DbHelper = new utils.DatabaseHelper();
		SalesForceManager sfManager = new SalesForceManager();

		try {

			User user = appContextProvider.getUserById(userId);

			if (null != user) {

				Payment payment = new Payment(bodyObject.getJSONObject("paymentInfo"));

				if (payment.statusMessage.equalsIgnoreCase(Constants.SUCCESS)) {

					Payment existingPayment = v1DbHelper.getPaymentById(payment.id);

					if (null != existingPayment && existingPayment.isSuccess()
							&& BasicUtils.isNotNullOrNA(existingPayment.receiptNumber)) {

						// Duplicate cal for a successful transaction
						LoggerUtils.log("++++++++++++++ Payment exist and is success in DB. Just return the same");

						dbHelper.close();
						v1DbHelper.close();

						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.SUCCESS);
						responseJson.put(Constants.MESSAGE, "Payment completed successfully.");
						responseJson.put("paymentDetail", existingPayment.toJson());

						return responseJson;

					}

					String receiptData = "{}";
					if (bodyObject.optJSONObject("paymentReceipt") != null) {
						receiptData = bodyObject.getJSONObject("paymentReceipt").toString();
					} else {
						receiptData = bodyObject.getString("paymentReceipt");
					}

					payment.receiptData = receiptData;
					String paymentSubType = payment.paymentSubType;
					payment = v1DbHelper.finalizePayment(payment);

					if (null != payment) {

						// see if the receipt already exist with given id and lai on SF
						LoggerUtils.log("++++++++++++++ Receipt doesn't exist, create a new one and update");

						String existingReceiptInfo = sfManager.getReceiptInfoByIdAndLAI(payment.paymentId,
								payment.loanAccountNumber);

						if (!BasicUtils.isNotNullOrNA(existingReceiptInfo)) {

							// receipt doesn't exist, create a new one and update

							payment.paymentSubType = paymentSubType;
							payment = sfManager.addPaymentReceipt(user, payment);

							if (null == payment.receiptNumber || !BasicUtils.isNotNullOrNA(payment.receiptNumber)) {

								v1DbHelper.close();
								dbHelper.close();

								responseJson = new JSONObject();
								responseJson.put(Constants.STATUS, Constants.FAILURE);
								responseJson.put(Constants.MESSAGE, Constants.PAYMENT_ERROR_MESSAGE);
								responseJson.put("paymentDetail", payment.toJson());
								LoggerUtils.log(
										"Couldn't create receipt in the salesforce while payment finalization process.");

								PaymentUtils.notifyError(ErrorType.SALESFORCE_RECEIPT, payment);

								return responseJson;

							}

						} else {

							// receipt exist on SF with give payment ID an LAI.
							LoggerUtils.log("++++++++++++++ Receipt exist on SF with give payment ID an LAI.");

							String[] receiptInfoArray = existingReceiptInfo.split(";");

							payment.receiptId = receiptInfoArray[0];
							payment.receiptNumber = receiptInfoArray[1];

						}

						boolean finalStatus = v1DbHelper.addReceiptIdInPayment(payment);

						if (finalStatus) {

							responseJson = new JSONObject();
							responseJson.put(Constants.STATUS, Constants.SUCCESS);
							responseJson.put(Constants.MESSAGE, "Payment completed successfully.");
							responseJson.put("paymentDetail", payment.toJson());

							// -------<< communicate payment confirmation to customer >>------ //

							UserCommunication userComm = new UserCommunication();
							userComm.recordType = ColumnsNFields.PAYMENT_INFO_TABLE;
							userComm.recordId = payment.id;
							userComm.userId = user.userId;
							userComm.createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

							new UserComHelper().insertUserCommunitcation(userComm);

							if (!payment.paymentNature.equals(PaymentType.PART_PAYMENT.value)
									&& BasicUtils.isNotNullOrNA(user.mobileNumber))
								//sendPaymentConfirmationSMS(user, payment);
								  sendPaymentConfirmationWhatsApp(user, payment);

							sendPaymentConfirmationNotification(user, payment);

							if (BasicUtils.isNotNullOrNA(user.emailId))
								sendPaymentConfirmationEmail(user, payment);

						} else {

							responseJson = new JSONObject();
							responseJson.put(Constants.STATUS, Constants.FAILURE);
							responseJson.put(Constants.MESSAGE, Constants.PAYMENT_ERROR_MESSAGE);
							responseJson.put("paymentDetail", payment.toJson());
							LoggerUtils.log(
									"Couldn't update receipt number in the database while payment finalization process.");

							PaymentUtils.notifyError(ErrorType.DATABASE_UPDATE_2, payment);

						}

					} else {
						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.PAYMENT_ERROR_MESSAGE);
						LoggerUtils.log("Couldn't udpate data in the database while payment finalization process.");

						PaymentUtils.notifyError(ErrorType.DATABASE_UPDATE_1, payment);
					}

				} else {

					v1DbHelper.updateFailedPayment(payment);

					responseJson.put(Constants.STATUS, Constants.FAILURE);

					if (payment.statusMessage.equalsIgnoreCase("canceled"))
						responseJson.put(Constants.MESSAGE, "You've canceled the payment.");
					else
						responseJson.put(Constants.MESSAGE, "Payment failed. Please try again later.");
				}

			} else {
				LocalResponse localResponse = new LocalResponse();
				localResponse.message = "No user associated with this ID";
				responseJson = localResponse.toJson();
			}

			v1DbHelper.close();
			dbHelper.close();

			return responseJson;

		} catch (Exception e) {
			v1DbHelper.close();
			dbHelper.close();
			throw e;
		}

	}

	public void sendPaymentConfirmationEmail(User user, Payment payment) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 10);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}

						String paymentType = payment.paymentNature;
						if (payment.paymentNature.equals(PaymentType.SERVICE_REQUEST.value))
							paymentType = "Document Request Charges";

						String dateTime = DateTimeUtils.getDateTimeFromString(payment.completionDateTime,
								DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.d_EEE_yyyy_hh_mm_a,
								DateTimeZone.IST);

						String htmlBody = "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n" + "\n"
								+ "  <meta charset=\"utf-8\">\n"
								+ "  <meta http-equiv=\"x-ua-compatible\" content=\"ie=edge\">\n"
								+ "  <title>Payment Receipt</title>\n"
								+ "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
								+ "  <meta name=\"x-apple-disable-message-reformatting\"> \n" + "\n"
								+ "  <style type=\"text/css\">\n" + "    \n" + "    /**\n"
								+ "   * Avoid browser level font resizing.\n" + "   * 1. Windows Mobile\n"
								+ "   * 2. iOS / OSX\n" + "   */\n" + "  body,\n" + "  table,\n" + "  td,\n" + "  a {\n"
								+ "    -ms-text-size-adjust: 100%; \n" + "    -webkit-text-size-adjust: 100%; \n"
								+ "  }\n" + "\n" + "  /**\n"
								+ "   * Remove extra space added to tables and cells in Outlook.\n" + "   */\n"
								+ "   table,\n" + "  td {\n" + "    mso-table-rspace: 0pt;\n"
								+ "    mso-table-lspace: 0pt;\n" + "  }\n" + "\n" + "  /**\n"
								+ "   * Better fluid images in Internet Explorer.\n" + "   */\n" + "   img {\n"
								+ "    -ms-interpolation-mode: bicubic;\n" + "  }\n" + "\n" + "  /**\n"
								+ "   * Remove blue links for iOS devices.\n" + "   */\n"
								+ "   a[x-apple-data-detectors] {\n" + "    font-family: inherit !important;\n"
								+ "    font-size: inherit !important;\n" + "    font-weight: inherit !important;\n"
								+ "    line-height: inherit !important;\n" + "    color: inherit !important;\n"
								+ "    text-decoration: none !important;\n" + "  }\n" + "\n" + "  /**\n"
								+ "   * Fix centering issues in Android 4.4.\n" + "   */\n"
								+ "   div[style*=\"margin: 16px 0;\"] {\n" + "    margin: 0 !important;\n" + "  }\n"
								+ "\n" + "  body {\n" + "    width: 100% !important;\n"
								+ "    height: 100% !important;\n" + "    padding: 0 !important;\n"
								+ "    margin: 0 !important;\n" + "  }\n" + "\n" + "  /**\n"
								+ "   * Collapse table borders to avoid space between cells.\n" + "   */\n"
								+ "   table {\n" + "    border-collapse: collapse !important;\n" + "  }\n" + "\n"
								+ "  a {\n" + "    color: #1a82e2;\n" + "  }\n" + "\n" + "  img {\n"
								+ "    height: auto;\n" + "    line-height: 100%;\n" + "    text-decoration: none;\n"
								+ "    border: 0;\n" + "    outline: none;\n" + "  }\n" + "  .w-b {\n"
								+ "    word-break: break-word }\n" + "\n"
								+ "    @media only screen and (max-width: 719px) {\n" + "        .tdFont {\n"
								+ "            font-size: 28px !important;\n" + "        }\n" + "\n"
								+ "        .hidden-mobile {\n" + "            display: none !important;\n"
								+ "        }\n" + "        \n" + "        .tableData-mobile {\n"
								+ "            width:  90%;\n" + "        }\n" + "    }\n" + "\n"
								+ "    .tableData-mobile {\n" + "      width:  500px;\n" + "    }\n" + "\n"
								+ "  </style>\n" + "</head>\n" + "<body style=\"background-color: #d8deec;\">\n"
								+ "  <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" >\n"
								+ "    <!-- Header (logo's) -->\n" + "    <tr>\n" + "      <td>\n"
								+ "        <table align=\"center\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 700px;min-width: 360px;\">\n"
								+ "          <tr>\n"
								+ "            <td align=\"left\" valign=\"top\" style=\"padding: 24px 0px 24px 24px;\">\n"
								+ "              <a href=\"https://homefirstindia.com\" target=\"_blank\" style=\"display: inline-block;\" title=\"Home First\">\n"
								+ "                <img src=\"https://homefirstindia-s3bucket.s3.ap-south-1.amazonaws.com/Resources/icons/homefirst_logo.png\" alt=\"\" border=\"0\"  style=\"display: block; max-height: 48px; width: auto;\">\n"
								+ "              </a>\n" + "            </td>\n"
								+ "            <td align=\"right\" valign=\"bottom\" style=\"padding: 24px 6px 24px 0px;\">\n"
								+ "              <a href=\"https://play.app.goo.gl/?link=https://play.google.com/store/apps/details?id=com.iexceed.hffc\" target=\"_blank\" style=\"display: inline-block;padding: 10px;\" title=\"Customer portal android app\">\n"
								+ "                <img src=\"https://homefirstindia-s3bucket.s3.ap-south-1.amazonaws.com/Resources/icons/16_android_playstore_icon.png\" alt=\"\" border=\"0\"  style=\"display: block; width: auto; max-width: 24px; height: auto;\">\n"
								+ "              </a>\n" + "\n"
								+ "              <a href=\"https://apps.apple.com/us/app/hffc-customer-portal/id1444845347\" target=\"_blank\" style=\"display: inline-block;padding: 10px;\" title=\"Customer portal ios app\">\n"
								+ "                <img src=\"https://homefirstindia-s3bucket.s3.ap-south-1.amazonaws.com/Resources/icons/16_apple_icon.png\" alt=\"\" border=\"0\"  style=\"display: block; width: auto; max-width: 24px; height: auto;\">\n"
								+ "              </a>\n" + "\n"
								+ "              <a href=\"https://customers.homefirstindia.com\" target=\"_blank\" style=\"display: inline-block;padding: 10px;\" title=\"Customer portal web\">\n"
								+ "                <img src=\"https://homefirstindia-s3bucket.s3.ap-south-1.amazonaws.com/Resources/icons/16_www_icon.png\" alt=\"\" border=\"0\"  style=\"display: block; width: auto;  max-width: 24px; height: auto;\">\n"
								+ "              </a>\n" + "            </td>\n" + "          </tr>\n"
								+ "        </table>\n" + "      </td>\n" + "    </tr>\n" + "    <!-- End Header -->\n"
								+ "\n" + "    <!-- Payment title -->\n" + "    <tr>\n" + "      <td>\n"
								+ "        <table align=\"center\" bgcolor=\"#ffffff\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 700px;min-width: 360px;\">\n"
								+ "          <tbody style=\"box-shadow: 0 6px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23);\">\n"
								+ "            <tr>\n"
								+ "              <td align=\"center\" class=\"tdFont\" style=\"padding: 24px; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif;  font-size: 36px; line-height: 48px;color: #444444;\">\n"
								+ "                  <p style=\"margin-bottom: 0px;margin-top: 0px;\">\n"
								+ "                    Your <span style=\"color: #4551c9;font-weight: 600;\">"
								+ paymentType + "</span> of  <br class=\"hidden-mobile\">\n"
								+ "                    &#8377; <span style=\"color: #4551c9;font-weight: 600;\">"
								+ BasicUtils.getCurrencyString(payment.paymentAmount)
								+ "</span> is successfully received\n" + "                  </p>\n"
								+ "              </td>\n" + "            </tr>\n" + "          </tbody>\n"
								+ "        </table>\n" + "      </td>\n" + "    </tr>\n"
								+ "    <!-- End of payment title -->\n" + "  \n" + "  <!-- Receipt imag -->\n"
								+ "    <tr>\n" + "      <td>\n"
								+ "        <table align=\"center\" bgcolor=\"#ffffff\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 700px;min-width: 360px;\">\n"
								+ "          <tbody style=\"box-shadow: 0 26px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23);\">\n"
								+ "            <tr>\n"
								+ "              <td align=\"center\" valign=\"top\" style=\"padding: 24px;\">\n"
								+ "                <img src=\"https://homefirstindia-s3bucket.s3.ap-south-1.amazonaws.com/HFCustomerPortal/Profile_picture/16-receipt.png\" alt=\"\" border=\"0\" style=\"display: block; width: 100%; height: auto; max-width: 297px;\">\n"
								+ "              </td>\n" + "            </tr>\n" + "          </tbody>\n"
								+ "        </table>\n" + "      </td>\n" + "    </tr>\n"
								+ "  <!-- End of receipt image -->\n" + "\n" + "  <!-- Payment detail -->\n"
								+ "    <tr>\n" + "      <td>\n"
								+ "        <table align=\"center\" bgcolor=\"#ffffff\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 700px;min-width: 360px;\">\n"
								+ "          <tbody style=\"box-shadow: 0 26px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23);\">\n"
								+ "            <tr>\n"
								+ "              <td align=\"center\" valign=\"top\" style=\"padding: 24px;\">\n"
								+ "                <span style=\"padding: 24px; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 24px; line-height: 24px;\">\n"
								+ "                  <strong> Receipt detail </strong> \n" + "                </span>\n"
								+ "              </td>\n" + "            </tr>\n" + "  \n" + "            <tr>\n"
								+ "              <td bgcolor=\"#ffffff\" style=\"padding: 0px 24px; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\">\n"
								+ "                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"tableData-mobile\" align=\"center\" >\n"
								+ "                  <tbody style=\"border-bottom: 1px solid #bac0cc;\">\n"
								+ "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 12px 12px 12px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Loan Account No </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 12px 48px 12px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\">"
								+ payment.loanAccountNumber + "</strong></td>\n" + "                    </tr>\n"
								+ "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 12px 12px 24px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Loan Type </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 12px 48px 24px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\" >"
								+ payment.loanType + "</strong> </td>\n" + "                    </tr>\n"
								+ "                  </tbody>\n" + "                </table>\n"
								+ "              </td>\n" + "            </tr>\n" + "\n" + "            <tr>\n"
								+ "              <td bgcolor=\"#ffffff\" style=\"padding: 0px 24px; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\">\n"
								+ "                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"tableData-mobile\" align=\"center\" >\n"
								+ "                  <tbody style=\"border-bottom: 1px solid #bac0cc;\">\n"
								+ "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 24px 12px 12px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Total Amount </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 24px 48px 12px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\" >&#8377; "
								+ BasicUtils.getCurrencyString(payment.paymentAmount) + "</strong></td>\n"
								+ "                    </tr>\n" + "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 12px 12px 12px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Payment Type </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 12px 48px 12px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\">"
								+ paymentType + "</strong> </td>\n" + "                    </tr>\n"
								+ "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 12px 12px 24px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Date </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 12px 48px 24px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\" >"
								+ dateTime + "</strong> </td>\n" + "                    </tr>\n"
								+ "                  </tbody>\n" + "                </table>\n"
								+ "              </td>\n" + "            </tr>\n" + "\n" + "            <tr>\n"
								+ "              <td bgcolor=\"#ffffff\" style=\"padding: 0px 24px 24px 24px; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\">\n"
								+ "                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"tableData-mobile\" align=\"center\" >\n"
								+ "                  <tbody >\n" + "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 24px 12px 12px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Payment ID </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 24px 48px 12px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\" >"
								+ payment.paymentId + "</strong></td>\n" + "                    </tr>\n"
								+ "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 12px 12px 12px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Order ID </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 12px 48px 12px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\">"
								+ payment.orderId + "</strong> </td>\n" + "                    </tr>\n"
								+ "                    <tr>\n"
								+ "                      <td align=\"left\" width=\"45%\" class=\"w-b\" style=\"padding: 12px 12px 24px 48px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 14px; line-height: 24px;color: #7a7a7a;\"> Receipt No </td>\n"
								+ "                      <td align=\"left\" width=\"55%\" class=\"w-b\" style=\"padding: 12px 48px 24px 12px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 16px; line-height: 24px;\"><strong style=\" font-size: 18px; font-weight: 500;\" >"
								+ payment.receiptNumber + "</strong> </td>\n" + "                    </tr>\n"
								+ "                  </tbody>\n" + "                </table>\n"
								+ "              </td>\n" + "            </tr>\n" + "          </tbody>\n"
								+ "        </table>\n" + "      </td>\n" + "    </tr>\n"
								+ "  <!-- End of payment detail -->\n" + "\n" + "  <!-- Footer -->\n" + "    <tr>\n"
								+ "      <td>\n"
								+ "        <table align=\"center\" bgcolor=\"#f2f2f2\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 700px;min-width: 360px;margin-bottom: 30px;\">\n"
								+ "          <tbody style=\"border-top: 1px solid #bac0cc;box-shadow: 0 26px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23);\">\n"
								+ "            <tr>\n"
								+ "              <td align=\"center\" valign=\"top\" style=\"padding: 24px;\">\n"
								+ "                <strong style=\"font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif;font-size: 14px;\">\n"
								+ "                  For any query regarding payments </strong>\n"
								+ "                <p style=\"margin-bottom: 0px; margin-top: 0px; padding: 10px 10px; font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif; font-size: 12px;\">\n"
								+ "                  Customer help line : <a href=\"tel:8880549911\"> +91-8880549911 </a>   &nbsp; | &nbsp;  Email : <a href=\"mailto: loanfirst@homefirstindia.com?subject=Payment query\"> loanfirst@homefirstindia.com </a>\n"
								+ "                </p>\n" + "              </td>          \n" + "            </tr>\n"
								+ "          </tbody>\n" + "        </table>\n" + "      </td>\n" + "    </tr>\n"
								+ "  <!-- End of footer -->\n" + "  </table>\n" + "\n"
								+ "  <table  align=\"center\"  border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 700px;min-width: 360px;\">\n"
								+ "    <tr>\n"
								+ "      <td  align=\"center\" valign=\"top\" style=\"padding: 24px;font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen-Sans,Ubuntu,Cantarell,'Helvetica Neue',sans-serif;line-height: 24px;\">\n"
								+ "        This is an automatic email generated by HomeFirst Customer portal.\n"
								+ "        <br> Please do not reply to this email.\n" + "      </td>\n" + "    </tr>\n"
								+ "  </table>\n" + "</body>\n" + "</html>";

						MailUtils.getInstance().sendDefaultMail(ContentType.TEXT_HTML,
								"Your payment receipt from HomeFirst", htmlBody,
								// "sarikabarge111@gmail.com",
								// "ayush.maurya@homefirstindia.com",
								// "maurya.ayush007@gmail.com",
								// "anuj.bhelkar@gmail.com",
								// "sarika.barge@homefirstindia.com",
								// "anuj.bhelkar@homefirstindia.com",
								// "rananrodriques@gmail.com",
								// "ranan.rodrigues@homefirstindia.com",
								// "ayushmaurya686@gmail.com"
								user.emailId);

						UserCommunication userComm = new UserCommunication();
						userComm.isEmail = true;
						userComm.emailDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						userComm.recordType = ColumnsNFields.PAYMENT_INFO_TABLE;
						userComm.recordId = payment.id;
						userComm.userId = user.userId;

						new UserComHelper().updateUserCommunication(userComm, CommunicationType.EMAIL);

						LoggerUtils.log("Payment confirmation Email Task completed; Iteration: " + count);
						timer.cancel();

					} catch (Exception e) {

						LoggerUtils.log("Error while sending Payment confirmation Email: " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Payment confirmation Email Task rescheduled, Iteration: " + count);

					}

				} else {

					timer.cancel();
					LoggerUtils.log("Time's up! Failed to send Payment confirmation Email.");

				}

			}
		}, time, 10000);

	}

	public void sendPaymentConfirmationSMS(User user, Payment payment) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 5);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}

						ContactManager cManager = new ContactManager();

						JSONObject smsBody = new JSONObject();

						smsBody.put("mobiles", "91" + user.mobileNumber);
						smsBody.put("flow_id", ContactManager.PAYMENT_CONFIRMATION_FLOW_ID);
						smsBody.put("amount", BasicUtils.getCurrencyString(payment.paymentAmount));
						smsBody.put("date", DateTimeUtils.getStringFromDateTimeString(payment.completionDateTime,
								DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.d_EEE_yyyy));
						smsBody.put("pay_id", payment.paymentId);
						smsBody.put("r_no", payment.receiptNumber);

						LocalResponse smsResponse = cManager.sendSMSViaFlow(smsBody);

						if (smsResponse.isSuccess) {

							UserCommunication userComm = new UserCommunication();

							userComm.recordType = ColumnsNFields.PAYMENT_INFO_TABLE;
							userComm.recordId = payment.id;
							userComm.userId = user.userId;
							userComm.isSMS = true;
							userComm.smsDatetime = DateTimeUtils.getCurrentDateTimeInIST();

							new UserComHelper().updateUserCommunication(userComm, CommunicationType.SMS);

							LoggerUtils.log("Payment confirmation SMS Task completed; Iteration: " + count);
							timer.cancel();

						} else {

							count++;
							LoggerUtils.log("Error while sening Payment confirmation SMS: " + smsResponse.error
									+ " | Rescheduled, Iteration: " + count);

						}

					} catch (Exception e) {

						LoggerUtils.log("Error while sending Payment confirmation SMS: " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Payment confirmation SMS Task rescheduled, Iteration: " + count);

					}

				} else {

					timer.cancel();
					LoggerUtils.log("Time's up! Failed to send Payment confirmation SMS.");

				}

			}
		}, time, 12000);

	}
	
	public void sendPaymentConfirmationWhatsApp(User user, Payment payment) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 5);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}
						
						ContactManager contactManager = new ContactManager();
						
						var requestJson = new JSONObject();
                        
                        var msg = "Your payment of Rs." + BasicUtils.getCurrencyString(payment.paymentAmount) + " on " +
                                 DateTimeUtils.getStringFromDateTimeString(payment.completionDateTime, DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.dd_MM_yyyy) + " has been successfully received. Payment ID: " + payment.paymentId + 
                                ". Receipt No: " + payment.receiptNumber + ".";
                        
                        var encodedMsg = URLEncoder.encode(msg, StandardCharsets.UTF_8);
                                                
                        requestJson.put(Constants.MOBILE_NUMBER, user.mobileNumber);
                        requestJson.put(Constants.CUSTOMER_NAME, user.name);
                        requestJson.put(Constants.MESSAGE, encodedMsg);
                        requestJson.put(Constants.SOURCE, Constants.SOURCE_CUSTOMER_PORTAL);
						
						var lResponse = contactManager.sendWhatsAppMessage(requestJson);

						if (lResponse.isSuccess) {
							
							var responseJson = new JSONObject(lResponse.message);
							
							var hfoId = responseJson.optString(Constants.ID, Constants.NA);
							
							var userComm = new UserCommunication();

							userComm.recordType = ColumnsNFields.PAYMENT_INFO_TABLE;
							userComm.recordId = payment.id;
							userComm.hfoId = hfoId;
							userComm.userId = user.userId;
							userComm.isSMS = true;
							userComm.smsDatetime = DateTimeUtils.getCurrentDateTimeInIST();

							new UserComHelper().updateUserCommunication(userComm, CommunicationType.SMS);

							LoggerUtils.log("Payment confirmation WhatsApp Message Task completed; Iteration: " + count);
							timer.cancel();
							
						} else {
							count++;
							LoggerUtils.log("Error while sening Payment confirmation WhatsApp Message: " + lResponse.message
									+ " | Rescheduled, Iteration: " + count);

						}

					} catch (Exception e) {

						LoggerUtils.log("Error while sending Payment confirmation WhatsApp Message: " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log("Payment confirmation WhatsApp Message Task rescheduled, Iteration: " + count);

					}

				} else {

					timer.cancel();
					LoggerUtils.log("Time's up! Failed to send Payment confirmation WhatsApp Message.");

				}

			}
		}, time, 12000);

	}

	private void sendPaymentConfirmationNotification(User user, Payment payment) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 15);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					// NotificationDatabaseHelper ndbHelper = new NotificationDatabaseHelper();

					try {

						final var userNotificationKeys = notificationRepo()
								.getUserNotificationTokensByMobile(user.mobileNumber);

						if (userNotificationKeys.size() == 0) {
							LoggerUtils
									.log("No registration keys found while sending Payment confirmation notification.");
							// ndbHelper.close();
							timer.cancel();
							return;
						}

						CPNotification cpNotification = new CPNotification();
						// cpNotification.audienceGroup = new JSONObject();
						cpNotification.audienceType = AudienceType.PERSONALIZED.value;

						String paymentType = payment.paymentNature;
						if (payment.paymentNature.equals(PaymentType.SERVICE_REQUEST.value))
							paymentType = "Document Request Charges";

						StringBuilder sb = new StringBuilder();
						sb.append("Your ");
						sb.append(paymentType.toLowerCase());
						sb.append(" of Rs. ");
						sb.append(BasicUtils.getCurrencyString(payment.paymentAmount));
						sb.append(" for loan account number ");
						sb.append(payment.loanAccountNumber);
						sb.append(
								" has been successfully received. Same will be reflected in your account once processed.");
						sb.append("\nReceipt No. ");
						sb.append(payment.receiptNumber);

						cpNotification.title = "Payment received";
						cpNotification.message = sb.toString();
						cpNotification.bigMessage = sb.toString();
						cpNotification.data = payment.toJson().toString();
						cpNotification.kind = NotificationKind.TRANSACTIONAL.value;
						cpNotification.priority = NotificationPriority.HIGH.value;
						cpNotification.screenToOpen = AppScreens.PAYMENT_RECEIPT.value;
						cpNotification.onClickAction = OnClickAction.IN_APP.value;
						cpNotification.shouldSchedule = false;

						String currentDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						cpNotification.datetime = currentDatetime;
						cpNotification.createDatetime = currentDatetime;
					
						cpNotification.platform = NotificationUtils.Platform.ALL.value;

						cpNotification.scheduleType = cpNotification.shouldSchedule ? ScheduleType.LATER.value
								: ScheduleType.NOW.value;

						NotificationHelper nHelper = new NotificationHelper(1);
						cpNotification.schedulerId = nHelper.aUser.id;
						cpNotification.schedulerName = nHelper.aUser.name;

						if (!notificationRepo().saveCpNotification(cpNotification)) {
							count++;
							LoggerUtils.log(
									"Failed to create Payment confirmation notification in the DB. Rescheduled, Iteration: "
											+ count);
							return;

						}

						LoggerUtils.log("Payment confirmation notification created successfully in DB.");

					
						nHelper.schedulePersonalizedNotification(cpNotification, userNotificationKeys, false);

						LoggerUtils.log("sendPaymentConfirmationNotification completed. Iteration: " + count);


						UserCommunication userComm = new UserCommunication();
						userComm.isNotification = true;
						userComm.notificationDatetime = DateTimeUtils.getCurrentDateTimeInIST();
						userComm.recordType = ColumnsNFields.PAYMENT_INFO_TABLE;
						userComm.recordId = payment.id;
						userComm.userId = user.userId;

						new UserComHelper().updateUserCommunication(userComm, CommunicationType.NOTIFICATION);

						timer.cancel();

					} catch (Exception e) {


						LoggerUtils.log("Error while sending Payment confirmation notification : " + e.getMessage());
						e.printStackTrace();

						count++;
						LoggerUtils.log("Payment confirmation notification Task rescheduled, Iteration: " + count);

					}

				} else {

					LoggerUtils.log("Time's up! Failed to send Payment confirmation notification.");
					timer.cancel();

				}

			}

		}, time, 10000);

	}

}
