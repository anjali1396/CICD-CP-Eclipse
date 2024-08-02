package models;

import org.json.JSONObject;

import utils.Constants;

public class CLContract {

	// Name , CRM_Account_Number__c,loan__Principal_Remaining__c ,
	// loan__Loan_Product_Name__c
	public String id = Constants.NA;
	public String name = Constants.NA;
	public String crmAccountNumber = Constants.NA;
	public String principalRemaining = Constants.NA;
	public String product = Constants.NA;

	public CLContract(JSONObject json) {

		id = json.optString("Id", id);
		name = json.optString("Name", name);
		crmAccountNumber = json.optString("CRM_Account_Number__c", crmAccountNumber);
		principalRemaining = json.optString("loan__Principal_Remaining__c", principalRemaining);
		product = json.optString("loan__Loan_Product_Name__c", product);

	}
}
