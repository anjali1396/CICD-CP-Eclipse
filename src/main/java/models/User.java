package models;

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
@Table(name = "user", schema = "HomeFirstCustomerPortal")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "user_id")
	public int userId = -1;
	public String name;
	public String password;
//	@Column(name = "n_password")
//	public String nPassword;
	@Column(name = "email_id")
	public String emailId;
	@Column(name = "image_url")
	public String imageUrl;
	@Column(name = "mobile_number")
	public String mobileNumber;
	@Column(name = "country_code")
	public String countryCode = "+91";
	@Column(name = "registeration_datetime")
	public String registrationDateTime;
	@Column(name = "loan_account_number")
	public String loanAccountNumber;
	@Column(name = "crm_account_number")
	public String crmAccountNumber;
	@Column(name = "session_passcode")
	public String sessionPasscode;
	@Column(name = "mobile_verified", columnDefinition = "bit(1) default 0")
	public boolean isMobileNumberVerified = false;
	@Transient
	public String panNumber;
	@Transient
	public String aadharNumber;
	@Column(name = "is_registration_marked_on_sf", columnDefinition = "bit(1) default 0")
	public boolean isRegistrationMarkedOnSF = false;

	@ColumnDefault("0")
	@Column(name = "force_password_reset")
	public boolean forcePasswordReset = false;

	@Column(name = "is_deleted", columnDefinition = "bit(1) default 0")
	public boolean isDeleted = false;

	@Column(name = "update_datetime", columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(name = "delete_datetime", columnDefinition = "DATETIME")
	public String deleteDatetime = null;

	@Column(name = "has_consented", columnDefinition = "bit(1) default 0")
	public boolean hasConsented = false;

	@Column(name = "consent_datetime", columnDefinition = "DATETIME")
	public String consentDatetime = null;

	@ColumnDefault("0")
	@Column(name = "new_portal_optin")
	public boolean newPortalOptIn = false;

	@Column(name = "new_portal_optin_datetime", columnDefinition = "DATETIME")
	public String newPortalOptInDatetime = null;

	@Column(name = "is_loan_active", columnDefinition = "bit(1) default 1")
	public boolean isLoanActive = true;
	
	@ColumnDefault("4")
	@Column(name = "loginAttempt")
	public int loginAttempt = 4;
	
	@Column(name = "lastLoginDatetime", columnDefinition = "DATETIME")
	public String lastLoginDatetime = null;
	
	@Column(name = "loginAttemptDateTime", columnDefinition = "DATETIME")
	public String loginAttemptDateTime = null;
	
	@Column(name = "passwordDateTime", columnDefinition = "DATETIME")
	public String passwordDateTime = DateTimeUtils.getCurrentDateTimeInIST();
	
	public User() {
	}

	public User(JSONObject jsonObject) throws JSONException {
		userId = jsonObject.optInt("userId", -1);
		name = jsonObject.optString("name", Constants.NA);
		password = jsonObject.optString("password", Constants.NA);
		emailId = jsonObject.optString("emailId", Constants.NA);
		imageUrl = jsonObject.optString("imageUrl", Constants.NA);
		mobileNumber = jsonObject.optString("mobileNumber", Constants.NA);
		countryCode = jsonObject.optString("countryCode", "+91");
		registrationDateTime = jsonObject.optString("datetime", Constants.NA);
		loanAccountNumber = jsonObject.optString("loanAccountNumber", Constants.NA);
		crmAccountNumber = jsonObject.optString("crmAccountNumber", Constants.NA);
		sessionPasscode = jsonObject.optString("sessionPasscode", Constants.NA);
		isMobileNumberVerified = jsonObject.optBoolean("isMobileNumberVerrified", false);
		newPortalOptIn = jsonObject.optBoolean("newPortalOptIn", false);
		newPortalOptInDatetime = jsonObject.optString("newPortalOptInDatetime", null);
		lastLoginDatetime = jsonObject.optString("lastLoginDatetime", null);
	}

	public JSONObject toJson(boolean withPasscode) throws JSONException {
		JSONObject userObject = new JSONObject();
		userObject.put("userId", userId);
		userObject.put("name", name);
		userObject.put("emailId", emailId);
		userObject.put("imageUrl", imageUrl);
		userObject.put("mobileNumber", mobileNumber);
		userObject.put("countryCode", countryCode);
		userObject.put("loanAccountNumber", loanAccountNumber);
		userObject.put("crmAccountNumber", crmAccountNumber);
		userObject.put("isMobileNumberVerified", isMobileNumberVerified);
		userObject.put("registrationDateTime", registrationDateTime);
		userObject.put("newPortalOptIn", newPortalOptIn);
		userObject.put("newPortalOptInDatetime", newPortalOptInDatetime);
		userObject.put("lastLoginDatetime", lastLoginDatetime);

		if (withPasscode)
			userObject.put("sessionPasscode", sessionPasscode);
		return userObject;
	}
}