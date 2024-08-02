package models.payment;

import org.json.JSONObject;

import utils.Constants;

public class SFAccountStatement {

	public String id = Constants.NA;
	public String name = Constants.NA;
	public String loanAccountId = Constants.NA;
	public String loanAccountNumber = Constants.NA;
	public double transactionAmount = 0;
	public double principalAmount = 0;
	public double interestAmount = 0;
	public double balanceAmount = 0;
	public String transactionDate = Constants.NA;
	public String paymentMode = Constants.NA;

	public SFAccountStatement() {
	}

	public SFAccountStatement(JSONObject json) {

		id = json.optString("Id", id);
		name = json.optString("Name", name);
		loanAccountId = json.optString("loan__Loan_Account__c", loanAccountId);

		if (null != json.optJSONObject("loan__Loan_Account__r"))
			loanAccountNumber = json.optJSONObject("loan__Loan_Account__r").optString("Name", loanAccountNumber);

		transactionAmount = json.optDouble("loan__Transaction_Amount__c", transactionAmount);
		principalAmount = json.optDouble("loan__Principal__c", principalAmount);
		interestAmount = json.optDouble("loan__Interest__c", interestAmount);
		balanceAmount = json.optDouble("loan__Balance__c", balanceAmount);
		transactionDate = json.optString("loan__Transaction_Date__c", transactionDate);

		if (null != json.optJSONObject("loan__Payment_Mode__r"))
			paymentMode = json.optJSONObject("loan__Payment_Mode__r").optString("Name", loanAccountNumber);

	}

	public JSONObject toJson() {

		JSONObject json = new JSONObject();

		json.put("id", id);
		json.put("name", name);
		json.put("loanAccountId", loanAccountId);
		json.put("loanAccountNumber", loanAccountNumber);
		json.put("transactionAmount", transactionAmount);
		json.put("principalAmount", principalAmount);
		json.put("interestAmount", interestAmount);
		json.put("balanceAmount", balanceAmount);
		json.put("transactionDate", transactionDate);
		json.put("paymentMode", paymentMode);

		return json;

	}

}
