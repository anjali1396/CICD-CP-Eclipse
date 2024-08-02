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

import v2.managers.SOManager;
import utils.Constants;
import utils.LoggerUtils;
import utils.OneResponse;

@Path("/V1/so")
public class SOServicesV1 {

	private Logger logger =  Logger.getLogger(SOServicesV1.class.getSimpleName());
	
	@Context
	private HttpServletRequest request;
	
	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("SOServices: V1 - " + methodName);
	}
	
	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {
		  
		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");
		
	    return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>"
	        + "<body><h1>" + "Welcome to HFFC Customer Portal SOServices V1!" + "</h1></body>" + "</html> ";
	}


	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/createProspect")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createProspect(
			String body
	) {
		
		logMethod("createProspect");
		LoggerUtils.logBody(body);
		
		try {
		
			return new managers.SOManager().createProspect(new JSONObject(body));
			  
		} catch (Exception e) {
			
			logger.info("Error while createPropect : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/reAuthenticateProspect")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response reAuthenticateProspect(
			String body
	) {
		
		logMethod("reAuthenticateProspect");
		LoggerUtils.logBody(body);
		
		try {
		
			return new SOManager().reAuthenticateProspect(new JSONObject(body));
			  
		} catch (Exception e) {
			
			logger.info("Error while reAuthenticateProspect : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/verifyProspect")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response verifyProspect(
			@HeaderParam(Constants.USER_ID) String userId,
			String body
	) {
		
		logMethod("verifyProspect");
		LoggerUtils.logBody(body);
		
		try {
		
			return new SOManager().verifyProspect(userId, new JSONObject(body));
			  
		} catch (Exception e) {
			
			logger.info("Error while verifyProspect : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/updateAndConvertLead")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateAndConvertLead(
			@HeaderParam(Constants.USER_ID) String userId,
			String body
	) {
		
		logMethod("updateAndConvertLead");
		LoggerUtils.logBody(body);
		
		try {
		
			//return new managers.SOManager().updateAndConvertLead(userId, new JSONObject(body));
			return new SOManager().updateCustomerInfo(userId, new JSONObject(body));
			  
		} catch (Exception e) {
			
			logger.info("Error while updateAndConvertLead : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/updateCustomerInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateCustomerInfo(
			@HeaderParam(Constants.USER_ID) String userId,
			String body
	) {
		
		logMethod("updateCustomerInfo");
		LoggerUtils.logBody(body);
		
		try {
		
			return new SOManager().updateCustomerInfo(userId, new JSONObject(body));
			  
		} catch (Exception e) {
			
			logger.info("Error while updateCustomerInfo : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@GET
	@Path("/processSoftApproval")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response processSoftApproval(
			@HeaderParam(Constants.USER_ID) String userId
	) {
		
		logMethod("processSoftApproval");
		
		try {
		
			return new SOManager().processSoftApproval(userId);
			  
		} catch (Exception e) {
			
			logger.info("Error while processSoftApproval : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@GET
	@Path("/fetchApplicationStatus")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchApplicationStatus(
			@HeaderParam(Constants.USER_ID) String userId
	) {
		
		logMethod("fetchApplicationStatus");
		
		try {
		
			return new SOManager().fetchApplicationStatus(userId);
			  
		} catch (Exception e) {
			
			logger.info("Error while fetchApplicationStatus : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/resendOTP")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response resendOTP(@HeaderParam(Constants.USER_ID) String userId) {

		logMethod("resendOTP");

		try {

			return new managers.SOManager().resendOTP(userId);

		} catch (Exception e) {

			logger.info("Error while resendOTP : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/processDocumentImage")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response processDocumentImage(
			@HeaderParam(Constants.USER_ID) String userId,
			String body
	) {
		
		logMethod("processDocumentImage");
		LoggerUtils.logBody(body);
		
		try {
		
			return new SOManager().processDocumentImage(userId, new JSONObject(body));
			  
		} catch (Exception e) {
			
			logger.info("Error while processDocumentImage : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();
			
		}
		
	}
	
}
