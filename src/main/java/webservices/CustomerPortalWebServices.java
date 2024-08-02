package webservices;

import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import dao.PaymentManager;
import dao.UserManager;
import models.User;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;

@Path("/CustomerPortalServices")
public class CustomerPortalWebServices {

	private Logger logger =  Logger.getLogger(CustomerPortalWebServices.class.getSimpleName());
	
	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {
		  
		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");
		
	    return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>"
	        + "<body><h1>" + "Welcome to HFFC Customer Portal Web Services!" + "</h1></body>" + "</html> ";
	}
	
	/**
	 * @deprecated use the webservices CustomerPortalWSV2 login
	 */
	
	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response Login(String body, 
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
		
		LoggerUtils.logMethodCall("Login");
		//LoggerUtils.logBody(body);
		
		try {
			
			JSONObject bodyObject = new JSONObject(body); 
			UserManager userManager = new UserManager();
			
			String responseString = userManager.performLogin(userId,bodyObject).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while login user: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}
	
	/**
	 * @deprecated use the webservices CustomerPortalWSV2 register
	 */
	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/register")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response Register(String body,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
				
		LoggerUtils.logMethodCall("Register");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();

			JSONObject bodyObject = new JSONObject(body);			
			String responseString = userManager.performRegisteration(bodyObject).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while registering user: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}
	
	/**
	 * @deprecated use the webservices CustomerPortalWSV2 addPassword
	 */
	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/addPassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addPassword(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
				
		LoggerUtils.logMethodCall("Add Password");
		//LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();

			User user = new User(new JSONObject(body));
			String responseString = userManager.addPassword(user.userId, user.password).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while adding password: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}
	
	/**
	 * @deprecated use the webservices CustomerPortalWSV2 updatePassword
	 */
	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/updatePassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePassword(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
		
		LoggerUtils.logMethodCall("Update Password");
		//LoggerUtils.logBody(body);
		
		try {
			
			JSONObject bodyObject = new JSONObject(body); 
			UserManager userManager = new UserManager();
			User user = new User(bodyObject);
			String responseString = userManager.updatePassword(user.userId, user.password).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while adding password: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}
	
	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/checkAccount")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkAccount(String body,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
		
		LoggerUtils.logMethodCall("Check Account");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			String responseString = userManager.checkAccount(bodyObject).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while checking account: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}
	
	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/verifyMobileNumber")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response verifyOTP(String body,
			@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
		
		LoggerUtils.logMethodCall("Verify OTP");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();

			JSONObject bodyObject = new JSONObject(body);
			String responseString = userManager.varifyMobileNumber(userId, bodyObject).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while verifying mobile number: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/generateOTP")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response generateOTP(String body,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
		
		LoggerUtils.logMethodCall("Generate OTP");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyOject = new JSONObject(body);
			String mobileNumber = bodyOject.getString("mobileNumber");
			String countryCode = bodyOject.optString("countryCode", "+91");
			String responseString = userManager.generateOTP(mobileNumber, countryCode).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while generating OTP: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
    @Deprecated
	@POST
	@Path("/resendOTP")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response resendOTP(String body,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {
		
		LoggerUtils.logMethodCall("Resend OTP");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyOject = new JSONObject(body);
			String mobileNumber = bodyOject.getString("mobileNumber");
			String countryCode = bodyOject.optString("countryCode", "+91");
			String responseString = userManager.resendOTP(mobileNumber, countryCode).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while resending OTP: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@GET
	@Path("/getLoanAccountList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getLoanAccountList(
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Get loan account list");
		
		try {			
			UserManager userManager = new UserManager();
			String responseString = userManager.getLoanAccountList(userId).toString();				
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting loan account list: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/getDashboard")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserDashboard(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Dashboard");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);			
			
			String responseString = userManager.getDashboard(user).toString();				
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting dashboard data: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getBranch")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getBranch(
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("getBranch");
		
		try {			
			UserManager userManager = new UserManager();
			String responseString = userManager.getBranch().toString();				
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting branch data: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}
	

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/getLoanDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserLoanDetail(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Laon details");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);
			String responseString = userManager.getLoanDetails(user).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting loan detail data: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getDisbursementDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserDisbursementDetail(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Disbursement details");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);
			String responseString = userManager.getDisbersementDetails(user).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting disbursement detail data: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/fetchUserProfile")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserProfile(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("User profile details");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			String responseString = userManager.getUserProfileDetails(userId).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting user profile details: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/getPaymentDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPaymentDetails(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Payments details");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);
			String responseString = userManager.getUserPaymentDetails(user).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting payments details: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/getRecentMonthlyPayments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getRecentMonthlyPayments(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Recent monthly payments details");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			String paymentType = bodyObject.getString("paymentType");
			User user = new User(bodyObject);
			String responseString = userManager.getRecentMonthlyPayments(user, paymentType).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting recent monthly payments details: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getServiceRequests")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getServiceRequests(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
	
		LoggerUtils.logMethodCall("Service requests");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);
			String responseString = userManager.getServiceRequests(user).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting service requests details: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/addServiceRequest")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addServiceRequest(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Add Service requests");
		//LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			JSONObject requestParams = bodyObject.getJSONObject("serviceRequestParams");
			User user = new User(bodyObject);
			String responseString = userManager.addServiceRequest(user, requestParams).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while adding service request: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getServiceRequestAttachment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getServiceRequestAttachment(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Get service request attachment");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			String responseString = userManager.getServiceRequestAttachment(bodyObject.getString("parentId")).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting attachment for service request: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getAttachmentData")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAttachmentData(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Get attachment");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			String responseString = userManager.getAttachmentData(bodyObject.getString("documentId")).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting attachment for service request: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/initiateMobileNumberChangeProccess")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response initiateMobileNumberChangeProccess(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Initiate mobile number changed process");
		LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);
			String newMobileNumber = bodyObject.getString("newMobileNumber");
			String countryCode = bodyObject.optString("newCountryCode", "+91");
			String responseString = userManager.initiateMobileNumberChangeProccess(user, newMobileNumber, countryCode).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while initiating mobile number change process: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/verifyAndUpdateNewMobileNumber")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response verifyAndUpdateNewMobileNumber(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Verify and update mobile number");
		LoggerUtils.logBody(body);
		
		try {
			
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			User user = new User(bodyObject);
			String newMobileNumber = bodyObject.getString("newMobileNumber");
			String countryCode = bodyObject.optString("newCountryCode", "+91");
			String OTP = bodyObject.getString("OTP");
			String responseString = userManager.verifyAndUpdateNewMobileNumber(user, newMobileNumber, countryCode, OTP).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while verifying and upating mobile number: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/initiatePayment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response initiatePayment(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
	
		LoggerUtils.logMethodCall("Initiate payment");
		LoggerUtils.logBody(body);
		
		try {
			
			JSONObject bodyObject = new JSONObject(body);
			PaymentManager pManager = new PaymentManager();
			String responseString = pManager.initiatePayment(userId, bodyObject).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while initiating payment: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/finalizePayment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response finalizePayment(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Finalize payment");
		LoggerUtils.logBody(body);
		
		try {
			
			JSONObject bodyObject = new JSONObject(body);
			PaymentManager pManager = new PaymentManager();
			String responseString = pManager.finalizePayment(userId, bodyObject).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while finalizing payment: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/setProfileImage")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setProfileImage(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Set profile image");
		//LoggerUtils.logBody(body);
		
		try {
			UserManager userManager = new UserManager();
			String responseString = userManager.setProfileImage(userId, body).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while setting profile image: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@Deprecated
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addNotificationToken")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addNotificationToken(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Add notification token");
		LoggerUtils.logBody(body);
		
		try {
			
//			UserManager userManager = new UserManager();
//			String responseString = userManager.addNotificationToken(userId, body).toString();			
//			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			
			return new v2.managers.UserManager().addNotificationToken(userId, body);
			  
		} catch (Exception e) {
			
			logger.info("Error while adding notification token: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}

	@Deprecated
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addApnsToken")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addApnsToken(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("Add apns token");
		LoggerUtils.logBody(body);
		
		try {
			
//			UserManager userManager = new UserManager();
//			String responseString = userManager.addApnsToken(userId, body).toString();
//			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			
			return new v2.managers.UserManager().addApnsToken(userId, body);						
			  
		} catch (Exception e) {
			
			logger.info("Error while adding apns token: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getAppPayments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAppPayments(@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("getAppPayments");
		
		try {
			UserManager userManager = new UserManager();
			String responseString = userManager.getAppPayments(userId).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting app payment list: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getCLAttachmentList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getCLAttachmentList(@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("getAppPayments");
		
		try {
			UserManager userManager = new UserManager();
			String responseString = userManager.getCLAttachmentList(userId).toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while getting document list from CL: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addAppsData")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addAppsData(
			@HeaderParam(Constants.USER_ID) int userSourceId,
			String body) {				

		LoggerUtils.logMethodCall("addAppsData");
		
		try {						
			
			JSONObject bodyObject = new JSONObject(body); 
			UserManager userManager = new UserManager();									
			String resp = userManager.addInstalledAppInfo(userSourceId, bodyObject).toString();
			
			return Response.ok(resp, MediaType.APPLICATION_JSON).build();
			  
		} catch (Exception e) {
			
			logger.info("Error while adding apps info: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
		}
		
	}

	
}
