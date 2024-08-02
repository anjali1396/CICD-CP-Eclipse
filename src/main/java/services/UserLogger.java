package services;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import models.admin.PartnerLog;
import utils.LoggerUtils;
import v2.dbhelpers.HFPartnerDBHelper;

public class UserLogger {

	private HFPartnerDBHelper hfpDBHelper;
	private Calendar calendar;
	private int count = 0;
	
	public UserLogger() {
		calendar = Calendar.getInstance();
		hfpDBHelper = new HFPartnerDBHelper();
	}
	
	private void closeResources() {
		hfpDBHelper.close();
	}

	public void logPartnerRequest(PartnerLog pLog) {

		calendar.add(Calendar.SECOND, 5);
		Date time = calendar.getTime();		

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {

			@Override
			public void run() {

				if (count < 3) {
					
					try {
						
						if (hfpDBHelper.addPartnerLog(pLog)) {
											
							LoggerUtils.log("Partner request log Task completed succesfully, Iteration: " + count);
							count = 0;
							closeResources();
							timer.cancel();
							
						} else {
							
							closeResources();
							count++;
							LoggerUtils.log("Partner request log Task rescheduled, Iteration: " + count);
							
						}
						
					} catch (Exception e) {

						closeResources();
						LoggerUtils.log("Error while loggin Partner request : " + e.getMessage());
						e.printStackTrace();

						count++;
						LoggerUtils.log("Partner request log Task rescheduled, Iteration: " + count);

					}


				} else {

					LoggerUtils.log("Time's up! Failed to logPartnerRequest.");
					timer.cancel();

				}
			}
			
		}, time, 30000);

	}

}
