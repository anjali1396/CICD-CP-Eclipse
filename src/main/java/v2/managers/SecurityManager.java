package v2.managers;

import javax.ws.rs.core.Response;

import dao.ProspectRepository;
import models.admin.ClientRequest;
import models.admin.HFPartner;
import models.sob.Prospect;
import utils.BasicAuthCreds;
import utils.BasicUtils;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import utils.ProptertyUtils;
import v2.dbhelpers.HFPartnerDBHelper;
import utils.Constants.ActionType;
import utils.Constants.Actions;
import utils.Constants.Errors;
import utils.ProptertyUtils.Keys;

public class SecurityManager {

	private final HFPartnerDBHelper hfpDbHelper;
	private final ProspectRepository prospectRepo;

	public SecurityManager() {
		hfpDbHelper = new HFPartnerDBHelper();
		prospectRepo = new ProspectRepository();
	}

	private Response closeResourcesAndReturn(Response response) {

		closeDB();
		return response;

	}

	private void closeDB() {
		hfpDbHelper.close();
	}

	public Response authenticateRequest(ClientRequest cRequest) throws Exception {

		try {

			if (!BasicUtils.isNotNullOrNA(cRequest.authorization)) {

				LoggerUtils.log("Invalid authorization header value received while authenticateRequest: "
						+ cRequest.authorization);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Invalid auth headers.").setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

			if (!BasicUtils.isNotNullOrNA(cRequest.orgId)) {

				LoggerUtils.log("Invalid Orgranization ID received while authenticateRequest: " + cRequest.orgId);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Invalid Orgranization ID.").setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

			BasicAuthCreds clientCreds = new BasicUtils().getClientCreds(cRequest.authorization);

			HFPartner hfPartner = hfpDbHelper.getPartnerInfo(cRequest.orgId);

			if (null == hfPartner) {

				LoggerUtils.log("No hf partner found while authenticateRequest.");

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("No organization info found.").setError(Errors.RESOURCE_NOT_FOUND.value).toJson()));

			}

			if (!hfPartner.isEnabled) {

				LoggerUtils.log("HF Partner is not enabled: " + cRequest.orgId);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Orgranization ID is disabled. Please contact admin.")
						.setError(Errors.ACCESS_DENIED.value).toJson()));

			}

			if (!hfPartner.servicesAllowed.contains(Constants.ROLE_ALL)
					&& !hfPartner.servicesAllowed.contains(cRequest.methodName)) {

				LoggerUtils.log(cRequest.methodName + " service is not enabled for orgId : " + cRequest.orgId);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("This service is not enabled for your Org ID. Please contact admin.")
						.setError(Errors.ACCESS_DENIED.value).setAction(ActionType.CONTACT_ADMIN.stringValue)
						.toNewJson()));

			}

			if (hfPartner.ipRestricted) {

				if (!hfpDbHelper.isPartnerIPAllowed(cRequest.orgId, cRequest.ipAddress)) {

					LoggerUtils.log("HF Partner's IP Address is blocked for orgId : " + cRequest.orgId);

					return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse()
							.setStatus(false).setMessage("IP Address is blocked. Please contact admin.")
							.setError(Errors.ACCESS_DENIED.value).toJson()));

				}

			}

			if (ProptertyUtils.getKeyBearer().decrypt(hfPartner.clientId).equals(clientCreds.clientId)
					&& ProptertyUtils.getKeyBearer().decrypt(hfPartner.clientSecret).equals(clientCreds.clientSecret)) {

				LoggerUtils
						.log("Client successfully authorized while authenticateRequest for orgId : " + cRequest.orgId);

				return closeResourcesAndReturn(new OneResponse()
						.getSuccessResponse(new LocalResponse().setStatus(true).setMessage("Authorized.").toJson()));

			} else {

				LoggerUtils.log(
						"Invalid cliendID and clientSecrete while authenticateRequest for orgId : " + cRequest.orgId);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(
						new LocalResponse().setStatus(false).setMessage("Invalid client credentials.")
								.setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

		} catch (Exception e) {
			closeDB();
			throw e;

		}

	}

	public Response verifyExternalPartnerSession(ClientRequest cRequest) throws Exception {

		try {

			if (!BasicUtils.isNotNullOrNA(cRequest.sessionPasscode)) {

				LoggerUtils.log("Invalid sessionPasscode while authenticateRequest: " + cRequest.sessionPasscode);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Invalid session passcode.").setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

			Response authenticateResponse = authenticateRequest(cRequest);

			if (authenticateResponse.getStatus() != 200) {

				LoggerUtils.log("authenticateRequest failed while verifySession.");
				return authenticateResponse;

			}

			HFPartner hfPartner = hfpDbHelper.getPartnerInfo(cRequest.orgId);
			
			if (hfPartner.isSessionValid() && cRequest.sessionPasscode.equals(hfPartner.sessionPasscode)) {

				hfPartner.updateSession(false); // Increase session validity by 1 hour

				return closeResourcesAndReturn(
						new OneResponse().getSuccessResponse(BasicUtils.getSuccessTemplateObject()));

			}

			return closeResourcesAndReturn(new OneResponse().getAccessDeniedResponse());

		} catch (Exception e) {
			closeDB();
			throw e;
		}

	}

	public Response authenticateProspectUser(ClientRequest cRequest) throws Exception {

		try {

			if (!BasicUtils.isNotNullOrNA(cRequest.sourcePasscode)) {

				LoggerUtils.log("authenticateProspectUser: Invalid sourcePasscode: " + cRequest.sourcePasscode);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Invalid source passcode.").setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

			if (BasicUtils.getTheKey(cRequest.sourcePasscode).equals(ProptertyUtils.getValurForKey(Keys.KEY_TO_THE_SOUCE))) {

				LoggerUtils.log("authenticateProspectUser: Client successfully authorized.");

				return closeResourcesAndReturn(new OneResponse()
						.getSuccessResponse(new LocalResponse().setStatus(true).setMessage("Authorized.").toJson()));

			} else {

				LoggerUtils.log("authenticateProspectUser: Invalid sourcePasscode.");

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(
						new LocalResponse().setStatus(false).setMessage("Invalid client credentials.")
								.setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

		} catch (Exception e) {
			closeDB();
			throw e;
		}

	}

	public Response verifyProspectSession(ClientRequest cRequest) throws Exception {

		try {

			if (!BasicUtils.isNotNullOrNA(cRequest.sessionPasscode)) {

				LoggerUtils.log("verifyProspectSession: Invalid sessionPasscode: " + cRequest.sessionPasscode);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Invalid session passcode.").setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

			if (!BasicUtils.isNotNullOrNA(cRequest.userId)) {

				LoggerUtils.log("verifyProspectSession: Invalid userId: " + cRequest.sessionPasscode);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage("Invalid userId.").setError(Errors.INVALID_CREDENTIALS.value).toJson()));

			}

			Response authenticateResponse = authenticateProspectUser(cRequest);

			if (authenticateResponse.getStatus() != 200) {

				LoggerUtils.log("verifyProspectSession: authenticateProspectUser authentication failed.");
				return authenticateResponse;

			}

			Prospect prospect = prospectRepo.findProspectById(cRequest.userId);
						
			if (null == prospect) {
				
				LoggerUtils.log(
						"verifyProspectSession: No prospect found for id : " + cRequest.userId);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse()
						.setMessage("No prospect found for id :" + cRequest.userId)
						.setAction(Actions.CANCEL.value).setError(Errors.RESOURCE_NOT_FOUND.value).toJson()));
				
			}
			
			if (!prospect.isMobileVerified) {

				LoggerUtils.log(
						"verifyProspectSession: Prospects's mobile number is not verified for id : " + prospect.id);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse()
						.setMessage("Mobile number is not verified. Please verify the mobile number first.")
						.setAction(Actions.DO_VERIFICATION.value).setError(Errors.ACCESS_DENIED.value).toJson()));

			}

			if (prospect.isSessionValid() && cRequest.sessionPasscode.equals(prospect.sessionPasscode)) {

				LoggerUtils.log("verifyProspectSession: Prospects's session is VALID for id : " + prospect.id);

				prospect.updateSession(false); // Increase session validity by 1 hour

				return closeResourcesAndReturn(
						new OneResponse().getSuccessResponse(BasicUtils.getSuccessTemplateObject()));

			} else
				LoggerUtils.log("verifyProspectSession: Prospect's session is INVALID for id: " + prospect.id + " | Aborting...");

			return closeResourcesAndReturn(new OneResponse().getAccessDeniedResponse());

		} catch (Exception e) {
			closeDB();
			throw e;
		}

	}

}
