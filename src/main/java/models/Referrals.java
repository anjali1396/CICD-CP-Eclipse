package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.json.JSONObject;

import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;

@Entity
@Table(name = "`referrals`")
public class Referrals { 
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	public int id = -1;
	
	@Column(name = "user_id")
	public int userId;
	
	@Column(name = "first_name")
	public String firstName;

	@Column(name = "last_name")
	public String lastName;
	
	@Column(name = "middle_name")
	public String middleName;

	@Column(name = "salutation")
	public String salutation;
	
	@Column(name = "mobile_number")
	public String mobileNumber;
	
	@Column(name = "email_id")
	public String emailId;
	
	@Column(name = "status")
	public String status;
	
//	public String leadDate;
//	public String company;
	@Column(name = "sf_lead_id")
	public String sfLeadId;

	@Transient
	public String sfLeadSource;
	
	@Transient
	public String referredById;
	
	@Transient
	public String referredByName;
	
	@Transient
	public String referralType;

    @Column(columnDefinition = "JSON" ,name = "address")
	public String addressJson ;
    
    @Transient
	public Address address ;

	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String datetime  = DateTimeUtils.getCurrentDateTimeInIST();
	
	
	public JSONObject getLMSRequestJson() {


		final var json = new JSONObject();

		json.put("userId", userId);
		json.put("emailId", emailId);
		json.put("firstName", firstName);

		if (BasicUtils.isNotNullOrNA(middleName))
			json.put("middleName", middleName);

		json.put("lastName", lastName);
		json.put("mobileNumber", mobileNumber);
		json.put("sfLeadSource", Constants.REFFERAL_LEAD_SOURCE);
		json.put("status", status);
		
		if(null != address && address.isValid())
		json.put("address", address.toJson());
		
		json.put("referralType", "Customer");

		if (BasicUtils.isNotNullOrNA(sfLeadId))
			json.put("sfLeadId", sfLeadId);
		
		if (BasicUtils.isNotNullOrNA(referredById))
			json.put("referredById", referredById);
		
		if (BasicUtils.isNotNullOrNA(referredByName))
			json.put("referredByName", referredByName);
		
		
		return json;
		
		}
	
	public String getFullName() {
		String fullName = firstName;
		
		if (BasicUtils.isNotNullOrNA(middleName))
			fullName += " " + middleName;

		if (BasicUtils.isNotNullOrNA(lastName))
		fullName += " " + lastName;
		
		return fullName;
	}
	
}