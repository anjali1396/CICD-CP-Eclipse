package models.notification;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import utils.DateTimeUtils;

@Entity
@Table(name = "`user_notification_info`")
public class UserNotificationInfo {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false)
	public String id;	
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "notification_id")
	public CPNotification notificationId;
	
//    @Column(name = "notification_id", nullable = false)
//    public int notificationId;
    
    @Column(name = "user_id", nullable = false)
    public int userId;
    
    @Column(name = "has_read", columnDefinition = "BOOLEAN default false")
    public boolean hasRead = false;
    
    @Column(name = "read_datetime")
    public String readDatetime;
    
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
    
    @Column(name = "dynamic_title")
    public String dynamicTitle;
    
    @Column(name = "dynamic_message", length = 512)
    public String dynamicMessage;
    
    @Column(name = "dynamic_web_url")
    public String dynamicWebUrl;
    
    @Column(name = "create_datetime", columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();
    
    @Column(name = "update_datetime", columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
	
}
