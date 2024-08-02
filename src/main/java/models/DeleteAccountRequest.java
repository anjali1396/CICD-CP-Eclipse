package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import utils.DateTimeUtils;

@Entity
@Table(name = "`DeleteAccountRequest`")
public class DeleteAccountRequest {

	@Id
	@GeneratedValue(generator = "UUID")
	
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id = null;

	
	public int userId = -1;
	
	public String reason = null;
	
	
	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	
	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();


	public DeleteAccountRequest() {

	}
}
