package models.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;

import utils.BasicUtils;
import utils.ColumnsNFields;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LoggerUtils;
import utils.ProptertyUtils;
import v2.dbhelpers.HFPartnerDBHelper;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;

public class HFPartner {
	
	public int id = -1;
	public String orgName = Constants.NA;
	public String orgId = Constants.NA;
	public String destination = Constants.NA;
	public String leadSource = Constants.NA;
	public String clientId = Constants.NA;
	public String clientSecret = Constants.NA;
	public ArrayList<String> servicesAllowed = new ArrayList<String>();
	public boolean isEnabled = false;
	public boolean ipRestricted = false;
	public String sessionPasscode = Constants.NA;
	public String sessionValidDatetime = Constants.NA;
	public String sessionUpdateDatetime = Constants.NA;
	public String createDatetime = Constants.NA;
	public String updateDatetime = Constants.NA;
	
	public HFPartner() {}
	
	public HFPartner(ResultSet rs) throws SQLException {
		
		id = rs.getInt(ColumnsNFields.COMMON_KEY_ID);
		orgName = rs.getString(ColumnsNFields.PartnerColumn.ORG_NAME.value);
		orgId = rs.getString(ColumnsNFields.COMMON_KEY_ORG_ID);
		destination = rs.getString(ColumnsNFields.PartnerColumn.DESTINATION.value);
		leadSource = rs.getString(ColumnsNFields.PartnerColumn.LEAD_SOURCE.value);
		clientId = rs.getString(ColumnsNFields.PartnerColumn.CLIENT_ID.value);
		clientSecret = rs.getString(ColumnsNFields.PartnerColumn.CLIENT_SECRET.value);
		
		String serviceJson = rs.getString(ColumnsNFields.PartnerColumn.SERVICES_ALLOWED.value);
		if (BasicUtils.isNotNullOrNA(serviceJson) && serviceJson.startsWith("[")) {
			JSONArray serviceArray = new JSONArray(serviceJson);
			for (int i = 0; i < serviceArray.length(); i++)
				servicesAllowed.add(serviceArray.getString(i));
		}
		
		isEnabled = rs.getBoolean(ColumnsNFields.COMMON_KEY_IS_ENABLED);
		ipRestricted = rs.getBoolean(ColumnsNFields.PartnerColumn.IP_RESTRICTED.value);
		sessionPasscode = rs.getString(ColumnsNFields.COMMON_KEY_SESSION_PASSCODE);
		sessionValidDatetime = rs.getString(ColumnsNFields.COMMON_KEY_SESSION_VALID_DATETIME);
		sessionUpdateDatetime = rs.getString(ColumnsNFields.COMMON_KEY_SESSION_UPDATE_DATETIME);
		createDatetime = rs.getString(ColumnsNFields.COMMON_KEY_CREATEDATETIME);
		updateDatetime = rs.getString(ColumnsNFields.COMMON_KEY_UPDATE_DATETIME);
		
	}
	
	public boolean isSessionValid() throws ParseException {
		
		if (
				BasicUtils.isNotNullOrNA(sessionPasscode)
				&& BasicUtils.isNotNullOrNA(sessionValidDatetime)
		) {
	
			Date currentDatetime = DateTimeUtils.getDateFromDateTimeString(
					DateTimeUtils.getCurrentDateTimeInIST(), 
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss
				);
			
			Date sessionValidDate = DateTimeUtils.getDateFromDateTimeString(
					sessionValidDatetime, 
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss
				);		
			
			return currentDatetime.before(sessionValidDate);
			
		} else return false;
		
	}
	
	public boolean updateSession(boolean shouldCreateNew) {
		
		HFPartnerDBHelper hfpDbHelper = new HFPartnerDBHelper();
		
		try {
			
			if (shouldCreateNew) {
//				sessionPasscode = ProptertyUtils.getKeyBearer()
//						.encrypt(orgId + BasicUtils.getRandomKey() + id);
				
				if (!isSessionValid()) {

					sessionPasscode = ProptertyUtils.getKeyBearer()
							.encrypt(orgId + BasicUtils.getRandomKey() + id);

				}

			}
			
			sessionUpdateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
			sessionValidDatetime = DateTimeUtils.getDateTimeAddingHours(1, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
					DateTimeZone.IST);
						
			boolean status = hfpDbHelper.updatePartnerSession(this);
			hfpDbHelper.close();
			
			if (status) LoggerUtils.log("Session updated successfully.");
			else LoggerUtils.log("Failed to update session.");
			
			return status;
			
		} catch (Exception e) {
			
			hfpDbHelper.close();
			LoggerUtils.log("Error while updating partner's session: " + e.getMessage());
			e.printStackTrace();
			return false;
			
		}		
		
	}

}
