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
@Table(name = "`AdBannerImage`")
public class AdBannerImage {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id;
	
	
	@Column(name = "image_url")
	public String imageUrl = Constants.NA;
	
	@ColumnDefault("0")
	public boolean isActive = false;
	
	@Column(name = "clickUrl")
	public String clickUrl = Constants.NA;
	
	
	@Column(name = "webImageUrl")
	public String webImageUrl = Constants.NA;
	
	
	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

}
