package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.json.JSONException;
import org.json.JSONObject;

import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;


@Entity
@Table(name = "user_request",schema = "HomeFirstCustomerPortal")
public class UserRequest {

	public UserRequest() {
	}
	@Id
	@Column(name = "id")
	public String id = Constants.NA;
	@Column(name = "user_id")
	public int userId = -1;
	@Column(name = "token")
	public String token = Constants.NA;
	@Column(name = "is_valid")
	public boolean isValid = false;
	@Column(name = "type")
	public String type = Constants.NA;
	@Column(name = "description")
	public String description = Constants.NA;
	@Column(name = "status")
	public String status = Constants.NA;
	@Column(name = "created_datetime")
	public String createdDateTime = Constants.NA;
	@Column(name = "updated_datetime")
	public String updatedDateTime = Constants.NA;
	@Column(name = "valid_datetime")
	public String validDatetime = Constants.NA;
	@Column(name = "ip_address")
	public String ipAddress = Constants.NA;
	
	
	@Column(name = "otp_transaction_id")
	public String otpTransactionId = Constants.NA;
	
	@Column(name = "otp_token")
	public String otpToken = Constants.NA;
 
	public UserRequest(JSONObject jsonObject) throws JSONException {

		id = jsonObject.optString("id", Constants.NA);
		userId = jsonObject.optInt("userId", -1);
		token = jsonObject.optString("token", Constants.NA);
		isValid = jsonObject.optBoolean("isValid", false);
		type = jsonObject.optString("type", Constants.NA);
		description = jsonObject.optString("description", Constants.NA);
		status = jsonObject.optString("status", Constants.NA);
		createdDateTime = jsonObject.optString("createdDateTime", Constants.NA);
		updatedDateTime = jsonObject.optString("updatedDateTime", Constants.NA);
		validDatetime = jsonObject.optString("validDatetime", Constants.NA);

	}

	public JSONObject toJson() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		jsonObject.put("userId", userId);
		jsonObject.put("token", token);
		jsonObject.put("isValid", isValid);
		jsonObject.put("type", type);
		jsonObject.put("description", description);
		jsonObject.put("status", status);
		jsonObject.put("createdDateTime", createdDateTime);
		jsonObject.put("updatedDateTime", updatedDateTime);
		jsonObject.put("validDatetime", validDatetime);

		return jsonObject;

	}
	
	public boolean isOpen() {
		return status.equals(Constants.OPEN);
	}

	public boolean isDateValid() throws Exception {
		
		if (null == validDatetime || validDatetime.equalsIgnoreCase(Constants.NA))
			return false;
		
		Date currentDateTime = DateTimeUtils.getDateFromString(
				DateTimeUtils.getCurrentDateTimeInIST(), 
				DateTimeFormat.yyyy_MM_dd_HH_mm_ss
		);
		
		Date validDateTime = DateTimeUtils.getDateFromString(
				validDatetime, 
				DateTimeFormat.yyyy_MM_dd_HH_mm_ss
		);
		
		return currentDateTime.before(validDateTime);		
	}
}
