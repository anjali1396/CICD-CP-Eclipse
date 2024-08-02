package v2.managers;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import models.admin.AdminDashboard;
import models.admin.AdminLog;
import models.admin.AdminUser;
import models.payment.Payment;
import utils.AdminUtils;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.MailUtils;
import utils.ProptertyUtils;
import utils.AdminUtils.LogStatus;
import utils.AdminUtils.RecordType;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.MailUtils.ContentType;
import v2.dbhelpers.AdminDatabaseHelper;

public class AdminUserManager {	
	
	private final AdminDatabaseHelper dbHelper;
	
	public AdminUserManager() {
		dbHelper = new AdminDatabaseHelper();
	}
	
	// ==================== START OF COMMON METHODS AND IMPLEMENTATOIN =============== //
	// =============================================================================== //
	
	private JSONObject closeResourcesAndReturn(JSONObject json) {
		dbHelper.close();
		return json;
	}
	
	public boolean verifySource(String sourceCode) {
		
		try {
		
			if (BasicUtils.getTheKey(sourceCode).equals(ProptertyUtils.getValurForKey(ProptertyUtils.Keys.AHAM_BRAHMASAMI_CP))) 
				return true;
			
		} catch (Exception e) {
			LoggerUtils.log("Error while verifying admin source code: " + e.toString());
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean verifyUser(int userId, String passcode) {
		
		if (!passcode.equals(Constants.NA)) {			
			
			try {			
				AdminUser user = getUserByUserId(userId);
				return user.passcode.equals(passcode);
			} catch (Exception e) {
				LoggerUtils.log("Error while getting admin passcode: " + e.getMessage());
				e.printStackTrace();
				return false;
			}			
			
		}
		
		return false;
	}
	
	private AdminUser getUserByEmailId(String emailId) throws Exception {
		
		try {
			AdminUser user = dbHelper.getAdminUserByEmailId(emailId);
			dbHelper.close();
			return user;
		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}				
		
	}
	
	public AdminUser getUserByUserId(int userId) throws Exception {
		
		try {
			AdminUser user = dbHelper.getAdminUserByUserId(userId);
			dbHelper.close();
			if (null == user) {
				throw new Exception("No user assosicated with User ID " + userId + ". Please contact system admin.");
			}
			return user;
		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}
		
	}
	
	public boolean verifyCron(String passcode, String ipAddress) throws Exception {

		try {

			if (BasicUtils.getTheKey(passcode)
					.equals(ProptertyUtils.getValurForKey(ProptertyUtils.Keys.AHAM_CROWN_CP))
					&& dbHelper.getCronWhiteListIP(ipAddress)) {
				dbHelper.close();
				return true;
			}

			dbHelper.close();
			return false;

		} catch (Exception e) {
			LoggerUtils.log("Error while verifying source code: " + e.toString());
			dbHelper.close();
			e.printStackTrace();
			return false;
		}
	}
	
	private AdminRole getAdminRole(int code) throws Exception {
		
		AdminRole aRole = AdminRole.get(code);
		
		if (null == aRole) 
			throw new Exception("Admin roles are not specified or invalid.");
		return aRole;
	}
	
	public enum AnalyticsRange {
		
		LAST_7_DAYS("last7Days", -7),
		LAST_28_DAYS("last28Days", -28),
		ALL_TIME("allTime", -1);
		
		public final String value;
		public final int dayCode;
		AnalyticsRange(String value, int dayCode) {
			this.value = value;
			this.dayCode = dayCode;
		}
		
		public static AnalyticsRange get(String value) throws Exception {
			for (AnalyticsRange item: AnalyticsRange.values()) {
				if (item.value.equals(value)) return item;
			}
			throw new Exception("Invalid analytics range selected.");
		}
		
	}
	
	public enum SearchType {

		LOAN_ACCOUNT_NUMBER("loan_account_number"), 
		TRANSACTION_ID("order_id"), 
		ORDER_ID("pg_order_id"),
		PAYMENT_ID("payment_id");
		

		public final String key;

		SearchType(String key) {
			this.key = key;
		}

		public static SearchType get(String key) throws Exception {
			for (SearchType item : SearchType.values()) {
				if (item.key.equals(key))
					return item;
			}
			
			throw new Exception("Invalid SearchType range selected.");
		}
	}
	
	public enum AdminAction {
		
		DASHBOARD("DASHBOARD"),
		SCHEDULE_NOTIFICATION("SCHEDULE_NOTIFICATION"),
		VIEW_PAYMENT("VIEW_PAYMENT"),
		UDPATE_PAYMENT("UDPATE_PAYMENT"),
		VIEW_LEAD("VIEW_LEAD"),
		UPDATE_LEAD("UPDATE_LEAD");
		
		
		public final String value;
		AdminAction(String value) {
			this.value = value;
		}
		
	}
	
	public enum AdminRole {

		SUPER_ADMIN(
				712,
				new ArrayList<String>(
						Arrays.asList(
							AdminAction.DASHBOARD.value,
							AdminAction.SCHEDULE_NOTIFICATION.value,
							AdminAction.VIEW_PAYMENT.value,
							AdminAction.UDPATE_PAYMENT.value,
							AdminAction.VIEW_LEAD.value,
							AdminAction.UPDATE_LEAD.value
						)
				)
		),
		ADMIN_L1(
				715,
				new ArrayList<String>(
						Arrays.asList(							
								AdminAction.VIEW_PAYMENT.value,
								AdminAction.UDPATE_PAYMENT.value							
						)
				)
		),
		ADMIN_L2(
				717,
				new ArrayList<String>(
						Arrays.asList(
								AdminAction.DASHBOARD.value,
								AdminAction.SCHEDULE_NOTIFICATION.value
						)
				)
		);

		public final int role;
		public final ArrayList<String> allowedActions;
		AdminRole(int role, ArrayList<String> allowedActions) {
			this.role = role;
			this.allowedActions = allowedActions;
		}
		
		public static AdminRole get(int code) {
			for (AdminRole item: AdminRole.values()) {
				if (item.role == code) return item;
			}
			return null;
		}
		
	}
	
	// *************** END OF COMMON METHODS AND IMPLEMENTATOIN ******************* //
	// **************************************************************************** //
	
	public JSONObject getUserId(String emailId) throws Exception {
		
		if (!BasicUtils.isNotNullOrNA(emailId)) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Invalid Email ID!";
			return eResponse.toJson();
		}
		
		LoggerUtils.log("RECEIVED EMAIL ID: " + emailId);
				
		AdminUser aUser = getUserByEmailId(emailId);
				
		if (null != aUser) {
			
			JSONObject responseObject = BasicUtils.getSuccessTemplateObject();
			responseObject.put("user", aUser.toJson());
			return responseObject;
			
		}
		
		return BasicUtils.getFailureTemplateObject();
		
	}
	
	public JSONObject performLogin(int userId, JSONObject requestJson, String ipAddress) throws Exception {
	
		String emailId = requestJson.optString(Constants.EMAIL_ID, Constants.NA);
		String password = requestJson.optString(Constants.PASSWORD, Constants.NA);
		
		if (emailId.equals(Constants.NA)) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Invalid Email ID!";
			return eResponse.toJson();
		}
		
		if (password.equals(Constants.NA)) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Invalid Password!";
			return eResponse.toJson();
		}
		
		AdminUser adminUser = getUserByEmailId(emailId);
		
		if (null == adminUser) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "No user assosicated with this Email Id.";
			return eResponse.toJson();
		}
		
		AdminRole aRole = getAdminRole(adminUser.role);
		
		if (adminUser.password.equals(BasicUtils.getTheKey(password))) {
			
			try {
				
				String nPasscode = dbHelper.updateAdminPasscode(adminUser); 
				if (null != nPasscode) {
					adminUser.passcode = nPasscode;							
					LoggerUtils.log("Updated admin passcode successfully.");
				} else LoggerUtils.log("Failed to update admin passcode.");
				
				if (dbHelper.createSecondaryInfo(adminUser.id)) 
					LoggerUtils.log("Either admin secondary info already exists or created successfully.");
				else LoggerUtils.log("Failed to create admin secondary info."); 
			
				if (dbHelper.addAdminLoginInfo(adminUser.id, requestJson, ipAddress)) 
					LoggerUtils.log("Inserted admin login info successfully.");
				else LoggerUtils.log("Failed to inserted admin login info.");
				
				dbHelper.close();
				
			} catch (Exception e) {
				dbHelper.close();
				LoggerUtils.log("Error while inserting admin secondary or login info: " + e.getMessage());
				e.printStackTrace();				
			}
			
			JSONObject rJson = BasicUtils.getSuccessTemplateObject();
			rJson.put(Constants.ADMIN_USER, adminUser.toJsonWithPasscode());								
			rJson.put(Constants.ADMIN_ROLE, String.join("|", aRole.allowedActions));
			return rJson;
			
		} else {
			
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Incorrect password!";
			return eResponse.toJson();
			
		}
		
	}
	
	public JSONObject getDashboard(int userId, JSONObject requestObject) throws Exception {
		
		AdminUser aUser = getUserByUserId(userId);
		AnalyticsRange aRange = AnalyticsRange.get(requestObject.optString("rangeType", Constants.NA));
        AdminRole aRole = getAdminRole(aUser.role);
		
		if (!aRole.allowedActions.contains(AdminAction.DASHBOARD.value)) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Action not allowed!";
			return eResponse.toJson();	
		}	
		try {
			
			AdminDashboard dashboard = dbHelper.getDashboard(aRange);
			dbHelper.close();
			
			if (null != dashboard) {
				
				JSONObject rJson = BasicUtils.getSuccessTemplateObject();
				rJson.put("dashboard", dashboard.toJson());
				return rJson;
						
			} else return BasicUtils.getFailureTemplateObject();
			
		} catch (Exception e) {
			
			dbHelper.close();
			e.printStackTrace();
			throw e;
			
		}		 
			
	}

	public JSONObject searchPaymentInfo(int adminId, JSONObject requestObject) throws Exception {

		AdminUser aUser = getUserByUserId(adminId);
		AdminRole aRole = getAdminRole(aUser.role);

		if (!aRole.allowedActions.contains(AdminAction.VIEW_PAYMENT.value))
			return new LocalResponse().setMessage("Action not allowed!").toJson();

		String searchStringType = requestObject.optString("searchType", Constants.NA);
		String searchId = requestObject.optString("searchId", Constants.NA);
		double amount = requestObject.optDouble("amount", 0.0);

		if (!BasicUtils.isNotNullOrNA(searchStringType))		
			return new LocalResponse().setMessage("Invalid Search Type").toJson();
		
		if (!BasicUtils.isNotNullOrNA(searchId))		
			return new LocalResponse().setMessage("Invalid Search ID").toJson();
		
		SearchType searchType = SearchType.get(searchStringType);

		try {
						
			JSONArray paymentList = dbHelper.searchPayments(searchType, searchId, amount);
			dbHelper.close();
			
			JSONObject responseObject = BasicUtils.getSuccessTemplateObject();
			responseObject.put("paymentList", paymentList);
			return responseObject;					

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}
		
	}

	public JSONObject updatePaymentInfo(int userId, JSONObject requestObject) throws Exception {

		AdminUser aUser = getUserByUserId(userId);
		AdminRole aRole = getAdminRole(aUser.role);

		if (!aRole.allowedActions.contains(AdminAction.UDPATE_PAYMENT.value)) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Action not allowed!";
			return eResponse.toJson();
		}

		if (!requestObject.has("paymentInfo"))
			return new LocalResponse().setMessage("No paymentInfo passed to update.").toJson();
			
		Payment payment = new Payment(requestObject.getJSONObject("paymentInfo"));
		
		JSONObject responseJson = new JSONObject();		
		
		try {
			Payment existingPayment = dbHelper.getPaymentById(payment.id);					

			if (null == existingPayment) 
				return closeResourcesAndReturn(new LocalResponse().setMessage("Invalid payment info passed to update.").toJson());
			
			if (existingPayment.status.equalsIgnoreCase(Constants.SUCCESS)) {			
				
				responseJson = BasicUtils.getSuccessTemplateObject();
				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.MESSAGE, "Payment status was already SUCCESS.");
				responseJson.put("paymentDetail", existingPayment.toJson());
				
				return closeResourcesAndReturn(responseJson);				
				
			}
						
			AdminUtils adminUtils = new AdminUtils();
			AdminLog aLog = adminUtils.addAdminLog(
					userId, 
					RecordType.PAYMENT, 
					payment.id, 
					utils.AdminUtils.AdminAction.UPDATE, 
					getPaymentLogString(existingPayment, payment)
			);
			
			if (null == aLog) {
				LoggerUtils.log("Error: Failed to add new admin log while Updating Payment Info through CPAdmin");
				return closeResourcesAndReturn(new LocalResponse().setError("Failed to create Admin Log.").toJson());
			}							
			
			JSONObject pReceiptData = new JSONObject();
			pReceiptData.put("transactionId", payment.orderId);
			pReceiptData.put("paymentAmount", payment.paymentAmount);
			pReceiptData.put("paymentId", payment.paymentId);
			pReceiptData.put("loanAccountNumber", payment.loanAccountNumber);
			
			payment.receiptData = pReceiptData.toString();
			
			PaymentManager v2pManager = new PaymentManager();
			JSONObject paymentObject = new JSONObject();
			paymentObject.put("paymentInfo", payment.toJson());
			paymentObject.put("paymentReceipt", payment.receiptData);

			
			JSONObject finalizeResponse = v2pManager.finalizePayment(payment.userId, paymentObject);
			
			if (finalizeResponse.getString(Constants.STATUS).equalsIgnoreCase(Constants.SUCCESS)) {
				
				payment = new Payment(finalizeResponse.getJSONObject("paymentDetail"));
				
				LoggerUtils.log("Payment process successful while Finalizing Payment through AdminPortal.");
				sendPaymentUpdateSuccessEmail(aUser, existingPayment, payment);
				
				adminUtils.updateAdminLog(
						userId, 
						aLog.id,
						LogStatus.SUCCESS
				);
				
			} else {
				
				LoggerUtils.log("Failed to FinalizePayment while updating through AdminPortal.");
				
				adminUtils.updateAdminLog(
						userId, 
						aLog.id,
						LogStatus.FAILED
				);
			}
			
			return closeResourcesAndReturn(finalizeResponse);			

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

	}
	
	public JSONObject updateManualPaymentInfo(int userId, JSONObject requestObject) throws Exception {

		AdminUser aUser = getUserByUserId(userId);
		AdminRole aRole = getAdminRole(aUser.role);

		if (!aRole.allowedActions.contains(AdminAction.UDPATE_PAYMENT.value)) {
			LocalResponse eResponse = new LocalResponse();
			eResponse.message = "Action not allowed!";
			return eResponse.toJson();
		}

		if (!requestObject.has("paymentInfo"))
			return new LocalResponse().setMessage("No paymentInfo passed to update.").toJson();
			
		Payment payment = new Payment(requestObject.getJSONObject("paymentInfo"));
		
		JSONObject responseJson = new JSONObject();		
		
		try {
			Payment existingPayment = dbHelper.getPaymentById(payment.id);					

			if (null == existingPayment) 
				return closeResourcesAndReturn(new LocalResponse().setMessage("Invalid payment info passed to update.").toJson());
			
			if (existingPayment.statusMessage.equalsIgnoreCase(Constants.SUCCESS)) {			
				
				responseJson = BasicUtils.getSuccessTemplateObject();
				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.MESSAGE, "Payment status was already SUCCESS.");
				responseJson.put("paymentDetail", existingPayment.toJson());
				
				return closeResourcesAndReturn(responseJson);				
				
			}
						
			AdminUtils adminUtils = new AdminUtils();
			AdminLog aLog = adminUtils.addAdminLog(
					userId, 
					RecordType.PAYMENT, 
					payment.id, 
					utils.AdminUtils.AdminAction.UPDATE, 
					getPaymentLogString(existingPayment, payment)
			);
			
			if (null == aLog) {
				LoggerUtils.log("Error: Failed to add new admin log while Updating Payment Info through CPAdmin");
				return closeResourcesAndReturn(new LocalResponse().setError("Failed to create Admin Log.").toJson());
			}							
			
			JSONObject pReceiptData = new JSONObject();
			pReceiptData.put("transactionId", payment.orderId);
			pReceiptData.put("paymentAmount", payment.paymentAmount);
			pReceiptData.put("paymentId", payment.paymentId);
			pReceiptData.put("loanAccountNumber", payment.loanAccountNumber);
			
			payment.receiptData = pReceiptData.toString();
		
			payment = dbHelper.updateManunalPayment(payment);
			
			if(null == payment)
			{
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "No user associated with this ID.");
				
				LoggerUtils.log("Couldn't udpate data in the database while payment finalization process.");
				
				adminUtils.updateAdminLog(
						userId, 
						aLog.id,
						LogStatus.FAILED
				);
				
				return closeResourcesAndReturn(responseJson);			
				
			}
					
			if (payment.statusMessage.equalsIgnoreCase(Constants.SUCCESS)) {
				
				responseJson.put(Constants.STATUS, Constants.SUCCESS);
				responseJson.put(Constants.MESSAGE, "Payment completed successfully.");
				responseJson.put("paymentDetail", payment.toJson());
				
				LoggerUtils.log("Payment process successful while Finalizing Payment through AdminPortal.");
				sendPaymentUpdateSuccessEmail(aUser, existingPayment, payment);
				
				adminUtils.updateAdminLog(
						userId, 
						aLog.id,
						LogStatus.SUCCESS
				);
				
			} else {
				
				responseJson.put(Constants.STATUS, Constants.FAILURE);
				responseJson.put(Constants.MESSAGE, "Payment failed. Please try again later.");
				
				LoggerUtils.log("Failed to FinalizePayment while updating through AdminPortal.");
				
				adminUtils.updateAdminLog(
						userId, 
						aLog.id,
						LogStatus.FAILED
				);
			}
			
			return closeResourcesAndReturn(responseJson);			

		} catch (Exception e) {
			dbHelper.close();
			throw e;
		}

	}
	
	private String getPaymentLogString(Payment existingPayment, Payment updatedPayment) {

		StringBuilder sb = new StringBuilder();

		sb.append("Payment Info updated-");
		sb.append("\nNew Payment ID: " + updatedPayment.paymentId);
		sb.append("\nOld Payment ID: " + existingPayment.paymentId);

		return sb.toString();

	}

	public void sendPaymentUpdateSuccessEmail(AdminUser aUser, Payment existingPayment, Payment updatedPayment) {

		Toolkit toolkit = Toolkit.getDefaultToolkit();

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

						StringBuilder sb = new StringBuilder();

						sb.append(aUser.name + " has updated HomeFirst Customer portal Payment Info.");

						sb.append("\n\nOld Payment ID : " + existingPayment.paymentId);
						sb.append("\nNew Payment ID : " + updatedPayment.paymentId);
						sb.append("\nReceipt Number : " + updatedPayment.receiptNumber);
						sb.append("\nLoan Account Num : " + updatedPayment.loanAccountNumber);
						sb.append("\nTransaction ID : " + updatedPayment.orderId);
						sb.append("\nPayment Method : " + updatedPayment.paymentMethod);
						sb.append("\nInitial Datetime : " + existingPayment.initialDateTime);
						sb.append("\nUpdated Datetime : "
								+ DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeZone.IST));						

						sb.append("\n\n\n============== Admin's Information ================");
						sb.append("\n Name: " + aUser.name);
						sb.append("\n Email: " + aUser.email);

						sb.append(
								"\n\n\nThis is an automatic email generated by HomeFirst HomeFirst Customer portal AI.");
						sb.append("\nPlease do not reply to this email.");

						MailUtils.getInstance().sendDefaultMail(
								ContentType.TEXT_PLAIN,
								"Payment Info Updated | CPAdmin", sb.toString(),
								aUser.email, 
								"sanjay.jaiswar@homefirstindia.com",
								"ranan.rodrigues@homefirstindia.com",
								Constants.EPAY_EMAIL_ID
						);
						
						timer.cancel();
						LoggerUtils.log("CPAdmin Payment Update Task completed; Iteration: " + count);

					} catch (Exception e) {
						LoggerUtils.log("Error while sending customer portal email: " + e.getMessage());
						e.printStackTrace();
						toolkit.beep();
						count++;
						LoggerUtils.log("Task rescheduled, Iteration: " + count);

					}

				} else {

					toolkit.beep();					
					timer.cancel();
					LoggerUtils.log("Time's up! Failed to send CPAdmin Payment Update Email.");
					
				}

			}
		}, 10000);

	}

}
