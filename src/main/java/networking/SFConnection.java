package networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import utils.Constants;
import utils.LocalHTTPResponse;
import utils.LoggerUtils;
import utils.ProptertyUtils;

public class SFConnection {

	private static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
	private static String REST_ENDPOINT = "/services/data";
	private static String APEX_REST_ENDPOINT = "/services/apexrest";
	private static String API_VERSION = "/v50.0";
	public static String baseUri = null;
	private static String instanceUri = null;
	public static String apexBaseUri = null;
	private static Header oauthHeader = null;
	private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private int retryCount = 0;

	public enum SFObjects {

		LEAD("/sobjects/Lead/"), OPPORTUNITY("/sobjects/Opportunity/"), CONTACT("/sobjects/Contact/"),
		CONNECTOR("/sobjects/Connector__c/"), APPLICATION_ID("/sobjects/ApplicationId__c/"),
		RECEIPT("/sobjects/Cash_Receipt__c/"), CONNECTOR_DOCUMENT("/sobjects/Connector_Document__c"), 
		SITE_PHOTOGRAPH("/sobjects/Site_Photograph__c");

		public final String value;

		SFObjects(String value) {
			this.value = value;
		}

	}

	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ //
	// ++++++++++++++ COMMON METHODS AND ATTRIBUTES ++++++++++++++ //
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ //

	
	private void checkAndAuthenticate() throws Exception {
		if (null == baseUri || null == oauthHeader)
			authenticate();
	}
	
	private void authenticate() throws Exception {

		HttpClient httpclient = HttpClientBuilder.create().build();

		String loginUrl = Constants.NA, clientId = Constants.NA, clientSecret = Constants.NA, username = Constants.NA,
				password = Constants.NA;

		if (Constants.IS_SF_LIVE) {
			loginUrl = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_LOGIN_URL);
			clientId = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_CLIENT_ID);
			clientSecret = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_CLIENT_SECRET);
			username = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_USERNAME);
			password = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_PASSWORD);
		} else {
			loginUrl = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_LOGIN_URL);
			clientId = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_CLIENT_ID);
			clientSecret = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_CLIENT_SECRET);
			username = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_USERNAME);
			password = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_PASSWORD);
		}

		// Assemble the login request URL
		String loginURL = loginUrl + GRANTSERVICE + "&client_id=" + clientId + "&client_secret=" + clientSecret
				+ "&username=" + username + "&password=" + password;

		// LoggerUtils.log(loginURL);

		// Login requests must be POSTs
		HttpPost httpPost = new HttpPost(loginURL);
		HttpResponse response = null;

		try {
			// Execute the login POST request
			response = httpclient.execute(httpPost);
		} catch (ClientProtocolException cpException) {
			cpException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			LoggerUtils
					.log("Error authenticating to Force.com: " + statusCode + " | error: " + response.getStatusLine());
			
			LoggerUtils
			.log("Error authenticating to Force.com response:  " + EntityUtils.toString(response.getEntity()));
			// Error is in EntityUtils.toString(response.getEntity())
			return;
		}

		String getResult = null;
		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		try {
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();

			LoggerUtils.log(" received data: " + jsonObject.toString());

			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
		}

		baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION;
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);

		System.out.println(response.getStatusLine());
		System.out.println("Successful login");
		System.out.println("  instance URL: " + loginInstanceUrl);
		System.out.println("  access token/session ID: " + loginAccessToken);
		System.out.println("baseUri: " + baseUri);

		// release connection
		httpPost.releaseConnection();

	}

	
	
	private String getModifiedQuery(String query) throws UnsupportedEncodingException {
		return URLEncoder.encode(query, "UTF-8");
	}

	private static String getBody(InputStream inputStream) {
		String result = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				result += inputLine;
				result += "\n";
			}
			in.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	public JSONObject get(String query) {

		try {

			checkAndAuthenticate();

			LoggerUtils.log("++++++++ Salesforce query: " + query);
			// Set up the HTTP objects needed to make the request.
			HttpClient httpClient = HttpClientBuilder.create().build();
			String uri = baseUri + "/query?q=" + getModifiedQuery(query);
			System.out.println("Query URL: " + uri);
			HttpGet httpGet = new HttpGet(uri);
			// System.out.println("oauthHeader2: " + oauthHeader);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);
			// Make the request.
			HttpResponse response = httpClient.execute(httpGet);
			// Process the result
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				retryCount = 0;
				String response_string = EntityUtils.toString(response.getEntity());
				try {
					JSONObject json = new JSONObject(response_string);
					System.out.println("JSON result of Query:\n" + json.toString());
					return json;
				} catch (JSONException je) {
					je.printStackTrace();
					return null;
				}
			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return get(query);
			} else {
				retryCount = 0;
				System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
				// System.exit(-1);
				return null;
			}
		} catch (Exception e) {
			LoggerUtils.log("error while getting data from salesforce: " + e.toString());
			e.printStackTrace();
			return null;
		}
	}

	public JSONObject apexGet(String uri) {

		try {

			checkAndAuthenticate();

			LoggerUtils.log("apexGet: query url : " + uri);

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(uri);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);
			HttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				retryCount = 0;
				String response_string = EntityUtils.toString(response.getEntity());

				LoggerUtils.log("apexGet: response: " + response_string);

				try {
					JSONObject json = new JSONObject(response_string);
					System.out.println("JSON result of Query:\n" + json.toString());
					return json;
				} catch (JSONException je) {
					je.printStackTrace();
					return null;
				}

			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return apexGet(uri);
			} else {
				retryCount = 0;
				System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
				// System.exit(-1);
				return null;
			}

		} catch (Exception e) {
			LoggerUtils.log("apexGet: Error : " + e.getMessage());
			e.printStackTrace();
			return null;
		}

	}

	@SuppressWarnings("unused")
	private String uploadAttachment(JSONObject jsonBody) throws Exception {

		checkAndAuthenticate();

		String uri = baseUri + "/sobjects/Attachment/";
		// String uri = baseUri + "/sobjects/ContentDocument/";

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri);
		httpPost.addHeader(oauthHeader);
		httpPost.addHeader(prettyPrintHeader);
		StringEntity body = new StringEntity(jsonBody.toString());
		body.setContentType("application/json");
		httpPost.setEntity(body);

		HttpResponse response = httpClient.execute(httpPost);
		String response_string = EntityUtils.toString(response.getEntity());
		LoggerUtils.log("SF upload attachment response: " + response_string);
		int statusCode = response.getStatusLine().getStatusCode();
		LoggerUtils.log("Upload attachment status code: " + statusCode);

		if (statusCode == 201) {
			JSONObject json = new JSONObject(response_string);
			return json.optString("id", Constants.NA);
		}

		return null;

	}

	public LocalHTTPResponse post(JSONObject requestObject, SFObjects sfObject) throws Exception {

		checkAndAuthenticate();

		LocalHTTPResponse lhResponse = new LocalHTTPResponse();

		String uri = baseUri + sfObject.value;

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri);
		httpPost.addHeader(oauthHeader);
		httpPost.addHeader(prettyPrintHeader);
		StringEntity body = new StringEntity(requestObject.toString(1));
		body.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON);
		httpPost.setEntity(body);

		HttpResponse response = httpClient.execute(httpPost);
		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode == 201) {

			retryCount = 0;
			String responseString = EntityUtils.toString(response.getEntity());
			LoggerUtils.log("SF POST Call Response: " + responseString);

			lhResponse.isSuccess = true;
			lhResponse.statusCode = statusCode;
			lhResponse.stringEntity = responseString;

		} else if (statusCode == 401 && retryCount < 3) {

			System.out.println("POST Call was unsuccessful. Access token was expired: " + statusCode);
			baseUri = null;
			oauthHeader = null;
			retryCount++;
			authenticate();

			return post(requestObject, sfObject);

		} else {
			retryCount = 0;
			LoggerUtils.log("POST Call unsuccessful. Status code returned is " + statusCode + " | error: "
					+ response.getStatusLine());
			JSONArray respArray = new JSONArray(getBody(response.getEntity().getContent()));
			JSONObject respObj = (JSONObject) respArray.get(0);
			String errorCode = respObj.optString("errorCode", Constants.NA);
			LoggerUtils.log("SF POST Call Error : " + errorCode + " | Response: " + respObj.toString());

			lhResponse.isSuccess = false;
			lhResponse.statusCode = statusCode;
			lhResponse.message = errorCode;

		}

		return lhResponse;

	}

	public LocalHTTPResponse patch(JSONObject requestObject, String uri) throws Exception {

		checkAndAuthenticate();

		LocalHTTPResponse lhResponse = new LocalHTTPResponse();

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPatch httpPatch = new HttpPatch(uri);
		httpPatch.addHeader(oauthHeader);
		httpPatch.addHeader(prettyPrintHeader);
		StringEntity body = new StringEntity(requestObject.toString(1));
		body.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON);
		httpPatch.setEntity(body);

		HttpResponse response = httpClient.execute(httpPatch);
		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode == 204) {

			retryCount = 0;

			
			lhResponse.isSuccess = true;
			lhResponse.statusCode = statusCode;
			lhResponse.stringEntity = Constants.NA;

		} else if (statusCode == 401 && retryCount < 3) {

			System.out.println("PATCH Call was unsuccessful. Access token was expired: " + statusCode);
			baseUri = null;
			oauthHeader = null;
			retryCount++;
			authenticate();

			return patch(requestObject, uri);

		} else {
			retryCount = 0;
			LoggerUtils.log("PATCH Call unsuccessful. Status code returned is " + statusCode + " | error: "
					+ response.getStatusLine());
			JSONArray respArray = new JSONArray(getBody(response.getEntity().getContent()));
			JSONObject respObj = (JSONObject) respArray.get(0);
			String errorCode = respObj.optString("errorCode", Constants.NA);
			LoggerUtils.log("SF PATCH Call Error : " + errorCode + " | Response: " + respObj.toString());

			lhResponse.isSuccess = false;
			lhResponse.statusCode = statusCode;
			lhResponse.message = errorCode;

		}

		return lhResponse;

	}

	public JSONObject getNextRecords(String nextRecordsUrl) {

		try {

			HttpClient httpClient = HttpClientBuilder.create().build();
			String uri = instanceUri + nextRecordsUrl;
			;
			HttpGet httpGet = new HttpGet(uri);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);

			HttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {
				retryCount = 0;
				String response_string = EntityUtils.toString(response.getEntity());
				try {

					JSONObject json = new JSONObject(response_string);
					System.out.println("JSON result of More Query:\n" + json.toString());
					return json;

				} catch (JSONException je) {
					je.printStackTrace();
					return null;
				}
			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("More Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return getNextRecords(nextRecordsUrl);
			} else {
				retryCount = 0;
				System.out.println("More Query was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
				// System.exit(-1);
				return null;
			}

		} catch (Exception e) {
			LoggerUtils.log("error while getting More data from salesforce: " + e.toString());
			e.printStackTrace();
			return null;
		}
	}

	public String getAttachmentData(String documentId) {

		String documentData = null;

		try {

			HttpClient httpClient = HttpClientBuilder.create().build();

			String uri = baseUri + "/sobjects/Attachment/" + documentId + "/Body";

			System.out.println("Query URL: " + uri);
			HttpGet httpGet = new HttpGet(uri);
			System.out.println("oauthHeader2: " + oauthHeader);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);

			HttpResponse response = httpClient.execute(httpGet);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				retryCount = 0;
				try {

					byte[] encoded = Base64.encodeBase64(IOUtils.toByteArray(response.getEntity().getContent()));
					documentData = new String(encoded);

				} catch (JSONException je) {
					je.printStackTrace();
					return null;
				}
			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return getAttachmentData(documentId);
			} else {
				retryCount = 0;
				System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
				// System.exit(-1);
				return null;
			}

		} catch (Exception e) {
			LoggerUtils.log("getAttachmentData - Failed to get attachment for for id: " + documentId);
			e.printStackTrace();
		}

		return documentData;

	}

	// ----------------------------------------------------------- //
	// ---------- END OF COMMON METHODS AND ATTRIBUTES ----------- //
	// ----------------------------------------------------------- //

}
