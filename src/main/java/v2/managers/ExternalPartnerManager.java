package v2.managers;

import javax.ws.rs.core.Response;

import org.json.JSONObject;

import models.admin.HFPartner;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import utils.Constants.ActionType;
import utils.Constants.Errors;
import v2.dbhelpers.HFPartnerDBHelper;

public class ExternalPartnerManager {

	private final HFPartnerDBHelper hfpDbHelper;

	public ExternalPartnerManager() {
		hfpDbHelper = new HFPartnerDBHelper();
	}

	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++ //
	// ++++++++++++++ START DEFAULT MEHTODS ++++++++++++++++++ //
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++ //

	private Response closeResourcesAndReturn(Response response) {
		closeDB();
		return response;

	}

	private void closeDB() {
		hfpDbHelper.close();
	}

	// ----------------------------------------------------- //
	// -------------- END OF DEFAULT METHODS --------------- //
	// ----------------------------------------------------- //

	public Response authenticateClient(String orgId) throws Exception {

		try {

			HFPartner hfPartner = hfpDbHelper.getPartnerInfo(orgId);
			closeDB();

			if (hfPartner.updateSession(true)) {

				LoggerUtils.log("Session update successfully for orgId : " + orgId);

				JSONObject responseJson = new JSONObject();
				responseJson.put(Constants.SESSION_PASSCODE, hfPartner.sessionPasscode);
				responseJson.put(Constants.VALID_UPTO, hfPartner.sessionValidDatetime);

				return closeResourcesAndReturn(new OneResponse().getSuccessResponse(responseJson));

			} else {

				LoggerUtils.log("Failed to update session for orgId : " + orgId);

				return closeResourcesAndReturn(new OneResponse().getFailureResponse(new LocalResponse().setStatus(false)
						.setMessage(Constants.DEFAULT_ERROR_MESSAGE).setError(Errors.OPERATION_FAILED.value)
						.setAction(ActionType.AUTHENTICATE_AGAIN.stringValue).toNewJson()));

			}

		} catch (Exception e) {

			closeDB();
			throw e;

		}

	}
}
