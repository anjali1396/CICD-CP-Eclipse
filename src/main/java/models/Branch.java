package models;

import org.json.JSONException;
import org.json.JSONObject;

import utils.Constants;

public class Branch {

	public String addressLine1 = Constants.NA;
	public String addressLine2 = Constants.NA;
	public String city = Constants.NA;
	public String lat = Constants.NA;
	public String longi = Constants.NA;
	public String pincode = Constants.NA;
	public String primarynumber = Constants.NA;
	public String secondarynumber = Constants.NA;
	public String state = Constants.NA;
	public String status = Constants.NA;
	public String name = Constants.NA;
	
	
	
	

	
	public Branch() {}
	
	public JSONObject getSummaryJson() throws JSONException {
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("addressLine1", addressLine1);
		jsonObject.put("addressLine2", addressLine2);
		jsonObject.put("city", city);
		jsonObject.put("lat", lat);
		jsonObject.put("longi", longi);
		jsonObject.put("postalCode", pincode);
		jsonObject.put("primarynumber", primarynumber);
		jsonObject.put("secondarynumber", secondarynumber);
		jsonObject.put("state", state);
		jsonObject.put("status", status);
		jsonObject.put("name", name);
		
		
		return jsonObject;
		
	}
	
	
	
}
