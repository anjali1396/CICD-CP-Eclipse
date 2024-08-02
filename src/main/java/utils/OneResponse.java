package utils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import utils.Constants.Actions;
import utils.Constants.Errors;

public class OneResponse {
	
	public OneResponse() {}
	
	public Response getAccessDeniedResponse() {
		return Response
				.status(401)
				.entity(
						new LocalResponse()
								.setStatus(false)
								.setMessage("Client authentication failed.")
								.setError(Errors.UNAUTHORIZED_ACCESS.value)
								.setAction(Actions.AUTHENTICATE_AGAIN.value)
								.toJson()
								.toString()
				)
				.build();
	}
	
	public Response getSuccessResponse(JSONObject successResponse) {
		return Response.ok(
				successResponse.toString(), 
				MediaType.APPLICATION_JSON
		).build();
	}
	
	public Response getFailureResponse(JSONObject failureResponse) {
		return Response
				.status(201)
				.entity(failureResponse.toString())
				.build();
	}
	
	public Response getDefaultFailureResponse() {
		return Response
				.status(500)
				.entity(new LocalResponse().toJson().toString())
				.build();
	}
	
	public Response simpleResponse(int code, String response) {
		return Response
				.status(code)
				.entity(response)
				.build();
	}
	
	public Response operationFailedResponse() {
		return Response
				.status(201)

				.entity(
						new LocalResponse()
								.setStatus(false)
								.setError(Errors.OPERATION_FAILED.value)
								.setAction(Actions.RETRY.value)
								.toJson()
								.toString()
				)
				.build();
	}
	
	public Response errorResponse(String message) {
		
		final var errorMessage = null != message ? message : Constants.DEFAULT_ERROR_MESSAGE;
		
		return Response
				.status(201)

				.entity(
						new LocalResponse()
								.setStatus(false)
								.setError(Errors.OPERATION_FAILED.value)
								.setAction(Actions.RETRY.value)
								.setMessage(errorMessage)
								.toJson()
								.toString()
				)
				.build();
	}

}
