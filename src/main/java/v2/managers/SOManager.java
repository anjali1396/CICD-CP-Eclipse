package v2.managers;

import javax.ws.rs.core.Response;

import org.json.JSONObject;

import com.google.gson.Gson;

import dao.ProspectRepository;
import helper.EmailHelper;
import managers.LMSManager;
import models.sob.Prospect;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import utils.Constants.Actions;
import utils.Constants.Errors;
import utils.Constants.ProductTypeMap;

public class SOManager {

	private final Gson gson;
	private final ProspectRepository prospectRepo;
	private final LMSManager lmsManager;

	public SOManager() throws Exception {
		gson = new Gson();
		prospectRepo = new ProspectRepository();
		lmsManager = new LMSManager();
	}

	private void log(String value) {
		LoggerUtils.log(SOManager.class.getSimpleName() + "." + value);
	}

	public Response reAuthenticateProspect(JSONObject requestObject) throws Exception {

		final var mobileNumber = requestObject.optString(Constants.MOBILE_NUMBER, Constants.NA);

		if (!BasicUtils.isNotNullOrNA(mobileNumber) || !(mobileNumber.length() == 10)) {

			log("reAuthenticateProspect - Invaid mobile number.");

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("Invaid mobile number.")
					.setError(Errors.INVALID_DATA.value).setAction(Actions.FIX_RETRY.value).toJson());

		}

		var prospect = prospectRepo.findProspectByMobileNumber(mobileNumber);
		
		final var lmsResponse = lmsManager.getLeadDetail(mobileNumber);
		
		if (!lmsResponse.isSuccess) {
			
			log("reAuthenticateProspect - No lead found on LMS for mobile number : " +  mobileNumber);
			
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("No Lead found for mobile Number : " + mobileNumber)
					.setError(Errors.RESOURCE_NOT_FOUND.value).setAction(Actions.CANCEL.value).toJson());
			
		}
		
		final var lmsProspect = new Prospect().initFromLMS(new JSONObject(lmsResponse.message));
		
		if (null != prospect) {
			
			final var pId = prospect.id;
			final var hasConsented = prospect.hasConsented;
			final var consentDatetime = prospect.consentDatetime;
			prospect = lmsProspect;
			prospect.id = pId;
			prospect.consentDatetime = consentDatetime;
			prospect.hasConsented = hasConsented;
			
		} else {
			
			log("reAuthenticateProspect - No propect found for mobile number : " + mobileNumber);
			prospect = lmsProspect;
			
		}
		
		if (!prospectRepo.addUpdateProspect(prospect)) {
			
			log("reAuthenticateProspect - Failed to create / update prospect in local db after getting lead from LMS for mobile number : " +  mobileNumber);
			
			return new OneResponse().getFailureResponse(new LocalResponse()
					.setError(Errors.OPERATION_FAILED.value).setAction(Actions.FIX_RETRY.value).toJson());
			
		}
		
		if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

			ContactManager contactManager = new ContactManager();

			var responseGenerateOTP = contactManager.generateOTP(prospect.mobileNumber);

			 if (!responseGenerateOTP.isSuccess) {

				log("reAuthenticateProspect - Failed to send OTP on mobile number: " + prospect.mobileNumber
						+ " | Prospect ID: " + prospect.id);

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

		}

		JSONObject successResponse = new JSONObject();
		successResponse.put(Constants.MESSAGE, "Please enter the OTP sent on mobile number: " + prospect.mobileNumber);
		successResponse.put(Constants.USER_ID, prospect.id);


		return new OneResponse().getSuccessResponse(successResponse);

	}

	public Response verifyProspect(String userId, JSONObject requestObject) throws Exception {

		final var prospect = prospectRepo.findProspectById(userId);

		final var OTP = requestObject.optString("OTP", Constants.NA);

		if (!BasicUtils.isNotNullOrNA(OTP)) {
			log("verifyProspect - Invalid OTP.");
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("Invalid OTP.")
					.setError(Errors.INVALID_DATA.value).setAction(Actions.FIX_RETRY.value).toJson());
		}

		if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
			
			final var cmResponse = new ContactManager().validateOTP(prospect.mobileNumber, OTP);
			
			if (!cmResponse.isSuccess) {

				log("verifyProspect - Failed to verify OTP on mobile number: " + prospect.mobileNumber
						+ " | Prospect ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse()
						.setMessage(cmResponse.message)
						.setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson());
			}
		}
		
		final var shouldReAuthenticate = requestObject.optBoolean("shouldReAuthenticate", false);

		if (!shouldReAuthenticate) {

			if (prospect.hasConsented && !BasicUtils.isNotNullOrNA(prospect.consentDatetime)) {
				prospect.hasConsented = true;
				prospect.consentDatetime = prospect.createDatetime;
				//prospect.consentDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			}

		} else {

			if (prospect.hasConsented && !BasicUtils.isNotNullOrNA(prospect.consentDatetime)) {
				prospect.consentDatetime = prospect.createDatetime;
			}
	

		}

		prospect.isMobileVerified = true;
		
		if (!prospect.updateSession(true)) {

			log("verifyProspect - Failed to udpate Prospect ID: " + prospect.id);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		if (shouldReAuthenticate) {

			// Request is just to re-authenticate prospect
			// return credentials

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Mobile number verified successfully.");
			responseJson.put(Constants.SESSION_PASSCODE, prospect.sessionPasscode);
			responseJson.put(Constants.USER_ID, prospect.id);

			return new OneResponse().getSuccessResponse(responseJson);

		}

		/*
		 * Request is to verify and create lead Continue the process from here
		 */

		prospect.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

		if (prospect.loanAmount < 1)
			prospect.loanAmount = 1000000;
		
		
		final var lmsResponse = lmsManager.createLead(prospect.getLMSRequestJson(false));
		

		if (!lmsResponse.isSuccess) {

			LoggerUtils.log("verifyProspect: Failed to create lead on LMS. Error: " + lmsResponse.error);

			if (lmsResponse.error.equals(Errors.DUPLICATE_RECORD.value)) {

				if (lmsResponse.action.equals(Actions.GO_BACK.value)) {

					lmsResponse.message = "Your loan application already exists with us.";

				} else {

					lmsResponse.message = "Your loan application already exists with us. Please use 'Track Status' option to track the current status of your Loan application.";

				}

				return new OneResponse().getFailureResponse(lmsResponse.toJson());

			} else {

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

		}

		final var leadResponseJson = new JSONObject(lmsResponse.message).getJSONObject(Constants.LEAD);

		prospect.hfoLeadId = leadResponseJson.getString(Constants.ID);

		final var applicantArray = leadResponseJson.optJSONArray("applicants");
		if (null != applicantArray && applicantArray.length() > 0) {

			final var primaryApplicant = applicantArray.getJSONObject(0);

			prospect.lmsApplicantId = primaryApplicant.getString(Constants.ID);
			prospect.lmsContactId = primaryApplicant.getJSONObject("contact").getString(Constants.ID);
//			prospect.stageProgress = 1;

		}

		if (!prospectRepo.addUpdateProspect(prospect)) {

			log("verifyProspect - Failed to update prospect after mobile verification and lead creation in DB for Prospect ID: "
					+ prospect.id);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		JSONObject responseJson = new JSONObject();
		responseJson.put(Constants.MESSAGE, "Mobile number verified successfully.");
		responseJson.put(Constants.LEAD_ID, prospect.hfoLeadId);
		responseJson.put(Constants.SESSION_PASSCODE, prospect.sessionPasscode);
		responseJson.put(Constants.USER_ID, prospect.id);

		return new OneResponse().getSuccessResponse(responseJson);

	}

	public Response updateCustomerInfo(String userId, JSONObject requestJson) throws Exception {

		final var prospect = prospectRepo.findProspectById(userId);
		prospect.addFields(requestJson);
		
		if (!BasicUtils.isNotNullOrNA(prospect.referenceId))
			prospect.referenceId = "la" + System.currentTimeMillis();

				
		var stageProgress = requestJson.optInt("stageProgress", -1);
		
		if(prospect.stageProgress < stageProgress)
		   prospect.stageProgress = stageProgress;
		
		if (!prospectRepo.addUpdateProspect(prospect)) {

			log("updateCustomerInfo - Failed to update prospect for ID: " + prospect.id);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		if (!lmsManager.updateCustomerInfo(prospect.getLMSRequestJson(true)).isSuccess) {

			log("updateCustomerInfo - Failed to update info in LMS for ID: " + prospect.id);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		JSONObject responseJson = new JSONObject();
		
		responseJson.put(Constants.MESSAGE, "Customer info updated successfully.");
		
		prospect.loanRequirment = ProductTypeMap.getDisplayName(prospect.loanRequirment);
		prospect.lockDocs();
		prospect.removeAadhaar();
		//prospect.tempFixForMissxingFileds();
		
		responseJson.put("prospect", new JSONObject(gson.toJson(prospect)));

		return new OneResponse().getSuccessResponse(responseJson);

	}

	public Response processSoftApproval(String userId) throws Exception {

		final var prospect = prospectRepo.findProspectById(userId);

		final var spResponse = lmsManager.processSoftApproval(prospect.hfoLeadId);

		if (!spResponse.isSuccess) {

			log("processSoftApproval - Failed to get soft approval from LMS for ID: " + prospect.id);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		final var spJson = new JSONObject(spResponse.message);
		spJson.put("applicationId", prospect.id);
		
		prospect.loanAmount = spJson.optDouble("loanAmount", -1);
		prospect.emiAmount = spJson.optDouble("emiAmount", -1);
		prospect.LTV = spJson.optDouble("LTV", -1);
		prospect.interestRate = spJson.optDouble("interestRate", -1);
		prospect.totalTenure = spJson.optInt("totalTenure", -1);
		
		

		if (!prospectRepo.addUpdateProspect(prospect)) {

			log("processSoftApproval - Failed to update prospect after soft approval for ID: " + prospect.id);
			return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
					.setAction(Actions.RETRY.value).toJson());

		}

		new EmailHelper().sendSobConfirmationSMS(prospect);

		return new OneResponse().getSuccessResponse(spJson);

	}

	public Response fetchApplicationStatus(String userId) throws Exception {

		final var prospect = prospectRepo.findProspectById(userId);
		
		final var lmsResponse = lmsManager.getApplicationStatus(prospect.hfoLeadId);
		
		if (!lmsResponse.isSuccess) {
			
			log("fetchApplicationStatus - Failed to get application status from LMS for prospect id : " + userId);
			return new OneResponse().getFailureResponse(lmsResponse.toJson());
			
		}
		
		final var lmsJson = new JSONObject(lmsResponse.message);
		prospect.unlockDocs();		
		prospect.removeAadhaar();
		
		prospect.loanRequirment = ProductTypeMap.getDisplayName(prospect.loanRequirment);
		prospect.tempFixForMissxingFileds();
			
		lmsJson.put("prospect", new JSONObject(gson.toJson(prospect)));

		return new OneResponse().getSuccessResponse(lmsJson);

	}
	
	public Response processDocumentImage(String userId, JSONObject requestJson) throws Exception {

		try {
			
			final var ocrData = requestJson.optJSONObject("ocrData");
			
			final var prospect = prospectRepo.findProspectById(userId);
			
			if (!ocrData.has("leadId") || !BasicUtils.isNotNullOrNA(ocrData.optString("leadId", Constants.NA))) {
				ocrData.put("leadId", prospect.hfoLeadId);
			}

			final var ocrResponse = lmsManager.performOCR(ocrData);
			
			if (!ocrResponse.isSuccess) {
				
				log("processDocumentImage - Failed to do OCR.");
				
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());
				
			}
			
			final var ocrJson = new JSONObject(ocrResponse.message);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "OCR info updated successfully.");
			responseJson.put("ocrJson", ocrJson);


			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

}
