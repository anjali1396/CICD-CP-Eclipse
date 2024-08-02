package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;

import utils.DateTimeUtils;


@Entity
@Table(name = "login_info", schema = "HomeFirstCustomerPortal")
public class LoginInfo {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id", updatable = false, nullable = true)
	public String id = null;

	@Column(name = "user_id")
	public int userId = -1;

	@Column(name = "login_datetime")
	public String loginDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(name = "ip_address")
	public String ipAddress;

	@Column(name = "device_id")
	public String deviceId;

	@Column(name = "device_type")
	public String deviceType;

	@Column(name = "device_model")
	public String deviceModel;

	@Column(name = "app_version")
	public String appVersion;

	@Column(name = "os_version")
	public String osVersion;
}
