package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import utils.Constants;

@Entity
@Table(name = "secondary_info", schema = "HomeFirstCustomerPortal")
public class SecondaryInfo {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	public int id = -1;
	
	@Column(name = "user_id")
	public Integer userId = -1;
	
	@Column(name = "login_info")
	public String loginInfo = Constants.NA;
	
	@Column(name = "device_type")
	public String deviceType = Constants.NA;
	
	@Column(name = "device_id")
	public String deviceId = Constants.NA;
	
	@Column(name = "apns_key")
	public String apnsKey = Constants.NA;
	
	@Column(name = "fcm_key")
	public String fcmKey = Constants.NA;
	
	@Column(name = "password_change_datetime")
	public String passwordChangeDatetime = Constants.NA;
	
	@Column(name = "mobile_number_change_datetime")
	public String mobileNumberChangeDatetime = Constants.NA;

	public SecondaryInfo() {
	}

}
