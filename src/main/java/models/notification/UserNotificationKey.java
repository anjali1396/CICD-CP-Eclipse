package models.notification;

import java.sql.ResultSet;
import java.util.ArrayList;

import models.User;

public class UserNotificationKey {
	
	public User user = new User();
	public ArrayList<RegistrationKey> registerationKeys = new ArrayList<>();
//	public ArrayList<UserNotificationToken> registerationKeys = new ArrayList<>();
	public NotificationCSV customNotification = null;

	
	public UserNotificationKey() {}
	public UserNotificationKey(ResultSet resultSet) {
		
	}

}
