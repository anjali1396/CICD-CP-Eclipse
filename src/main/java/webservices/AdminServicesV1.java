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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import v2.managers.AdminUserManager;
import v2.managers.ExternalPartnerManager;
import v2.managers.NotificationHelper;

@Path("/V1/as")
public class AdminServicesV1 {

	private Logger logger = Logger.getLogger(AdminServicesV1.class.getSimpleName());

	@Context
	private HttpServletRequest request;

	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("AdminServicesV1: V1 - " + methodName);
	}

	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {

		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");

		return "<html> " + "<title>" + "AdminServicesV1" + "</title>" + "<body><h1>"
				+ "Welcome to AdminServicesV1 Services V1!" + "</h1></body>" + "</html> ";
	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@GET
	@Path("/authenticateClient")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response authenticateClient(@HeaderParam(Constants.ORG_ID) String orgId) {

		logMethod("authenticateClient");

		try {

			return new ExternalPartnerManager().authenticateClient(orgId);

		} catch (Exception e) {

			logger.info("Error while authenticateClient: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@POST
	@Path("/pushNotification")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response pushNotification(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("pushNotification");
		//LoggerUtils.logBody(body);

		try {

			JSONObject bodyObject = new JSONObject(body);
			NotificationHelper nHelper = new NotificationHelper(userId);

			return nHelper.pushNotification(bodyObject);

		} catch (Exception e) {

			logger.info("Error while pushing notification: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("login");

		try {

			JSONObject bodyObject = new JSONObject(body);
			AdminUserManager manager = new AdminUserManager();

			String ipAddress = request.getHeader("X-FORWARDED-FOR");
			if (null == ipAddress) {
				ipAddress = request.getRemoteAddr();
			}
			// String ipAddress = request.getRemoteAddr();
			String responseString = manager.performLogin(userId, bodyObject, ipAddress).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while admin user login: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@POST
	@Path("/getDashboard")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getDashboard(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("getDashboard");
		LoggerUtils.logBody(body);

		try {

			AdminUserManager manager = new AdminUserManager();

			String responseString = manager.getDashboard(userId, new JSONObject(body)).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while admin dashboard: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse(e.getMessage()).toJson().toString(), MediaType.APPLICATION_JSON)
					.build();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@POST
	@Path("/searchPaymentInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response searchPaymentInfo(@HeaderParam(Constants.USER_ID) int adminId, String body) {

		logMethod("searchPaymentInfo");
		LoggerUtils.logBody(body);

		try {

			AdminUserManager manager = new AdminUserManager();

			String responseString = manager.searchPaymentInfo(adminId, new JSONObject(body)).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while searching payment : " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse(e.getMessage()).toJson().toString(), MediaType.APPLICATION_JSON)
					.build();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@POST
	@Path("/updatePaymentInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePaymentInfo(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("updatePaymentInfo");
		LoggerUtils.logBody(body);

		try {

			AdminUserManager aManager = new AdminUserManager();
			String responseString = aManager.updatePaymentInfo(userId, new JSONObject(body)).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while updating payment: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse(e.getMessage()).toJson().toString(), MediaType.APPLICATION_JSON)
					.build();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@POST
	@Path("/updateManualPaymentInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateManualPaymentInfo(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("updateManualPaymentInfo");
		LoggerUtils.logBody(body);

		try {

			AdminUserManager aManager = new AdminUserManager();
			String responseString = aManager.updateManualPaymentInfo(userId, new JSONObject(body)).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while manual updating payment: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse(e.getMessage()).toJson().toString(), MediaType.APPLICATION_JSON)
					.build();

		}

	}

	@RolesAllowed(Constants.CRON)
	@POST
	@Path("/scheduleCustomerBirthdayNotification")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response scheduleCustomerBirthdayNotification(@HeaderParam(Constants.USER_ID) int userId, String body) {

		logMethod("scheduleCustomerBirthdayNotification");

		try {

			NotificationHelper nHelper = new NotificationHelper(userId);
			String responseString = nHelper.scheduleCustomerBirthdayNotification(new JSONObject(body)).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while scheduleCustomerBirthdayNotification: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse(e.getMessage()).toJson().toString(), MediaType.APPLICATION_JSON)
					.build();

		}

	}

	@RolesAllowed(Constants.ADMIN_SERVICES)
	@GET
	@Path("/getUserId")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserId(@QueryParam(Constants.EMAIL_ID) String emailId) {

		logMethod("getUserId");

		try {

			AdminUserManager aManager = new AdminUserManager();
			String responseString = aManager.getUserId(emailId).toString();

			return Response.ok(responseString, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {

			logger.info("Error while getUserId: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse(e.getMessage()).toJson().toString(), MediaType.APPLICATION_JSON)
					.build();

		}

	}

}
