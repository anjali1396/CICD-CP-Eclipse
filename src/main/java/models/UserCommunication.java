package models;

import utils.Constants;

public class UserCommunication {

	public int id = -1;
	public int userId = -1;
	public String recordType = Constants.NA;
	public int recordId = -1;
	public String hfoId = Constants.NA;
	public boolean isEmail = false;
    public String emailDatetime = Constants.NA;
	public boolean isSMS = false;
    public String smsDatetime = Constants.NA;
	public boolean isNotification = false;
    public String notificationDatetime = Constants.NA;
    public String createDatetime = Constants.NA;
	
    public UserCommunication() {}

}
