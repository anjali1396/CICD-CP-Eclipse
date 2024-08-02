package utils;

public class BasicAuthCreds {

	public String clientId = Constants.NA;
	public String clientSecret = Constants.NA;
	
	public BasicAuthCreds(String clientId, String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}
}
