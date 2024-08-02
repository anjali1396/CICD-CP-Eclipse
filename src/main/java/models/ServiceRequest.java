package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.json.JSONObject;

import utils.Constants;
import utils.DateTimeUtils;

@Entity
@Table(name = "`ServiceRequest`")
public class ServiceRequest {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id;

	public String caseId;
	public String caseType;
	public String masterCaseReason;
	public String caseReasonSub;
	public String status;

	public String origin;
	public String subject;

	public String description;
	public String priority;

	@Transient
	public String attachmentFile;

	public String attachmentName;

	public String loanAccountNumber;

	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	public ServiceRequest initForSR(JSONObject json) {

		if (null != json) {

			subject = json.optString("Subject", Constants.NA);
			caseType = json.optString("Case_Type__c", Constants.NA);
			masterCaseReason = json.optString("Master_Case_Reason__c", Constants.NA);
			caseReasonSub = json.optString("Case_Reason_sub__c", Constants.NA);
			status = json.optString("Status", Constants.NA);
			origin = json.optString("Origin", Constants.NA);
			description = json.optString("Description", Constants.NA);
			priority = json.optString("Priority", Constants.NA);
			loanAccountNumber = json.optString("loanAccountNumber", Constants.NA);
			attachmentFile = json.optString("attachment", Constants.NA);
			attachmentName = json.optString("attachmentName", Constants.NA);

		}
		return this;

	}

}
