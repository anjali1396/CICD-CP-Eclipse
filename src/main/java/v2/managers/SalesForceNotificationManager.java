package v2.managers;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import models.SFContact;
import utils.Constants;
import utils.LoggerUtils;
import utils.ProptertyUtils;

public class SalesForceNotificationManager {

	private static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
	private static String REST_ENDPOINT = "/services/data";
	private static String API_VERSION = "/v45.0";
	private static String baseUri = null;
	private static Header oauthHeader = null;
	private static String instanceURL = null;
	private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");

	private int retryCount = 0;

	public SalesForceNotificationManager() {
	}

	// ====================== COMMON METHODS AND ATTRIBUTES =======================
	// //

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

		instanceURL = loginInstanceUrl;
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

	private JSONObject getSalesforceData(String query) {

		try {

			// Set up the HTTP objects needed to make the request.
			HttpClient httpClient = HttpClientBuilder.create().build();

			String uri = baseUri + "/query?q=" + query;
			// System.out.println("Query URL: " + uri);
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
				return getSalesforceData(query);
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

	private JSONObject getSalesforceMoreData(String nextRecordsUrl) {
		try {

			// Set up the HTTP objects needed to make the request.
			HttpClient httpClient = HttpClientBuilder.create().build();

			String uri = instanceURL + nextRecordsUrl;

			HttpGet httpGet = new HttpGet(uri);
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
				return getSalesforceMoreData(nextRecordsUrl);
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

	private String getModifiedQuery(String query) throws UnsupportedEncodingException {
		// return query.replace(" ", "+");
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

	// ******************* END OF COMMON METHODS AND ATTRIBUTES ********************
	
	public ArrayList<SFContact> getCustomersWithBirthday(String day, String month)
			throws Exception {

		checkAndAuthenticate();
		
		String query = "select Id, FirstName, Date_of_Birth__c, MobilePhone, Phone, "
				+ "CreatedDate from Contact where MobilePhone != '' and MobilePhone != null"
				+ " and DAY_IN_MONTH(Date_of_Birth__c) = " + day
				+ "AND CALENDAR_MONTH(Date_of_Birth__c) = " + month;

		JSONObject sfResponseJson = getSalesforceData(getModifiedQuery(query));

		if (null == sfResponseJson)
			return null;

		ArrayList<SFContact> contacts = new ArrayList<SFContact>();

		if (sfResponseJson.getInt("totalSize") > 0) {
			JSONArray records = sfResponseJson.getJSONArray("records");

			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);
				contacts.add(new SFContact(current));

			}
		}

		if (!sfResponseJson.getBoolean("done")) {
			getMoreContacts(contacts, sfResponseJson.getString("nextRecordsUrl"));
		}

		return contacts;

	}

	private void getMoreContacts(ArrayList<SFContact> contacts, String nextRecordsUrl) {

		JSONObject contactJson = getSalesforceMoreData(nextRecordsUrl);

		if (contactJson.getInt("totalSize") > 0) {

			JSONArray contractArray = contactJson.getJSONArray("records");

			for (int i = 0; i < contractArray.length(); i++) {

				JSONObject current = contractArray.getJSONObject(i);
				contacts.add(new SFContact(current));

			}

		}

		if (!contactJson.getBoolean("done")) {
			getMoreContacts(contacts, contactJson.getString("nextRecordsUrl"));
		}

	}

}