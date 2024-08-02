package security;

import java.util.ArrayList;
import java.util.Arrays;

public class AllowedResource {
	
	public static ArrayList<String> AllowedMethodsWithSourcePasscode = new ArrayList<String>(
			Arrays.asList(
					"checkAccount", 
					"verifyOTP", 
					"updatePassword", 
					"resendOTP", 
					"Register", 
					"Login",
					"WebLogin",
					"generateOTP", 
					"WebInitiateForgotOrChangePassword",
					"initiateForgotOrChangePassword", 
					"getBranch", 
					"getPromo", 
					"autoLogin"

			));

	public static ArrayList<String> AllowedAdminMethodsWithSourcePasscode = new ArrayList<String>(Arrays.asList("login"));

	public static ArrayList<String> AllowedCronMethodsWithSourcePasscode = new ArrayList<String>(
			Arrays.asList(
					"scheduleCustomerBirthdayNotification", 
					"getUserDetailsToCSV", 
					"rSyncRZPPayoutStatus",
					"rCreateFailedReceiptOnSF",
					"sendRepriceCSV",
					"processRepriceEligibility",
					"processFailedReprice",
					"checkAndNotifyLoanOutstanding"
			));

	public static ArrayList<String> AllowedPartnerMethodsWithDefaultAccess = new ArrayList<String>(
			Arrays.asList("authenticateClient"));

	public static ArrayList<String> AllowedProspectMethodsWithDefaultAccess = new ArrayList<String>(
			Arrays.asList(
					"createProspect", 
					"reAuthenticateProspect", 
					"verifyProspect", 
					"resendOTP"
			));

}
