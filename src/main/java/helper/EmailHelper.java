package helper;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import models.sob.Prospect;
import utils.Constants;
import utils.LocalResponse;
import utils.LoggerUtils;
import v2.managers.ContactManager;

public class EmailHelper {

	public void sendSobConfirmationSMS(Prospect prospect) {

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 5);
		Date time = calendar.getTime();

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			int count = 0;

			@Override
			public void run() {

				if (count < 3) {

					try {

						if (!Constants.IS_STRICT_PROD_PROCESS_ACTIVE) {
							timer.cancel();
							return;
						}

						ContactManager cManager = new ContactManager();

						JSONObject smsBody = new JSONObject();

						smsBody.put("mobiles", "91" + prospect.mobileNumber);
						smsBody.put("flow_id", ContactManager.SOB_CONFIRMATION_FLOW_ID);
						smsBody.put("ref", prospect.referenceId);

						LocalResponse smsResponse = cManager.sendSMSViaFlow(smsBody);

						if (smsResponse.isSuccess) {

							LoggerUtils.log("sendSobConfirmationSMS - SOB confirmation SMS Task completed; Iteration: "
									+ count);
							timer.cancel();

						} else {

							count++;
							LoggerUtils.log("sendSobConfirmationSMS - Error while sening SOB confirmation SMS: "
									+ smsResponse.error + " | Rescheduled, Iteration: " + count);

						}

					} catch (Exception e) {

						LoggerUtils.log(
								"sendSobConfirmationSMS - Error while sending SOB confirmation SMS: " + e.getMessage());
						e.printStackTrace();
						count++;
						LoggerUtils.log(
								"sendSobConfirmationSMS - SOB confirmation SMS Task rescheduled, Iteration: " + count);

					}

				} else {

					timer.cancel();
					LoggerUtils.log("sendSobConfirmationSMS - Time's up! Failed to send SOB confirmation SMS.");

				}

			}
		}, time, 15000);

	}

}
