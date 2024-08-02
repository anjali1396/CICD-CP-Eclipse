package dao;

import org.json.JSONObject;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import helper.AppContextProvider;
import models.User;
import models.payment.Payment;
import salesforce.SalesForceManager;
import utils.BasicUtils;
import utils.Constants;
import utils.DatabaseHelper;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.PaymentUtils;
import utils.PaymentUtils.ErrorType;
import utils.ProptertyUtils;
import utils.ProptertyUtils.Keys;

public class PaymentManager {
	
	private SalesForceManager sfManager = new SalesForceManager();
	private AppContextProvider appContextProvider = new AppContextProvider();
	
	public PaymentManager() {}
	
	@Deprecated
	public JSONObject initiatePayment(int userId, JSONObject bodyObject) throws Exception {
		
		JSONObject responseJson = null;
		
		DatabaseHelper dbHelper = new DatabaseHelper();
		
		try {
			
			User user = appContextProvider.getUserById(userId);
			
			if (null != user) {
				
				Payment payment = new Payment(bodyObject);
				payment.userId = userId;
				String paymentSubType = payment.paymentSubType;
				payment = dbHelper.initiatePayment(payment);			
				
				if (null != payment) {
					
					payment.paymentSubType = paymentSubType;
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
		
		JSONObject responseJson = null;	
		
		try {
			
			User user = appContextProvider.getUserById(userId);
			
			if (null != user) { 
				
				JSONObject requestJson = new JSONObject(body);
				
				String orderId = requestJson.optString(PaymentUtils.ORDER_ID, Constants.NA);
				double paymentAmount = requestJson.optDouble(PaymentUtils.PAYMENT_AMOUNT, 0.0);
				
				try {
					
					  JSONObject orderRequest = new JSONObject();
					  orderRequest.put("amount", paymentAmount * 100); // amount in the Paise
					  orderRequest.put("currency", "INR");
					  orderRequest.put("receipt", orderId);
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
					  
					  //LoggerUtils.log("generate order id: " + order.get("id"));					 
					  responseJson = BasicUtils.getSuccessTemplateObject();
					  responseJson.put("order", order.toJson());					 
					  
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
			
		} catch (Exception e) {
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}
		
		return responseJson;
	}

	
	@Deprecated
	public JSONObject finalizePayment(int userId, JSONObject bodyObject) throws Exception {
		
		JSONObject responseJson = new JSONObject();
		
		DatabaseHelper dbHelper = new DatabaseHelper();
		
		try {
			
			User user = appContextProvider.getUserById(userId);
			
			if (null != user) {
				
				Payment payment = new Payment(bodyObject.getJSONObject("paymentInfo"));
				
				if (payment.statusMessage.equalsIgnoreCase(Constants.SUCCESS)) { 
					
					String receiptData = "{}";
					if (bodyObject.optJSONObject("paymentReceipt") != null) {
						receiptData = bodyObject.getJSONObject("paymentReceipt").toString();
					} else {
						receiptData = bodyObject.getString("paymentReceipt");
					}
					
					payment.receiptData = receiptData;
					String paymentSubType = payment.paymentSubType;
					payment = dbHelper.finalizePayment(payment);
					
					if (null != payment) {
						payment.paymentSubType = paymentSubType;
						payment = sfManager.addPaymentReceipt(user, payment);
						
						if (null != payment.receiptNumber && !payment.receiptNumber.equals(Constants.NA)) {
							
							boolean finalStatus = dbHelper.addReceiptIdInPayment(payment);
							
							if (finalStatus) {
								
								responseJson = new JSONObject();
								responseJson.put(Constants.STATUS, Constants.SUCCESS);
								responseJson.put(Constants.MESSAGE, "Payment completed successfully.");
								responseJson.put("paymentDetail", payment.toJson());
								
							} else {
								
								responseJson = new JSONObject();
								responseJson.put(Constants.STATUS, Constants.FAILURE);
								responseJson.put(Constants.MESSAGE, Constants.PAYMENT_ERROR_MESSAGE);
								responseJson.put("paymentDetail", payment.toJson());
								LoggerUtils.log("Couldn't update receipt number in the database while payment finalization process.");
								
								PaymentUtils.notifyError(ErrorType.DATABASE_UPDATE_2, payment);
								
							}

						} else {
							
							responseJson = new JSONObject();
							responseJson.put(Constants.STATUS, Constants.FAILURE);
							responseJson.put(Constants.MESSAGE, Constants.PAYMENT_ERROR_MESSAGE);
							responseJson.put("paymentDetail", payment.toJson());
							LoggerUtils.log("Couldn't create receipt in the salesforce while payment finalization process.");
							
							PaymentUtils.notifyError(ErrorType.SALESFORCE_RECEIPT, payment);
							
						}
						
					} else {
						responseJson = new JSONObject();
						responseJson.put(Constants.STATUS, Constants.FAILURE);
						responseJson.put(Constants.MESSAGE, Constants.PAYMENT_ERROR_MESSAGE);
						LoggerUtils.log("Couldn't udpate data in the database while payment finalization process.");
						
						PaymentUtils.notifyError(ErrorType.DATABASE_UPDATE_1, payment);
					}
					
				} else {
			
					dbHelper.updateFailedPayment(payment);
					
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
			
			dbHelper.close();
			
		} catch (Exception e) {
			dbHelper.close();
			LoggerUtils.log("Error: " + e.toString());
			e.printStackTrace();
			throw e;
		}
		
		return responseJson;
		
	}
	
}
