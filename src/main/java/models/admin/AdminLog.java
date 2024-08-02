package models.admin;

import java.sql.ResultSet;
import java.sql.SQLException;

import utils.ColumnsNFields;
import utils.Constants;

public class AdminLog {
	
	public int id = -1;
	public int userId = -1;
	public String recordType = Constants.NA;
	public int recordId = -1;
	public String action = Constants.NA;
	public String status = Constants.NA;
	public String description = Constants.NA;
    public String dateTime = Constants.NA;
	
    public AdminLog() {}

	public AdminLog(ResultSet resultSet) throws SQLException {

		id = resultSet.getInt(ColumnsNFields.COMMON_KEY_ID);
		userId = resultSet.getInt(ColumnsNFields.COMMON_KEY_USER_ID);
		recordType = resultSet.getString(ColumnsNFields.AdminLogInfoColumn.RECORD_TYPE.value);
		recordId = resultSet.getInt(ColumnsNFields.AdminLogInfoColumn.RECORD_ID.value);
		action = resultSet.getString(ColumnsNFields.AdminLogInfoColumn.ACTION.value);
		status = resultSet.getString(ColumnsNFields.AdminLogInfoColumn.STATUS.value);
		description = resultSet.getString(ColumnsNFields.AdminLogInfoColumn.DESCRIPTION.value);

	}
}
