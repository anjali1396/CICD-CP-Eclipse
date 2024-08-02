package dao;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import utils.Constants;

public class DataProvider {
	
	private static DataSource dataSource = null;
	
	public static DataSource getDataSource() {
		
		if (null != dataSource) return dataSource;
		
		try {
			
			Context initContext = new InitialContext();
			Context envContext = (Context) initContext.lookup("java:/comp/env");
			
			// PRODUCTION CONNECTION
			if (Constants.IS_DB_LIVE) dataSource = (DataSource) envContext.lookup("jdbc/hffc_db_server");
			
			// STAGING CONNECTION
			else if (Constants.IS_STAGING) dataSource = (DataSource) envContext.lookup("jdbc/hffc_db_staging_server");
			
			// LOCAL MACHING DB CONNECTION
			else dataSource = (DataSource) envContext.lookup("jdbc/hffc_db_local_server");
			
		} catch (NamingException e) {
			e.printStackTrace();
		}
		
		return dataSource;
		
	}	

}
