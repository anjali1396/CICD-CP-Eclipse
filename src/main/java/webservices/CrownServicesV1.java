package webservices;

import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import managers.CrownServiceManager;
import utils.Constants;
import utils.LoggerUtils;
import utils.OneResponse;

@Path("/V1/cs")
public class CrownServicesV1 {

	private Logger logger =  Logger.getLogger(CrownServicesV1.class.getSimpleName());
	
	private void logMethod(String methodName) {
		LoggerUtils.logMethodCall("CrownServices: V1 - " + methodName);
	}
	
	@PermitAll
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {
		  
		logger.info("\n\n----------------------\n  Landing page invoked  \n----------------------\n\n");
		
	    return "<html> " + "<title>" + "HFFC Customer Portal" + "</title>"
	        + "<body><h1>" + "Welcome to HFFC Customer Portal Crows Services V1!" + "</h1></body>" + "</html> ";
	}
	
	@RolesAllowed({Constants.CRON})
	@GET
	@Path("/Payment.syncRZPStatus")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response rSyncRZPPayoutStatus() {

		logMethod("Payment.syncRZPStatus");

		try {

			return new CrownServiceManager().syncRazorPaymentStatus();

		} catch (Exception e) {

			logger.info("Error while getting Payment.syncRZPStatus : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
	@RolesAllowed(Constants.CRON)
	@GET
	@Path("/Payment.createFailedReceiptsOnSF")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response rCreateFailedReceiptOnSF() {

		logMethod("Payment.createFailedReceiptsOnSF");

		try {

			return new CrownServiceManager().createFailedReceiptOnSF();

		} catch (Exception e) {

			logger.info("Error while getting Payment.createFailedReceiptsOnSF : " + e.toString());
			e.printStackTrace();
			return new OneResponse().getDefaultFailureResponse();

		}

	}
	
}
