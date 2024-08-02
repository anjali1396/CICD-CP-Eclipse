package models.sob;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import dao.ProspectRepository;
import models.Address;
import utils.BasicUtils;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.ProptertyUtils;
import utils.Constants.Actions;
import utils.Constants.Errors;
import utils.Constants.ProductTypeMap;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;

@Entity
@Table(name = "`Prospect`")
public class Prospect {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id = null;
	public String fullName = null;
	public String firstName = null;
	public String middleName = null;
	public String lastName = null;

	public String leadSource = null;
	public String source = null;
	public String medium = null;
	public String campaign = null;
	public String platform = null;
	
	@Column(updatable = false)
	public String promoCode = null;

	@Column(length = 10, unique = true, nullable = false)
	public String mobileNumber = null;

	@Column(length = 10)
	public String phoneNumber;

	@ColumnDefault("0")
	public boolean isMobileVerified = false;

	@ColumnDefault("0")
	public boolean hasConsented = false;

	@ColumnDefault("0")
	public boolean isConverted = false;

	public String dob = null;
	public String gender = null;
	public String maritalStatus = null;

	public String pan = null;
	public String aadhaar = null;
	public String gstin = null;
	public String personalEmail = null;
	public String residenseStatus = null;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "currentAddress", referencedColumnName = "id")
	public Address currentAddress = null;

	public String loanRequirment = null;

	@ColumnDefault("-1")
	public double loanAmount = -1;

	@ColumnDefault("-1")
	public double existingLoanOutstanding = -1;

	@ColumnDefault("0")
	public boolean topUpRequired = false;

	public String profession = null;
	public String officialEmail = null;

	@ColumnDefault("-1")
	public double yearOfWorkExperience = -1;

	public String organizationName = null;
	public String designation = null;

	@ColumnDefault("0")
	public boolean itrFiled = false;

	@ColumnDefault("-1")
	public double monthlyIncome = -1;

	@ColumnDefault("-1")
	public double monthlyIncomeInCash = -1;

	@ColumnDefault("-1")
	public double existingEMIAmount = -1;

	@ColumnDefault("0")
	public boolean propertyIdentified = false;
	

	@ColumnDefault("0")
	public boolean municipalProperty = false;

	@ColumnDefault("-1")
	public double emiAmount = -1;

	@ColumnDefault("-1")
	public double LTV = -1;

	@ColumnDefault("-1")
	public double interestRate = -1;

	@ColumnDefault("-1")
	public int totalTenure = -1;
	
	@ColumnDefault("-1")
	public int stageProgress = -1;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "propertyAddress", referencedColumnName = "id")
	public Address propertyAddress = null;

	@ColumnDefault("-1")
	public double propertyValue = -1;

	public String sessionPasscode = null;

	@Column(columnDefinition = "DATETIME")
	public String sessionUpdateDatetime = null;

	@Column(columnDefinition = "DATETIME")
	public String sessionValidDatetime = null;

	public String hfoLeadId = null;
	public String hfoAccountId = null;
	public String hfoLoanId = null;
	public String hfoApplicantId = null;
	public String hfoOpportunityId = null;

	public String lmsContactId = null;
	public String lmsApplicantId = null;

	public String referenceId = null;

	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
	
	@Column(columnDefinition = "DATETIME")
	public String consentDatetime = null;
	
	

	public Prospect() {
	}

	public Prospect initFromLMS(JSONObject json) {

		hfoLeadId = json.optString("id");

		firstName = json.optString("firstName");
		middleName = json.optString("middleName", Constants.NA);
		lastName = json.optString("lastName");
		promoCode = json.optString("promoCode", Constants.NA);

		
		
		setFullName();
		
		mobileNumber = json.optString("mobileNumber");
		loanAmount = json.optDouble("loanAmount");
		existingLoanOutstanding = json.optDouble("existingLoanOutstanding");		
		loanRequirment = json.optString("loanType");
		isMobileVerified = json.optBoolean("mobileVerified", false);
		hasConsented = json.optBoolean("hasConsented", false);
		consentDatetime = json.optString("consentDatetime", consentDatetime);
		existingEMIAmount = json.optDouble("existingLoanEMI", -1);
		source = json.optString("source", source);
		medium = json.optString("medium", medium);
		campaign = json.optString("campaign", campaign);
		leadSource = json.optString("sfLeadSource", leadSource);
		isConverted = json.optBoolean("isConverted", false);
		personalEmail = json.optString("emailId", personalEmail);
		propertyIdentified = json.optBoolean("propertyIdentified", propertyIdentified);
		totalTenure = json.optInt("tenure", totalTenure);
		monthlyIncome = json.optDouble("monthlyIncome", monthlyIncome);
		propertyValue = json.optDouble("propertyValue", propertyValue);
		itrFiled = json.optBoolean("itrAvailable", false);
		profession = json.optString("employmentType", profession);
		municipalProperty = json.optBoolean("municipalProperty", municipalProperty);
		topUpRequired = json.optBoolean("topUpRequired", topUpRequired);
		referenceId = json.optString("partnerReferenceId", referenceId);

		if (null != json.optJSONObject("propertyAddress") && !json.optJSONObject("propertyAddress").isEmpty()) {

			var propertyAdd = json.optJSONObject("propertyAddress");

			if (null != propertyAdd && !propertyAdd.isEmpty()) {
				propertyAddress = new Address(propertyAdd);
			}

		}

		if (json.has("address")) {
			final var currentAdd = json.optJSONObject("address");
			if (null != currentAdd && !currentAdd.isEmpty())
				currentAddress = new Address(currentAdd);
		}

		final var aplicantJson = new JSONArray(json.optString("applicants"));

		for (int i = 0; i < aplicantJson.length();) {

			final var appObj = aplicantJson.getJSONObject(i);

			if (appObj.optString("type").equals(Constants.PRIMARY)) {

				lmsApplicantId = appObj.optString("id");

				final var contactJson = new JSONObject(appObj.optString("contact"));

				lmsContactId = contactJson.optString("id");
				gender = contactJson.optString("gender");
				maritalStatus = contactJson.optString("maritalStatus", maritalStatus);
				dob = contactJson.optString("dob", dob);
				gstin = contactJson.optString("gstin", gstin);
				aadhaar = contactJson.optString("aadhaar", aadhaar);
				pan = contactJson.optString("pan", pan);
				officialEmail = contactJson.optString("officialEmail", officialEmail);
				personalEmail = contactJson.optString("personalEmail", personalEmail);
				organizationName = contactJson.optString("organization", organizationName);
				designation = contactJson.optString("designation", designation);
				yearOfWorkExperience = contactJson.optDouble("yearOfWorkExperience", yearOfWorkExperience);
				itrFiled = contactJson.optBoolean("itrAvailable", false);
				monthlyIncome = contactJson.optDouble("monthlyIncome", -1);
				monthlyIncomeInCash = contactJson.optDouble("monthlyIncomeInCash", -1);

				if (contactJson.has("currentAddress")) {
					final var currentAdd = contactJson.optJSONObject("currentAddress");
					if (null != currentAdd && !currentAdd.isEmpty())
						currentAddress = new Address(currentAdd);
				}

			}

			break;

		}

		lockDocs();

		return this;

	}

	public LocalResponse allMandatoryFieldsPresent() {

		LocalResponse lResponse = new LocalResponse();
		lResponse.isSuccess = false;
		lResponse.error = Errors.INVALID_DATA.value;
		lResponse.action = Actions.FIX_RETRY.value;

		if (!BasicUtils.isNotNullOrNA(fullName)) {
			lResponse.message = "fullName is mandatory.";
			return lResponse;
		}

		if (!BasicUtils.isNotNullOrNA(profession)) {
			lResponse.message = "profession is mandatory.";
			return lResponse;
		}

		if (!BasicUtils.isNotNullOrNA(loanRequirment)) {
			lResponse.message = "loanRequirment is mandatory.";
			return lResponse;
		}

		if (!BasicUtils.isNotNullOrNA(mobileNumber)) {
			lResponse.message = "mobileNumber is mandatory.";
			return lResponse;
		}

		if (mobileNumber.length() != 10) {
			lResponse.message = "mobileNumber length has to be 10.";
			return lResponse;
		}

		if (!BasicUtils.isNotNullOrNA(dob)) {
			lResponse.message = "dob is mandatory.";
			return lResponse;
		}

		lResponse.isSuccess = true;
		lResponse.message = Constants.NA;
		lResponse.error = Constants.NA;
		lResponse.action = Constants.NA;

		return lResponse;

	}

	public boolean isSessionValid() throws ParseException {

		if (BasicUtils.isNotNullOrNA(sessionPasscode) && BasicUtils.isNotNullOrNA(sessionValidDatetime)) {

			Date currentDatetime = DateTimeUtils.getDateFromDateTimeString(DateTimeUtils.getCurrentDateTimeInIST(),
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss);

			Date sessionValidDate = DateTimeUtils.getDateFromDateTimeString(sessionValidDatetime,
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss);

			return currentDatetime.before(sessionValidDate);

		} else
			return false;

	}

	public boolean updateSession(boolean shouldCreateNew) {

		try {

			if (shouldCreateNew) {
				sessionPasscode = ProptertyUtils.getKeyBearer().encrypt(mobileNumber + BasicUtils.getRandomKey() + id);
			} else {

				int minutesLeft = DateTimeUtils.getDateDifferenceInMinutes(sessionValidDatetime);

				if (minutesLeft > 15) {
					LoggerUtils.log("Prospect - minutes left in session validity : " + minutesLeft
							+ " | Continuing with the same session.");
					return true;
				}

			}

			sessionUpdateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			sessionValidDatetime = DateTimeUtils.getDateTimeAddingHours(1, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
					DateTimeZone.IST);

			boolean status = new ProspectRepository().addUpdateProspect(this);

			if (status)
				LoggerUtils.log("Session updated successfully.");
			else
				LoggerUtils.log("Failed to update session.");

			return status;

		} catch (Exception e) {

			LoggerUtils.log("Error while updating prospect's session: " + e.getMessage());
			e.printStackTrace();
			return false;

		}

	}

	public void processName() {

		String[] name = fullName.trim().replace("   ", " ").replace("  ", " ").split(" ");

		int size = name.length;

		if (size > 2)
			middleName = "";

		for (int i = 0; i < size; i++) {

			if (i == 0)
				firstName = name[i];
			else if (i == size - 1)
				lastName = name[i];
			else {
				if (!middleName.isEmpty())
					middleName += " ";
				middleName += name[i];
			}

		}

	}
	
	public void setFullName() {

        final var sb = new StringBuilder();
        
        sb.append(firstName + " ");
        if (BasicUtils.isNotNullOrNA(middleName))
            sb.append(middleName + " ");
        sb.append(lastName);

        fullName = sb.toString();

    }

	public JSONObject getLeadRequestJson() {

		JSONObject json = new JSONObject();

		json.put("firstName", firstName);

		if (BasicUtils.isNotNullOrNA(middleName))
			json.put("middleName", middleName);

		json.put("lastName", lastName);
		json.put("mobileNumber", mobileNumber);
		json.put("loanAmount", loanAmount);
		json.put("existingLoanOutstanding", existingLoanOutstanding);

		json.put("loanType", loanRequirment);
		json.put("dob", dob);
		json.put("mobileVerified", isMobileVerified);
		json.put("hasConsented", hasConsented);
		json.put("consentDatetime", consentDatetime);

		if (BasicUtils.isNotNullOrNA(source))
			json.put("source", source);

		if (BasicUtils.isNotNullOrNA(medium))
			json.put("medium", medium);

		if (BasicUtils.isNotNullOrNA(campaign))
			json.put("campaign", campaign);

		if (BasicUtils.isNotNullOrNA(leadSource))
			json.put("sfLeadSource", leadSource);

		if (BasicUtils.isNotNullOrNA(referenceId))
			json.put("partnerReferenceId", referenceId);
		
		if (BasicUtils.isNotNullOrNA(promoCode))
			json.put("promoCode", promoCode);

		return json;

	}

	public JSONObject getLMSRequestJson(boolean withApplicant) {

		unlockDocs();

		final var json = new JSONObject();

		json.put("firstName", firstName);

		if (BasicUtils.isNotNullOrNA(middleName))
			json.put("middleName", middleName);

		json.put("lastName", lastName);
		json.put("mobileNumber", mobileNumber);
		json.put("loanAmount", loanAmount);
		json.put("existingLoanOutstanding", existingLoanOutstanding);
		json.put("loanType", loanRequirment);
		json.put("dob", dob);
		json.put("mobileVerified", isMobileVerified);
		json.put("hasConsented", hasConsented);
		json.put("consentDatetime", consentDatetime);
		json.put("stageProgress", stageProgress);
		
		
		if (BasicUtils.isNotNullOrNA(hfoLeadId))
			json.put("id", hfoLeadId);

		if (BasicUtils.isNotNullOrNA(source))
			json.put("source", source);

		if (BasicUtils.isNotNullOrNA(medium))
			json.put("medium", medium);

		if (BasicUtils.isNotNullOrNA(campaign))
			json.put("campaign", campaign);

		if (BasicUtils.isNotNullOrNA(leadSource))
			json.put("sfLeadSource", leadSource);

		json.put("itrAvailable", itrFiled);

		if (BasicUtils.isNotNullOrNA(personalEmail))
			json.put("emailId", personalEmail);

		if (BasicUtils.isNotNullOrNA(gender))
			json.put("gender", gender);

		json.put("propertyIdentified", propertyIdentified);
		
		json.put("municipalProperty", municipalProperty);
		json.put("topUpRequired", topUpRequired);

		


		if (totalTenure > 0)
			json.put("tenure", totalTenure);

		if (monthlyIncome > 0)
			json.put("monthlyIncome", monthlyIncome);

		if (propertyValue > 0)
			json.put("propertyValue", propertyValue);

		if (BasicUtils.isNotNullOrNA(officialEmail))
			json.put("officialEmail", officialEmail);

		if (null != currentAddress)
			json.put("address", currentAddress.toJson());

		if (null != propertyAddress)
			json.put("propertyAddress", propertyAddress.toJson());

		if (BasicUtils.isNotNullOrNA(profession))
			json.put("employmentType", profession);
		
		if(BasicUtils.isNotNullOrNA(profession)) {
			
			if (monthlyIncome > 0)
				json.put("salariedType", "Bank");
			else json.put("salariedType", "Cash"); 
			
		}

		if (BasicUtils.isNotNullOrNA(organizationName))
			json.put("organization", organizationName);

		if (existingEMIAmount > 0) {
			json.put("anyExistingLoan", true);
			json.put("existingLoanEMI", existingEMIAmount);
		}

		if (BasicUtils.isNotNullOrNA(referenceId))
			json.put("partnerReferenceId", referenceId);
		
		if (BasicUtils.isNotNullOrNA(promoCode))
			json.put("promoCode", promoCode);

		if (withApplicant) {

			final var applicantJson = new JSONObject();

			applicantJson.put("id", lmsApplicantId);
			applicantJson.put("lead", new JSONObject().put("id", hfoLeadId));
			applicantJson.put("type", "Primary");

			final var contactJson = new JSONObject();

			contactJson.put("id", lmsContactId);
			
			if (BasicUtils.isNotNullOrNA(gender))
				contactJson.put("gender", gender);

			if (BasicUtils.isNotNullOrNA(maritalStatus))
				contactJson.put("maritalStatus", maritalStatus);

			if (BasicUtils.isNotNullOrNA(aadhaar))
				contactJson.put("aadhaar", aadhaar);

			if (BasicUtils.isNotNullOrNA(pan))
				contactJson.put("pan", pan);

			contactJson.put("firstName", firstName);

			if (BasicUtils.isNotNullOrNA(middleName))
				contactJson.put("middleName", middleName);

			contactJson.put("lastName", lastName);

			if (BasicUtils.isNotNullOrNA(dob))
				contactJson.put("dob", dob);

			contactJson.put("mobileNumber", mobileNumber);
			contactJson.put("mobileVerified", isMobileVerified);

			if (BasicUtils.isNotNullOrNA(personalEmail))
				contactJson.put("personalEmail", personalEmail);

			if (BasicUtils.isNotNullOrNA(officialEmail))
				contactJson.put("officialEmail", officialEmail);

			if (null != currentAddress)
				contactJson.put("currentAddress", currentAddress.toJson());
			

			if (BasicUtils.isNotNullOrNA(profession))
				contactJson.put("employmentType", profession);

			if(BasicUtils.isNotNullOrNA(profession)) {
				
				if (monthlyIncome > 0)
					contactJson.put("salariedType", "Bank");
				else contactJson.put("salariedType", "Cash"); 
				
			}		

			if (BasicUtils.isNotNullOrNA(organizationName))
				contactJson.put("organization", organizationName);

			if (BasicUtils.isNotNullOrNA(designation))
				contactJson.put("designation", designation);

			if (BasicUtils.isNotNullOrNA(gstin))
				contactJson.put("gstin", gstin);

			if (BasicUtils.isNotNullOrNA(monthlyIncome))
				contactJson.put("monthlyIncome", monthlyIncome);

			if (BasicUtils.isNotNullOrNA(monthlyIncomeInCash))
				contactJson.put("monthlyIncomeInCash", monthlyIncomeInCash);

			contactJson.put("itrAvailable", itrFiled);
			
			if (yearOfWorkExperience > 0)
				contactJson.put("yearOfWorkExperience", yearOfWorkExperience);

			applicantJson.put("contact", contactJson);

			json.put("applicants", new JSONArray().put(applicantJson));

		}

		return json;

	}

	public void addFields(JSONObject json) throws Exception {
		addBasicDetails(json);
		addProfessionalFields(json);
		addLoanFields(json);
		addPropertyFields(json);
		addPersonalFields(json);
		lockDocs();
	}
	
	public void addBasicDetails(JSONObject json) {

		if (null == json)
			return;
		
		if (json.has("fullName")) {
			fullName = json.getString("fullName");
			processName();
		}			
		
		if (json.has("profession"))
			profession = json.getString("profession");
		
		if (json.has("loanRequirment"))
			loanRequirment = json.getString("loanRequirment");
		
		if (json.has("dob"))
			dob = json.getString("dob");
		
		if (json.has("platform"))
			platform = json.getString("platform");
		
		
	}

	public void addProfessionalFields(JSONObject json) {

		if (null == json)
			return;

		if (json.has("yearOfWorkExperience"))
			yearOfWorkExperience = json.getDouble("yearOfWorkExperience");

		if (json.has("organizationName"))
			organizationName = json.getString("organizationName");

		if (json.has("designation"))
			designation = json.getString("designation");

		if (json.has("profession"))
			profession = json.getString("profession");

		if (json.has("monthlyIncome"))
			monthlyIncome = json.getDouble("monthlyIncome");

		if (json.has("monthlyIncomeInCash"))
			monthlyIncomeInCash = json.getDouble("monthlyIncomeInCash");

		if (json.has("itrFiled"))
			itrFiled = json.getBoolean("itrFiled");

	}

	public void addLoanFields(JSONObject json) {

		if (null == json)
			return;

		if (json.has("existingEMIAmount"))
			existingEMIAmount = json.getDouble("existingEMIAmount");

		if (json.has("existingLoanOutstanding"))
			existingLoanOutstanding = json.getDouble("existingLoanOutstanding");

	}

	public void addPropertyFields(JSONObject json) {

		if (null == json)
			return;

		if (json.has("propertyIdentified"))
			propertyIdentified = json.getBoolean("propertyIdentified");
		
		if (json.has("municipalProperty"))
			municipalProperty = json.getBoolean("municipalProperty");
		

		if (json.has("residenseStatus"))
			residenseStatus = json.getString("residenseStatus");

		if (json.has("propertyAddress")) {

			Address address = new Gson().fromJson(json.getJSONObject("propertyAddress").toString(), Address.class);
			if (null == propertyAddress)
				propertyAddress = address;
			else
				propertyAddress.updateInfo(address);

		}

		if (json.has("propertyValue"))
			propertyValue = json.getDouble("propertyValue");
		
		if (json.has("topUpRequired"))
			topUpRequired = json.getBoolean("topUpRequired");

	}

	public void addPersonalFields(JSONObject json) throws Exception {

		if (null == json)
			return;

		if (json.has("dob"))
			dob = json.getString("dob");

		if (json.has("gender"))
			gender = json.getString("gender");

		if (json.has("personalEmail"))
			personalEmail = json.getString("personalEmail");

		if (json.has("officialEmail"))
			officialEmail = json.getString("officialEmail");

		if (json.has("phoneNumber"))
			phoneNumber = json.getString("phoneNumber");

		if (json.has("maritalStatus"))
			maritalStatus = json.getString("maritalStatus");

		if (json.has("currentAddress")) {

			Address address = new Gson().fromJson(json.getJSONObject("currentAddress").toString(), Address.class);
			if (null == currentAddress)
				currentAddress = address;
			else
				currentAddress.updateInfo(address);

		}

		if (json.has("pan")) {
			String number = json.getString("pan");
			if (BasicUtils.isNotNullOrNA(number) && number.length() == 10)
				pan = ProptertyUtils.getKeyBearer().encrypt(number);
		}

		if (json.has("aadhaar")) {
			String number = json.getString("aadhaar");
			if (BasicUtils.isNotNullOrNA(number) && number.length() == 12)
				aadhaar = ProptertyUtils.getKeyBearer().encrypt(number);
		}

		if (json.has("gstin")) {
			String number = json.getString("gstin");
			if (BasicUtils.isNotNullOrNA(number) && number.length() == 15)
				gstin = ProptertyUtils.getKeyBearer().encrypt(number);
		}

	}

	public JSONObject getJsonForHFO() throws Exception {

		JSONObject json = new JSONObject();

		json.put("applicantId", hfoApplicantId);
		json.put("loanId", hfoLoanId);
		json.put("opportunityId", hfoOpportunityId);

		JSONObject professionalInfo = new JSONObject();
		professionalInfo.put("monthlyIncome", monthlyIncome);
		professionalInfo.put("monthlyIncomeInCash", monthlyIncomeInCash);
		professionalInfo.put("yearOfWorkExperience", yearOfWorkExperience);
		professionalInfo.put("professionType", profession);
		professionalInfo.put("organizationName", organizationName);
		professionalInfo.put("designation", designation);
		professionalInfo.put("itrFiled", itrFiled);

		json.put("professionalInfo", professionalInfo);

		JSONObject contactInfo = new JSONObject();
		contactInfo.put("dob", dob);
		contactInfo.put("gender", gender);
		contactInfo.put("personalEmail", personalEmail);
		contactInfo.put("officialEmail", officialEmail);
		contactInfo.put("phoneNumber", phoneNumber);
		if (null != currentAddress)
			contactInfo.put("currentAddress", currentAddress.toJson());

		json.put("contactInfo", contactInfo);

		JSONObject personalInfo = new JSONObject();
		personalInfo.put("maritalStatus", maritalStatus);

		json.put("personalInfo", personalInfo);

		JSONObject propertyInfo = new JSONObject();
		propertyInfo.put("hasIdentified", propertyIdentified);
		if (null != propertyAddress)
			propertyInfo.put("address", propertyAddress.toJson());
		propertyInfo.put("estimatedValue", propertyValue);

		json.put("propertyInfo", propertyInfo);

		JSONObject documentInfo = new JSONObject();

		if (BasicUtils.isNotNullOrNA(pan))
			documentInfo.put("pan", pan.length() > 10 ? ProptertyUtils.getKeyBearer().decrypt(pan) : pan);

		if (BasicUtils.isNotNullOrNA(aadhaar))
			documentInfo.put("aadhaar",
					aadhaar.length() > 12 ? ProptertyUtils.getKeyBearer().decrypt(aadhaar) : aadhaar);

		if (BasicUtils.isNotNullOrNA(gstin))
			documentInfo.put("gstin", gstin.length() > 15 ? ProptertyUtils.getKeyBearer().decrypt(gstin) : gstin);

		json.put("documentInfo", documentInfo);

		JSONObject loanInfo = new JSONObject();
		loanInfo.put("existingEmiAmount", existingEMIAmount);
		loanInfo.put("existingLoanOutstanding", existingLoanOutstanding);
		loanInfo.put("topUpRequired", topUpRequired);

		json.put("loanInfo", loanInfo);

		return json;

	}

	public void unlockDocs() {

		try {

			if (BasicUtils.isNotNullOrNA(pan) && pan.length() > 10)
				pan = ProptertyUtils.getKeyBearer().decrypt(pan);

			if (BasicUtils.isNotNullOrNA(aadhaar) && aadhaar.length() > 12)
				aadhaar = ProptertyUtils.getKeyBearer().decrypt(aadhaar);

			if (BasicUtils.isNotNullOrNA(gstin) && gstin.length() > 15)
				gstin = ProptertyUtils.getKeyBearer().decrypt(gstin);

		} catch (Exception e) {
			LoggerUtils.log("Prospect - failed to unlock docs : " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void lockDocs() {

		try {

			if (BasicUtils.isNotNullOrNA(pan) && pan.length() == 10)
				pan = ProptertyUtils.getKeyBearer().encrypt(pan);

			if (BasicUtils.isNotNullOrNA(aadhaar) && aadhaar.length() == 12)
				aadhaar = ProptertyUtils.getKeyBearer().encrypt(aadhaar);

			if (BasicUtils.isNotNullOrNA(gstin) && gstin.length() == 15)
				gstin = ProptertyUtils.getKeyBearer().encrypt(gstin);

		} catch (Exception e) {
			LoggerUtils.log("Prospect - failed to lock docs : " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void setSFLoanType() {
		try {
			if(!ProductTypeMap.checkSfNameFromType(loanRequirment))
			loanRequirment = ProductTypeMap.getSfNameFromType(loanRequirment);
		} catch (Exception e) {
			LoggerUtils.log("Prospect.setSFLoanType - Failed to convert loan requirement to sf loan type for : "
					+ loanRequirment);
		}

	}
	
	public void removeAadhaar() {
//		if (BasicUtils.isNotNullOrNA(aadhaar)) 
			aadhaar = null;
	}
	
	public void tempFixForMissxingFileds() {
	
		if (!BasicUtils.isNotNullOrNA(profession))	
			profession = "Salaried";						
		
	}

}
