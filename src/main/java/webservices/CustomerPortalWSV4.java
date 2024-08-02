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

import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import utils.Constants.Errors;
import v4.managers.UserManager;
import v4.managers.PaymentManager;


@Path("/CustomerPortalServices/V4")
public class CustomerPortalWSV4 {
	
private Logger logger =  Logger.getLogger(CustomerPortalWSV4.class.getSimpleName());
	
	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("CustomerPortalServices: V4 - " + methodName);
	}
	
	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {
		  
		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");
		
	    return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>"
	        + "<body><h1>" + "Welcome to HFFC Customer Portal Web Services V3!" + "</h1></body>" + "</html> ";
	}
	
	@RolesAllowed({Constants.AUTH_TWO,Constants.CP_USER})
	@POST
	@Path("/initiatePayment")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response initiatePayment(
			String body,
			@HeaderParam(Constants.USER_ID) int userId
	) {		
		
		logMethod("initiatePayment");
		LoggerUtils.logBody(body);
		
		try {
			
			JSONObject bodyObject = new JSONObject(body);
			PaymentManager pManager = new PaymentManager();
			return pManager.initiatePayment(userId, bodyObject);			
			  
		} catch (Exception e) {
			
			logger.info("Error while initiating payment: " + e.toString());
			e.printStackTrace();
			return Response.ok(new LocalResponse().toJson().toString(), MediaType.APPLICATION_JSON).build();
			
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
	
	
	@RolesAllowed({Constants.AUTH_TWO, Constants.CP_USER})
	@POST
	@Path("/requestPOS")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response requestPOS(String body, @HeaderParam(Constants.USER_ID) int userId) {

		LoggerUtils.logMethodCall("requestPOS");

		try {
			final var userManager = new UserManager();
			return userManager.requestOutstanding(userId, new JSONObject(body));

		} catch (Exception e) {

			logger.info("Error while requestPOS request: " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
		
}
