package managers;

import org.json.JSONObject;

import networking.LMSNetworkingClient;
import networking.LMSNetworkingClient.Endpoints;
import utils.LocalHTTPResponse;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.Constants.Actions;
import utils.Constants.Errors;

public class LMSManager {
	
	private LMSNetworkingClient _lmsNetworkingClient = null;
	
	private LMSNetworkingClient lmsNetworkingClient() throws Exception {
		if (null == _lmsNetworkingClient) {
			_lmsNetworkingClient = new LMSNetworkingClient();
		}
		return _lmsNetworkingClient;
	}
	
	private void log(String value) {
		LoggerUtils.log("LMSManager." + value);	
	}

	public LocalResponse createLead(JSONObject requestJson) throws Exception {


		final var lhResponse = lmsNetworkingClient().POST(Endpoints.CREATE_LEAD.getFullUrl(),
				requestJson);
		var lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			log("createLead - Lead created successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			log("createLead - Error while creating lead: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}
	
	public LocalResponse updateCustomerInfo(JSONObject requestJson) throws Exception {
		
		LocalHTTPResponse lhResponse = null;

	  	 lhResponse = lmsNetworkingClient().POST(Endpoints.UPDATE_LEAD.getFullUrl(),
 				requestJson);
	
	  	var lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			log("updateCustomerInfo - Info updated successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			log("updateCustomerInfo - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}
	
	public LocalResponse processSoftApproval(String id) throws Exception {

		final var lhResponse = lmsNetworkingClient().GET(Endpoints.PROCESS_SOFT_APPROVAL.getFullUrl() + id);
		
		var lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			log("processSoftApproval - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			log("processSoftApproval - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}
	
	public LocalResponse getLeadDetail(String id) throws Exception {

		final var lhResponse = lmsNetworkingClient().GET(Endpoints.LEAD_DETAIL.getFullUrl() + id);
		
		var lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			log("getLeadDetail - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			log("getLeadDetail - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}
	
	public LocalResponse getApplicationStatus(String id) throws Exception {

		final var lhResponse = lmsNetworkingClient().GET(Endpoints.LEAD_STATUS.getFullUrl() + id);
		
		var lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			log("getApplicationStatus - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			log("getApplicationStatus - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}
	
	public LocalResponse performOCR(JSONObject requestJson) throws Exception {
		
		LocalHTTPResponse lhResponse = null;

	  	 lhResponse = lmsNetworkingClient().POST(Endpoints.PERFORM_OCR.getFullUrl(),
 				requestJson);
	
	  	var lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			log("performOCR - OCR performed successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			log("performOCR - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

}
