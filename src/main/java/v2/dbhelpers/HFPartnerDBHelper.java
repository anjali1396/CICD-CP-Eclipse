package v2.dbhelpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import dao.DataProvider;
import models.admin.HFPartner;
import models.admin.PartnerLog;
import utils.BasicUtils;
import utils.ColumnsNFields;
import utils.LoggerUtils;
import utils.ColumnsNFields.WhiteListColumn;

public class HFPartnerDBHelper {
	
	
	private Connection connection = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public HFPartnerDBHelper() {
	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++ //
	// +++++++++++++ START OF COMMON METHODS ++++++++++++++++++ //
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++ //

	public void close() {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
			resultSet = null;
		}
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
			preparedStatement = null;
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
			connection = null;
		}
	}

	private void checkAndConnect() throws SQLException {
		if (null == connection || !connection.isValid(10))
			connection = DataProvider.getDataSource().getConnection();
	}

	// -------------------------------------------------------- //
	// --------------- END OF COMMON METHODS ------------------ //
	// -------------------------------------------------------- //

	
	public boolean addPartnerLog(PartnerLog pLog) throws SQLException {

		checkAndConnect();

		String query = "INSERT INTO `HomeFirstCustomerPortal`.`PartnerLog` (" + "`id`," + "`orgId`," + "`serviceName`,"
				+ "`responseStatus`," + "`ipAddress`," + "`datetime`" + ") VALUES (?,?,?,?,?,?)";

		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, BasicUtils.generateUUID());
		preparedStatement.setString(2, pLog.orgId);
		preparedStatement.setString(3, pLog.serviceName);
		preparedStatement.setInt(4, pLog.responseStatus);
		preparedStatement.setString(5, pLog.ipAddress);
		preparedStatement.setString(6, pLog.datetime);

		boolean status = preparedStatement.executeUpdate() == 1;

		if (status)
			LoggerUtils.log("Partner log added successfully.");
		else
			LoggerUtils.log("Failed to add Partner log.");

		return status;

	}

	
	public boolean updatePartnerSession(HFPartner hfPartner) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("update ");
		sb.append(ColumnsNFields.getTableName(ColumnsNFields.TABLE_PARTNER));
		sb.append(" set ");
		sb.append(ColumnsNFields.COMMON_KEY_SESSION_PASSCODE + "=?,");
		sb.append(ColumnsNFields.COMMON_KEY_SESSION_UPDATE_DATETIME + "=?,");
		sb.append(ColumnsNFields.COMMON_KEY_SESSION_VALID_DATETIME + "=?");
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_ID + "=?");

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setString(1, hfPartner.sessionPasscode);
		preparedStatement.setString(2, hfPartner.sessionUpdateDatetime);
		preparedStatement.setString(3, hfPartner.sessionValidDatetime);
		preparedStatement.setInt(4, hfPartner.id);

		return preparedStatement.executeUpdate() == 1;

	}

	public HFPartner getPartnerInfo(String orgId) throws SQLException {

		checkAndConnect();

		StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(ColumnsNFields.getTableName(ColumnsNFields.TABLE_PARTNER));
		sb.append(" where ");
		sb.append(ColumnsNFields.COMMON_KEY_ORG_ID);
		sb.append("=?");

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setString(1, orgId);

		resultSet = preparedStatement.executeQuery();

		if (null != resultSet && resultSet.first())
			return new HFPartner(resultSet);

		return null;

	}

	public boolean isPartnerIPAllowed(String orgId, String ipAddress) throws SQLException {

		checkAndConnect();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select ");
		sb.append(WhiteListColumn.IS_ACTIVE.value);
		sb.append(" from ");
		sb.append(ColumnsNFields.getTableName(ColumnsNFields.TABLE_WHITELISTED_IP));
		sb.append(" where ");
		sb.append(WhiteListColumn.ORG_ID.value + "=?");
		sb.append(" and ");
		sb.append(WhiteListColumn.IP_ADDRESS.value + "=?");

		preparedStatement = connection.prepareStatement(sb.toString());
		preparedStatement.setString(1, orgId);
		preparedStatement.setString(2, ipAddress);

		resultSet = preparedStatement.executeQuery();

		if (null != resultSet && resultSet.first())
			return resultSet.getBoolean(WhiteListColumn.IS_ACTIVE.value);

		return false;

	}
	

}
