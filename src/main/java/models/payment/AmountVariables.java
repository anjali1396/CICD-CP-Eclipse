package models.payment;

import org.json.JSONObject;

import utils.PaymentUtils.AmountType;

public class AmountVariables {

	public boolean isAmountVariableApplicable = false;
	public AmountVariable emiAmount = new AmountVariable(AmountType.EMI_AMOUNT.value);
	public AmountVariable insuranceAmount = new AmountVariable(AmountType.INSURANCE_AMOUNT.value);
	public AmountVariable bounceCharges = new AmountVariable(AmountType.BOUNCE_CHARGES.value);
	
	public AmountVariables() {}
	
	public AmountVariables(JSONObject json) {
		if (json != null) {
			isAmountVariableApplicable = json.optBoolean("isAmountVariableApplicable", isAmountVariableApplicable);
			emiAmount = new AmountVariable(json.optJSONObject("emiAmount"));
			insuranceAmount = new AmountVariable(json.optJSONObject("insuranceAmount"));
			bounceCharges = new AmountVariable(json.optJSONObject("bounceCharges"));
		}
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		
		json.put("isAmountVariableApplicable", isAmountVariableApplicable);
		json.put("emiAmount", emiAmount.toJson());
		json.put("insuranceAmount", insuranceAmount.toJson());
		json.put("bounceCharges", bounceCharges.toJson());
		
		return json;
	}
	
}
