package v4.managers;

import javax.ws.rs.core.Response;

import org.json.JSONObject;

import com.google.gson.Gson;

import dao.PaymentRepository;
import dao.UserRepository;
import models.User;
import models.payment.Payment;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.OneResponse;
import utils.PaymentUtils;
import utils.Constants.Errors;

public class PaymentManager {

	private final UserRepository userRepo;
	private final PaymentRepository paymentRepo;
	private final Gson gson;

	public PaymentManager() {
		userRepo = new UserRepository();
		paymentRepo = new PaymentRepository();
		gson = new Gson();
	}

	public Response initiatePayment(int userId, JSONObject bodyObject) throws Exception {

		User fetchedUser = userRepo.findUserByUserId(userId);

		if (null == fetchedUser) {

			return new OneResponse().getFailureResponse(new LocalResponse().setMessage("User doesn't exist.").toJson());
		}

		String authType = bodyObject.optString("authType", Constants.NA);
		if (!authType.equalsIgnoreCase(Constants.PASSWORD) && !authType.equalsIgnoreCase(Constants.BIOMETRIC)) {

			return new OneResponse().getFailureResponse(
					new LocalResponse().setMessage("Invalid Auth type").setError(Errors.INVALID_DATA.value).toJson());

		}

		if (authType.equalsIgnoreCase(Constants.PASSWORD)) {

			String password = bodyObject.optString("password", Constants.NA);
			if (!password.equalsIgnoreCase(Constants.NA)) {
				LoggerUtils.log("initiatePayment - Authenticatin using password");

				if (!fetchedUser.password.equals(BasicUtils.getTheSecureKey(password))) {
					return new OneResponse().getFailureResponse(
							new LocalResponse().setMessage("Wrong password. Please enter correct password.")
									.setError(Errors.INVALID_PASSWORD.value).toJson());
				}
			}
			else
			{
				return new OneResponse().getFailureResponse(
						new LocalResponse().setMessage("Please enter a valid password.")
								.setError(Errors.INVALID_PASSWORD.value).toJson());
			}
		}

		JSONObject responseJson = new JSONObject();

		final var paymentRequest = bodyObject.optJSONObject("paymentRequest");
		paymentRequest.remove("id");
		
		Payment payment = gson.fromJson(paymentRequest.toString(), Payment.class);
		
		LoggerUtils.log("initiatePayment - received payment : " + gson.toJson(payment));

		//payment.id = -1;
		payment.userId = userId;
		payment.initialDateTime = DateTimeUtils.getCurrentDateTimeInIST();
		payment.orderId = "TXN" + System.currentTimeMillis();

		payment.completionDateTime = null;

		payment.status = "N";
		payment.statusMessage = "Payment Initiated";

		if (payment.paymentNature.equals(PaymentUtils.PaymentType.PART_PAYMENT.value)
				|| payment.paymentNature.equals(PaymentUtils.PaymentType.AUTO_PREPAY.value)) {
			payment.emiReduction = payment.prePaymentType.equals("EMI Reduction") ? 1 : 0;
			payment.tenurReduction = payment.prePaymentType.equals("Tenor Reduction") ? 1 : 0;
		}
		
		Payment storedPaymentValues = paymentRepo.addPayment(payment);

		if (null != storedPaymentValues) {

			payment.id = storedPaymentValues.id;

			responseJson = new JSONObject();
			responseJson.put(Constants.STATUS, Constants.SUCCESS);
			responseJson.put(Constants.MESSAGE, "Payment initiated successfully");
			responseJson.put("paymentDetail", payment.toJson());

		} else {
			return new OneResponse()
					.getFailureResponse(new LocalResponse().setMessage(Constants.DEFAULT_ERROR_MESSAGE).toJson());
		}

		return new OneResponse().getSuccessResponse(responseJson);

	}

}
