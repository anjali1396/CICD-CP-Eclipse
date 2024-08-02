package webservices;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import models.admin.ClientRequest;
import security.AuthManager;
import utils.BasicUtils;
import utils.Constants;
import utils.LoggerUtils;
import utils.OneResponse;

@Provider
public class SecurityInterceptor implements ContainerRequestFilter {

	@Context
	private ResourceInfo resourceInfo;

	@Context
	private HttpServletRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext) {

		try {

			Method method = resourceInfo.getResourceMethod();
			String methodName = method.getName();
			String className = resourceInfo.getResourceClass().getName();
			String ipAddress = new BasicUtils().getIPAddress(request);

			ClientRequest cRequest = getClientRequest(requestContext);
			cRequest.methodName = methodName;
			cRequest.ipAddress = ipAddress;
			
			LoggerUtils.log("Interceptor - Remote IP : " + ipAddress);
			LoggerUtils.log("Interceptor - Calling method: " + methodName + " | Calling class: " + className);

			if (method.isAnnotationPresent(PermitAll.class)) {

				LoggerUtils.log("Interceptor - PermitAll access authorized.");
				return;

			} else if (method.isAnnotationPresent(RolesAllowed.class)) {

				LoggerUtils.log("Interceptor - initializing RolesAllowed access check...");				

				RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);				
				Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
				cRequest.rolesSet = rolesSet;
				
				AuthManager authManager = new AuthManager(cRequest);

				Response authResponse = authManager.authenticateRoleSet();
				
				if (null != authResponse && authResponse.getStatus() == 200) {
					LoggerUtils.log("Interceptor - RoleSet Authorized.");
					return;
				} else {
					LoggerUtils.log("Interceptor - RoleSet denied. Aborting.");
					requestContext.abortWith(new OneResponse().getAccessDeniedResponse());	
				}

			} 
			
			requestContext.abortWith(new OneResponse().getAccessDeniedResponse());
			
		} catch (Exception e) {
			
			LoggerUtils.log("Error while interception APIs: " + e.getMessage());
			e.printStackTrace();
			requestContext.abortWith(new OneResponse().getAccessDeniedResponse());
			
		}
	}
	
	private ClientRequest getClientRequest(final ContainerRequestContext requestContext) {
		
		ClientRequest cRequest = new ClientRequest();
		
		String authorization = requestContext.getHeaderString(Constants.AUTHORIZATION);
		if (null == authorization)
			authorization = Constants.NA;
		
		String orgId = requestContext.getHeaderString(Constants.ORG_ID);
		if (null == orgId)
			orgId = Constants.NA;

		String sessionPasscode = requestContext.getHeaderString(Constants.SESSION_PASSCODE);
		if (null == sessionPasscode)
			sessionPasscode = Constants.NA;

		String sourcePasscode = requestContext.getHeaderString(Constants.SOURCE_PASSCODE);
		if (null == sourcePasscode)
			sourcePasscode = Constants.NA;
		
		String crownPasscode = requestContext.getHeaderString(Constants.CROWN_PASSCODE);
		if (null == crownPasscode)
			crownPasscode = Constants.NA;

		String userId = requestContext.getHeaderString(Constants.USER_ID);
		if (null == userId)
			userId = Constants.NA;
		
		cRequest.cypher = requestContext.getHeaderString(Constants.CYPHER);
		cRequest.crypt = requestContext.getHeaderString(Constants.CRYPT);
		cRequest.sessionPasscode = sessionPasscode;
		cRequest.sourcePasscode = sourcePasscode;
		cRequest.crownPasscode = crownPasscode;
		cRequest.userId = userId;
		cRequest.orgId = orgId;
		cRequest.authorization = authorization;
		
		return cRequest;
		
	}

}