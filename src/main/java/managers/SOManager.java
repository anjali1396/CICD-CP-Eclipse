package managers;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.core.Response;

import org.json.JSONObject;

import com.google.gson.Gson;

import dao.ProspectRepository;
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
import v2.managers.ContactManager;

public class SOManager {

	private final HFOManager hfoManager;
	private final Gson gson;
	private final ProspectRepository prospectRepo;

	public SOManager() throws Exception {
		hfoManager = new HFOManager();
		gson = new Gson();
		prospectRepo = new ProspectRepository();
	}

	private void log(String value) {
		LoggerUtils.log(SOManager.class.getSimpleName() + "." + value);
	}

	public Response createProspect(JSONObject requestJson) throws Exception {

		try {

			Prospect prospect = gson.fromJson(requestJson.toString(), Prospect.class);

			prospect.referenceId = "la" + System.currentTimeMillis();

			LocalResponse pSanityCheck = prospect.allMandatoryFieldsPresent();

			if (!pSanityCheck.isSuccess) {

				log("createProspect - Mandatory fields are not present.");
				return new OneResponse().getFailureResponse(pSanityCheck.toJson());

			}

			if (!BasicUtils.isNotNullOrNA(prospect.leadSource))
				prospect.leadSource = Constants.DEFAULT_LEAD_SOURCE;

			boolean recordExist = false;
			Prospect eProspect = prospectRepo.findProspectByMobileNumber(prospect.mobileNumber);

			if (null != eProspect) {

				log("createProspect - Prospect already exists for mobile number: " + prospect.mobileNumber);

				if (eProspect.isMobileVerified) {
					return new OneResponse().getFailureResponse(new LocalResponse().setMessage(
							"Your loan application already exists with us. Please use 'Track Status' option to track the current status of your Loan application.")
							.setError(Errors.DUPLICATE_RECORD.value).setAction(Actions.DO_LOGIN.value).toJson());
				} else {
					recordExist = true;
					prospect.id = eProspect.id;
					prospect.promoCode = eProspect.promoCode;
					prospect.createDatetime = eProspect.createDatetime;
					prospect.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
				}

			}

			if (!recordExist)
				prospect.createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

			prospect.processName();

			prospect.hasConsented = true;
			prospect.consentDatetime = prospect.createDatetime;

			if (!prospectRepo.addUpdateProspect(prospect)) {

				log("createProspect - Failed to insert new prospect in DB.");
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("createProspect - Successfully inserted new prospect in DB.");

			if (!prospect.updateSession(true)) {

				log("createPropect - Failed to udpate Prospect ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("createProspect - Session updated successfully for Prospect ID: " + prospect.id);

			if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

				ContactManager contactManager = new ContactManager();

				var responseGenerateOTP = contactManager.generateOTP(prospect.mobileNumber);

				if (!responseGenerateOTP.isSuccess) {

					log("createProspect - Failed to send OTP on mobile number: " + prospect.mobileNumber
							+ " | Prospect ID: " + prospect.id);
					return new OneResponse().getFailureResponse(new LocalResponse()
							.setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson());

				}

			}

			log("createProspect - OTP sent successfully on mobile number: " + prospect.mobileNumber + " | Prospect ID: "
					+ prospect.id);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Please enter the OTP sent on mobile number: " + prospect.mobileNumber);
			responseJson.put(Constants.USER_ID, prospect.id);

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response reAuthenticateProspect(JSONObject requestObject) throws Exception {

		try {

			String mobileNumber = requestObject.optString(Constants.MOBILE_NUMBER, Constants.NA);

			if (!BasicUtils.isNotNullOrNA(mobileNumber) || !(mobileNumber.length() == 10)) {

				log("reAuthenticateProspect - Invaid mobile number.");

				return new OneResponse().getFailureResponse(new LocalResponse().setMessage("Invaid mobile number.")
						.setError(Errors.INVALID_DATA.value).setAction(Actions.FIX_RETRY.value).toJson());

			}

			Prospect prospect = prospectRepo.findProspectByMobileNumber(mobileNumber);

			if (null == prospect) {

				log("reAuthenticateProspect - No propect found for mobile number : " + mobileNumber);

				return new OneResponse().getFailureResponse(
						new LocalResponse().setMessage("No loan application found for mobile number : " + mobileNumber)
								.setError(Errors.RESOURCE_NOT_FOUND.value).setAction(Actions.CANCEL.value).toJson());

			}

			if (!prospect.isMobileVerified) {

				log("reAuthenticateProspect - Prospect's mobile number is not verified. Mobile number : "
						+ mobileNumber);

				return new OneResponse().getFailureResponse(
						new LocalResponse().setMessage("No loan application found for mobile number : " + mobileNumber)
								.setError(Errors.RESOURCE_NOT_FOUND.value).setAction(Actions.CANCEL.value).toJson());

			}

			if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

				ContactManager contactManager = new ContactManager();

				var responseGenerateOTP = contactManager.generateOTP(prospect.mobileNumber);

				if (!responseGenerateOTP.isSuccess) {

					log("reAuthenticateProspect - Failed to send OTP on mobile number: " + prospect.mobileNumber
							+ " | Prospect ID: " + prospect.id);

					return new OneResponse().getFailureResponse(new LocalResponse()
							.setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson());

				}

			}

			log("reAuthenticateProspect - OTP sent successfully on mobile number: " + prospect.mobileNumber
					+ " | Prospect ID: " + prospect.id);

			JSONObject successResponse = new JSONObject();
			successResponse.put(Constants.MESSAGE,
					"Please enter the OTP sent on mobile number: " + prospect.mobileNumber);
			successResponse.put(Constants.USER_ID, prospect.id);

			return new OneResponse().getSuccessResponse(successResponse);

		} catch (Exception e) {
			throw e;
		}

	}

	@Deprecated
	public Response verifyProspect(String userId, JSONObject requestObject) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);

			String OTP = requestObject.optString("OTP", Constants.NA);
			if (!BasicUtils.isNotNullOrNA(OTP)) {
				log("verifyProspect - Invalid OTP.");
				return new OneResponse().getFailureResponse(new LocalResponse().setMessage("Invalid OTP.")
						.setError(Errors.INVALID_DATA.value).setAction(Actions.FIX_RETRY.value).toJson());
			}

			if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

				var lResponse = new ContactManager().validateOTP(prospect.mobileNumber, OTP);

				if (!lResponse.isSuccess) {

					log("verifyProspect - Failed to verify OTP on mobile number: " + prospect.mobileNumber
							+ " | Prospect ID: " + prospect.id);
					return new OneResponse().getFailureResponse(new LocalResponse().setMessage(lResponse.message)
							.setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson());

				}

			}

			log("verifyProspect - OTP verified successfully on mobile number: " + prospect.mobileNumber
					+ " | Prospect ID: " + prospect.id);

			prospect.hasConsented = true;

			prospect.consentDatetime = DateTimeUtils.getCurrentDateTimeInIST();

			prospect.isMobileVerified = true;

			if (!prospect.updateSession(true)) {

				log("verifyProspect - Failed to udpate Prospect ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			boolean shouldReAuthenticate = requestObject.optBoolean("shouldReAuthenticate", false);

			if (shouldReAuthenticate) {

				// Request is just to re-authenticate prospect
				// return credentials

				log("verifyProspect - Postpect has been successfully re-authenticated for ID: " + prospect.id);

				JSONObject responseJson = new JSONObject();
				responseJson.put(Constants.MESSAGE, "Mobile number verified successfully.");
				responseJson.put(Constants.SESSION_PASSCODE, prospect.sessionPasscode);
				responseJson.put(Constants.USER_ID, prospect.id);

				return new OneResponse().getSuccessResponse(responseJson);

			}

			// Request is to verify and create lead
			// Continue the process from here

			prospect.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			prospect.loanAmount = 1000000;

			LocalResponse hfoResponse = hfoManager.createLead(prospect.getLeadRequestJson());

			if (!hfoResponse.isSuccess) {

				LoggerUtils.log("verifyProspect: Failed to create lead on HFO. Error: " + hfoResponse.error);

				if (hfoResponse.error.equals(Errors.DUPLICATE_RECORD.value)) {

					if (hfoResponse.action.equals(Actions.GO_BACK.value)) {

						hfoResponse.message = "Your loan application already exists with us.";

					} else {

						hfoResponse.message = "Your loan application already exists with us. Please use 'Track Status' option to track the current status of your Loan application.";

					}

					return new OneResponse().getFailureResponse(hfoResponse.toJson());

				} else {

					return new OneResponse().getFailureResponse(new LocalResponse()
							.setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson());

				}

			}

			prospect.hfoLeadId = new JSONObject(hfoResponse.message).getString(Constants.LEAD_ID);

			if (!prospectRepo.addUpdateProspect(prospect)) {

				log("verifyProspect - Failed to update prospect after mobile verification and lead creation in DB for Prospect ID: "
						+ prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("verifyProspect - Mobile verification and lead creation status updated successfully in DB for Prospect ID: "
					+ prospect.id);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Mobile number verified successfully.");
			responseJson.put(Constants.LEAD_ID, prospect.hfoLeadId);
			responseJson.put(Constants.SESSION_PASSCODE, prospect.sessionPasscode);
			responseJson.put(Constants.USER_ID, prospect.id);

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response updateAndConvertLead(String userId, JSONObject requestObject) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);
			prospect.addFields(requestObject);
			prospect.updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

			if (!prospectRepo.addUpdateProspect(prospect)) {

				log("updateAndConvertLead - Failed to update prospect in DB for Prospect ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("updateAndConvertLead - Prospect updated successfully in DB for Prospect ID: " + prospect.id);

			LoggerUtils.log("Prospect: " + gson.toJson(prospect));

			JSONObject rJson = new JSONObject();
			rJson.put("leadId", prospect.hfoLeadId);
			if (null != prospect.currentAddress)
				rJson.put("currentAddress", prospect.currentAddress.toJson());
			rJson.put("monthlyIncome", prospect.monthlyIncome);
			rJson.put("monthlyIncomeInCash", prospect.monthlyIncomeInCash);

			LocalResponse convertResponse = hfoManager.updateAndConvertLead(rJson);

			if (!convertResponse.isSuccess) {

				log("updateAndConvertLead - Failed to udpate and convert on HFO for Prospect ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("updateAndConvertLead - Lead updated and converted successfully on HFO for Prospect ID: "
					+ prospect.id);

			JSONObject convertJson = new JSONObject(convertResponse.message);

			prospect.hfoAccountId = convertJson.optString("hfoAccountId");
			prospect.hfoLoanId = convertJson.optString("hfoLoanId");
			prospect.hfoApplicantId = convertJson.optString("hfoApplicantId");
			prospect.hfoOpportunityId = convertJson.optString("hfoOpportunityId");
			prospect.isConverted = true;

			if (!prospectRepo.addUpdateProspect(prospect)) {

				log("updateAndConvertLead - Failed to update prospect in DB after converting on HFO for Prospect ID: "
						+ prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("updateAndConvertLead - Prospect updated successfully in DB after converting on HFO for Prospect ID: "
					+ prospect.id);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Lead converted successfully.");

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}
	}

	public Response updateCustomerInfo(String userId, JSONObject requestJson) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);
			prospect.addFields(requestJson);

			if (!prospectRepo.addUpdateProspect(prospect)) {

				log("updateCustomerInfo - Failed to update prospect for ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("updateCustomerInfo - Prospect updated successfully for ID: " + prospect.id);

			if (!hfoManager.updateCustomerInfo(prospect.getJsonForHFO()).isSuccess) {

				log("updateCustomerInfo - Failed to update info in HFO for ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("updateCustomerInfo - Info updated successfully in HFO for ID: " + prospect.id);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Customer info updated successfully.");

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response processDocumentImage(String userId, JSONObject requestJson) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);
			prospect.addFields(requestJson);

			JSONObject ocrData = requestJson.optJSONObject("ocrData");

			LocalResponse ocrResponse = hfoManager.processDocumentImage(prospect.getJsonForHFO(), ocrData);

			if (!ocrResponse.isSuccess) {

				log("processDocumentImage - Failed to do OCR.");

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			JSONObject ocrJson = new JSONObject(ocrResponse.message);

			if (!prospectRepo.addUpdateProspect(prospect)) {

				log("processDocumentImage - Failed to update prospect for ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("processDocumentImage - Prospect updated successfully for ID: " + prospect.id);

			if (!ocrResponse.isSuccess) {

				log("processDocumentImage - Failed to update info in HFO for ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("processDocumentImage - Info updated successfully in HFO for ID: " + prospect.id);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "OCR info updated successfully.");
			responseJson.put("ocrJson", ocrJson);

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response processSoftApproval(String userId) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);

			JSONObject json = new JSONObject();

			json.put("applicantId", prospect.hfoApplicantId);
			json.put("loanId", prospect.hfoLoanId);
			json.put("opportunityId", prospect.hfoOpportunityId);

			LocalResponse spResponse = hfoManager.processSoftApproval(json);

			if (!spResponse.isSuccess) {

				log("processSoftApproval - Failed to get soft approval from HFO for ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("processSoftApproval - Soft approval processed successfully in HFO for ID: " + prospect.id);

			JSONObject spJson = new JSONObject(spResponse.message);
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

			log("processSoftApproval - Prospect updated successfully after soft approval for ID: " + prospect.id);

			sendSobConfirmationSMS(prospect);

			spJson.put("applicationId", prospect.id);

			return new OneResponse().getSuccessResponse(spJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public void sendSobConfirmationSMS(Prospect prospect) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 5);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}

						String oppId = hfoManager.getOpportunityNumber(prospect.hfoOpportunityId);

						if (BasicUtils.isNotNullOrNA(oppId)) {

							ContactManager cManager = new ContactManager();

							JSONObject smsBody = new JSONObject();

							smsBody.put("mobiles", "91" + prospect.mobileNumber);
							smsBody.put("flow_id", ContactManager.SOB_CONFIRMATION_FLOW_ID);
							smsBody.put("ref", oppId);

							LocalResponse smsResponse = cManager.sendSMSViaFlow(smsBody);

							if (smsResponse.isSuccess) {

								LoggerUtils
										.log("sendSobConfirmationSMS - SOB confirmation SMS Task completed; Iteration: "
												+ count);
								timer.cancel();

							} else {

								count++;
								LoggerUtils.log("sendSobConfirmationSMS - Error while sening SOB confirmation SMS: "
										+ smsResponse.error + " | Rescheduled, Iteration: " + count);

							}

						} else {

							count++;
							LoggerUtils.log(
									"sendSobConfirmationSMS - Error while getting Opp Number from HFO for SOB confirmation SMS: Rescheduled, Iteration: "
											+ count);

						}

					} catch (Exception e) {

						LoggerUtils.log(
								"sendSobConfirmationSMS - Error while sending SOB confirmation SMS: " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log(
								"sendSobConfirmationSMS - SOB confirmation SMS Task rescheduled, Iteration: " + count);

					}

				} else {

					timer.cancel();
					LoggerUtils.log("sendSobConfirmationSMS - Time's up! Failed to send SOB confirmation SMS.");

				}

			}
		}, time, 15000);

	}

	public Response fetchApplicationStatus(String userId) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);

			JSONObject json = new JSONObject();

			json.put("applicantId", prospect.hfoApplicantId);
			json.put("loanId", prospect.hfoLoanId);
			json.put("opportunityId", prospect.hfoOpportunityId);
			json.put("leadId", prospect.hfoLeadId);

			LocalResponse spResponse = hfoManager.fetchApplicationStatus(json);

			if (!spResponse.isSuccess) {

				log("fetchApplicationStatus - Failed to get status from HFO for ID: " + prospect.id);
				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			log("fetchApplicationStatus - Status fetched successfully in HFO for ID: " + prospect.id);

			JSONObject hfoJson = new JSONObject(spResponse.message);
			prospect.unlockDocs();

			prospect.loanRequirment = ProductTypeMap.getDisplayName(prospect.loanRequirment);
			hfoJson.put("prospect", new JSONObject(gson.toJson(prospect)));

			return new OneResponse().getSuccessResponse(hfoJson);

		} catch (Exception e) {
			throw e;
		}

	}

	public Response resendOTP(String userId) throws Exception {

		try {

			Prospect prospect = prospectRepo.findProspectById(userId);

			if (null == prospect) {

				log("resendOTP - No prospect found for ID: " + userId);

				return new OneResponse().getFailureResponse(new LocalResponse().setError(Errors.OPERATION_FAILED.value)
						.setAction(Actions.RETRY.value).toJson());

			}

			if (Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
				
				ContactManager contactManager = new ContactManager();
				
				var responseGenerateOTP = contactManager.generateOTP(prospect.mobileNumber);

				if (!responseGenerateOTP.isSuccess) {
					log("resendOTP - Failed to resend OTP on mobile number: " + prospect.mobileNumber
							+ " | Prospect ID: " + prospect.id);
					return new OneResponse().getFailureResponse(new LocalResponse()
							.setError(Errors.OPERATION_FAILED.value).setAction(Actions.RETRY.value).toJson());
				}

			}

			log("resendOTP - OTP resent successfully on mobile number: " + prospect.mobileNumber + " | Prospect ID: "
					+ prospect.id);

			JSONObject responseJson = new JSONObject();
			responseJson.put(Constants.MESSAGE, "Please enter the OTP sent on mobile number: " + prospect.mobileNumber);

			return new OneResponse().getSuccessResponse(responseJson);

		} catch (Exception e) {
			throw e;
		}

	}

}
