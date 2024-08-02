package utils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dao.BreachedPasswordRepository;
import models.User;
import utils.DateTimeUtils.DateTimeFormat;

public class BasicUtils {
	
	public static BreachedPasswordRepository breachPassWordRepo;

	public BasicUtils() {
		breachPassWordRepo = new BreachedPasswordRepository();
		
	}

	public Gson getGsonWithProxy() {

		GsonBuilder b = new GsonBuilder();
		b.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
		return b.create();
	}

	public static String getBase64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes());
	}

	public String getIPAddress(HttpServletRequest request) {

		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (null == ipAddress)
			ipAddress = request.getRemoteAddr();

		return ipAddress;

	}

	public String getMD5Hash(String string) throws NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] hashInBytes = md.digest(string.getBytes(StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString();

	}

	public static String getTheKey(String string) throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");
		String thePasscode = string + ProptertyUtils.getMamasSpaghetti();
		byte[] hashInBytes = md.digest(thePasscode.getBytes(StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString();

	}
	
	public static String getTheSecureKey(String string) throws Exception {

		MessageDigest md = MessageDigest.getInstance("SHA-512");
		String thePasscode = string + ProptertyUtils.getMamasSpaghetti();
		byte[] hashInBytes = md.digest(thePasscode.getBytes(StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString();

	}
	
	public static String getTheSecureKeyTemp(String string) throws Exception {

		MessageDigest md = MessageDigest.getInstance("SHA-512");
		String thePasscode = string;
		byte[] hashInBytes = md.digest(thePasscode.getBytes(StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString() + "GX#ZN|S}^yOb=,i|(82cHv!SaqAM-m";

	}


	public static JSONObject getSuccessTemplateObject() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(Constants.STATUS, Constants.SUCCESS);
		jsonObject.put(Constants.MESSAGE, Constants.NA);
		return jsonObject;
	}

	public static JSONObject getFailureTemplateObject() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(Constants.STATUS, Constants.FAILURE);
		jsonObject.put(Constants.MESSAGE, Constants.DEFAULT_ERROR_MESSAGE);
		return jsonObject;
	}

	public static boolean isLoanTopUp(String loanType) {

		String type = loanType.toLowerCase();

		return (type.contains("top-up") || type.contains("topup") || type.contains("top up"));
	}

	public static boolean isInsuranceLoan(String loanType) {

		String type = loanType.toLowerCase();
		return (type.contains("insurance"));
	}

	public enum LoanListRequestType {
		EMI_PAYMENT("emiPayment"), PART_PAYMENT("partPayment"), SERVICE_REQUEST_PAYMENT("serviceRequestPayment"),
		SERVICE_REQUEST("serviceRequest"), AUTO_PREPAY("autoPrepay"),  PROPERTY_IMAGES("propertyImages") ;

		public final String value;

		LoanListRequestType(String value) {
			this.value = value;
		}

		public static LoanListRequestType get(String value) {
			for (LoanListRequestType item : LoanListRequestType.values()) {
				if (item.value.equals(value))
					return item;
			}
			return null;
		}

	}

	public static boolean isNotNullOrNA(String value) {
		return (null != value && !value.equalsIgnoreCase(Constants.NA) && !value.equalsIgnoreCase("Null")
				&& !value.isEmpty());
	}

	public static boolean isNotNullOrNA(LocalDateTime value) {
		return null != value;
	}

	public static boolean isNotNullOrNA(double value) {
		return value != -1;
	}

	public static String getCurrencyString(double amount) {

		Format format = com.ibm.icu.text.NumberFormat.getCurrencyInstance(new Locale("en", "in"));
		String currency = format.format(new BigDecimal(amount));
		return currency.replace("₹ ", "").replace("₹", "").replace(".00", "").trim();

	}

	public static String getRandomKey() {

		Random random = new Random();
		double randomNumber = random.nextInt(999999); // Six 9s
		randomNumber += 213;
		return "YH8_" + (int) (randomNumber / 7) + "-8u" + (int) (randomNumber / 103) + "IK";

	}

	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}

	public BasicAuthCreds getClientCreds(String autherizationHeader) throws UnsupportedEncodingException {

		byte[] decodedBytes = Base64.getDecoder().decode(autherizationHeader.replaceFirst("Basic ", ""));
		String clientCredsString = new String(decodedBytes, Constants.UTF_8);
		StringTokenizer tokenizer = new StringTokenizer(clientCredsString, ":");

		String clientId = tokenizer.nextToken();
		String clientSecret = tokenizer.nextToken();

		return new BasicAuthCreds(clientId, clientSecret);

	}

	public static String convertToIndianCurrency(String num) {
		try {
			BigDecimal bd = new BigDecimal(num);
			long number = bd.longValue();
			long no = bd.longValue();
			int decimal = (int) (bd.remainder(BigDecimal.ONE).doubleValue() * 100);
			int digits_length = String.valueOf(no).length();
			int i = 0;
			ArrayList<String> str = new ArrayList<String>();
			HashMap<Integer, String> words = new HashMap<Integer, String>();
			words.put(0, "");
			words.put(1, "One");
			words.put(2, "Two");
			words.put(3, "Three");
			words.put(4, "Four");
			words.put(5, "Five");
			words.put(6, "Six");
			words.put(7, "Seven");
			words.put(8, "Eight");
			words.put(9, "Nine");
			words.put(10, "Ten");
			words.put(11, "Eleven");
			words.put(12, "Twelve");
			words.put(13, "Thirteen");
			words.put(14, "Fourteen");
			words.put(15, "Fifteen");
			words.put(16, "Sixteen");
			words.put(17, "Seventeen");
			words.put(18, "Eighteen");
			words.put(19, "Nineteen");
			words.put(20, "Twenty");
			words.put(30, "Thirty");
			words.put(40, "Forty");
			words.put(50, "Fifty");
			words.put(60, "Sixty");
			words.put(70, "Seventy");
			words.put(80, "Eighty");
			words.put(90, "Ninety");
			String digits[] = { "", "Hundred", "Thousand", "Lakh", "Crore" };
			while (i < digits_length) {
				int divider = (i == 2) ? 10 : 100;
				number = no % divider;
				no = no / divider;
				i += divider == 10 ? 1 : 2;
				if (number > 0) {
					int counter = str.size();
					String plural = (counter > 0 && number > 9) ? "s" : "";
					String tmp = (number < 21)
							? words.get(Integer.valueOf((int) number)) + " " + digits[counter] + plural
							: words.get(Integer.valueOf((int) Math.floor(number / 10) * 10)) + " "
									+ words.get(Integer.valueOf((int) (number % 10))) + " " + digits[counter] + plural;
					str.add(tmp);
				} else {
					str.add("");
				}
			}

			Collections.reverse(str);
			String Rupees = String.join(" ", str).trim();

			String paise = (decimal) > 0
					? " And " + words.get(Integer.valueOf((int) (decimal - decimal % 10))) + " "
							+ words.get(Integer.valueOf((int) (decimal % 10))) + " Paise"
					: "";
			return "Rupees " + Rupees + paise + " Only";

		} catch (Exception e) {
			LoggerUtils.log(" BasicUtils.convertToIndianCurrency - error while converting amount to words.");
			return Constants.NA;
		}
	}
	
	public static boolean isSignMatch(String bioCypher,String bioCrypt) throws Exception {

		
		//System.out.println("\nBioCyph-- cypher:" + bioCypher);
		//System.out.println("\nBioCyph-- crypt:" + bioCrypt);
		String cypher = new String(Base64.getDecoder().decode(bioCypher));
		String crypt = new String(Base64.getDecoder().decode(bioCrypt));
		//System.out.println("\nBioCyph-- cypher:" + cypher);
		//System.out.println("\nBioCyph-- crypt:" + crypt);

		String[] cryptSplit = crypt.split("[.]");
		String timeStamp = cryptSplit[0];
		//System.out.println("\nBioCyph-- timeStamp:" + timeStamp);

		String order = cryptSplit[1];
		//System.out.println("\nBioCyph-- order:" + order);

		String[] valList = cypher.split("-");

		ArrayList<String> valArray = new ArrayList<String>(3);
		valArray.add("");
		valArray.add("");
		valArray.add("");

		int[] orderArray = new int[order.length()];
		for (int i = 0; i < order.length(); i++) {
			orderArray[i] = Integer.parseInt(String.valueOf(order.charAt(i)));
		}

		valArray.set(orderArray[0], valList[1]);
		valArray.set(orderArray[1], valList[3]);
		valArray.set(orderArray[2], valList[5]);

		StringBuilder mills = new StringBuilder();
		for (int i = 0; i < valArray.size(); i++) {

			mills.append(valArray.get(i));

		}

		//System.out.println("\nBioCyph-- mills:" + mills);
		String dateTime = DateTimeUtils.getDateTimeFromMills(mills.toString(), DateTimeFormat.yyyy_MM_dd_HH_mm_ss);
		//System.out.println("\nBioCyph-- dateTime:" + dateTime);
		
		//System.out.println("\nBioCyph-- dateTime:" + DateTimeUtils.getDateDifferenceInMinutes(dateTime));

		//LoggerUtils.log("CypherVerify1:"+ timeStamp.equalsIgnoreCase(dateTime) );
		//LoggerUtils.log("CypherVerify2:"+ DateTimeUtils.getDateDifferenceInMinutes(dateTime));
	
		
		if (timeStamp.equalsIgnoreCase(dateTime) && DateTimeUtils.getDateDifferenceInMinutes(dateTime) == 0)
			return true;
		else {
			
			LoggerUtils.log("\nBioCyph-- Rcypher:" + bioCypher	+"\tBioCyph-- Rcrypt:" + bioCrypt);
			LoggerUtils.log("\nBioCyph-- AfterDecode cypher:" + cypher + "\tBioCyph-- crypt:" + crypt);
			LoggerUtils.log("\nBioCyph-- timeStamp:" + timeStamp);
			LoggerUtils.log("\nBioCyph-- order:" + order);
			LoggerUtils.log("\nBioCyph-- DecodedMills:" + mills);
			LoggerUtils.log("\nBioCyph-- SystemDateTime:" + dateTime);
			
			return false;
		}

	}
	
	public enum MimeMap {

		PDF("application/pdf", ".pdf"), PNG("image/png", ".png"), JPG("image/jpeg", ".jpg");

		public final String mime;
		public final String extention;

		private MimeMap(String mime, String extention) {
			this.mime = mime;
			this.extention = extention;
		}

		public static String mapMimetoExt(String mime) {
			for (MimeMap item : MimeMap.values()) {
				if (item.mime.equals(mime))
					return item.extention;
			}
			return "Unknown";

		}

		public static String mapExtToMime(String ext) {
			for (MimeMap item : MimeMap.values()) {
				if (item.extention.equals(ext))
					return item.mime;
			}
			return "Unknown";

		}

	}
	
	public static String getPasscodeHash(User user) throws Exception {
		Random random = new Random();
		double randomNumber = random.nextInt(99999);
		return (new BasicUtils()).getMD5Hash(user.mobileNumber + user.crmAccountNumber + (randomNumber));
	}
	
	public static boolean isBreachedPassword(String cPassword) {
		
		var passwords = breachPassWordRepo.findEncryptedPasswords();
		
		if(passwords.contains(cPassword)) {
			return true;
		}else return false;
		
	}

}
