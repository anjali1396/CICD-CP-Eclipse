package models.notification;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import utils.DateTimeUtils;

@Entity
@Table(name = "`UserNotificationToken`")
public class UserNotificationToken {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false)
	public String id;

	public int userId = -1;

	@Column(nullable = false)
	public String notificationKey = null;

	public String deviceId = null;
	public String deviceType = null;
	public String deviceModel = null;
	
	public String notificationService = null;
	
	@ColumnDefault("1")
	public boolean isValid = true;

	@Column(name = "createDatetime", columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(name = "updateDatetime", columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
	
	
	

	public UserNotificationToken() {
		super();
	}



	public UserNotificationToken(int userId, String notificationKey, String deviceId, String deviceType,
			String deviceModel, String notificationService, boolean isValid) {
		super();
		this.userId = userId;
		this.notificationKey = notificationKey;
		this.deviceId = deviceId;
		this.deviceType = deviceType;
		this.deviceModel = deviceModel;
		this.notificationService = notificationService;
		this.isValid = isValid;
	}	
	

}
