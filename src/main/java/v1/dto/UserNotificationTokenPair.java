package v1.dto;

import models.User;
import models.notification.NotificationCSV;
import models.notification.UserNotificationToken;

public class UserNotificationTokenPair {
	
	public User user;
	public UserNotificationToken userNotificationToken;
	public NotificationCSV customNotification = null;
	
	public UserNotificationTokenPair() {}
	
	public UserNotificationTokenPair(Object[] unkObject) {		
		
		user = new User();
		user.userId = (Integer) unkObject[0];
		user.name = (String) unkObject[1];
		user.mobileNumber = (String) unkObject[2];
		
		
		userNotificationToken = new UserNotificationToken();
		userNotificationToken.notificationKey = (String) unkObject[3];
		userNotificationToken.userId = user.userId;
		userNotificationToken.id = (String) unkObject[4];
		userNotificationToken.deviceId = (String) unkObject[5];		
		
		user.loanAccountNumber = (String) unkObject[6];
	}
	
}
