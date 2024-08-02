package v4.managers;

import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;


import dao.RequestOutstandingRepository;
import models.RequestOutstanding;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LocalResponse;
import utils.OneResponse;
import utils.Constants.Errors;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;

import v3.managers.SalesForceManager;

public class UserManager {
	
	private final SalesForceManager sfManager;
	private final Gson gson;
	private RequestOutstandingRepository _outstandingRepo = null;
	
	public UserManager() throws Exception {

		sfManager = new SalesForceManager();
		gson = new Gson();

	}

	private RequestOutstandingRepository outstandingRepo() {
		if (null == _outstandingRepo)
			_outstandingRepo = new RequestOutstandingRepository();
		return _outstandingRepo;
	}
	
	
	public Response requestOutstanding(int userId, JSONObject bodyObject) throws Exception {

		final var reqOutstanding = gson.fromJson(bodyObject.toString(), RequestOutstanding.class);
		
		final var reqOutstandingData = outstandingRepo().findValidRequest(reqOutstanding.loanAccountNumber, reqOutstanding.userId);
		
		
		if (null != reqOutstandingData) {
			
			var checkDateTimeDifferece = DateTimeUtils.getDateDifferenceInHours(reqOutstandingData.validityDatetime);
			
			if(checkDateTimeDifferece > 26)
			return new OneResponse().getFailureResponse(new LocalResponse().setMessage(
					"You already have a valid request, You'd be able to see your outstanding on MyLoans Screen.")
					.setError(Errors.DUPLICATE_RECORD.value).toJson());
		}
		
		reqOutstanding.validityDatetime = DateTimeUtils.getDateTimeAddingMinutes(5, DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
				DateTimeZone.IST);
		
	
		if (null == outstandingRepo().saveRequestOutstanding(reqOutstanding)) {
			return new OneResponse().operationFailedResponse();
		} else {
			return new OneResponse().getSuccessResponse(new LocalResponse().setMessage(
					"Your request for outstanding amount has been successfully submitted.")
					.toJson());
		}

	}
	
	
	public Response getLoanDetails(int userId, JSONObject bodyObject) throws Exception {


		var selectedLoanAccountNumber = bodyObject.optString("loanAccountNumber");

		var loanData = sfManager.getLoanDetail(selectedLoanAccountNumber);

		var outstandingStatus = outstandingRepo().findValidRequest(loanData.get(0).accountNumber, userId);
		if (null != outstandingStatus) {

			try {

				if (DateTimeUtils.getDateDifferenceInMinutes(outstandingStatus.createDatetime) < -5)
					loanData.get(0).outstandingStatus = Constants.INVALID;
				else
					loanData.get(0).outstandingStatus = Constants.VALID;

			} catch (Exception e) {
				e.printStackTrace();
//				return new OneResponse().getDefaultFailureResponse();
			}
		} else {
			loanData.get(0).outstandingStatus = Constants.INVALID;
		}

		if (null != loanData && !loanData.isEmpty()) {
			var response = new JSONObject();
			response.put("loanData", new JSONArray(gson.toJson(loanData)));
			return new OneResponse().getSuccessResponse(response);
		} else
			return new OneResponse().operationFailedResponse();

	}
}