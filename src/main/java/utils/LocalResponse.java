package utils;

import org.json.JSONObject;

public class LocalResponse {

	public boolean isSuccess = false;
	public String message = Constants.DEFAULT_ERROR_MESSAGE;
	public String error = Constants.NA;
	public String action = Constants.NA;
	
	public LocalResponse() {}
	public LocalResponse(
			boolean isSuccess,
			String message,
			String error
	) {
		this.isSuccess = isSuccess;
		this.message = message;
		this.error = error;
	}
	
	public LocalResponse(		
			String error
	) {
		this.error = error;
	}
	
	public LocalResponse(JSONObject json) {
		
		if (null == json) return;
				
		isSuccess = json.optBoolean(Constants.SUCCESS, false);
		message = json.optString(Constants.MESSAGE, Constants.NA);
		error = json.optString(Constants.ERROR, Constants.NA);
		action = json.optString(Constants.ACTION, Constants.NA);
		
	}
	
	public LocalResponse setStatus(Boolean status) {
		this.isSuccess = status;
		return this;
	}
	
	public LocalResponse setMessage(String message) {
		this.message = message;
		return this;
	}
	
	public LocalResponse setError(String error) {
		this.error = error;
		return this;
	}
	
	public LocalResponse setAction(String action) {
		this.action = action;
		return this;
	}
	
	public JSONObject toJson() {
		
		JSONObject jsonObject = new JSONObject();
		
		try {
			
			jsonObject.put(Constants.STATUS, isSuccess ? Constants.SUCCESS : Constants.FAILURE);
			jsonObject.put(Constants.MESSAGE, message);
			jsonObject.put(Constants.ERROR, error);
			jsonObject.put(Constants.ACTION, action);
			
		} catch (Exception e) {
			System.out.println("error while generating json object from LocalResponse: " + e.toString());
		}
		
		return jsonObject;
		
	}
	
	public JSONObject toNewJson() {

		JSONObject jsonObject = new JSONObject();

		try {

			jsonObject.put(Constants.STATUS, isSuccess ? Constants.SUCCESS : Constants.FAILURE);
			jsonObject.put(Constants.MESSAGE, message);
			jsonObject.put(Constants.ERROR, error);
			jsonObject.put(Constants.ACTION, action);

		} catch (Exception e) {
			System.out.println("error while generating json object from LocalResponse: " + e.toString());
		}

		return jsonObject;

	}
	
}
