package webservices;

import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import models.User;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import v3.managers.UserManager;
import v3.managers.PaymentManager;

@Path("/CustomerPortalServices/V3")
public class CustomerPortalWSV3 {
	
	@Context
	private HttpServletRequest request;

	private Logger logger = Logger.getLogger(CustomerPortalWSV3.class.getSimpleName());

	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("CustomerPortalServices: V3 - " + methodName);
	}

	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {

		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");

		return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>" + "<body><h1>"
				+ "Welcome to HFFC Customer Portal Web Services V3!" + "</h1></body>" + "</html> ";
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/initiatePayment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response initiatePayment(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("initiatePayment");
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

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/addServiceRequest")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addServiceRequest(String body, @HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId) {

		LoggerUtils.logMethodCall("Add Service requests");

		try {
			UserManager userManager = new UserManager();
			JSONObject bodyObject = new JSONObject(body);
			return userManager.addServiceRequest(userId, bodyObject);

		} catch (Exception e) {

			logger.info("Error while adding service request: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response Login(String body, @HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId, @HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {

		logMethod("Login");
		LoggerUtils.logBody("UserLogin"+userId);
		LoggerUtils.logBody(body);
		

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager userManager = new UserManager();
			String ipAddress = new BasicUtils().getIPAddress(request);

			return userManager.performLogin(userId, bodyObject, ipAddress);

		} catch (Exception e) {

			logger.info("Error while login user: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}
	
	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/web.login")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response WebLogin(String body, @HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId, @HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {

		logMethod("WebLogin");
		LoggerUtils.logBody("UserLogin"+userId);
		LoggerUtils.logBody(body);
		

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager userManager = new UserManager();
			String ipAddress = new BasicUtils().getIPAddress(request);
				
			return userManager.reCaptchaVerify(userId, bodyObject, ipAddress);

		} catch (Exception e) {

			logger.info("Error while recaptcha verify user: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/addPassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addPassword(@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.REQUEST_ID) String requestId, @HeaderParam(Constants.TOKEN) String token,
			String body) {

		logMethod("addPassword");
		LoggerUtils.logBody(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			String ipAddress = new BasicUtils().getIPAddress(request);

			return uManager.addPassword(userId, requestId, token, bodyObject, ipAddress);

		} catch (Exception e) {
			logger.info("Error while adding password :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/initiateMobileNumberChangeProcess")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response initiateMobileNumberChangeProcess(@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.REQUEST_ID) String requestId, String body) {

		logMethod("Initiate mobile number changed process");
		LoggerUtils.log(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			String ipAddress = new BasicUtils().getIPAddress(request);

			return uManager.initiateMobileNumberChangeProcess(userId, requestId, bodyObject, ipAddress);

		} catch (Exception e) {
			logger.info("Error while initiating change mobile number :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}


	@RolesAllowed({Constants.AUTH_TWO,Constants.CP_USER})
	@GET
	@Path("/getDashboard")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserDashboard(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getUserDashboard");

		try {

			return new UserManager().getDashboard(userId);

		} catch (Exception e) {

			logger.info("Error while getting dashboard data: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.AUTH_TWO,Constants.CP_USER})
	@GET
	@Path("/getLoanList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getLoanList(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getLoanList");

		try {

			return new UserManager().getLoanList(userId);

		} catch (Exception e) {

			logger.info("Error while getting loan list data: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.AUTH_TWO, Constants.CP_USER})
	@POST
	@Path("/getLoanDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getLoanDetails(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getLoanDetails");

		try {

			final var userManager = new UserManager();
			return userManager.getLoanDetails(userId, new JSONObject(body));

		} catch (Exception e) {

			logger.info("Error while getting loan detail data: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.AUTH_TWO, Constants.CP_USER})
	@POST
	@Path("/requestOutstanding")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response requestOutstanding(String body, @HeaderParam(Constants.USER_ID) int userId) {

		LoggerUtils.logMethodCall("requestOutstanding");

		try {
			
//			final var userManager = new UserManager();
//			return userManager.requestOutstanding(userId, new JSONObject(body));
			
			String message = "Please update the app to view the outstanding amount.";
			return new OneResponse().getSuccessResponse(new LocalResponse().setMessage(message).toJson());
					

		} catch (Exception e) {

			logger.info("Error while requestOutstanding request: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.AUTH_TWO,Constants.CP_USER})
	@GET
	@Path("/getPromoBanners")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPromoBanners() {

		logMethod("getPromoBanners");

		try {

			return new UserManager().getPromoBanners();

		} catch (Exception e) {

			logger.info("Error while getPromoBanners : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

//	@RolesAllowed({ Constants.CRON })
//	@GET
//	@Path("/checkAndNotifyLoanOutstanding")
//	@Produces(MediaType.APPLICATION_JSON)
//	@Consumes(MediaType.APPLICATION_JSON)
//	public Response checkAndNotifyLoanOutstanding() {
//
//		logMethod("checkAndNotifyLoanOutstanding");
//
//		try {
//
//			final var userManager = new UserManager();
//			return userManager.checkAndNotifyLoanOutstanding();
//
//		} catch (Exception e) {
//
//			logger.info("Error while checkAndNotifyLoanOutstanding : " + e.toString());
//			e.printStackTrace();
//			return new OneResponse().getDefaultFailureResponse();
//
//		}
//
//	}
	
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
	@Path("/getNotifications")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getNotifications(
			String body, 
			@HeaderParam(Constants.USER_ID) int userId
	) {

		logMethod("getNotifications");
		LoggerUtils.logBody(body);

		try {

			return new UserManager().getNotifications(userId, new JSONObject(body));

		} catch (Exception e) {

			logger.info("Error while getting notifications: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/updateNotificationStatus")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateNotificationStatus(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("updateNotificationStatus");
		LoggerUtils.logBody(body);

		try {
			
			return new UserManager().updateNotificationStatus(userId, body);
			

		} catch (Exception e) {

			logger.info("Error while setting notification status: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getUnreadNotificationCount")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUnreadNotificationCount(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getUnreadNotificationCount");

		try {
			
			return new UserManager().getUnreadNotificationCount(userId); 

		} catch (Exception e) {

			logger.info("Error while getting unread notification count: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/register")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response Register(
			@HeaderParam(Constants.REQUEST_ID) String requestId, 
			String body
			) throws Exception {

		logMethod("register");
		LoggerUtils.logBody(body);
		LoggerUtils.logBody("-------->" + requestId);

		try {

			final var bodyObject = new JSONObject(body);
			final var uManager = new UserManager();		
			
			var ipAddress = new BasicUtils().getIPAddress(request);
			
			if (null == ipAddress)
				ipAddress = request.getRemoteAddr();
			
			return uManager.performRegisteration(requestId, bodyObject, ipAddress);
			
		} catch (Exception e) {
			logger.info("Error while registeration process :" + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
		}
	}
		
	
}
