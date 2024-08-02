package utils;

import java.sql.ResultSet;
import java.sql.SQLException;

import models.payment.Payment;
import utils.MailUtils.ContentType;

public class PaymentUtils {
	
	//private static final String PAYMENT_DATA = "paymentData";
	public static final String PAYMENT_AMOUNT = "paymentAmount";
	//private static final String P_GATEWAY_ORDER_ID = "pGatewayOrderId";
	public static final String ORDER_ID = "orderId";

	
	public enum ErrorType {
		DATABASE_UPDATE_1, SALESFORCE_RECEIPT, DATABASE_UPDATE_2
	}
	
	public enum PaymentMethod {
	    RAZORPAY("Razorpay"),
	    PAYNIMO("PayNimo");
	    
	    public final String value;
	    PaymentMethod(String value) {
			this.value = value;
		}
	    
	    public static PaymentMethod get(String value) {
			for (PaymentMethod item: PaymentMethod.values()) {
				if (item.value.equals(value)) return item;
			}
			return null;
		}
	}
	
	public enum PaymentType {
	    EMI("EMI"),
	    PART_PAYMENT("Partial Pre-Payment"),
	    SERVICE_REQUEST("Service Request"),
		AUTO_PREPAY("Auto Pre-payment"),
	    OTHER_CHARGES("Other Charges");

		
		public final String value;
		PaymentType(String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			return value;
		}
		
		public static PaymentType get(String value) {
			for (PaymentType item: PaymentType.values()) {
				if (item.value.equals(value)) return item;
			}
			return null;
		}
	}
	
	public enum AmountType {
	    EMI_AMOUNT("emiAmount"),
	    PART_PAYMENT_AMOUNT("partPaymentAmount"),
	    BOUNCE_CHARGES("bounceCharges"),
	    INSURANCE_AMOUNT("insuranceAmount");
	    
	    public final String value;
	    AmountType(String value) {
			this.value = value;
		}
	    
	    public static AmountType get(String value) {
            for (AmountType item : AmountType.values()) {
                if (item.value.equals(value)) return item;
            }
            return null;
        }
	}
	
	public static void notifyError(ErrorType errorType, Payment payment) {
		
		if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE)
			return;
		
		try {
			
			String errorString = "";
			if (errorType == ErrorType.DATABASE_UPDATE_1) 
				errorString = "Error happened while updating the receipt data and payment identifier in the Application Database";
			else if (errorType == ErrorType.SALESFORCE_RECEIPT) 
				errorString = "Error happened while creating Payment Receipt on Salesforce";
			else if (errorType == ErrorType.DATABASE_UPDATE_2) 
				errorString = "Error happened while updating the Receipt Number in the Application Database";
			
			StringBuilder sb = new StringBuilder();
			
			sb.append("ERROR WHILE RECEIPT GENERATION in HomeFirst Customer Portal App. Find details below.");
			sb.append("\n\n" + errorString);
			sb.append("\n\n Loan account number ==> " + payment.loanAccountNumber);
			sb.append("\n Loan type ==> " + payment.loanType);
			
			String receiptNumber = Constants.NA; 
			if (null != payment.receiptNumber) 
				receiptNumber = payment.receiptNumber;
			sb.append("\n Receipt number ==> " + receiptNumber);			
			sb.append("\n Payment ID ==> " + payment.paymentId);
			sb.append("\n Payment Amount ==> " + payment.paymentAmount);
			sb.append("\n Order ID ==> " + payment.orderId);
			sb.append("\n Nature of payment ==> " + payment.paymentNature);
			sb.append("\n Pre-Payment type ==> " + payment.prePaymentType);
			sb.append("\n User's application database ID ==> " + payment.userId);
			sb.append("\n\n Receipt data ==> " + payment.receiptData + "\n");
			

			MailUtils.getInstance().sendDefaultMail(
					ContentType.TEXT_PLAIN,
					"Receipt Generation Error | HomeFirst Customer Portal App",
					sb.toString(), // TODO: uncomment below email ids
					"mahesh.saggurthi@homefirstindia.com",
					"gaurav.khetia@homefirstindia.com", 
					"ranan.rodrigues@homefirstindia.com",
					"sanjay.jaiswar@homefirstindia.com"					 		 
			);
			
		} catch(Exception e) {
			LoggerUtils.log("Error while notifying error email: " + e.toString());
			e.printStackTrace();
		}
		
	}
	
    public static Payment getPaymentObjectFromResultSet(ResultSet resultSet, boolean shouldGetReceipt) throws SQLException {
		
		Payment payment = new Payment();
				
		payment.id = resultSet.getInt(ColumnsNFields.PaymentInfoColumn.ID.value);
		payment.userId = resultSet.getInt(ColumnsNFields.PaymentInfoColumn.USER_ID.value);
		payment.orderId = resultSet.getString(ColumnsNFields.PaymentInfoColumn.ORDER_ID.value);
		payment.paymentId = resultSet.getString(ColumnsNFields.PaymentInfoColumn.PAYMENT_ID.value);
		payment.loanAccountNumber = resultSet.getString(ColumnsNFields.PaymentInfoColumn.LOAN_ACCOUNT_NUMBER.value);
		payment.loanType = resultSet.getString(ColumnsNFields.PaymentInfoColumn.LOAN_TYPE.value);
		payment.paymentNature = resultSet.getString(ColumnsNFields.PaymentInfoColumn.PAYMENT_NATURE.value);
		payment.pGatewayOrderId = resultSet.getString(ColumnsNFields.PaymentInfoColumn.PG_ORDER_ID.value);
		
		if (!payment.paymentNature.equals("EMI")) {		
			if (resultSet.getInt(ColumnsNFields.PaymentInfoColumn.EMI_REDUCTION.value) == 1)
				payment.prePaymentType = "EMI Reduction";
			else if (resultSet.getInt(ColumnsNFields.PaymentInfoColumn.TENURE_REDUCTION.value) == 1)
				payment.prePaymentType = "Tenor Reduction";
		}

	    payment.paymentSubType = resultSet.getString(ColumnsNFields.PaymentInfoColumn.PAYMENT_SUB_TYPE.value);
		payment.paymentAmount = resultSet.getDouble(ColumnsNFields.PaymentInfoColumn.AMOUNT.value);
		payment.paymentMethod = resultSet.getString(ColumnsNFields.PaymentInfoColumn.PAYMENT_METHOD.value);
		payment.deviceType = resultSet.getString(ColumnsNFields.PaymentInfoColumn.DEVICE_TYPE.value);
		payment.initialDateTime = resultSet.getString(ColumnsNFields.PaymentInfoColumn.INITIAL_DATETIME.value);
		payment.completionDateTime = resultSet.getString(ColumnsNFields.PaymentInfoColumn.COMPLETION_DATETIME.value);
		
		if (shouldGetReceipt)
			payment.receiptData = resultSet.getString(ColumnsNFields.PaymentInfoColumn.RECEIPT_DATA.value);
		
		String receiptId = resultSet.getString(ColumnsNFields.PaymentInfoColumn.RECEIPT_ID.value);
		if (null == receiptId) receiptId = Constants.NA; 		
		payment.receiptId = receiptId;
				
		payment.receiptNumber = resultSet.getString(ColumnsNFields.PaymentInfoColumn.RECEIPT_NUMBER.value);
		payment.status = resultSet.getString(ColumnsNFields.PaymentInfoColumn.STATUS.value);
		payment.statusMessage = resultSet.getString(ColumnsNFields.PaymentInfoColumn.STATUS_MESSAGE.value);
		
		return payment;
		
	}

}
