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

import services.Recaptcha;
import utils.BasicUtils;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import v2.managers.PaymentManager;
import v2.managers.UserManager;

@Path("/CustomerPortalServices/V2")
public class CustomerPortalWSV2 {

	private Logger logger = Logger.getLogger(CustomerPortalWSV2.class.getSimpleName());

	@Context
	private HttpServletRequest request;

	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("CustomerPortalServices: V2 - " + methodName);
	}

	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {

		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");

		return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>" + "<body><h1>"
				+ "Welcome to HFFC Customer Portal Web Services V2!" + "</h1></body>" + "</html> ";
	}

	@RolesAllowed(Constants.CP_USER)
	@Deprecated
	@POST
	@Path("/createOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createOrder(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("createOrder");
		LoggerUtils.logBody(body);

		try {

			dao.PaymentManager pManager = new dao.PaymentManager();
			String responseString = pManager.createOrder(userId, body).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while creating razorpay order: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/createPaymentOrderForRazorpay")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createPaymentOrderForRazorpay(String body,
			@HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode, @HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {

		logMethod("createPaymentOrderForRazorpay");
		LoggerUtils.logBody(body);

		try {

			PaymentManager pManager = new PaymentManager();
			String responseString = pManager.createPaymentOrderForRazorpay(userId, body).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while creating razorpay payment order: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@Deprecated
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response Login(String body, @HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId, @HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {

		logMethod("Login");

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager userManager = new UserManager();
			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}

			// String ipAddress = request.getRemoteAddr();
			String responseString = userManager.performLogin(userId, bodyObject, ipAddress).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while login user: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}


	
	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getDashboard")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserDashboard(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getUserDashboard");

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.getDashboard(userId).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting dashboard data: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getLoanDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getLoanDetails(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getLoanDetails");

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.getLoanDetails(userId).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting loan detail data: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getPaymentDetailForLoan")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPaymentDetailForLoan(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getPaymentDetailForLoan");
		LoggerUtils.logBody(body);

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.getPaymentDetailForLoan(new JSONObject(body)).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting payment detail for loan: " + e.toString());
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

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/finalizePayment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response finalizePayment(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("finalizePayment");
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
	@GET
	@Path("/getPaymentDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPaymentDetails(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getPaymentDetails");

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.getUserPaymentDetails(userId).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting payments details: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getLoanAccountList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getLoanAccountList(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getLoanAccountList");
		LoggerUtils.logBody(body);

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.getLoanAccountList(userId, new JSONObject(body)).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting loan account list: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getRecentMonthlyPayments")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getRecentMonthlyPayments(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getRecentMonthlyPayments");
		LoggerUtils.logBody(body);

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.getRecentMonthlyPayments(userId, body).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting recent monthly payments details: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addReferral")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addReferral(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("addReferral");
		LoggerUtils.logBody(body);

		try {

			UserManager userManager = new UserManager();
			String responseString = userManager.addReferral(userId, new JSONObject(body)).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while adding referrals: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addCfData")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addCfData(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("addCfData");
		LoggerUtils.logBody(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager userManager = new UserManager();

			String resp = userManager.addCFInfo(userId, bodyObject).toString();

			return Response.ok(resp, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while adding contact info: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@Deprecated
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/updateNotificationStatus")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateNotificationStatus(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("updateNotificationStatus");
		LoggerUtils.logBody(body);

		try {

//			UserManager manager = new UserManager();

//			String responseString = manager.updateNotificationStatus(userId, new JSONObject(body)).toString();
			
			var resp = new v3.managers.UserManager().updateNotificationStatus(userId, body);

			var jResp = new JSONObject(resp.getEntity().toString());

			System.out.println("updateNotificationStatus - " + jResp.toString());

			LocalResponse lResponse = new LocalResponse();

			if (resp.getStatus() == 200)
				lResponse.isSuccess = true;
			else
				lResponse.isSuccess = false;

			lResponse.message = jResp.optString("message", Constants.NA);

			return Response.ok(lResponse.toJson().toString(), MediaType.APPLICATION_JSON).build();	

		} catch (Exception e) {

			logger.info("Error while setting notification status: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@Deprecated
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getNotifications")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getNotifications(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getNotifications");
		LoggerUtils.logBody(body);

		try {

			UserManager manager = new UserManager();

			String responseString = manager.getNotifications(userId, new JSONObject(body)).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting notifications: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@Deprecated
	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getUnreadNotificationCount")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUnreadNotificationCount(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getUnreadNotificationCount");

		try {

			UserManager manager = new UserManager();

			String responseString = manager.getUnreadNotificationCount(userId).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting unread notification count: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/setProfileImage")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setProfileImage(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("setProfileImage");

		try {

			UserManager uManager = new UserManager();

			String responseString = uManager.setProfileImage(userId, body).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while setting profile image: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/getAccountStatement")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAccountStatement(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("getAccountStatement");
		LoggerUtils.logBody(body);

		try {

			UserManager uManager = new UserManager();

			String responseString = uManager.getAccountStatement(userId, body).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getting account statement: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/register")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response Register(@HeaderParam(Constants.REQUEST_ID) String requestId, String body) throws Exception {

		logMethod("register");
		LoggerUtils.logBody(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			
			//String ipAddress = request.getHeader("X-FORWARDED-FOR");
			
			String ipAddress = new BasicUtils().getIPAddress(request);
			
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}
			// String ipAddress = request.getRemoteAddr();
			
			String responseString = uManager.performRegisteration(requestId, bodyObject, ipAddress).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			logger.info("Error while registeration process :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}


	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/verifyMobileNumber")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response verifyMobileNumber(@HeaderParam(Constants.REQUEST_ID) String requestId,
			@HeaderParam(Constants.USER_ID) int userId, @HeaderParam(Constants.TOKEN) String token, String body) {

		logMethod("verifyMobileNumber");
		LoggerUtils.logBody(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			// String ipAddress = request.getRemoteAddr();

			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}

			String responseString = uManager.verifyMobileNumber(requestId, userId, token, bodyObject, ipAddress)
					.toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			logger.info("Error while verifying mobile number :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}

	}

	@RolesAllowed(Constants.CP_USER)
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
			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}
			// String ipAddress = request.getRemoteAddr();

			String responseString = uManager.addPassword(userId, requestId, token, bodyObject, ipAddress).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			logger.info("Error while adding password :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}
	
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/web.initiateForgotOrChangePassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response WebInitiateForgotOrChangePassword(@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.REQUEST_ID) String requestId, String body) throws Exception	{
		
		logMethod("WebInitiateForgotOrChangePassword");
		LoggerUtils.logBody(body);
		
		try {
			
			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}

			String responseString = uManager.initiateForgot(userId, requestId, bodyObject, ipAddress)
					.toString();
			
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
			
		
		}  catch (Exception e) {
			logger.info("Error while initiating forgot process :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		
		}
		
	
	}
	

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/initiateForgotOrChangePassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response initiateForgotOrChangePassword(@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.REQUEST_ID) String requestId, String body) {

		logMethod("initiateForgotOrChangePassword");
		LoggerUtils.logBody(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}
			// String ipAddress = request.getRemoteAddr();
			String responseString = uManager.initiateForgotOrChangePassword(userId, requestId, bodyObject, ipAddress)
					.toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			logger.info("Error while initiating forgot and change process :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}

	}

	@RolesAllowed(Constants.CP_USER)
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
			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}
			// String ipAddress = request.getRemoteAddr();
			String responseString = uManager.initiateMobileNumberChangeProcess(userId, requestId, bodyObject, ipAddress)
					.toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			logger.info("Error while initiating change mobile number :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/verifyAndUpdateNewMobileNumber")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response verifyAndUpdateNewMobileNumber(@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.REQUEST_ID) String requestId, @HeaderParam(Constants.TOKEN) String token,
			String body) {

		logMethod("verifyAndUpdateNewMobileNumber");
		LoggerUtils.log(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}
			// String ipAddress = request.getRemoteAddr();
			String responseString = uManager
					.verifyAndUpdateNewMobileNumber(userId, requestId, token, bodyObject, ipAddress).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			logger.info("Error while verify new mobile number :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}

	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/resendOTP")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response resendOTP(@HeaderParam(Constants.USER_ID) int userId,
			@HeaderParam(Constants.REQUEST_ID) String requestId, @HeaderParam(Constants.TOKEN) String token,
			String body) {

		logMethod("Resend OTP");
		LoggerUtils.log(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager uManager = new UserManager();
			String responseString = uManager.resendOTP(userId, requestId, token, bodyObject).toString();
			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			logger.info("Error while resending OTP :" + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
		}
	}

	@RolesAllowed(Constants.CP_USER)
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
			String responseString = userManager.addServiceRequest(userId, bodyObject).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while adding service request: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.CP_USER)
	@GET
	@Path("/getPromo")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPromo() {

		logMethod("getPromo");

		try {

			return new UserManager().getPromo();

		} catch (Exception e) {

			logger.info("Error while getPromo : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed(Constants.CRON)
	@GET
	@Path("/getUserDetailsToCSV")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserDetailsToCSV() {

		logMethod("getUserDetailsToCSV");

		try {

			return new UserManager().getUserDetailsToCSV();

		} catch (Exception e) {

			logger.info("Error while getUserDetailsToCSV : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/autoLogin")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response autoLogin(String body, @HeaderParam(Constants.SESSION_PASSCODE) String sessionPasscode,
			@HeaderParam(Constants.USER_ID) int userId, @HeaderParam(Constants.SOURCE_PASSCODE) String sourcePasscode) {

		logMethod("autoLogin");
		LoggerUtils.log(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			UserManager userManager = new UserManager();

			String ipAddress = new BasicUtils().getIPAddress(request);

			return userManager.performAutoLogin(userId, bodyObject, ipAddress, sessionPasscode);

		} catch (Exception e) {

			logger.info("Error while autoLogin user: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/addSitePhotograph")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addSitePhotograph(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("addSitePhotograph");

		try {

			UserManager userManager = new UserManager();
			return userManager.addSitePhotograph(userId, new JSONObject(body));

		} catch (Exception e) {

			logger.info("Error while addSitePhotograph : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@GET
	@Path("/getSitePhotographList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getSitePhotographList(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("getSitePhotographList");

		try {

			UserManager userManager = new UserManager();
			return userManager.getSitePhotographList(userId);

		} catch (Exception e) {

			logger.info("Error while getSitePhotographList : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}


	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@GET
	@Path("/checkRepriceEligibility")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkRepriceEligibility(@HeaderParam(Constants.USER_ID) int userId) {

		logMethod("checkRepriceEligibility");

		try {

			UserManager userManager = new UserManager();
			return userManager.checkRepriceEligibility(userId);

		} catch (Exception e) {

			logger.info("Error while checkRepriceEligibility : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.CRON})
	@GET
	@Path("/processRepriceEligibility")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response processRepriceEligibility() {

		logMethod("processRepriceEligibility");

		try {

			UserManager userManager = new UserManager();
			return userManager.processRepriceEligibility();

		} catch (Exception e) {

			logger.info("Error while processRepriceEligibility : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/finalizeReprice")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response finalizeReprice(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("finalizeReprice");

		try {

			UserManager userManager = new UserManager();
			return userManager.finalizeReprice(userId, new JSONObject(body));

		} catch (Exception e) {

			logger.info("Error while finalizeReprice : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.CRON})
	@GET
	@Path("/processFailedReprice")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response processFailedReprice() {

		logMethod("processFailedReprice");

		try {

			UserManager userManager = new UserManager();
			return userManager.processFailedReprice();

		} catch (Exception e) {

			logger.info("Error while processFailedReprice : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed({Constants.CRON})
	@GET
	@Path("/sendRepriceCSV")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response sendRepriceCSV() {

		logMethod("sendRepriceCSV");

		try {

			UserManager userManager = new UserManager();
			return userManager.sendRepriceCSV();

		} catch (Exception e) {

			logger.info("Error while sendRepriceCSV : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	
	@RolesAllowed({ Constants.AUTH_TWO, Constants.CP_USER })
	@POST
	@Path("/deleteAccount")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteAccount(String body, @HeaderParam(Constants.USER_ID) int userId) {

		logMethod("deleteAccount");

		try {

			final var userManager = new UserManager();
			return userManager.deleteAccount(userId, new JSONObject(body));

		} catch (Exception e) {

			logger.info("Error while deleteAccount : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addNotificationToken")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addNotificationToken(String body,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("addNotificationToken");
		LoggerUtils.logBody(body);
		
		try {

			return new UserManager().addNotificationToken(userId, body);		
			  
		} catch (Exception e) {
			
			logger.info("Error while adding notification token: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.CP_USER)
	@POST
	@Path("/addApnsToken")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addApnsToken(String body,
			@HeaderParam(Constants.USER_ID) int userId) {
		
		LoggerUtils.logMethodCall("addNotificationToken");
		LoggerUtils.logBody(body);
		
		try {

			return new UserManager().addApnsToken(userId, body);		
			  
		} catch (Exception e) {
			
			logger.info("Error while adding apns token: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}

}
