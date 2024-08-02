package v1.dto;

public class NotificationDTO {

	public int id;
	public int userId;
	public String title;
	public String message;
	public String bigMessage;
	public String imageUrl;
	public String thumbnailUrl;
	public String webUrl;
	public String onClickAction;
	public String screenToOpen;
	public String deeplink;
	public String data;
	public String priority;
	public String kind;
	public boolean hasRead;
	public String createDatetime;
	public String datetime;

	public NotificationDTO() {
	}

	public NotificationDTO(int id, int userId, String title, String message, 
			//String bigMessage, 
			String imageUrl,
			String thumbnailUrl, String webUrl, String onClickAction, String screenToOpen, String deeplink, String data,
			String priority, String kind, boolean hasRead, String createDatetime, String datetime) {
		this.id = id;
		this.userId = userId;
		this.title = title;
		this.message = message;
		//this.bigMessage = bigMessage;
		this.imageUrl = imageUrl;
		this.thumbnailUrl = thumbnailUrl;
		this.webUrl = webUrl;
		this.onClickAction = onClickAction;
		this.screenToOpen = screenToOpen;
		this.deeplink = deeplink;
		this.data = data;
		this.priority = priority;
		this.kind = kind;
		this.hasRead = hasRead;
		this.createDatetime = createDatetime;
		this.datetime = datetime;

	}

}
