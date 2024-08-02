package models.payment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ColumnDefault;
import org.json.JSONException;
import org.json.JSONObject;

import utils.Constants;
import utils.DateTimeUtils;

@Entity
@Table(name = "payment_info",schema = "HomeFirstCustomerPortal")
public class Payment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	public int id;
	
	@Column(name = "user_id")
	public int userId = -1;
	
	@Column(name = "order_id")
	public String orderId;
	
	@Column(name = "pg_order_id")
	public String pGatewayOrderId;
	
	@Column(name = "payment_id")
	public String paymentId;
	
	@Column(name = "loan_account_number")
	public String loanAccountNumber;
	
	@Column(name = "loan_account_type")
	public String loanType;
	
	@Column(name = "payment_nature")
	public String paymentNature;
	
	@Column(name = "payment_sub_type")
	public String paymentSubType = Constants.NA;
	
	@Column(name = "emi_reduction")
	@ColumnDefault("0")
	public int emiReduction = 0;
	
	@Column(name = "tenure_reduction")
	@ColumnDefault("0")
	public int tenurReduction = 0;
	
	@Transient
	public String prePaymentType = Constants.NA;;
	
	@Column(name = "amount")
	public double paymentAmount = 0;
	
	@Column(name = "payment_method")
	public String paymentMethod;
	
	@Column(name = "device_type")
	public String deviceType;
	
	@Column(name = "receipt_data")
	public String receiptData;
	
	@Column(name = "receipt_id")
	public String receiptId;
	
	@Column(name = "receipt_number")
	public String receiptNumber;
	
	public String status;
	
	@Column(name = "razorpay_status")
	public String razarpayStatus;
	
	@Column(name = "status_message")
	public String statusMessage;
	
	@Column(name = "initial_datetime", columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String initialDateTime;
	
	@Column(name = "completion_datetime", columnDefinition = "DATETIME")
	public String completionDateTime;
	
	@Column(name = "update_datetime", columnDefinition = "DATETIME")
	public String updateDateTime = DateTimeUtils.getCurrentDateTimeInIST();
	
	@Transient
	public AmountVariables amountVariables = new AmountVariables();

	public Payment() {
	}

	public Payment(JSONObject object) {
		
		if (null != object) {
			id = object.optInt("id", -1);
			userId = object.optInt("userId", -1);
			orderId = object.optString("orderId", Constants.NA);
			pGatewayOrderId = object.optString("pGatewayOrderId", Constants.NA);
			paymentId = object.optString("paymentId", Constants.NA);
			loanAccountNumber = object.optString("loanAccountNumber", Constants.NA);
			loanType = object.optString("loanType", Constants.NA);
			paymentNature = object.optString("paymentNature", Constants.NA);
			paymentSubType = object.optString("paymentSubType", Constants.NA);
			prePaymentType = object.optString("prePaymentType", Constants.NA);
			paymentAmount = object.optDouble("paymentAmount", 0);
			paymentMethod = object.optString("paymentMethod", Constants.NA);
			deviceType = object.optString("deviceType", Constants.NA);
			initialDateTime = object.optString("initialDateTime", Constants.NA);
			completionDateTime = object.optString("completionDateTime", Constants.NA);
			receiptData = object.optString("receiptData", Constants.NA);
			receiptId = object.optString("receiptId", Constants.NA);
			receiptNumber = object.optString("receiptNumber", Constants.NA);
			status = object.optString("status", Constants.NA);
			statusMessage = object.optString("statusMessage", Constants.NA);
			amountVariables = new AmountVariables(object.optJSONObject("amountVariables"));
		}
	}

	public JSONObject toJson() throws JSONException {
		JSONObject object = new JSONObject();
		object.put("id", id);
		object.put("userId", userId);
		object.put("orderId", orderId);
		object.put("pGatewayOrderId", pGatewayOrderId);
		object.put("paymentId", paymentId);
		object.put("loanAccountNumber", loanAccountNumber);
		object.put("loanType", loanType);
		object.put("paymentNature", paymentNature);
		object.put("paymentSubType", paymentSubType);
		object.put("prePaymentType", prePaymentType);
		object.put("paymentAmount", paymentAmount);
		object.put("paymentMethod", paymentMethod);
		object.put("deviceType", deviceType);
		object.put("initialDateTime", initialDateTime);
		object.put("completionDateTime", completionDateTime);
		object.put("receiptData", receiptData);
		object.put("receiptId", receiptId);
		object.put("receiptNumber", receiptNumber);
		object.put("status", status);
		object.put("statusMessage", statusMessage);
		object.put("amountVariables", amountVariables.toJson());
		return object;
	}

	public boolean isSuccess() {
		return statusMessage.equalsIgnoreCase(Constants.SUCCESS);
	}

}
