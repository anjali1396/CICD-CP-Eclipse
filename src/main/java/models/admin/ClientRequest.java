package models.admin;


import java.util.Set;

import utils.Constants;

public class ClientRequest {

	public String userId = Constants.NA;
	public String methodName = Constants.NA;
	public String ipAddress = Constants.NA;
	public String sessionPasscode = Constants.NA;
	public String sourcePasscode = Constants.NA;
	public String crownPasscode = Constants.NA;
	public String orgId = Constants.NA;
	public String authorization = Constants.NA;
	public String cypher = Constants.NA;
	public String crypt = Constants.NA;
	public Set<String> rolesSet = null;
	
	public ClientRequest() {
		
	}
	
}
