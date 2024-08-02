package models;

import org.json.JSONObject;

import utils.Constants;

public class SFContact {

	public String id = Constants.NA;
    public String accountId = Constants.NA;
    public String salutation = Constants.NA;
    public String firstName = Constants.NA;
    public String lastName = Constants.NA;
    public String fullName = Constants.NA;
    public String gender = Constants.NA;
    public String dob = Constants.NA;
    public String email = Constants.NA;
    public String phone = Constants.NA;
    public String mobilePhone = Constants.NA;
    public String panCardNumber = Constants.NA;
    public String aadharCardNumber = Constants.NA;
    public String passportNumber = Constants.NA;
    public String drivingLicenseNumber = Constants.NA;
    public String voterIdNumber = Constants.NA;
    public String rationNumber = Constants.NA;
    public String leadSource = Constants.NA;
    //public String mailingAddress : SFAddress = SFAddress(),
    //public String otherAddress : SFAddress = SFAddress(),
    public double loanAmount = 0;
    public String loanType = "Home Loan";
    public String religion = Constants.NA;
    public String createdDate = Constants.NA;
    
    public SFContact(JSONObject json) {        
    	
    	id = json.optString("Id", id);
        accountId = json.optString("AccountId", accountId);
        salutation = json.optString("Salutation", salutation);
        firstName = json.optString("FirstName", firstName);
        lastName = json.optString("LastName", lastName);
        fullName = json.optString("Name", fullName);
        gender = json.optString("Gender__c", gender);
        dob = json.optString("Date_of_Birth__c", dob);
        email = json.optString("Email", email);
        phone = json.optString("Phone", phone);
        mobilePhone = json.optString("MobilePhone", mobilePhone);
        panCardNumber = json.optString("ID_Proof_1_PAN_Card__c", panCardNumber);
        aadharCardNumber = json.optString("ID_Proof_2_Aadhar_Card__c", aadharCardNumber);
        passportNumber = json.optString("ID_Proof_3_Passport_Number__c", passportNumber);
        drivingLicenseNumber = json.optString("ID_Proof_4_Driving_License__c", drivingLicenseNumber);
        voterIdNumber = json.optString("ID_Proof_5_Voter_ID__c", voterIdNumber);
        rationNumber = json.optString("Ration_Card_No__c", rationNumber);
        leadSource = json.optString("LeadSource", leadSource);
        
//        json.optJSONObject("MailingAddress")?.let {
//            mailingAddress = SFAddress(it)
//        }
//        json.optJSONObject("OtherAddress")?.let {
//            otherAddress = SFAddress(it)
//        }
        
        loanAmount = json.optDouble("Loan_Amount__c", loanAmount);
        loanType = json.optString("Loan_Type__c", loanType);
        religion = json.optString("Religion__c", religion);
        createdDate = json.optString("CreatedDate", createdDate);
    	
    }
}
