package utils;

import java.util.logging.Logger;

public class LoggerUtils {
	
	private static Logger logger =  Logger.getLogger(LoggerUtils.class.getSimpleName());
	
	public static void log(String value) {
		logger.info("\n\nCustomerPortal - value: " + value + "\n\n");
	}
	
	public static void logBody(String body) {
		logger.info("\n\nCustomerPortal - received body: " + body + "\n\n");
	}
	
	public static void logMethodCall(String value) {
		logger.info("\nCustomerPortal -\n----------------------\n  " + value + " method called  \n----------------------\n\n");
	}

}
