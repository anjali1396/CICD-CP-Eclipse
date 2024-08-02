package v2.managers;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.google.gson.Gson;

import models.OTPVerificationDTO;
import networking.HFOSpringNetworkingClient;
import networking.HFOSpringNetworkingClient.Endpoints;
import utils.BasicUtils;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.ProptertyUtils;
import utils.ProptertyUtils.Keys;

public class ContactManager {

	private static String AUTH_KEY = null;
	private final String OTP_TEMPLATE_ID = "60433ad181e2af5400305525";
	public static final String PAYMENT_CONFIRMATION_FLOW_ID = "5f0da744d6fc057085129aba";
	public static final String SOB_CONFIRMATION_FLOW_ID = "60433d6bf195c650fd12f9ae";

	private final Gson gson;

	private HFOSpringNetworkingClient _hfoSpringNetworkingClient = null;

	private HFOSpringNetworkingClient hfoSpringNetworkingClient() throws Exception {
		if (null == _hfoSpringNetworkingClient) {
			_hfoSpringNetworkingClient = new HFOSpringNetworkingClient();
		}
		return _hfoSpringNetworkingClient;
	}

	public ContactManager() throws Exception {
		if (null == AUTH_KEY)
			AUTH_KEY = ProptertyUtils.getValurForKey(Keys.AUTH_KEY);

		gson = new Gson();
	}

	public LocalResponse generateOTP(String mobileNumber) throws Exception {

		var lResponse = new LocalResponse();

		if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
			lResponse.isSuccess = true;
			return lResponse;
		}

		final var url = HFOSpringNetworkingClient.Endpoints.OTP_GENERATE.getFullUrl() + "?source="
				+ Constants.SOURCE_CUSTOMER_PORTAL + "&mobileNumber=" + mobileNumber;

		final var hfosResponse = hfoSpringNetworkingClient().GET(url);

		LoggerUtils.log("generateOTP - response: " + hfosResponse.stringEntity);

		lResponse.isSuccess = hfosResponse.isSuccess;
		lResponse.message = hfosResponse.stringEntity;

		return lResponse;

	}

	public LocalResponse validateOTP(String mobileNumber, String OTP) throws Exception {

		var lResponse = new LocalResponse();

		if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
			lResponse.isSuccess = true;
			return lResponse;
		}
		var optVerificationDTO = new OTPVerificationDTO();

		optVerificationDTO.mobileNumber = mobileNumber;
		optVerificationDTO.otp = OTP;

		final var url = HFOSpringNetworkingClient.Endpoints.VALIDATE_OTP.getFullUrl();

		final var hfosResponse = hfoSpringNetworkingClient().POST(url, new JSONObject(gson.toJson(optVerificationDTO)));

		lResponse.isSuccess = hfosResponse.isSuccess;

		var hfoResJson = new JSONObject(hfosResponse.stringEntity);

		lResponse.message = hfoResJson.optString("message", Constants.NA);

		if (lResponse.message.contains("Incorrect OTP")) {

			var responseGenerateOTP = new ContactManager().generateOTP(mobileNumber);

			if (!responseGenerateOTP.isSuccess) {
				
				LoggerUtils.log("validateOTP - Failed to resent OTP on mobile number: " + mobileNumber);
			}
		}

		return lResponse;
	}

	public JSONObject sendOTP(String mobileNumber, String countryCode) throws Exception {

		JSONObject responseObject = new JSONObject();

		if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

			responseObject = BasicUtils.getSuccessTemplateObject();
			responseObject.put("otpReferenceId", "dummyRequestId");
			return responseObject;
		}

		String uri = "https://api.msg91.com/api/v5/otp?" + "authkey=" + AUTH_KEY + "&template_id=" + OTP_TEMPLATE_ID
				+ "&mobile=" + (countryCode.replace("+", "") + mobileNumber);

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri.replace(" ", "+"));

		HttpResponse response = httpClient.execute(httpPost);
		String response_string = EntityUtils.toString(response.getEntity());
		JSONObject json = new JSONObject(response_string);

		LoggerUtils.log("mobilenumber : " + mobileNumber);

		LoggerUtils.log("Send OTP response: " + json);

		if (!json.getString("type").equalsIgnoreCase(Constants.SUCCESS)) {

			LoggerUtils.log("Error while sending OTP: " + json.getString("request_id"));

			responseObject.put(Constants.STATUS, Constants.FAILURE);
			responseObject.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);

			return responseObject;

		} else {

			responseObject = BasicUtils.getSuccessTemplateObject();
			responseObject.put("otpReferenceId", json.getString("request_id"));

			return responseObject;

		}

	}

	public JSONObject verifyOTP(String mobileNumber, String countryCode, String OTP) throws Exception {

		JSONObject responseObject = new JSONObject();

		if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

			responseObject.put(Constants.STATUS, Constants.SUCCESS);
			responseObject.put(Constants.MESSAGE, Constants.NA);

			return responseObject;
		}

		String uri = "https://api.msg91.com/api/v5/otp/verify?" + "authkey=" + AUTH_KEY + "&mobile="
				+ (countryCode.replace("+", "") + mobileNumber) + "&otp=" + OTP;

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri.replace(" ", "+"));
		httpPost.addHeader("content-type", "application/x-www-form-urlencoded");

		HttpResponse response = httpClient.execute(httpPost);
		String response_string = EntityUtils.toString(response.getEntity());

		JSONObject json = new JSONObject(response_string);

		LoggerUtils.log("Verify OTP response: " + json);

		if (!json.getString("type").equalsIgnoreCase(Constants.SUCCESS)) {

			String responseMessage = json.optString("message");

			if (responseMessage.equalsIgnoreCase("invalid_otp") || responseMessage.equalsIgnoreCase("otp_not_verified")
					|| responseMessage.equalsIgnoreCase("otp_expired")
					|| responseMessage.equalsIgnoreCase("Mobile no. not found")
					|| responseMessage.equalsIgnoreCase("OTP request invalid")
					|| responseMessage.equalsIgnoreCase("OTP not match")) {

				LoggerUtils.log("Error while Verifying OTP: " + responseMessage);

				responseObject.put(Constants.STATUS, Constants.FAILURE);
				responseObject.put(Constants.MESSAGE, responseMessage);

			} else if (responseMessage.equalsIgnoreCase("already_verified")
					|| responseMessage.equalsIgnoreCase("Mobile no. already verified")) {

				responseObject.put(Constants.STATUS, Constants.SUCCESS);
				responseObject.put(Constants.MESSAGE, Constants.NA);

			} else {

				LoggerUtils.log("Error while Verifying OTP: " + responseMessage);

				responseObject.put(Constants.STATUS, Constants.FAILURE);
				responseObject.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);

			}

			return responseObject;
		} else {

			responseObject.put(Constants.STATUS, Constants.SUCCESS);
			responseObject.put(Constants.MESSAGE, Constants.NA);

			return responseObject;

		}

	}

	public JSONObject resendOTP(String mobileNumber, String countryCode) throws Exception {

		JSONObject responseObject = new JSONObject();

		if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {

			responseObject.put(Constants.STATUS, Constants.SUCCESS);
			responseObject.put(Constants.MESSAGE, Constants.NA);

			return responseObject;
		}

		String uri = "https://api.msg91.com/api/v5/otp/retry?" + "authkey=" + AUTH_KEY + "&mobile="
				+ (countryCode.replace("+", "") + mobileNumber);

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri.replace(" ", "+"));
		httpPost.addHeader("content-type", "application/x-www-form-urlencoded");

		HttpResponse response = httpClient.execute(httpPost);
		String response_string = EntityUtils.toString(response.getEntity());
		JSONObject json = new JSONObject(response_string);

		LoggerUtils.log("Resend OTP response: " + json);

		if (!json.getString("type").equalsIgnoreCase(Constants.SUCCESS)) {

			LoggerUtils.log("Error while Resending OTP: " + json.optString("message"));

			if (json.optString("message").equalsIgnoreCase("invalid_mobile_number")
					|| json.optString("message").equalsIgnoreCase("No OTP request found to retryotp")) {

				responseObject.put(Constants.STATUS, Constants.FAILURE);
				responseObject.put(Constants.MESSAGE, "Invalid mobile number");

			} else {

				responseObject.put(Constants.STATUS, Constants.FAILURE);
				responseObject.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);

			}

			return responseObject;

		} else {

			responseObject.put(Constants.STATUS, Constants.SUCCESS);
			responseObject.put(Constants.MESSAGE, Constants.NA);

			return responseObject;

		}

	}

	public LocalResponse sendSMSViaFlow(JSONObject smsBodyObject) throws Exception {

		String uri = "https://api.msg91.com/api/v5/flow/";

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("authkey", AUTH_KEY);
		httpPost.setHeader("Content-Type", "application/json");

		StringEntity stringEntity = new StringEntity(smsBodyObject.toString());
		httpPost.setEntity(stringEntity);

		HttpResponse response = httpClient.execute(httpPost);
		String response_string = EntityUtils.toString(response.getEntity());
		int responseCode = response.getStatusLine().getStatusCode();
		JSONObject json = new JSONObject(response_string);

		LoggerUtils.log("Send SMS via flow status code : " + responseCode);
		LoggerUtils.log("Send SMS via flow response: " + json);

		if (responseCode == 200 && json.optString("type", Constants.NA).equalsIgnoreCase("success")) {

			return new LocalResponse().setStatus(true).setMessage(json.optString("message", Constants.NA));

		} else {

			String errorMessage = json.optString("message", Constants.NA);
			LoggerUtils.log("Error while sending SMS via flow : " + errorMessage);
			return new LocalResponse().setStatus(false).setMessage(Constants.DEFAULT_ERROR_MESSAGE)
					.setError(errorMessage);

		}

	}
	
	public LocalResponse sendWhatsAppMessage(JSONObject requestJson) throws Exception {
		
		var lResponse = new LocalResponse();
		
		final var url = Endpoints.SEND_WHATSAPP_MESSAGE.getFullUrl();

		final var hfosResponse = hfoSpringNetworkingClient().POST(url, requestJson);
		
		LoggerUtils.log("sendWhatsAppMessage - response: " + hfosResponse.stringEntity);
		
		lResponse.isSuccess = hfosResponse.isSuccess;
		lResponse.message = hfosResponse.stringEntity;
		
		return lResponse;

	}

}