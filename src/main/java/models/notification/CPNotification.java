package models.notification;

import java.text.ParseException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.json.JSONObject;

import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.LoggerUtils;
import utils.NotificationUtils;

@Entity
@Table(name = "`notification`")
public class CPNotification implements Comparable<CPNotification> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false)
	public int id;

	@Column(name = "cn_id")
	public String cnId; // cross notification id
	public String origin;
	public String event;

	@Column(name = "title")
	public String title = Constants.NA;

	@Column(name = "message")
	public String message = Constants.NA;

	@Column(name = "big_message")
	public String bigMessage = Constants.NA;

	@Column(name = "image_url")
	public String imageUrl = Constants.NA;

	@Column(name = "thumbnail_url")
	public String thumbnailUrl = Constants.NA;

	@Column(name = "web_url")
	public String webUrl = Constants.NA;

	@Column(name = "should_schedule", columnDefinition = "BOOLEAN default false")
	public boolean shouldSchedule = false;

	@Column(name = "audience_type")
	public String audienceType = Constants.NA;

	@Column(name = "has_dynamic_contect", columnDefinition = "BOOLEAN default false")
	public boolean hasDynamicContent = false;

	@Column(name = "audience_group", columnDefinition = "JSON")
	public String audienceGroup;

	@Column(name = "on_click_action")
	public String onClickAction = Constants.NA;

	@Column(name = "screen_to_open")
	public String screenToOpen = Constants.NA;

	@Column(name = "deeplink")
	public String deeplink = Constants.NA;

	@Column(name = "data", columnDefinition = "JSON")
	public String data = Constants.NA;

//	@Column(name = "data", columnDefinition = "JSON")
//	public String _data  = Constants.NA;

	@Column(name = "priority")
	public String priority = Constants.NA;

	@Column(name = "kind")
	public String kind = NotificationUtils.NotificationKind.TRANSACTIONAL.value;

	@Transient
	public boolean hasRead = false;

	@Column(name = "is_scheduled", columnDefinition = "BOOLEAN default false")
	public boolean isScheduled = false;

	@Column(name = "create_datetime", columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(name = "update_datetime", columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(name = "schedule_datetime", columnDefinition = "DATETIME")
	public String datetime;

	@Column(name = "sent_datetime", columnDefinition = "DATETIME")
	public String sentDatetime;

	@Column(name = "platform")
	public String platform;

	@Column(name = "schedule_type")
	public String scheduleType;

	@Column(name = "total_count")
	public int totalCount = 0;

	@Column(name = "success_count")
	public int successCount = 0;

	@Column(name = "failure_count")
	public int failureCount = 0;

	@Column(name = "scheduler_id")
	public int schedulerId = 0;

	@Column(name = "scheduler_name")
	public String schedulerName;

	@Column(name = "is_valid", columnDefinition = "BOOLEAN default true")
	public boolean isValid = true;

	@Transient
	public String file;

	@Transient
	public String imageFile;

	@Transient
	public String thumbnailFile;

	public CPNotification() {
	}

//    public CPNotification(JSONObject json) {
//    
//    	id = json.optInt("id", id);
//    	title = json.optString("title", title);
//        message = json.optString("message", message);
//        bigMessage = json.optString("bigMessage", bigMessage);
//        imageUrl = json.optString("imageUrl", imageUrl);
//        datetime = json.optString("datetime", datetime);
//        webUrl = json.optString("webUrl", webUrl);
//        shouldSchedule = json.optBoolean("shouldSchedule", shouldSchedule);
//        audienceType = json.optString("audienceType", audienceType);
//        onClickAction = json.optString("onClickAction", onClickAction);
//        deeplink = json.optString("deeplink", deeplink);
//        screenToOpen = json.optString("screenToOpen", screenToOpen);
//
//        JSONObject dataObject = json.optJSONObject("data");
//        if (null != dataObject) {
//        	if (dataObject.has("nameValuePairs"))
//        		data = dataObject.getJSONObject("nameValuePairs").toString();
//        	else data = dataObject.toString(); 
//        }
//        
//        JSONObject targetObject = json.optJSONObject("audienceGroup");
//        if (null != targetObject) {
//        	if (targetObject.has("nameValuePairs"))
//        		audienceGroup = targetObject.getJSONObject("nameValuePairs").toString();
//        	else audienceGroup = targetObject.toString(); 
//        }
//
//        priority = json.optString("priority", priority);
//        kind = json.optString("kind", kind);
//    	
//    }

	public JSONObject toJson() {

		JSONObject json = new JSONObject();

		json.put("id", id);
		json.put("title", title);
		json.put("message", message);
		json.put("bigMessage", bigMessage);
		json.put("imageUrl", imageUrl);
		json.put("thumbnailUrl", thumbnailUrl);
		json.put("datetime", datetime);
		json.put("webUrl", webUrl);
		json.optBoolean("shouldSchedule", shouldSchedule);
		json.put("audienceType", audienceType);
		json.put("audienceGroup", audienceGroup);
		json.put("onClickAction", onClickAction);
		json.put("screenToOpen", screenToOpen);
		json.put("deeplink", deeplink);
		if (BasicUtils.isNotNullOrNA(data) && data.startsWith("{"))
			json.put("data", new JSONObject(data));

		json.put("priority", priority);
		json.put("kind", kind);
		json.put("hasRead", hasRead);

		return json;

	}

	public CPNotification asCopy() {

		CPNotification copy = new CPNotification();

		copy.id = id;
		copy.title = title;
		copy.message = message;
		copy.bigMessage = bigMessage;
		copy.imageUrl = imageUrl;
		copy.thumbnailUrl = thumbnailUrl;
		copy.datetime = datetime;
		copy.webUrl = webUrl;
		copy.shouldSchedule = shouldSchedule;
		copy.audienceType = audienceType;
		copy.audienceGroup = audienceGroup;
		copy.onClickAction = onClickAction;
		copy.screenToOpen = screenToOpen;
		copy.deeplink = deeplink;
		copy.data = data;
		copy.priority = priority;
		copy.kind = kind;
		copy.hasRead = hasRead;

		return copy;

	}

	@Override
	public int compareTo(CPNotification o) {
		try {
			Date scheduleDateA = DateTimeUtils.getDateFromDateTimeString(datetime, DateTimeFormat.yyyy_MM_dd_HH_mm_ss);
			Date scheduleDateB = DateTimeUtils.getDateFromDateTimeString(o.datetime,
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss);

			if (scheduleDateA.before(scheduleDateB))
				return 1;
			else if (scheduleDateA.after(scheduleDateB))
				return -1;
			else
				return 0;

		} catch (ParseException e) {
			LoggerUtils.log("Error while comparing schedule datetime of notificaton: " + e.getMessage());
			e.printStackTrace();
		}

		return 0;
	}

}

//	public int id = 0;
//	public String title = Constants.NA;
//	public String message = Constants.NA;
//	public String bigMessage = Constants.NA;
//	public String imageUrl = Constants.NA;
//	public String webUrl = Constants.NA;
//	public boolean shouldSchedule = false;
//	public String audienceType = Constants.NA;
//	public JSONObject audienceGroup = new JSONObject();
//	public String onClickAction = Constants.NA;
//	public String screenToOpen = Constants.NA;
//	public String deeplink = Constants.NA;
//	public JSONObject data = new JSONObject();
//	public String priority = Constants.NA;
//	public String kind = Constants.NA;
//	public boolean hasRead = false;
//
//	public String createDatetime = Constants.NA;
//	public String datetime = Constants.NA; // SCHEDULE DATETIME
//	public String sentDatetime = Constants.NA;
//
//	public CPNotification() {
//	}
//
//	public CPNotification(JSONObject json) {
//
//		id = json.optInt("id", id);
//		title = json.optString("title", title);
//		message = json.optString("message", message);
//		bigMessage = json.optString("bigMessage", bigMessage);
//		imageUrl = json.optString("imageUrl", imageUrl);
//		datetime = json.optString("datetime", datetime);
//		webUrl = json.optString("webUrl", webUrl);
//		shouldSchedule = json.optBoolean("shouldSchedule", shouldSchedule);
//		audienceType = json.optString("audienceType", audienceType);
//		onClickAction = json.optString("onClickAction", onClickAction);
//		deeplink = json.optString("deeplink", deeplink);
//		screenToOpen = json.optString("screenToOpen", screenToOpen);
//
//		JSONObject dataObject = json.optJSONObject("data");
//		if (null != dataObject) {
//			if (dataObject.has("nameValuePairs"))
//				data = dataObject.getJSONObject("nameValuePairs");
//			else
//				data = dataObject;
//		}
//
//		JSONObject targetObject = json.optJSONObject("audienceGroup");
//		if (null != targetObject) {
//			if (targetObject.has("nameValuePairs"))
//				audienceGroup = targetObject.getJSONObject("nameValuePairs");
//			else
//				audienceGroup = targetObject;
//		}
//
//		priority = json.optString("priority", priority);
//		kind = json.optString("kind", kind);
//
//	}
//
//	public JSONObject toJson() {
//
//		JSONObject json = new JSONObject();
//
//		json.put("id", id);
//		json.put("title", title);
//		json.put("message", message);
//		json.put("bigMessage", bigMessage);
//		json.put("imageUrl", imageUrl);
//		json.put("datetime", datetime);
//		json.put("webUrl", webUrl);
//		json.optBoolean("shouldSchedule", shouldSchedule);
//		json.put("audienceType", audienceType);
//		json.put("audienceGroup", audienceGroup);
//		json.put("onClickAction", onClickAction);
//		json.put("screenToOpen", screenToOpen);
//		json.put("deeplink", deeplink);
//		json.put("data", data);
//		json.put("priority", priority);
//		json.put("kind", kind);
//		json.put("hasRead", hasRead);
//
//		return json;
//
//	}
//
//	public CPNotification asCopy() {
//
//		CPNotification copy = new CPNotification();
//
//		copy.id = id;
//		copy.title = title;
//		copy.message = message;
//		copy.bigMessage = bigMessage;
//		copy.imageUrl = imageUrl;
//		copy.datetime = datetime;
//		copy.webUrl = webUrl;
//		copy.shouldSchedule = shouldSchedule;
//		copy.audienceType = audienceType;
//		copy.audienceGroup = audienceGroup;
//		copy.onClickAction = onClickAction;
//		copy.screenToOpen = screenToOpen;
//		copy.deeplink = deeplink;
//		copy.data = data;
//		copy.priority = priority;
//		copy.kind = kind;
//		copy.hasRead = hasRead;
//
//		return copy;
//
//	}
//
//	@Override
//	public int compareTo(CPNotification o) {
//
//		try {
//			Date scheduleDateA = DateTimeUtils.getDateFromDateTimeString(datetime, DateTimeFormat.yyyy_MM_dd_HH_mm_ss);
//			Date scheduleDateB = DateTimeUtils.getDateFromDateTimeString(o.datetime,
//					DateTimeFormat.yyyy_MM_dd_HH_mm_ss);
//
//			if (scheduleDateA.before(scheduleDateB))
//				return 1;
//			else if (scheduleDateA.after(scheduleDateB))
//				return -1;
//			else
//				return 0;
//
//		} catch (ParseException e) {
//			LoggerUtils.log("Error while comparing schedule datetime of notificaton: " + e.getMessage());
//			e.printStackTrace();
//		}
//		
//		return 0;
//
//	}
//
//}
