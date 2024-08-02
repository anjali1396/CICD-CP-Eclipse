package managers;

import org.json.JSONObject;

import networking.HFONetworkingClient;
import utils.LocalHTTPResponse;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.Constants.Actions;
import utils.Constants.Errors;

public class HFOManager {

	private HFONetworkingClient hfoClient = null;

	public HFOManager() {
	}

	private HFONetworkingClient getHFOClient() throws Exception {
		if (null == hfoClient)
			hfoClient = new HFONetworkingClient();
		return hfoClient;
	}
	
	private HFONetworkingClient getHFOClient(int timeout) throws Exception {
		if (null == hfoClient)
			hfoClient = new HFONetworkingClient(timeout);
		return hfoClient;
	}

	public LocalResponse createLead(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient().POST(HFONetworkingClient.Endpoints.CREATE_LEAD.getFullUrl(),
				requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("HFOManager: Lead created successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("HFOManager: Error while creating lead: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse updateAndConvertLead(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient()
				.POST(HFONetworkingClient.Endpoints.UPDATE_AND_CONVERT_LEAD.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("updateAndConvertLead - Lead updated successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("updateAndConvertLead - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse updateCustomerInfo(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient()
				.POST(HFONetworkingClient.Endpoints.UPDATE_CUSTOMER_INFO.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("updateCustomerInfo - Info updated successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("updateCustomerInfo - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse processSoftApproval(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient()
				.POST(HFONetworkingClient.Endpoints.PROCESS_SOFT_APPROVAL.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("processSoftApproval - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("processSoftApproval - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse fetchApplicationStatus(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient()
				.POST(HFONetworkingClient.Endpoints.FETCH_APPLICATION_STATUS.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("fetchApplicationStatus - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("fetchApplicationStatus - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse processDocumentImage(JSONObject requestJson, JSONObject ocrData) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient().POST(HFONetworkingClient.Endpoints.DOCUMENT_OCR.getFullUrl(),
				requestJson.put("ocrData", ocrData));
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("processDocumentImage - Info updated successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("processDocumentImage - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public String getOpportunityNumber(String oppId) throws Exception {

		LocalResponse lResponse = fetchOpportunityDetails(oppId, false);

		if (lResponse.isSuccess) {

			JSONObject oppJson = new JSONObject(lResponse.message);

			return oppJson.getJSONObject("Opportunity").getString("sfOpportunityNumber");

		}

		return null;

	}

	private LocalResponse fetchOpportunityDetails(String oppId, boolean fetchAll) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient()
				.GET(HFONetworkingClient.Endpoints.FETCH_OPPORTUNITY_DETAILS.getFullUrl() + "?oppId=" + oppId
						+ "&fetchAll=" + fetchAll);

		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("fetchOpportunityDetails - Info updated successfully.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("fetchOpportunityDetails - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse addSitePhotograph(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient(240)
				.POST(HFONetworkingClient.Endpoints.ADD_SITE_PHOTOGRAPH.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("addSitePhotograph - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("addSitePhotograph - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

	public LocalResponse getSitePhotographList(JSONObject requestJson) throws Exception {
		
		LocalHTTPResponse lhResponse = getHFOClient()
				.POST(HFONetworkingClient.Endpoints.GET_SITE_PHOTOGRAPH_LIST.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();
		
		if (lhResponse.isSuccess) {
			
			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("getSitePhotographList - Success.");
			
		} else {
			
			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}
			
			LoggerUtils.log("getSitePhotographList - Error: " + lResponse.message);
			
			return lResponse;
			
		}
		
		return lResponse;
		
	}
	
	
	public LocalResponse getSitePhotographUrl(JSONObject requestJson) throws Exception {

		LocalHTTPResponse lhResponse = getHFOClient()
				.POST(HFONetworkingClient.Endpoints.GET_SITE_PHOTOGRAPH_URL.getFullUrl(), requestJson);
		LocalResponse lResponse = new LocalResponse();

		if (lhResponse.isSuccess) {

			lResponse = new LocalResponse().setStatus(true).setMessage(lhResponse.stringEntity);
			LoggerUtils.log("getSitePhotographUrl - Success.");

		} else {

			if (lhResponse.stringEntity.startsWith("{"))
				lResponse = new LocalResponse(new JSONObject(lhResponse.stringEntity));
			else {
				lResponse.action = Actions.RETRY.value;
				lResponse.error = Errors.OPERATION_FAILED.value;
			}

			LoggerUtils.log("getSitePhotographUrl - Error: " + lResponse.message);

			return lResponse;

		}

		return lResponse;

	}

}
