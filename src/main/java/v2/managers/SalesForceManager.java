package v2.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import dao.LendingProductsRepository;
import models.CLContract;
import models.Loan;
import models.Referrals;
import models.User;
import models.payment.CPPaymentInfo;
import models.payment.Payment;
import models.payment.SFAccountStatement;
import utils.BasicUtils;
import utils.BasicUtils.LoanListRequestType;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.LocalResponse;
import utils.LoggerUtils;
import utils.PaymentUtils;
import utils.ProptertyUtils;

public class SalesForceManager {

	private static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
	private static String REST_ENDPOINT = "/services/data";
	private static String API_VERSION = "/v45.0";
	private static String baseUri = null;
	private static Header oauthHeader = null;
	private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");

	private int retryCount = 0;

	enum PaymentType {
		RECENT_MONTHLY_PAYMENT("Recent monthly payments"), MONTHLY_PAYMENTS_DUE("Monthly payments due"),
		__MONTHLY_PAYMENTS_DUE("Montly payments due"), RECENT_PART_PAYMENTS("Recent part payments"),
		AUTO_PREPAY("Auto Prepay"), AUTO_REPAY("Auto repay");

		public final String value;

		PaymentType(String value) {
			this.value = value;
		}
	}

	public SalesForceManager() {
	}

	// ====================== COMMON METHODS AND ATTRIBUTES =======================
	// //

	private void checkAndAuthenticate() throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

	}

	private void authenticate() throws Exception {

		HttpClient httpclient = HttpClientBuilder.create().build();

		String loginUrl = Constants.NA, clientId = Constants.NA, clientSecret = Constants.NA, username = Constants.NA,
				password = Constants.NA;

		if (Constants.IS_SF_LIVE) {
			loginUrl = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_LOGIN_URL);
			clientId = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_CLIENT_ID);
			clientSecret = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_CLIENT_SECRET);
			username = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_USERNAME);
			password = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_PROD_PASSWORD);
		} else {
			loginUrl = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_LOGIN_URL);
			clientId = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_CLIENT_ID);
			clientSecret = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_CLIENT_SECRET);
			username = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_USERNAME);
			password = ProptertyUtils.getValurForKey(ProptertyUtils.Keys.SF_TEST_PASSWORD);
		}

		// Assemble the login request URL
		String loginURL = loginUrl + GRANTSERVICE + "&client_id=" + clientId + "&client_secret=" + clientSecret
				+ "&username=" + username + "&password=" + password;

		// LoggerUtils.log(loginURL);

		// Login requests must be POSTs
		HttpPost httpPost = new HttpPost(loginURL);
		HttpResponse response = null;

		try {
			// Execute the login POST request
			response = httpclient.execute(httpPost);
		} catch (ClientProtocolException cpException) {
			cpException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			LoggerUtils
					.log("Error authenticating to Force.com: " + statusCode + " | error: " + response.getStatusLine());
			// Error is in EntityUtils.toString(response.getEntity())
			return;
		}

		String getResult = null;
		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		try {
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();

			LoggerUtils.log(" received data: " + jsonObject.toString());

			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
		}

		baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION;
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);

		System.out.println(response.getStatusLine());
		System.out.println("Successful login");
		System.out.println("  instance URL: " + loginInstanceUrl);
		System.out.println("  access token/session ID: " + loginAccessToken);
		System.out.println("baseUri: " + baseUri);

		// release connection
		httpPost.releaseConnection();

	}

	private JSONObject getSalesforceData(String query) {

		try {

			// Set up the HTTP objects needed to make the request.
			HttpClient httpClient = HttpClientBuilder.create().build();

			String uri = baseUri + "/query?q=" + query;
			// System.out.println("Query URL: " + uri);
			HttpGet httpGet = new HttpGet(uri);
			// System.out.println("oauthHeader2: " + oauthHeader);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);

			// Make the request.
			HttpResponse response = httpClient.execute(httpGet);

			// Process the result
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				retryCount = 0;
				String response_string = EntityUtils.toString(response.getEntity());
				try {

					JSONObject json = new JSONObject(response_string);
					System.out.println("JSON result of Query:\n" + json.toString());
					return json;

				} catch (JSONException je) {
					je.printStackTrace();
					return null;
				}
			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return getSalesforceData(query);
			} else {
				retryCount = 0;
				System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
				System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
				System.out.println(getBody(response.getEntity().getContent()));
				// System.exit(-1);
				return null;
			}

		} catch (Exception e) {
			LoggerUtils.log("error while getting data from salesforce: " + e.toString());
			e.printStackTrace();
			return null;
		}

	}

	private String getModifiedQuery(String query) throws UnsupportedEncodingException {
		// return query.replace(" ", "+");
		return URLEncoder.encode(query, "UTF-8");
	}

	private static String getBody(InputStream inputStream) {
		String result = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				result += inputLine;
				result += "\n";
			}
			in.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	// ******************* END OF COMMON METHODS AND ATTRIBUTES ********************
	// //

	// ================= START OF ALL HOME IMPLEMENTATIONS ================== //
	// ======================================================================= //

	public JSONObject getUserDashboard(String crmAccountNumber) throws Exception {

		checkAndAuthenticate();

		JSONObject responseJson = new JSONObject();

		String query = "select Name, Sub_Product_Type__c, loan__Principal_Remaining__c,"
				+ " X_Sell_Products__r.Insurance_Sum_Assured__c, Opportunity__c, Opportunity_Number__c,"
				+ " loan__ACH_Debit_Amount__c, loan__Loan_Status__c, loan__Loan_Amount__c from loan__Loan_Account__c"
				+ " where CRM_Account_Number__c='" + crmAccountNumber + "'"
				+ " and loan__Loan_Status__c in ('Active - Good Standing', 'Active - Bad Standing', 'Approved', "
				+ "'Closed - Obligations met', 'Closed- Written Off')";
		

		JSONObject loanJson = getSalesforceData(getModifiedQuery(query));

		query = "select CL_Contract_No_LAI__c,CaseNumber,Case_Reason_sub__c,Status,"
				+ "Resolution_Comments_new__c from Case where Account.CRM_ACCOUNT_NO__c='" + crmAccountNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == loanJson || null == requestsJson)
			return null;

		ArrayList<Loan> loans = new ArrayList<>();

		if (loanJson.getInt("totalSize") > 0) {
			JSONArray records = loanJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);

				Loan loan = new Loan();
				loan.accountNumber = current.optString("Name", Constants.NA);

				if (!loan.accountNumber.equals(Constants.NA)) {

					loan.loanType = current.optString("Sub_Product_Type__c", Constants.NA);
					loan.principalAmount = current.optDouble("loan__Principal_Remaining__c", 0.0);
					loan.loanAmount = current.optDouble("loan__Loan_Amount__c", 0.0);
					loan.debitAmount = current.optDouble("loan__ACH_Debit_Amount__c", 0.0);
					loan.status = current.optString("loan__Loan_Status__c", Constants.NA);
					if (loan.status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED)) {
						loan.loanType += " (Closed)";
						loan.principalAmount = 0.0;
						loan.loanAmount = 0.0;
						loan.debitAmount = 0.0;
					}

					JSONObject xSellProduct = current.optJSONObject("X_Sell_Products__r");
					if (null != xSellProduct) {
						double totalInsuredAmount = xSellProduct.optDouble("Insurance_Sum_Assured__c", -1);
						loan.totalInsuredAmount = totalInsuredAmount;
					}

					loans.add(loan);

				}

			}
		}

		loans = getLoansWithUpatedAmount(loans);

		JSONArray accounts = new JSONArray();

		for (Loan loan : loans) {

			// a single loan account detail object
			JSONObject account = new JSONObject();
			account.put("account", loan.getSummaryJson());

			// get service request for this loan and add it to the account object
			int closedRequests = 0;
			int newRequests = 0;

			if (requestsJson.getInt("totalSize") > 0) {

				JSONArray rRecords = requestsJson.getJSONArray("records");
				for (int j = 0; j < rRecords.length(); j++) {
					String lai = rRecords.getJSONObject(j).optString("CL_Contract_No_LAI__c", Constants.NA);
					if (lai.equalsIgnoreCase(loan.accountNumber)) {
						if (rRecords.getJSONObject(j).getString("Status").equalsIgnoreCase("Closed"))
							closedRequests++;
						else
							newRequests++;
					}
				}

			}

			JSONObject userRequest = new JSONObject();
			userRequest.put("closed", closedRequests);
			userRequest.put("new", newRequests);
			account.put("serviceRequest", userRequest);

			// finally add single account detail to the accounts array
			accounts.put(account);

		}

		responseJson.put("accounts", accounts);

		return responseJson;

	}

	public JSONObject getLoanDetail(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Name,Sub_Product_Type__c,loan__Loan_Amount__c,loan__Principal_Remaining__c,"
				+ "loan__Interest_Rate__c,loan__ACH_Debit_Amount__c,"
				+ "loan__Disbursed_Amount__c,loan__Number_of_Installments__c,Loan_Type_Indicator1__c,loan__Loan_Status__c"
				+ " from loan__Loan_Account__c where CRM_Account_Number__c='" + crmAccountNumber + "'"
				+ " and loan__Loan_Status__c in ('Active - Good Standing', 'Active - Bad Standing', 'Approved', "
				+ "'Closed - Obligations met', 'Closed- Written Off')";
		

		JSONObject loanJson = getSalesforceData(getModifiedQuery(query));

		if (null == loanJson)
			return null;

		ArrayList<Loan> loans = new ArrayList<Loan>();

		if (loanJson.getInt("totalSize") > 0) {
			JSONArray records = loanJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);
				Loan loan = new Loan();
				loan.accountNumber = current.optString("Name", Constants.NA);
				if (!loan.accountNumber.equalsIgnoreCase(Constants.NA)) {

					loan.loanType = current.optString("Sub_Product_Type__c", Constants.NA);
					loan.principalAmount = current.optDouble("loan__Principal_Remaining__c", 0.0);
					loan.debitAmount = current.optDouble("loan__ACH_Debit_Amount__c", 0.0);
					loan.loanAmount = current.optDouble("loan__Loan_Amount__c", 0.0);
					loan.paymentType = current.optString("Loan_Type_Indicator1__c", Constants.NA);
					loan.interestRate = current.optDouble("loan__Interest_Rate__c", 0.0);
					loan.disbursedAmount = current.optDouble("loan__Disbursed_Amount__c", 0.0);
					loan.numberOfInstallments = current.optInt("loan__Number_of_Installments__c", 0);
					loan.status = current.optString("loan__Loan_Status__c", Constants.NA);
					if (loan.status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED)) {
						loan.loanType += " (Closed)";
						loan.principalAmount = 0.0;
						loan.debitAmount = 0.0;
					}
					loans.add(loan);

				}
			}
		}

		loans = getLoansWithUpatedAmount(loans);

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getCompleteJson());
		}

		responseJson.put("loans", loanDetails);

		return responseJson;
	}

	public JSONObject getPaymentDetails(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Name,Sub_Product_Type__c,loan__Principal_Remaining__c,"
				+ "loan__ACH_Debit_Amount__c,loan__Loan_Status__c from loan__Loan_Account__c where CRM_Account_Number__c='"
				+ crmAccountNumber + "'";

		JSONObject loanJson = getSalesforceData(getModifiedQuery(query));

		if (null == loanJson)
			return null;

		ArrayList<Loan> loans = new ArrayList<Loan>();

		if (loanJson.getInt("totalSize") > 0) {
			JSONArray records = loanJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				Loan loan = new Loan();
				loan.accountNumber = current.optString("Name", Constants.NA);
				if (!loan.accountNumber.equalsIgnoreCase(Constants.NA)) {
					loan.loanType = current.optString("Sub_Product_Type__c", Constants.NA);
					loan.principalAmount = current.optDouble("loan__Principal_Remaining__c", 0.0);
					loan.debitAmount = current.optDouble("loan__ACH_Debit_Amount__c", 0.0);
					loan.status = current.optString("loan__Loan_Status__c", Constants.NA);
					if (loan.status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED)) {
						loan.loanType += " (Closed)";
						loan.principalAmount = 0.0;
						loan.debitAmount = 0.0;
					}
					loans.add(loan);
				}
			}
		}

		loans = getLoansWithUpatedAmount(loans);

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getSummaryJson());
		}

		responseJson.put("loans", loanDetails);

		return responseJson;

	}

	private ArrayList<Loan> getLoansWithUpatedAmount(ArrayList<Loan> loans) throws Exception {

		if (loans.size() > 0) {

			ArrayList<Loan> homeLoans = new ArrayList<>();
			ArrayList<Loan> topUpLoans = new ArrayList<>();

			for (Loan loan : loans) {
				if (BasicUtils.isLoanTopUp(loan.loanType))
					topUpLoans.add(loan);
				else
					homeLoans.add(loan);
			}

			String homeLoanListString = "";
			String topUpLoanListString = "";

			if (homeLoans.size() > 0) {
				for (int i = 0; i < homeLoans.size(); i++) {
					homeLoanListString += "'" + homeLoans.get(i).accountNumber + "'";
					if (homeLoans.size() > 1 && i < homeLoans.size() - 1)
						homeLoanListString += ",";
				}
			}

			if (topUpLoans.size() > 0) {
				for (int i = 0; i < topUpLoans.size(); i++) {
					topUpLoanListString += "'" + topUpLoans.get(i).accountNumber + "'";
					if (topUpLoans.size() > 1 && i < topUpLoans.size() - 1)
						topUpLoanListString += ",";
				}
			}

			ArrayList<CPPaymentInfo> topupPayments = new ArrayList<>();
			ArrayList<CPPaymentInfo> homePayments = new ArrayList<>();

			if (!topUpLoanListString.isEmpty())
				topupPayments = getTopUpDetailsForDashboard(topUpLoanListString);
			if (!homeLoanListString.isEmpty())
				homePayments = getHomeLoanDetailsForDashboard(homeLoanListString);

			for (Loan loan : loans) {

				if (BasicUtils.isLoanTopUp(loan.loanType)) {
					loan = updateLoanCurrentStatus(loan, topupPayments);
				} else {
					loan = updateLoanCurrentStatus(loan, homePayments);
				}

			}

		}

		return loans;

	}

	private Loan updateLoanCurrentStatus(Loan loan, ArrayList<CPPaymentInfo> pLoans) {

		for (CPPaymentInfo item : pLoans) {
			if (item.loanAccountNumber.equalsIgnoreCase(loan.accountNumber)) {

				loan.debitAmount = item.loanAmount;
				// double amountDue = item.totalDebitAmount - item.collectedAmount;
				loan.totalDebitAmount = item.excessShortfallAmount;
				loan.insuranceDebitAmount = item.insuranceLoanAmount;

				break;
			}
		}

		return loan;
	}

	private ArrayList<CPPaymentInfo> getTopUpDetailsForDashboard(String lai) throws Exception {

		checkAndAuthenticate();

		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "Top_Up_CL_Contract_No_LAI__c, Debit_Amount_Top_Up__c, Top_Up_Insurance__c,"
				+ "Total_Debit_Amount_Topup__c,Top_Up_Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c WHERE Top_Up_CL_Contract_No_LAI__c IN (" + lai + ") "
				+ "and RecordType.DeveloperName = 'X_Sell_Product_Collection_Layout' order by CreatedDate desc limit 20";

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		ArrayList<CPPaymentInfo> payments = new ArrayList<CPPaymentInfo>();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				payments.add(getParsedCPPaymnetInfoForTL(current));

			}
		}

		return payments;
	}

	private ArrayList<CPPaymentInfo> getHomeLoanDetailsForDashboard(String lai) throws Exception {

		checkAndAuthenticate();

		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "CL_Contract_No_LAI__c, Debit_Amount_Housing__c, Debit_Amount_Insurance__c,"
				+ "Total_Debit_Amount__c, Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c WHERE CL_Contract_No_LAI__c IN (" + lai + ") "
				+ "and RecordType.DeveloperName = 'Housing_Loan_Collections' order by CreatedDate desc limit 20";

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		ArrayList<CPPaymentInfo> payments = new ArrayList<CPPaymentInfo>();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				payments.add(getParsedCPPaymnetInfoForHL(current));

			}
		}

		return payments;
	}

	// ******************* END OF ALL HOME IMPLEMENTATIONS ****************** //
	// *********************************************************************** //

	// ================= START OF PAYMENT IMPLEMENTATIONS ==================== //
	// ======================================================================= //

	public JSONArray getPaymentDetailForLoan(JSONObject requestBody) throws Exception {

		String loanType = requestBody.getString("loanType");
		String lai = requestBody.getString("loanAccountNumber");

		if (BasicUtils.isLoanTopUp(loanType))
			return getTopUpPaymentDetail(lai);
		else
			return getHLPaymentDetail(lai);

	}

	private JSONArray getTopUpPaymentDetail(String lai) throws Exception {

		checkAndAuthenticate();

		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "Top_Up_CL_Contract_No_LAI__c, Debit_Amount_Top_Up__c, Top_Up_Insurance__c,"
				+ "Total_Debit_Amount_Topup__c,Top_Up_Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c "
				+ "WHERE Clearance_Status__c not in ('Waived', 'Loan Closed', 'EMI Extension', 'Cleared', 'EMI Moratorium', "
				+ "'EMI Moratorium - (Partial Cleared)', 'Restructured 2.0' , 'Restructured 2.0 - (Partial Cleared)')"
				+ "AND Top_Up_CL_Contract_No_LAI__c = '" + lai + "' and RecordType.DeveloperName = 'X_Sell_Product_Collection_Layout' "
				+ "order by CreatedDate desc";

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		JSONArray payments = new JSONArray();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);
				payments.put(getParsedCPPaymnetInfoForTL(current).toJson());

			}
		}

		return payments;
	}

	private JSONArray getHLPaymentDetail(String lai) throws Exception {

		checkAndAuthenticate();

		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "CL_Contract_No_LAI__c, Debit_Amount_Housing__c, Debit_Amount_Insurance__c,"
				+ "Total_Debit_Amount__c, Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c "
				+ "WHERE Clearance_Status__c not in ('Waived', 'Loan Closed', 'EMI Extension', 'Cleared', 'EMI Moratorium', "
				+ "'EMI Moratorium - (Partial Cleared)', 'Restructured 2.0' , 'Restructured 2.0 - (Partial Cleared)')"
				+ "AND CL_Contract_No_LAI__c = '" + lai + "' and RecordType.DeveloperName = 'Housing_Loan_Collections' "
				+ "order by CreatedDate desc";

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		JSONArray payments = new JSONArray();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);
				payments.put(getParsedCPPaymnetInfoForHL(current).toJson());

			}
		}

		return payments;
	}

	public Payment addPaymentReceipt(User user, Payment payment) throws Exception {

		checkAndAuthenticate();

		String uri = baseUri + "/sobjects/Cash_Receipt__c/";

		try {

			JSONObject metaData = getAccountNameAndOpportunity(payment.loanAccountNumber);

			JSONObject receiptObject = new JSONObject();
			receiptObject.put("Opportunity__c", metaData.getString("opportunityId"));

			if (payment.paymentMethod.equalsIgnoreCase(PaymentUtils.PaymentMethod.PAYNIMO.value)) {
				receiptObject.put("Payment_Mode__c", "payNimo");
				receiptObject.put("Bank_Deposit_Mode__c", "PayNimo");
				receiptObject.put("Deposit_in_HFFC_Bank_Account__c", Constants.BANK_ACCOUNT_FOR_PAYNIMO);
			} else {
				receiptObject.put("Payment_Mode__c", payment.paymentMethod);
				receiptObject.put("Bank_Deposit_Mode__c", payment.paymentMethod);
				receiptObject.put("Deposit_in_HFFC_Bank_Account__c", Constants.BANK_ACCOUNT_FOR_RAZORPAY);
			}

			String completetionDateTime = DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd_T_HH_mm_ss_SSSZ,
					DateTimeZone.IST);
			receiptObject.put("Receipt_Date__c", completetionDateTime);

			receiptObject.put("Cheque_Deposit_Date__c", DateTimeUtils.getDateTimeFromString(payment.initialDateTime,
					DateTimeFormat.yyyy_MM_dd_HH_mm_ss, DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST));

			receiptObject.put("Receipt_Type_Nature_of_Payme__c", payment.paymentNature);

			if (payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.PART_PAYMENT.value)
					|| payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.AUTO_PREPAY.value)) {

				receiptObject.put("Prepayment_Type__c", payment.prePaymentType);

			} else if (payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.SERVICE_REQUEST.value)) {

				receiptObject.put("Nature_of_Payment_Payment_Sub_type_1__c", payment.paymentSubType);

			} else if (payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.EMI.value)) {

				if (null != payment.paymentSubType && !payment.paymentSubType.isEmpty()
						&& !payment.paymentSubType.equalsIgnoreCase(Constants.NA)
						&& !payment.paymentSubType.equalsIgnoreCase(Constants.KEY_ACCOUNT_STATEMENT)) {

					receiptObject.put("Nature_of_Payment_Payment_Sub_type_1__c", payment.paymentSubType);

				}

			} else if (payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.OTHER_CHARGES.value)) {

				receiptObject.put("Nature_of_Payment_Payment_Sub_type_1__c", payment.paymentSubType);

			}

			String amountInWords = BasicUtils.convertToIndianCurrency(String.valueOf(payment.paymentAmount));

			if (BasicUtils.isNotNullOrNA(amountInWords))
				receiptObject.put("Amount_in_Words_1__c", amountInWords);

			// receiptObject.put("Insurance_Refund__c", "No");
			receiptObject.put("Paynimo_Response__c", payment.receiptData);
			receiptObject.put("Receipt_Amount__c", payment.paymentAmount);
			receiptObject.put("Cheque_No_Transaction_No__c", payment.paymentId);

			String xSellProduct = metaData.optString("xSellProductId", Constants.NA);
			String loanProductType = metaData.optString("loanProductType", Constants.NA);

			if (null != new LendingProductsRepository().findTopupProductById(loanProductType)) {

				receiptObject.put("RecordTypeId", Constants.RECORD_TYPE_TOPUP_LOAN);

				if (BasicUtils.isNotNullOrNA(xSellProduct))
					receiptObject.put("X_Sell_Products_Receipts__c", xSellProduct);

			} else
				receiptObject.put("RecordTypeId", Constants.RECORD_TYPE_HOME_LAON);

			HttpClient httpClient = HttpClientBuilder.create().build();

			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);
			StringEntity body = new StringEntity(receiptObject.toString(1));
			body.setContentType("application/json");
			httpPost.setEntity(body);

			HttpResponse response = httpClient.execute(httpPost);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 201) {
				retryCount = 0;
				String response_string = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(response_string);
				LoggerUtils.log("Receipt response: " + json);
				String receiptId = json.getString("id");
				LoggerUtils.log("New receipt created with id: " + receiptId);
				payment.receiptId = receiptId;
				payment.receiptNumber = getReceiptNumber(receiptId);

			} else if (statusCode == 401 && retryCount < 3) {
				LoggerUtils.log("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return addPaymentReceipt(user, payment);
			} else {
				retryCount = 0;
				LoggerUtils.log("Insertion unsuccessful. Status code returned is " + statusCode + " | error: "
						+ response.getStatusLine());
				System.out.println(getBody(response.getEntity().getContent()));
			}
		} catch (JSONException e) {
			System.out.println("Issue creating JSON or processing results");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		return payment;

	}

	public String getReceiptInfoByIdAndLAI(String paymentId, String loanAccountNumber) throws Exception {

		checkAndAuthenticate();

		String receiptInfo = Constants.NA;

		String query = "Select Name, Id from Cash_Receipt__c where Cheque_No_Transaction_No__c='" + paymentId + "'"
				+ " and (CL_Contract_No_LAI__c = '" + loanAccountNumber + "'" + " or Top_Up_CL_Contract_No_LAI__c = '"
				+ loanAccountNumber + "')";

		LoggerUtils.log("getReceiptInfoByIdAndLAI - query : " + query);

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return receiptInfo;

		if (requestsJson.getInt("totalSize") > 0) {
			receiptInfo = requestsJson.getJSONArray("records").getJSONObject(0).getString("Id");
			receiptInfo += ";" + requestsJson.getJSONArray("records").getJSONObject(0).getString("Name");
		}

		return receiptInfo;

	}

	private JSONObject getAccountNameAndOpportunity(String loanAccountNumber) throws Exception {
		salesforce.SalesForceManager v1SFManager = new salesforce.SalesForceManager();
		return v1SFManager.getAccountNameAndOpportunity(loanAccountNumber);
	}

	private String getReceiptNumber(String receiptId) throws Exception {
		salesforce.SalesForceManager v1SFManager = new salesforce.SalesForceManager();
		return v1SFManager.getReceiptNumber(receiptId);
	}

	// ******************* END OF PAYMENT IMPLEMENTATIONS ******************** //
	// *********************************************************************** //

	private CPPaymentInfo getParsedCPPaymnetInfoForHL(JSONObject json) {

		CPPaymentInfo pInfo = new CPPaymentInfo();

		pInfo.loanAmount = json.optDouble("Debit_Amount_Housing__c", 0);
		pInfo.insuranceLoanAmount = json.optDouble("Debit_Amount_Insurance__c", 0);
		pInfo.totalDebitAmount = json.optDouble("Total_Debit_Amount__c", 0);
		pInfo.collectedAmount = json.optDouble("Collected_Amount__c", 0);
		pInfo.excessShortfallAmount = json.optDouble("Payment_Excess_Shortfall__c", 0);
		pInfo.dueDate = json.optString("Due_Date__c", Constants.NA);
		pInfo.collectionMonthYear = json.optString("Month__c", Constants.NA);
		pInfo.paymentClearedDate = json.optString("Payment_Cleared_Date__c", Constants.NA);
		pInfo.loanAccountNumber = json.optString("CL_Contract_No_LAI__c", Constants.NA);
		pInfo.clearanceStatus = json.optString("Clearance_Status__c", Constants.NA);
		pInfo.isBounced = json.optString("First_Time_Bounce__c", "No").equalsIgnoreCase("Yes")
				|| pInfo.clearanceStatus.equalsIgnoreCase("Bounced");

		return pInfo;

	}

	private CPPaymentInfo getParsedCPPaymnetInfoForTL(JSONObject json) {

		CPPaymentInfo pInfo = new CPPaymentInfo();

		pInfo.loanAmount = json.optDouble("Debit_Amount_Top_Up__c", 0);
		pInfo.insuranceLoanAmount = json.optDouble("Top_Up_Insurance__c", 0);
		pInfo.totalDebitAmount = json.optDouble("Total_Debit_Amount_Topup__c", 0);
		pInfo.collectedAmount = json.optDouble("Collected_Amount__c", 0);
		pInfo.dueDate = json.optString("Due_Date__c", Constants.NA);
		pInfo.excessShortfallAmount = json.optDouble("Top_Up_Payment_Excess_Shortfall__c", 0);
		pInfo.collectionMonthYear = json.optString("Month__c", Constants.NA);
		pInfo.paymentClearedDate = json.optString("Payment_Cleared_Date__c", Constants.NA);
		pInfo.loanAccountNumber = json.optString("Top_Up_CL_Contract_No_LAI__c", Constants.NA);
		pInfo.clearanceStatus = json.optString("Clearance_Status__c", Constants.NA);
		pInfo.isBounced = json.optString("First_Time_Bounce__c", "No").equalsIgnoreCase("Yes")
				|| pInfo.clearanceStatus.equalsIgnoreCase("Bounced");

		return pInfo;

	}

	public JSONObject getLoanAccountList(String crmAccountNumber, LoanListRequestType condition, String filter)
			throws Exception {

		checkAndAuthenticate();

		JSONObject responseJson = new JSONObject();

		StringBuilder query = new StringBuilder("select Name,Sub_Product_Type__c,loan__Loan_Status__c "
				+ "from loan__Loan_Account__c where CRM_Account_Number__c='" + crmAccountNumber + "'");

		if (condition == LoanListRequestType.PROPERTY_IMAGES) {
			query = new StringBuilder("select Name,Sub_Product_Type__c,loan__Loan_Status__c ,"
					+ " Opportunity__r.Property__r.Name , Opportunity__r.Property__r.Id ,Opportunity__r.Property__r.Property_code__c"
					+ " ,Opportunity__r.Property__r.Property_Geo_Location__c , Opportunity__r.Property__r.OwnerId , "
					+ " Opportunity__r.Property__r.Plot_ID__c , Opportunity__r.Property__r.Project_Address__c ,"
					+ " Opportunity__r.Property__r.Project_City__c , Opportunity__r.Property__r.Project_State__c ,"
					+ " Opportunity__r.Property__r.Project_Pin_Code__c, " + " Opportunity__r.Property__r.Owner.Email "
					+ "from loan__Loan_Account__c where CRM_Account_Number__c='" + crmAccountNumber + "'");

			if (BasicUtils.isNotNullOrNA(filter))
				query.append(" and loan__Loan_Product_Name__c in (" + filter + ")");

			LoggerUtils.log(query.toString());
		}

		query.append(" and loan__Loan_Status__c != '" + Constants.LOAN_TYPE_CANCELED + "'");
		query.append(" and loan__Loan_Status__c != '" + Constants.LOAN_TYPE_CANCELLED + "'");

		if (condition == LoanListRequestType.EMI_PAYMENT || condition == LoanListRequestType.PART_PAYMENT
				|| condition == LoanListRequestType.AUTO_PREPAY) {
			query.append(" and loan__Loan_Status__c != '" + Constants.LOAN_TYPE_CLOSED + "'");
		}

		JSONObject loanJson = getSalesforceData(getModifiedQuery(query.toString()));

		if (null == loanJson)
			return null;

		ArrayList<Loan> loans = new ArrayList<Loan>();

		if (loanJson.getInt("totalSize") > 0) {
			JSONArray records = loanJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				Loan loan = new Loan();
				loan.accountNumber = current.optString("Name", Constants.NA);

				if (condition == LoanListRequestType.PROPERTY_IMAGES) {

					JSONObject propertyJson = current.optJSONObject("Opportunity__r").optJSONObject("Property__r");
					if (null != propertyJson)
						loan.propertyName = propertyJson.optString("Name");
					loan.propertyId = propertyJson.optString("Id");
					loan.propertyCode = propertyJson.optString("Property_code__c");
					loan.propertyGeo = propertyJson.optString("Property_Geo_Location__c");
					loan.propertyOwner = propertyJson.optString("OwnerId");
					loan.propertyOwnerEmail = propertyJson.optJSONObject("Owner").optString("Email");

					String plotId = propertyJson.optString("Plot_ID__c");
					String projectAddress = propertyJson.optString("Project_Address__c");
					String projectCity = propertyJson.optString("Project_City__c");
					String projectState = propertyJson.optString("Project_State__c");
					String projectPincode = propertyJson.optString("Project_Pin_Code__c");

					StringBuilder propertyAddress = new StringBuilder("");

					if (BasicUtils.isNotNullOrNA(plotId))
						propertyAddress.append(plotId + ",");

					if (BasicUtils.isNotNullOrNA(projectAddress))
						propertyAddress.append(projectAddress + ",");

					if (BasicUtils.isNotNullOrNA(projectCity))
						propertyAddress.append(projectCity + ",");

					if (BasicUtils.isNotNullOrNA(projectState))
						propertyAddress.append(projectState + ",");

					if (BasicUtils.isNotNullOrNA(projectPincode))
						propertyAddress.append(projectPincode);

					loan.propertyAddress = propertyAddress.toString();

				}

				if (!loan.accountNumber.equalsIgnoreCase(Constants.NA)) {
					loan.loanType = current.optString("Sub_Product_Type__c", Constants.NA);
					loan.status = current.optString("loan__Loan_Status__c", Constants.NA);
					if (loan.status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED)) {
						loan.loanType += " (Closed)";
					}
					loans.add(loan);
				}
			}
		}

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			if (condition == LoanListRequestType.PROPERTY_IMAGES)
				loanDetails.put(loan.getPropertInfoJson());
			else
				loanDetails.put(loan.getNameIdJson());
		}

		responseJson.put("loans", loanDetails);

		return responseJson;

	}

	public JSONObject getRecentMonthlyPayments(String crmAccountNumber, String loanAccountNumber, String queryType)
			throws Exception {

		JSONObject responseJson = new JSONObject();

		checkAndAuthenticate();

		String query = "";

		if (queryType.equals(PaymentType.RECENT_MONTHLY_PAYMENT.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Payment_Date__c,loan__Payment_Amt__c"
					+ " from loan__Loan_account_Due_Details__c " + "where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "' " + "and loan__Payment_Satisfied__c=true"
					+ " ORDER BY loan__Payment_Date__c desc LIMIT 50";

		} else if (queryType.equals(PaymentType.MONTHLY_PAYMENTS_DUE.value)
				|| queryType.equals(PaymentType.__MONTHLY_PAYMENTS_DUE.value)) {

			query = "select Name,Sub_Product_Type__c,loan__Loan_Status__c from loan__Loan_Account__c " + "where Name='"
					+ loanAccountNumber + "'";

		} else if (queryType.equals(PaymentType.RECENT_PART_PAYMENTS.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Transaction_Date__c,loan__Transaction_Amount__c"
					+ " from loan__Loan_Payment_Transaction__c" + " where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "'"
					+ " and (Part_Payment_Type__c='part payments' or Part_Payment_Type__c='Subvention')"
					+ " and loan__Cleared__c=true ORDER BY loan__Transaction_Date__c desc LIMIT 50";

		} else if (queryType.equals(PaymentType.AUTO_REPAY.value) || queryType.equals(PaymentType.AUTO_PREPAY.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Transaction_Date__c,loan__Transaction_Amount__c"
					+ " from loan__Loan_Payment_Transaction__c" + " where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "'" + " and loan__Cleared__c=true and Part_Payment_Type__c='Auto Pre Pay'"
					+ " ORDER BY loan__Transaction_Date__c desc LIMIT 50";

		}

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		JSONArray payments = new JSONArray();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);

				if (queryType.equals(PaymentType.MONTHLY_PAYMENTS_DUE.value)
						|| queryType.equals(PaymentType.__MONTHLY_PAYMENTS_DUE.value)) {

					String loanType = current.optString("Sub_Product_Type__c", Constants.NA);
					String loanAccNum = current.optString("Name", Constants.NA);

					if (!loanType.equalsIgnoreCase(Constants.NA)) {

						/*
						 * IF THE LOAN ACCOUNT TYPE IS INSURANCE LOAN, THEN MAKE QUERY ON
						 * loan__Loan_account_Due_Details__c TO GET PAYMENT LIST. ELSE GO TO COLLECTION
						 * TO GET THE PENDING PAYMENT LIST OF HOME OR TOP-UP LOAN ACCOUNT
						 */

						if (BasicUtils.isInsuranceLoan(loanType)) {

							query = "select loan__Loan_Account__r.Name,loan__Due_Date__c,loan__Due_Amt__c "
									+ "from loan__Loan_account_Due_Details__c where loan__Loan_Account__r.CRM_Account_Number__c='"
									+ crmAccountNumber + "'"
									+ " and loan__DD_Primary_Flag__c=true and loan__Payment_Satisfied__c=false"
									+ " ORDER BY loan__Due_Date__c desc";

							paymentJson = getSalesforceData(getModifiedQuery(query));

							if (null == paymentJson)
								return null;

							if (paymentJson.getInt("totalSize") > 0) {
								JSONArray jRecords = paymentJson.getJSONArray("records");
								for (int j = 0; j < jRecords.length(); j++) {

									JSONObject jCurrent = jRecords.getJSONObject(j);

									JSONObject payment = new JSONObject();

									payment.put("loanAccountNumber",
											jCurrent.getJSONObject("loan__Loan_Account__r").getString("Name"));
									payment.put("paymentAmount", jCurrent.getDouble("loan__Due_Amt__c"));
									payment.put("completionDateTime",
											jCurrent.optString("loan__Due_Date__c", Constants.NA));

									payments.put(payment);

								}
							}

						} else {

							ArrayList<CPPaymentInfo> pendingPayments = new ArrayList<>();

							if (BasicUtils.isLoanTopUp(loanType)) {
								pendingPayments = getTopUpLoanCollectionDetails(loanAccNum);
							} else {
								pendingPayments = getHomeLoanCollectionDetails(loanAccNum);
							}

							for (CPPaymentInfo item : pendingPayments) {

								JSONObject payment = new JSONObject();

								payment.put("loanAccountNumber", loanAccNum);

								if (BasicUtils.isInsuranceLoan(loanType))
									payment.put("paymentAmount", item.insuranceLoanAmount);
								else {
									double paymentAmount = 0;
									if (item.excessShortfallAmount < 0) {
										paymentAmount = Math.abs(item.excessShortfallAmount);
									}
									payment.put("paymentAmount", paymentAmount);
								}

								payment.put("completionDateTime", item.dueDate);

								payments.put(payment);

							}

						}

					}

				} else {

					JSONObject payment = new JSONObject();

					if (queryType.equals(PaymentType.RECENT_MONTHLY_PAYMENT.value)) {

						payment.put("loanAccountNumber",
								current.getJSONObject("loan__Loan_Account__r").getString("Name"));
						payment.put("paymentAmount", current.getDouble("loan__Payment_Amt__c"));
						payment.put("completionDateTime", current.optString("loan__Payment_Date__c", Constants.NA));

					} else if (queryType.equals(PaymentType.RECENT_PART_PAYMENTS.value)) {

						payment.put("loanAccountNumber",
								current.getJSONObject("loan__Loan_Account__r").getString("Name"));
						payment.put("paymentAmount", current.getDouble("loan__Transaction_Amount__c"));
						payment.put("completionDateTime", current.optString("loan__Transaction_Date__c", Constants.NA));

					} else if (queryType.equals(PaymentType.AUTO_REPAY.value)
							|| queryType.equals(PaymentType.AUTO_PREPAY.value)) {

						payment.put("loanAccountNumber",
								current.getJSONObject("loan__Loan_Account__r").getString("Name"));
						payment.put("paymentAmount", current.getDouble("loan__Transaction_Amount__c"));
						payment.put("completionDateTime", current.optString("loan__Transaction_Date__c", Constants.NA));

					}

					payments.put(payment);

				}

			}
		}

		responseJson.put("payments", payments);

		return responseJson;
	}

	private ArrayList<CPPaymentInfo> getTopUpLoanCollectionDetails(String lai) throws Exception {

		checkAndAuthenticate();

		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "Top_Up_CL_Contract_No_LAI__c, Debit_Amount_Top_Up__c, Top_Up_Insurance__c,"
				+ "Total_Debit_Amount_Topup__c,Top_Up_Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c WHERE Top_Up_CL_Contract_No_LAI__c = '" + lai
				+ "' and Clearance_Status__c != 'Cleared'"
				+ " and RecordType.DeveloperName = 'X_Sell_Product_Collection_Layout' order by CreatedDate desc"; // LIMIT
																													// 1
																													// LAI-00040541

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		ArrayList<CPPaymentInfo> payments = new ArrayList<CPPaymentInfo>();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				payments.add(getParsedCPPaymnetInfoForTL(current));

			}
		}

		return payments;
	}

	private ArrayList<CPPaymentInfo> getHomeLoanCollectionDetails(String lai) throws Exception {

		checkAndAuthenticate();

		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "CL_Contract_No_LAI__c, Debit_Amount_Housing__c, Debit_Amount_Insurance__c,"
				+ "Total_Debit_Amount__c, Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c WHERE CL_Contract_No_LAI__c = '" + lai + "' and Clearance_Status__c != 'Cleared'"
				+ " and RecordType.DeveloperName = 'Housing_Loan_Collections' order by CreatedDate desc"; // LIMIT 1
																											// LAI-00040541

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		ArrayList<CPPaymentInfo> payments = new ArrayList<CPPaymentInfo>();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				payments.add(getParsedCPPaymnetInfoForHL(current));

			}
		}

		return payments;
	}

//	public LocalResponse createLead(String crmNumber, Referrals lead) throws Exception {
//
//		LocalResponse lResponse = new LocalResponse();
//
//		checkAndAuthenticate();
//
//		String uri = baseUri + "/sobjects/Lead/";
//
//		try {
//
//			salesforce.SalesForceManager v1SfManager = new salesforce.SalesForceManager();
//
//			String accountId = v1SfManager.getAccountIDFromCRM(crmNumber);
//			String currentDateTime = DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd_T_HH_mm_ss_SSSZ,
//					DateTimeZone.IST);
//
//			JSONObject leadObject = new JSONObject();
//
//			leadObject.put("FirstName", lead.firstName);
//			leadObject.put("Middle_Name__c", lead.middleName);
//			leadObject.put("LastName", lead.lastName);
//			leadObject.put("Salutation", lead.salutation);
//			leadObject.put("MobilePhone", lead.mobileNumber);
//
//			if (!lead.emailId.isEmpty() && !lead.emailId.equalsIgnoreCase(Constants.NA))
//				leadObject.put("Email", lead.emailId);
//
//			leadObject.put("Status", lead.status);
//			leadObject.put("LeadSource", lead.sfLeadSource);
//
//			if (lead.address.isValid()) {
//				leadObject.put("City", lead.address.city);
//				leadObject.put("State", lead.address.state);
//				leadObject.put("Street", lead.address.street);
//				leadObject.put("PostalCode", lead.address.postalCode);
//			}
//
//			leadObject.put("Company", lead.getFullName());
//			leadObject.put("Referred_By_Account__c", accountId);
//			leadObject.put("Lead_Date__c", currentDateTime);
//
//			HttpClient httpClient = HttpClientBuilder.create().build();
//
//			HttpPost httpPost = new HttpPost(uri);
//			httpPost.addHeader(oauthHeader);
//			httpPost.addHeader(prettyPrintHeader);
//			StringEntity body = new StringEntity(leadObject.toString(1));
//			body.setContentType("application/json");
//			httpPost.setEntity(body);
//
//			HttpResponse response = httpClient.execute(httpPost);
//
//			int statusCode = response.getStatusLine().getStatusCode();
//			if (statusCode == 201) {
//
//				retryCount = 0;
//				String response_string = EntityUtils.toString(response.getEntity());
//
//				LoggerUtils.log("Lead Create response: " + response_string);
//
//				if (response_string.equalsIgnoreCase("DUPLICATES_DETECTED")) {
//
//					lResponse.isSuccess = false;
//					lResponse.message = response_string;
//
//				} else {
//
//					JSONObject json = new JSONObject(response_string);
//
//					String sfLeadId = json.getString("id");
//					lResponse.isSuccess = true;
//					lResponse.message = sfLeadId;
//				}
//
//			} else if (statusCode == 401 && retryCount < 3) {
//				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
//				baseUri = null;
//				oauthHeader = null;
//				retryCount++;
//				authenticate();
//				return createLead(crmNumber, lead);
//			} else {
//				retryCount = 0;
//				System.out.println("Insertion unsuccessful. Status code returned is " + statusCode + " | error: "
//						+ response.getStatusLine());
//				// System.out.println(getBody(response.getEntity().getContent()));
//				JSONArray respArray = new JSONArray(getBody(response.getEntity().getContent()));
//				JSONObject respObj = (JSONObject) respArray.get(0);
//				String errorCode = respObj.optString("errorCode", Constants.NA);
//				LoggerUtils.log("Lead Create failed response: " + errorCode);
//
//				lResponse.isSuccess = false;
//				lResponse.message = errorCode;
//
//			}
//		} catch (JSONException e) {
//			System.out.println("Issue creating JSON or processing results");
//			e.printStackTrace();
//		} catch (IOException ioe) {
//			ioe.printStackTrace();
//		} catch (NullPointerException npe) {
//			npe.printStackTrace();
//		}
//
//		return lResponse;
//
//	}

	public ArrayList<SFAccountStatement> getAccountStatement(String loanAccountNumber, String startDate, String endDate)
			throws Exception {

		checkAndAuthenticate();

		StringBuilder sb = new StringBuilder();

		sb.append("Select Id, Name, loan__Loan_Account__c, loan__Loan_Account__r.Name, ");
		sb.append("loan__Transaction_Amount__c, loan__Principal__c, loan__Interest__c, ");
		sb.append("loan__Balance__c, loan__Transaction_Date__c, loan__Cleared__c, loan__Payment_Mode__r.Name ");
		sb.append("FROM loan__Loan_Payment_Transaction__c ");
		sb.append("where loan__Loan_Account__r.Name = '" + loanAccountNumber + "' ");
		sb.append("and loan__Cleared__c = true ");
		sb.append("and loan__Rejected__c = false ");
		sb.append("and loan__Reversed__c = false ");
		sb.append("and loan__Transaction_Date__c >= " + startDate + " and loan__Transaction_Date__c <= " + endDate);
		sb.append(" order by loan__Transaction_Date__c desc");

		JSONObject statementJson = getSalesforceData(getModifiedQuery(sb.toString()));

		if (null == statementJson)
			return null;

		ArrayList<SFAccountStatement> aStatments = new ArrayList<>();

		if (statementJson.getInt("totalSize") > 0) {
			JSONArray records = statementJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				if (null != current.optJSONObject("loan__Payment_Mode__r"))
					aStatments.add(new SFAccountStatement(current));
			}
		}

		return aStatments;

	}

	public boolean updateRegisteredInfo(User user) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select AccountId from Contact where CRM_Account_Number__c='" + user.crmAccountNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return false;

		if (requestsJson.getInt("totalSize") > 0) {
			String accountId = requestsJson.getJSONArray("records").getJSONObject(0).optString("AccountId",
					Constants.NA);
			if (!accountId.equals(Constants.NA)) {

				String currentDateTime = DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd_T_HH_mm_ss_SSSZ,
						DateTimeZone.IST);
				JSONObject contactRequestObject = new JSONObject();
				contactRequestObject.put("CP_Registered__c", true);
				contactRequestObject.put("CP_Registration_Date__c", currentDateTime);
				contactRequestObject.put("CP_Last_Login_Date__c", currentDateTime);

				String uri = baseUri + "/sobjects/Account/" + accountId;

				HttpClient httpClient = HttpClientBuilder.create().build();
				HttpPatch httpPatch = new HttpPatch(uri);
				httpPatch.addHeader(oauthHeader);
				httpPatch.addHeader(prettyPrintHeader);
				StringEntity body = new StringEntity(contactRequestObject.toString(1));
				body.setContentType("application/json");
				httpPatch.setEntity(body);

				HttpResponse response = httpClient.execute(httpPatch);

				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 204) {
					retryCount = 0;
					return true;
				} else if (statusCode == 401 && retryCount < 3) {
					System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
					baseUri = null;
					oauthHeader = null;
					retryCount++;
					authenticate();
					return updateRegisteredInfo(user);
				} else {
					retryCount = 0;
					return false;
				}
			}

		}
		return false;

	}

	public boolean updateLoginInfo(User user) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String currentDateTime = DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd_T_HH_mm_ss_SSSZ, DateTimeZone.IST);
		String query = "select AccountId from Contact where CRM_Account_Number__c='" + user.crmAccountNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return false;

		if (requestsJson.getInt("totalSize") > 0) {
			String accountId = requestsJson.getJSONArray("records").getJSONObject(0).optString("AccountId",
					Constants.NA);
			if (!accountId.equals(Constants.NA)) {

//				LoggerUtils.log("CurrentDateTime:" + currentDateTime);
				JSONObject contactRequestObject = new JSONObject();
				contactRequestObject.put("CP_Last_Login_Date__c", currentDateTime);

				if (!user.isRegistrationMarkedOnSF) {

					String regDateTime = DateTimeUtils.getDateForSalesforce(user.registrationDateTime);
//					LoggerUtils.log("=====> registered datetime: " + regDateTime);
					contactRequestObject.put("CP_Registered__c", true);
					contactRequestObject.put("CP_Registration_Date__c", regDateTime);

				}

				String uri = baseUri + "/sobjects/Account/" + accountId;

				HttpClient httpClient = HttpClientBuilder.create().build();
				HttpPatch httpPatch = new HttpPatch(uri);
				httpPatch.addHeader(oauthHeader);
				httpPatch.addHeader(prettyPrintHeader);
				StringEntity body = new StringEntity(contactRequestObject.toString(1));
				body.setContentType("application/json");
				httpPatch.setEntity(body);

				HttpResponse response = httpClient.execute(httpPatch);

				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 204) {
					retryCount = 0;
					LoggerUtils.log("updateLoginInfo - Login data Successfully updated on SF");
					return true;
				} else if (statusCode == 401 && retryCount < 3) {
					LoggerUtils.log("updateLoginInfo - failed to update Login data on SF");
					System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
					baseUri = null;
					oauthHeader = null;
					retryCount++;
					authenticate();
					return updateLoginInfo(user);
				} else {
					retryCount = 0;
					return false;
				}
			}

		}
		return false;

	}

	
	public ArrayList<CLContract> getAllUserCrmAcc(String lai) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String crmQuery = "select Name , CRM_Account_Number__c,loan__Principal_Remaining__c , loan__Loan_Product_Name__c"
				+ " from loan__Loan_Account__c where Name in (" + lai + ")";

		LoggerUtils.log("crmQuery: " + crmQuery);
		JSONObject crmJson = getSalesforceData(getModifiedQuery(crmQuery));

		// LoggerUtils.log("crmQuery: " + crmJson);
//		if (crmJson.getInt("totalSize") > 0) {
//			JSONArray records = crmJson.getJSONArray("records");
//			return records.getJSONObject(0).optString("CRM_Account_Number__c");
//		}
//		
		ArrayList<CLContract> resp = new ArrayList<CLContract>();

		if (crmJson.getInt("totalSize") > 0) {
			JSONArray records = crmJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				resp.add(new CLContract(current));
				// resp.put(current.optString("Name"),
				// current.optString("CRM_Account_Number__c"));
				// resp.add(getParsedCPPaymnetInfoForHL(current));

			}
		}

		return resp;

	}

}
