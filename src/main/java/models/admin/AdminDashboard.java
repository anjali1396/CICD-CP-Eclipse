package models.admin;

import org.json.JSONObject;

import utils.Constants;

public class AdminDashboard {
	
	public String startDatetime = Constants.NA;
	public String endDatetime = Constants.NA;
	public int activeUser = 0;
	public int registeredUser = 0;
	public double totalPaymentAmount = 0;
	public int totalPaymentCount = 0;
	public int totalLeads = 0;
	public int totalReferrers = 0;
	
	public AdminDashboard() {}
	
	public JSONObject toJson() {
		
		JSONObject json = new JSONObject();
		
		json.put("startDatetime", startDatetime);
		json.put("endDatetime", endDatetime);
		json.put("activeUser", activeUser);
		json.put("registeredUser", registeredUser);
		json.put("totalPaymentAmount", totalPaymentAmount);
		json.put("totalPaymentCount", totalPaymentCount);
		json.put("totalLeads", totalLeads);
		json.put("totalReferrers", totalReferrers);
		
		return json;
		
	}

}
