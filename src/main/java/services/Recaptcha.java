package services;


import org.json.JSONObject;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import utils.BasicUtils;
import utils.LoggerUtils;

public class Recaptcha {

	 private static final String RECAPTCHA_SERVICE_URL = "https://www.google.com/recaptcha/api/siteverify";
	    private static final String SECRET_KEY = "6LdpZKgZAAAAAJTNdD2VSlhYzk1c2H0p2QpTWsbi";

	   
		public static boolean isValid(String clientRecaptchaResponse) throws Exception {

			try {
				
				if (!BasicUtils.isNotNullOrNA(clientRecaptchaResponse))
					return false;		
				
				final var client = new OkHttpClient().newBuilder().build();
				
				final var urlBuilder  = HttpUrl.parse(RECAPTCHA_SERVICE_URL).newBuilder();
			    urlBuilder.addQueryParameter("secret", SECRET_KEY);
			    urlBuilder.addQueryParameter("response", clientRecaptchaResponse);
				
			    final var url = urlBuilder.build().toString();
			    
			    System.out.println("url is : " + url);
			    
			    final var request = new Request.Builder().url(url).build();
			    final var vcResponse = client.newCall(request).execute();
				
			    final var responseCode = vcResponse.code();
			    final var responseEntity = vcResponse.body().string().toString();
			    
			    vcResponse.body().close();
			    vcResponse.close();	
			    
			    
			    if (responseCode != 200) { 
			    	LoggerUtils.log("Recaptcha.isValid - Response body: " + responseEntity + "  " +responseCode);
			    	return false;
			    }
			    
			    
				final var vcJsonResponse = new JSONObject(responseEntity);
				
				final var success = vcJsonResponse.optBoolean("success", false);
				
				final var score = vcJsonResponse.optDouble("score", 0);
				
				//result should be sucessfull and spam score above 0.5
				if (success && score >= 0.5)
					return true;

			} catch (Exception e) {
				LoggerUtils.log("Error while verifying captcha : " + e.getMessage());
				e.printStackTrace();
			
			}
			
			return false;
			
			
		}
	    
	    	    
}
