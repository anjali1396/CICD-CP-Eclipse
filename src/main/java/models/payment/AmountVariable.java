package models.payment;

import org.json.JSONObject;

import utils.Constants;
import utils.PaymentUtils.AmountType;

public class AmountVariable {

	public String type = AmountType.EMI_AMOUNT.value;
	public double amount = 0.0;
	public boolean isIncluded = false;
	public String amountDescription = Constants.NA;
	
	public AmountVariable(String type) {
		this.type = type;
	}
	
	public AmountVariable(JSONObject json) {
		if (null != json) {
			type = json.optString("type", type);
			amount = json.optDouble("amount", amount);
			isIncluded = json.optBoolean("isIncluded", isIncluded);
			amountDescription = json.optString("amountDescription", amountDescription);
		}
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", type);
		json.put("amount", amount);
		json.put("isIncluded", isIncluded);
		json.put("amountDescription", amountDescription);
		return json;
	}
	
}
