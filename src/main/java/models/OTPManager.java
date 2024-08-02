package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import utils.DateTimeUtils;


public class OTPManager {
	
	public String id;
	
	public String otp;
	public String mobileNumber;
	public String token;
	public String transactionId;
	public int userId;	
	
	public boolean isVerified = false;	


}

