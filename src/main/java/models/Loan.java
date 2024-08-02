package models;

import org.json.JSONException;
import org.json.JSONObject;

import utils.Constants;

public class Loan {

	public String sfId = Constants.NA;
	public String accountNumber = Constants.NA;
	public String loanType = Constants.NA;
	public double principalAmount = 0;
	public double loanAmount = 0;
	public double emiAmount = 0;
	public double debitAmount = 0;	
	public double insuranceDebitAmount = 0;
	public double totalDebitAmount = 0;
	public double totalInsuredAmount = 0;
	public String paymentType = Constants.NA;
	public double interestRate = 0;
	public double disbursedAmount = 0;
	public double lastFYPOS = 0; 
	

	public int numberOfInstallments = 0;
	public String status = Constants.NA;
	
	public String propertyId = Constants.NA;
	public String propertyName = Constants.NA;
	public String propertyCode = Constants.NA;
	public String propertyGeo = Constants.NA;
	public String propertyOwner = Constants.NA;
	public String propertyAddress = Constants.NA;
	public String propertyOwnerEmail = Constants.NA;
	
	public String outstandingStatus = Constants.NA;
	public String disbursalStatus = Constants.NA;

	public double totalTenure = 0;
	public double remainingTenure = 0;	
	
	public Loan() {}
	
	public JSONObject getSummaryJson() throws JSONException {
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("accountNumber", accountNumber);
		jsonObject.put("loanType", loanType);
		jsonObject.put("principalAmount", principalAmount);
		jsonObject.put("lastFYPOS", lastFYPOS);
		jsonObject.put("debitAmount", debitAmount);
		jsonObject.put("insuranceDebitAmount", insuranceDebitAmount);
		jsonObject.put("totalDebitAmount", totalDebitAmount);
		jsonObject.put("totalInsuredAmount", totalInsuredAmount);
		jsonObject.put("status", status);
		jsonObject.put("propertyId", propertyId);
		jsonObject.put("propertyName", propertyName);
		jsonObject.put("propertyGeo", propertyGeo);
		jsonObject.put("propertyCode", propertyCode);
		jsonObject.put("propertyOwner", propertyOwner);
		jsonObject.put("propertyAddress", propertyAddress);
		jsonObject.put("propertyOwnerEmail", propertyOwnerEmail);
		jsonObject.put("loanAmount", loanAmount);
		jsonObject.put("outstandingStatus", outstandingStatus);
		jsonObject.put("disbursalStatus", disbursalStatus);
		
		return jsonObject;
		
	}
	
	public JSONObject getNameIdJson() throws JSONException {
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("accountNumber", accountNumber);
		jsonObject.put("loanType", loanType);
		
		return jsonObject;
		
	}
	public JSONObject getPropertInfoJson() throws JSONException {
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("accountNumber", accountNumber);
		jsonObject.put("loanType", loanType);
		jsonObject.put("propertyId", propertyId);
		jsonObject.put("propertyName", propertyName);
		jsonObject.put("propertyGeo", propertyGeo);
		jsonObject.put("propertyCode", propertyCode);
		jsonObject.put("propertyOwner", propertyOwner);
		jsonObject.put("propertyAddress", propertyAddress);
		jsonObject.put("propertyOwnerEmail", propertyOwnerEmail);
		
		return jsonObject;
		
	}
	
	public JSONObject getCompleteJson() throws JSONException {
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("accountNumber", accountNumber);
		jsonObject.put("loanType", loanType);
		jsonObject.put("principalAmount", principalAmount);
		jsonObject.put("loanAmount", loanAmount);
		jsonObject.put("debitAmount", debitAmount);
		jsonObject.put("emiAmount", emiAmount);		
		jsonObject.put("insuranceDebitAmount", insuranceDebitAmount);
		jsonObject.put("totalDebitAmount", totalDebitAmount);
		jsonObject.put("totalInsuredAmount", totalInsuredAmount);
		jsonObject.put("paymentType", paymentType);
		jsonObject.put("interestRate", interestRate);
		jsonObject.put("disbursedAmount", disbursedAmount);
		jsonObject.put("totalTenure", totalTenure);
		jsonObject.put("remainingTenure", remainingTenure);
		jsonObject.put("numberOfInstallments", numberOfInstallments);
		jsonObject.put("status", status);
		jsonObject.put("propertyId", propertyId);
		jsonObject.put("propertyName", propertyName);
		jsonObject.put("propertyGeo", propertyGeo);
		jsonObject.put("propertyCode", propertyCode);
		jsonObject.put("propertyOwner", propertyOwner);
		jsonObject.put("propertyAddress", propertyAddress);
		jsonObject.put("propertyOwnerEmail", propertyOwnerEmail);
		
		return jsonObject;
		
	}
	
	
	
}
