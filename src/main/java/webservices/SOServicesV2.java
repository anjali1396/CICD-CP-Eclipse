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

import utils.Constants;
import utils.LoggerUtils;
import utils.OneResponse;
import v2.managers.SOManager;

@Path("/V2/so")
public class SOServicesV2 {

	private Logger logger = Logger.getLogger(SOServicesV2.class.getSimpleName());

	@Context
	private HttpServletRequest request;

	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("SOServices: V2 - " + methodName);
	}

	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {

		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");

		return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>" + "<body><h1>"
				+ "Welcome to HFFC Customer Portal SOServices V2!" + "</h1></body>" + "</html> ";
	}

	@RolesAllowed(Constants.PROSPECT_SERVICES)
	@POST
	@Path("/reAuthenticateProspect")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response reAuthenticateProspect(String body) {

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
	public Response verifyProspect(@HeaderParam(Constants.USER_ID) String userId, String body) {

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
	@Path("/updateCustomerInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateCustomerInfo(@HeaderParam(Constants.USER_ID) String userId, String body) {

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
	public Response processSoftApproval(@HeaderParam(Constants.USER_ID) String userId) {

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
	public Response fetchApplicationStatus(@HeaderParam(Constants.USER_ID) String userId) {

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
	@Path("/processDocumentImage")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response processDocumentImage(@HeaderParam(Constants.USER_ID) String userId, String body) {

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
