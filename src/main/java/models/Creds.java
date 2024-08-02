package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import utils.DateTimeUtils;

@Entity
@Table(name = "`Creds`")
public class Creds {
	
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id;
	
	@Column(nullable = false)
	public String partnerName;
	
	public String credType;
	public String username;
	public String password;
	public String memberId;
	public String memberPasscode;
	public String salt;
	public String apiKey;
	public String apiUrl;
	
	@ColumnDefault("0")
	public boolean isValid = false;
	
	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

}
