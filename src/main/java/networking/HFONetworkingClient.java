package networking;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import utils.BasicUtils;
import utils.Constants;
import utils.LocalHTTPResponse;
import utils.LoggerUtils;
import utils.ProptertyUtils;
import utils.ProptertyUtils.Keys;

public class HFONetworkingClient {
	
	private static String CLIENT_ID = null;
	private static String CLIENT_SECRET = null;
	private static String CLIENT_ORG_ID = null;
	private static String SESSION_PASSCODE = null;

	private final static int CALL_TIMEOUT = 60;
	private int retryCount = 0;

	public static final String BASE_URL_PROD = "https://one.homefirstindia.com:8443/HomefirstOne"; // TODO : use this for prod
//	public static final String BASE_URL_PROD = "http://localhost:8080/HomefirstOne"; // TODO: use this for local
	public static final String BASE_URL_UAT = "http://localhost:8080/HomefirstOne";

	private final OkHttpClient client;

	public enum Endpoints {

		AUTHENTICATE_CLIENT("/V1/client/authenticateClient"), 
		CREATE_LEAD("/V1/cs/createLead"),
		UPDATE_AND_CONVERT_LEAD("/V1/cs/updateAndConvertLead"),
		UPDATE_CUSTOMER_INFO("/V1/cs/updateCustomerInfo"),
		DOCUMENT_OCR("/V1/cs/processDocumentImage"),
		PROCESS_SOFT_APPROVAL("/V1/cs/processSoftApproval"),
		FETCH_APPLICATION_STATUS("/V1/cs/fetchApplicationStatus"),
		ADD_SITE_PHOTOGRAPH("/V1/cs/addSitePhotograph"),
		GET_SITE_PHOTOGRAPH_LIST("/V1/cs/getSitePhotographList"),
		GET_SITE_PHOTOGRAPH_URL("/V1/cs/getSitePhotographUrl"),
		FETCH_OPPORTUNITY_DETAILS("/V1/cs/fetchOpportunityDetails");		

		public final String value;

		Endpoints(String value) {
			this.value = value;
		}
		
		public String getFullUrl() {
			return (Constants.IS_PRODUCTION ? BASE_URL_PROD : BASE_URL_UAT) + this.value;
		}

	}

	public HFONetworkingClient() throws Exception {
		client = new OkHttpClient.Builder().connectTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
				.writeTimeout(CALL_TIMEOUT, TimeUnit.SECONDS).readTimeout(CALL_TIMEOUT, TimeUnit.SECONDS).build();
		authenticateCPClient();
	}
	
	public HFONetworkingClient(int timeout) throws Exception {
		client = new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
				.writeTimeout(timeout, TimeUnit.SECONDS).readTimeout(timeout, TimeUnit.SECONDS).build();
		authenticateCPClient();
	}
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++ //
	// ++++++++++++ STAR TO COMMON MEHTODS +++++++++++++++++++ //
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++ //

	public LocalHTTPResponse GET(String url) throws Exception {

		Request request = new Request.Builder()
				.url(url)
				.method("GET", null)
				.addHeader("Authorization", "Basic " + BasicUtils.getBase64(CLIENT_ID + ":" + CLIENT_SECRET))
				.addHeader("orgId", CLIENT_ORG_ID)
				.addHeader("sessionPasscode", SESSION_PASSCODE).build();

		Response response = client.newCall(request).execute();

		String responseString = response.body().string().toString();
		int responseCode = response.code();
		response.body().close();
		response.close();
		LoggerUtils.log("HFO GET response code: " + responseCode + " body: " + responseString);

		LocalHTTPResponse localHTTPResponse = new LocalHTTPResponse();
		localHTTPResponse.statusCode = responseCode;
		localHTTPResponse.stringEntity = responseString;

		if (responseCode == 200) {
			
			retryCount = 0;
			localHTTPResponse.isSuccess = true;

//			JSONObject jsonResponse = new JSONObject(responseString);			
//			
//			if (jsonResponse.has(Constants.STATUS))
//				localHTTPResponse.isSuccess = jsonResponse.optString(Constants.STATUS).equals(Constants.SUCCESS);

		} else if (responseCode == 401) {

			LoggerUtils.log("Unauthorized access while GET.");

			if (retryCount < 3) {

				retryCount++;

				reAuthenticateClient();
				return GET(url);

			} else {

				retryCount = 0;
				localHTTPResponse.isSuccess = false;
				localHTTPResponse.message = "Unauthorized access.";

			}
			
		} else {

			retryCount = 0;
			localHTTPResponse.isSuccess = false;
			localHTTPResponse.message = Constants.DEFAULT_ERROR_MESSAGE;			
			
		}
		
		return localHTTPResponse;

	}
	
	public LocalHTTPResponse POST(String url, JSONObject requestJson) throws Exception {

		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(requestJson.toString(), mediaType);
		Request request = new Request.Builder()
				.url(url)
				.method("POST", body)
				.addHeader("Authorization", "Basic " + BasicUtils.getBase64(CLIENT_ID + ":" + CLIENT_SECRET))
				.addHeader("orgId", CLIENT_ORG_ID)
				.addHeader("sessionPasscode", SESSION_PASSCODE)
				.addHeader("Content-Type", "application/json").build();
		
		Response response = client.newCall(request).execute();

		String responseString = response.body().string().toString();
		int responseCode = response.code();
		response.body().close();
		response.close();
		LoggerUtils.log("HFO POST response code: " + responseCode + " body: " + responseString);

		LocalHTTPResponse localHTTPResponse = new LocalHTTPResponse();
		localHTTPResponse.statusCode = responseCode;
		localHTTPResponse.stringEntity = responseString;

		if (responseCode == 200) {
			
			retryCount = 0;
			localHTTPResponse.isSuccess = true;

			//JSONObject jsonResponse = new JSONObject(responseString);			
			
//			if (jsonResponse.has(Constants.STATUS))
//				localHTTPResponse.isSuccess = jsonResponse.optString(Constants.STATUS).equals(Constants.SUCCESS);

		} else if (responseCode == 401) {

			LoggerUtils.log("Unauthorized access while POST. Retry Count: " + retryCount);

			if (retryCount < 3) {

				retryCount++;

				reAuthenticateClient();
				return POST(url, requestJson);

			} else {

				retryCount = 0;
				localHTTPResponse.isSuccess = false;
				localHTTPResponse.message = "Unauthorized access.";

			}
			
		} else {

			retryCount = 0;
			localHTTPResponse.isSuccess = false;
			localHTTPResponse.message = Constants.DEFAULT_ERROR_MESSAGE;			
			
		}
		
		return localHTTPResponse;

	}
	
	private void reAuthenticateClient() throws Exception {
		
		SESSION_PASSCODE = null;
		authenticateCPClient();
		
		
	}
	
	// ------------------------------------------------------- //
	// ---------------- END OF COMMON METHODS ---------------- //
	// ------------------------------------------------------- //

	private void authenticateCPClient() throws Exception {

		try {

			if (null == CLIENT_ID || null == CLIENT_SECRET || null == CLIENT_ORG_ID || null == SESSION_PASSCODE) {

				if (Constants.IS_PRODUCTION) {
				
					CLIENT_ID = ProptertyUtils.getValurForKey(Keys.HFO_CLIENT_ID_PROD);
					CLIENT_SECRET = ProptertyUtils.getValurForKey(Keys.HFO_CLIENT_SECRET_PROD);
					
				} else {
					
					CLIENT_ID = ProptertyUtils.getValurForKey(Keys.HFO_CLIENT_ID_TEST);
					CLIENT_SECRET = ProptertyUtils.getValurForKey(Keys.HFO_CLIENT_SECRET_TEST);
					
				}		
				
				CLIENT_ORG_ID = ProptertyUtils.getValurForKey(Keys.HFO_ORG_ID);

			} else
				return;

			OkHttpClient client = new OkHttpClient().newBuilder().build();
			Request request = new Request.Builder()
					.url(Endpoints.AUTHENTICATE_CLIENT.getFullUrl())
					.method("GET", null)
					.addHeader("Authorization", "Basic " + BasicUtils.getBase64(CLIENT_ID + ":" + CLIENT_SECRET))
					.addHeader("orgId", CLIENT_ORG_ID)
					.build();
			okhttp3.Response lsResponse = client.newCall(request).execute();
			
			String responseEntity = lsResponse.body().string().toString();
			
			LoggerUtils.log("authenticateCPClient - Response body: " + responseEntity);

			JSONObject lsJsonResponse = new JSONObject(responseEntity);
			int lsResponseCode = lsResponse.code();
			lsResponse.body().close();
			lsResponse.close();
			LoggerUtils.log("HFO response code: " + lsResponseCode + " body: " + lsJsonResponse.toString());

			if (lsResponseCode == 200) {

				LoggerUtils.log("HFO Client authorized successfully.");

				SESSION_PASSCODE = lsJsonResponse.getString("sessionPasscode");

			} else if (lsResponseCode == 401) {

				LoggerUtils.log("Unauthorized access while authenticateCPClient.");
				throw new Exception("Unauthorized access while authenticateCPClient.");

			} else {

				LoggerUtils.log("Error while authenticateCPClient.");
				throw new Exception("Error while authenticateCPClient.");

			}

		} catch (Exception e) {
			LoggerUtils.log("Error while authenticateCPClient: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

	}

}
