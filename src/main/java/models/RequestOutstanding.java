package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import models.payment.AmountVariables;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LoggerUtils;

@Entity
@Table(name = "`RequestOutstanding`")
public class RequestOutstanding {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id;
	
	public int userId = -1;
	
	@Transient
	public JSONObject selectedLoanAccount;
	
	public String requestReason;

	public String loanAccountNumber;
	
	@ColumnDefault("0")
	public boolean isNotified = false;
	
	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String validityDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	public RequestOutstanding initForRO(JSONObject json) {

		if (null != json) {
		
			userId = json.optInt("userId", -1);
			requestReason = json.optString("requestReason", Constants.NA);
			loanAccountNumber = json.optString("loanAccountNumber", Constants.NA);
			selectedLoanAccount = json.getJSONObject("selectedLoanAccount");

		
		}
		return this;

	}
	
	
	


}
