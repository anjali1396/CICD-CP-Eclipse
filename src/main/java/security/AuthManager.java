package security;

import java.security.NoSuchAlgorithmException;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import helper.AppContextProvider;
import models.admin.ClientRequest;
import models.admin.PartnerLog;
import services.UserLogger;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LoggerUtils;
import utils.OneResponse;
import v2.managers.AdminUserManager;
import v2.managers.SecurityManager;
import v2.managers.UserManager;

public class AuthManager {

	private final ClientRequest cRequest;
	private SecurityManager _sManager = null;
	private AdminUserManager _aManager = null;
	private UserManager _uManager = null;
	private final OneResponse oneResponse;
	private AppContextProvider appContextProvider = new AppContextProvider();

	public AuthManager(ClientRequest cRequest) {
		this.cRequest = cRequest;
		oneResponse = new OneResponse();
	}

	private SecurityManager securityManager() {
		if (null == _sManager)
			_sManager = new SecurityManager();
		return _sManager;
	}

	private AdminUserManager adminManager() {
		if (null == _aManager)
			_aManager = new AdminUserManager();
		return _aManager;
	}

	private UserManager userManager() {
		if (null == _uManager)
			_uManager = new UserManager();
		return _uManager;
	}

	private void log(String value) {
		LoggerUtils.log("AuthManager." + value);
	}

	public Response authenticateRoleSet() throws Exception {

		for (String role : cRequest.rolesSet) {

			Response response = null;

			switch (role) {
			
			case Constants.AUTH_TWO:

				if (!BasicUtils.isSignMatch(cRequest.cypher, cRequest.crypt))
					response = oneResponse.getAccessDeniedResponse();
				else
					response = oneResponse.simpleResponse(200, "Authenticated");

				break;
				
			case Constants.ADMIN:
				response = isAdminUserAllowed();
				break;
				
			case Constants.CRON:
				response = isCronUserAllowed();
				break;
				
			case Constants.PROSPECT_SERVICES:
				response = authenticateProspectServices();
				break;
				
			case Constants.ADMIN_SERVICES:
				response = authenticatePartnerAdminServices();
				break;
				
			case Constants.CP_USER:
				response = isUserAllowed();
				break;
				
			}

			if (null != response && response.getStatus() == 200) {
				log("authenticateRoleSet - " + role + " Role access has been authenticated sucessfully.");
			} else {
				log("authenticateRoleSet - " + role + " Role access been denied.");
				return response;
			}

		}

		return new OneResponse().getSuccessResponse(new JSONObject().put(Constants.MESSAGE, "Authenticated."));

	}

	private Response isUserAllowed() throws NoSuchAlgorithmException {

		int userId = -1;
		if (null != cRequest.userId)
			userId = Integer.parseInt(cRequest.userId);

		if (AllowedResource.AllowedMethodsWithSourcePasscode.contains(cRequest.methodName)) {

			log("isUserAllowed - Verifying user via sourcePasscode");
			if (userManager().verifySource(cRequest.sourcePasscode))
				return oneResponse.simpleResponse(200, "Authenticated");
			else
				return oneResponse.getAccessDeniedResponse();

		} else if (appContextProvider.verifyUser(userId, cRequest.sessionPasscode)) {

			log("isUserAllowed - Verifying user via ID and sessionPasscode");
			return oneResponse.simpleResponse(200, "Authenticated");

		}

		return oneResponse.getAccessDeniedResponse();

	}

	private Response isAdminUserAllowed() {

		int userId = -1;
		if (null != cRequest.userId)
			userId = Integer.parseInt(cRequest.userId);

		if (adminManager().verifyUser(userId, cRequest.sessionPasscode)) {
			log("isAdminUserAllowed - Verifying admin via ID and sessionPasscode");
			return oneResponse.simpleResponse(200, "Authenticated");
		} else if (AllowedResource.AllowedAdminMethodsWithSourcePasscode.contains(cRequest.methodName)) {
			log("isAdminUserAllowed - Verifying admin via sourcePasscode");
			if (adminManager().verifySource(cRequest.sourcePasscode))
				return oneResponse.simpleResponse(200, "Authenticated");
			else
				return oneResponse.getAccessDeniedResponse();
		}

		return oneResponse.getAccessDeniedResponse();

	}

	private Response isCronUserAllowed() throws Exception {

		if (AllowedResource.AllowedCronMethodsWithSourcePasscode.contains(cRequest.methodName)) {
			log("isCronUserAllowed - Verifying Cron via crownPasscode");
			if (adminManager().verifyCron(cRequest.crownPasscode, cRequest.ipAddress))
				return oneResponse.simpleResponse(200, "Authenticated");
			else
				return oneResponse.getAccessDeniedResponse();
		}

		return oneResponse.getAccessDeniedResponse();

	}

	// <<<<<<<<< PARTNER USER AUTHENTICATION >>>>>>>>> //

	private Response authenticatePartnerAdminServices() throws Exception {

		Response response;

		if (AllowedResource.AllowedPartnerMethodsWithDefaultAccess.contains(cRequest.methodName)) {
			log("authenticatePartnerAdminServices - Verifying Admin partner via Default Access");
			response = securityManager().authenticateRequest(cRequest);
		} else {
			log("authenticatePartnerAdminServices - Verifying Admin partner via client creds and sessionPasscode");
			response = securityManager().verifyExternalPartnerSession(cRequest);
		}

		PartnerLog pLog = new PartnerLog();
		pLog.ipAddress = cRequest.ipAddress;
		pLog.datetime = DateTimeUtils.getCurrentDateTimeInIST();
		pLog.orgId = cRequest.orgId;
		pLog.responseStatus = response.getStatus();
		pLog.serviceName = cRequest.methodName;

		new UserLogger().logPartnerRequest(pLog);

		return response;

	}

	// <<<<<<<<< PROSPECT USER AUTHENTICATION >>>>>>>>> //

	private Response authenticateProspectServices() throws Exception {

		Response response;

		if (AllowedResource.AllowedProspectMethodsWithDefaultAccess.contains(cRequest.methodName)) {
			log("authenticateProspectServices - Verifying Prospect via Default Access");
			response = securityManager().authenticateProspectUser(cRequest);
		} else {
			log("authenticateProspectServices - Verifying Prospect via client creds and sessionPasscode");
			response = securityManager().verifyProspectSession(cRequest);
		}

		return response;

	}

}
