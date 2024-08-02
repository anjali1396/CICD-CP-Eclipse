package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import utils.Constants;
import utils.DateTimeUtils;

@Entity
@Table(name = "`PromoBanners`")
public class PromoBanners {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id;
	
	public String webUrl;
	
	@Column(name = "image_url")
	public String imageUrl = Constants.NA;
	
	public String webImageUrl = Constants.NA;
	
	@ColumnDefault("-1")
	public int position;
	
	@ColumnDefault("0")
	public boolean isActive = false;
	
	@Column(name = "on_click_action")
	public String onClickAction = Constants.NA;
	
	public String eventLogType = Constants.NA;
	
	public String eventLogValue = Constants.NA;
	
	@Column(name = "screen_to_open")
	public String screenToOpen = Constants.NA;
	
		
	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
	
}
