package networking;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import dao.CommonRepository;
import models.Creds;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import utils.BasicUtils;
import utils.Constants;
import utils.Constants.CredType;
import utils.LocalHTTPResponse;
import utils.LoggerUtils;

public class LMSNetworkingClient {

	private static Creds _creds = null;
	private final OkHttpClient client;
	private int retryCount = 0;
	private static String SESSION_PASSCODE = null;

	private final static int CALL_TIMEOUT = 60;

	public enum Endpoints {

		AUTHENTICATE("/v1/ep/authenticate"), CREATE_LEAD("/v1/ep/Lead.create"), UPDATE_LEAD("/v1/ep/Lead.update"),
		PROCESS_SOFT_APPROVAL("/v1/ep/Lead.softApproval/"), LEAD_DETAIL("/v1/ep/Lead.detail/"),
		LEAD_STATUS("/v1/ep/Lead.status/"), PERFORM_OCR("/v1/ep/Document.ocr/");

		public final String value;

		Endpoints(String value) {
			this.value = value;
		}

		public String getFullUrl() throws Exception {
			return lmsCreds().apiUrl + this.value;
//			return "http://localhost:8007/cas" + this.value;
		}

	}

	private static void log(String value) {
		LoggerUtils.log("LMSNetworkingClient." + value);
	}

	public LMSNetworkingClient() throws Exception {
		lmsCreds();
		client = new OkHttpClient.Builder().connectTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
				.writeTimeout(CALL_TIMEOUT, TimeUnit.SECONDS).readTimeout(CALL_TIMEOUT, TimeUnit.SECONDS).build();
	}

	public LMSNetworkingClient(int timeout) throws Exception {
		lmsCreds();
		client = new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
				.writeTimeout(timeout, TimeUnit.SECONDS).readTimeout(timeout, TimeUnit.SECONDS).build();
	}

	private static Creds lmsCreds() throws Exception {

		if (null == _creds || null == SESSION_PASSCODE) {
			_creds = new CommonRepository().findCredsByPartnerName(Constants.PARTNER_HOMEFIRST_LMS,
					Constants.IS_PRODUCTION ? CredType.PRODUCTION : CredType.UAT);

			if (null == _creds) {
				log("lmsCreds - failed to get HomefirstLMS Creds from DB.");
				throw new Exception("failed to get HomefirstLMS Creds from DB.");
			}

		}
		return _creds;

	}

	private void authenticate() throws Exception {
		
		lmsCreds();
		
		if (null != SESSION_PASSCODE) return;

		// OkHttpClient client = new OkHttpClient().newBuilder().build();
		final var request = new Request.Builder().url(Endpoints.AUTHENTICATE.getFullUrl()).method("GET", null)
				.addHeader("Authorization",
						"Basic " + BasicUtils.getBase64(lmsCreds().username + ":" + lmsCreds().password))
				.addHeader("orgId", lmsCreds().memberPasscode).build();
		okhttp3.Response lsResponse = client.newCall(request).execute();

		final var responseString = lsResponse.body().string().toString();
		log("authenticate - Response string: " + responseString);

		final var lsJsonResponse = new JSONObject(responseString);
		final var lsResponseCode = lsResponse.code();

		lsResponse.body().close();
		lsResponse.close();

		log("authenticate - Response code: " + lsResponseCode + " Body: " + lsJsonResponse.toString());

		if (lsResponseCode == 200) {

			log("authenticate - Client authorized successfully.");

			SESSION_PASSCODE = lsJsonResponse.getString("sessionPasscode");
			retryCount = 0;

		} else if (lsResponseCode == 401) {

			log("authenticate - Unauthorized access.");
			SESSION_PASSCODE = null;
			throw new Exception("Unauthorized access while authenticate.");

		} else {

			if (retryCount < 3) {

				retryCount++;

				log("authenticate - Error. retrying...");
				SESSION_PASSCODE = null;
				authenticate();

			} else {

				retryCount = 0;
				log("authenticate - Error.");
				SESSION_PASSCODE = null;
				throw new Exception("Error while authenticate.");

			}

		}

	}

	private void reAuthenticate() throws Exception {
		_creds = null;
		SESSION_PASSCODE = null;
		authenticate();
	}

	public LocalHTTPResponse GET(String url) throws Exception {

		authenticate();

		final var request = new Request.Builder().url(url).method("GET", null)
				.addHeader("Authorization",
						"Basic " + BasicUtils.getBase64(lmsCreds().username + ":" + lmsCreds().password))
				.addHeader("orgId", lmsCreds().memberPasscode).addHeader("sessionPasscode", SESSION_PASSCODE).build();

		final var response = client.newCall(request).execute();
		final var responseString = response.body().string().toString();
		final var responseCode = response.code();

		response.body().close();
		response.close();

		log("GET - response code: " + responseCode + " body: " + responseString);

		final var localHTTPResponse = new LocalHTTPResponse();
		localHTTPResponse.statusCode = responseCode;
		localHTTPResponse.stringEntity = responseString;

		if (responseCode == 200) {

			retryCount = 0;

			final var jsonResponse = new JSONObject(responseString);

			localHTTPResponse.isSuccess = true;

//			if (jsonResponse.has(Constants.STATUS))
//				localHTTPResponse.isSuccess = jsonResponse.optString(Constants.STATUS).equals(Constants.SUCCESS);

			if (jsonResponse.has(Constants.MESSAGE))
				localHTTPResponse.message = jsonResponse.optString(Constants.MESSAGE);

		} else if (responseCode == 401) {

			log("GET - Unauthorized access while GET.");

			if (retryCount < 3) {

				retryCount++;

				reAuthenticate();
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

		authenticate();

		final var mediaType = MediaType.parse("application/json");
		final var body = RequestBody.create(requestJson.toString(), mediaType);
		final var request = new Request.Builder().url(url).method("POST", body)
				.addHeader("Authorization",
						"Basic " + BasicUtils.getBase64(lmsCreds().username + ":" + lmsCreds().password))
				.addHeader("orgId", lmsCreds().memberPasscode).addHeader("sessionPasscode", SESSION_PASSCODE)
				.addHeader("Content-Type", "application/json").build();

		final var response = client.newCall(request).execute();
		final var responseString = response.body().string().toString();
		final var responseCode = response.code();

		response.body().close();
		response.close();

		log("POST - response code: " + responseCode + " body: " + responseString);

		LocalHTTPResponse localHTTPResponse = new LocalHTTPResponse();
		localHTTPResponse.statusCode = responseCode;
		localHTTPResponse.stringEntity = responseString;

		if (responseCode == 200) {

			retryCount = 0;

			final var jsonResponse = new JSONObject(responseString);

			localHTTPResponse.isSuccess = true;

			if (jsonResponse.has(Constants.STATUS))
				localHTTPResponse.isSuccess = jsonResponse.optString(Constants.STATUS).equals(Constants.SUCCESS);

			if (jsonResponse.has(Constants.MESSAGE))
				localHTTPResponse.message = jsonResponse.optString(Constants.MESSAGE);

		} else if (responseCode == 401) {

			log("POST - Unauthorized access while POST.");

			if (retryCount < 3) {

				retryCount++;

				reAuthenticate();
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

}
