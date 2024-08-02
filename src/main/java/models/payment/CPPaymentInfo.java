package models.payment;

import org.json.JSONObject;

import utils.Constants;

public class CPPaymentInfo {

	public double loanAmount = 0;
	public double insuranceLoanAmount = 0;
	public double totalDebitAmount = 0;
	public double collectedAmount = 0;
	public double bounceAmount = 590;
	public double excessShortfallAmount = 0;
	public String dueDate = Constants.NA;		
	public String collectionMonthYear = Constants.NA;
	public String paymentClearedDate = Constants.NA;
	public String loanAccountNumber = Constants.NA;
	public String clearanceStatus = Constants.NA;
	public boolean isBounced = false;
	
	public JSONObject toJson() {
		
		JSONObject json = new JSONObject();
		
		json.put("loanAmount", loanAmount);
		json.put("insuranceLoanAmount", insuranceLoanAmount);
		json.put("totalDebitAmount", totalDebitAmount);
		json.put("collectedAmount", collectedAmount);
		json.put("bounceAmount", bounceAmount);
		json.put("excessShortfallAmount", excessShortfallAmount);
		json.put("dueDate", dueDate);		
		json.put("collectionMonthYear", collectionMonthYear);
		json.put("paymentClearedDate", paymentClearedDate);
		json.put("loanAccountNumber", loanAccountNumber);
		json.put("clearanceStatus", clearanceStatus);
		json.put("isBounced", isBounced);
		
		return json;
		
	}
	
}
