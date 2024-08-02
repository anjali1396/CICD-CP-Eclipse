package salesforce;

import java.io.BufferedReader;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;
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

import models.Branch;
import models.Loan;
import models.User;
import models.payment.Payment;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
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

	public void authenticate() throws Exception {

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

	public User getSalesforceUser(String loanAccountNumber) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Primary_Contact__r.Name, CRM_Account_No__c,"
				+ " Primary_Contact__r.MobilePhone, Primary_Contact__r.Email,"
				+ " Primary_Contact__r.ID_Proof_1_PAN_Card__c, Primary_Contact__r.ID_Proof_2_Aadhar_Card__c,"
				+ " Primary_Contact__r.Aadhar_Card__c" + " from Opportunity where CL_Contract_No_LAI__c = '"
				+ loanAccountNumber + "'"
				+ " and StageName = '8 - System Approved (Gradatim)'";
			

		query = query.replace(" ", "+");

		JSONObject json = getSalesforceData(query);

		if (null != json) {

			if (json.getInt("totalSize") > 0) {

				JSONObject userContact = json.getJSONArray("records").getJSONObject(0)
						.getJSONObject("Primary_Contact__r");

				User user = new User();
				user.emailId = userContact.optString("Email", Constants.NA);
				user.name = userContact.getString("Name");
				user.panNumber = userContact.optString("ID_Proof_1_PAN_Card__c", Constants.NA);

				String aadharNumber = userContact.optString("Aadhar_Card__c", Constants.NA);

				if (!BasicUtils.isNotNullOrNA(aadharNumber)) {
					aadharNumber = userContact.optString("ID_Proof_2_Aadhar_Card__c", Constants.NA);
				}

				user.aadharNumber = aadharNumber;
				user.mobileNumber = userContact.getString("MobilePhone");

				user.crmAccountNumber = json.getJSONArray("records").getJSONObject(0).getString("CRM_Account_No__c");

				return user;

			} else {
				return null;
			}

		} else {
			return null;
		}

	}

	@Deprecated
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

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getSummaryJson());
		}

		responseJson.put("loans", loanDetails);

		return responseJson;

	}

	@Deprecated
	public JSONObject getLoanAccountList(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Name,Sub_Product_Type__c from loan__Loan_Account__c where CRM_Account_Number__c='"
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
					loans.add(loan);
				}
			}
		}

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getNameIdJson());
		}

		responseJson.put("loans", loanDetails);

		return responseJson;

	}

	/**
	 * @deprecated use the v2/managers/SalesForceManager.getUserDashboard()
	 */
	@Deprecated
	public JSONObject getUserDashboard(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Name,Sub_Product_Type__c,loan__Principal_Remaining__c,"
				+ "loan__ACH_Debit_Amount__c,loan__Loan_Status__c from loan__Loan_Account__c where CRM_Account_Number__c='"
				+ crmAccountNumber + "'";

		JSONObject loanJson = getSalesforceData(getModifiedQuery(query));

		query = "select CL_Contract_No_LAI__c,CaseNumber,Case_Reason_sub__c,Status,"
				+ "Resolution_Comments_new__c from Case where Account.CRM_ACCOUNT_NO__c='" + crmAccountNumber + "'"; // and
																														// CreatedDate
																														// "+
																														// URLEncoder.encode(">",
																														// "UTF-8")
																														// +"
																														// date
																														// ORDER
																														// BY
																														// createddate
																														// DESC
																														// limit
																														// 3";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == loanJson || null == requestsJson)
			return null;

		JSONArray accounts = new JSONArray();

		if (loanJson.getInt("totalSize") > 0) {
			JSONArray records = loanJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);

				// a single loan account detail object
				JSONObject account = new JSONObject();

				// get on loan at a time and add it to the account object
				Loan loan = new Loan();
				loan.accountNumber = current.optString("Name", Constants.NA);

				if (!loan.accountNumber.equals(Constants.NA)) {

					loan.loanType = current.optString("Sub_Product_Type__c", Constants.NA);
					loan.principalAmount = current.optDouble("loan__Principal_Remaining__c", 0.0);
					loan.debitAmount = current.optDouble("loan__ACH_Debit_Amount__c", 0.0);
					loan.status = current.optString("loan__Loan_Status__c", Constants.NA);
					if (loan.status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED)) {
						loan.loanType += " (Closed)";
						loan.principalAmount = 0.0;
						loan.debitAmount = 0.0;
					}
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

			}
		}

		responseJson.put("accounts", accounts);

		return responseJson;
	}

	public JSONObject getUBranch() throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "SELECT Branch_Address_line_1__c,Branch_Address_line_2__c,"
				+ "Branch_City__c,Branch_Geo_Location__Latitude__s,Branch_Geo_Location__Longitude__s,"
				+ "Branch_Pincode__c,Branch_Primary_Landline__c,Branch_Secondary_Landline__c,Branch_State__c,"
				+ "Branch_Status__c,HFFC_Physical_Branch__c,Id,Name FROM Branch__c "
				+ " where HFFC_Physical_Branch__c in ('Planned', 'WIP', 'Yes', 'Virtual') order by Name";

		JSONObject loanJson = getSalesforceData(getModifiedQuery(query));

		if (null == loanJson)
			return null;

		JSONArray branches = new JSONArray();

		if (loanJson.getInt("totalSize") > 0) {

			JSONArray records = loanJson.getJSONArray("records");

			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);

				Branch branch = new Branch();

				branch.addressLine1 = current.optString("Branch_Address_line_1__c", Constants.NA);
				branch.addressLine2 = current.optString("Branch_Address_line_2__c", Constants.NA);
				branch.city = current.optString("Branch_City__c", Constants.NA);
				branch.lat = current.optString("Branch_Geo_Location__Latitude__s", Constants.NA);
				branch.longi = current.optString("Branch_Geo_Location__Longitude__s", Constants.NA);
				branch.pincode = current.optString("Branch_Pincode__c", Constants.NA);
				branch.primarynumber = current.optString("Branch_Primary_Landline__c", Constants.NA);
				branch.secondarynumber = current.optString("Branch_Secondary_Landline__c", Constants.NA);
				branch.state = current.optString("Branch_State__c", Constants.NA);
				branch.status = current.optString("Branch_Status__c", Constants.NA);
				branch.name = current.optString("Name", Constants.NA);

				branches.put(branch.getSummaryJson());

			}
		}

		responseJson.put("branches", branches);

		return responseJson;
	}

	@Deprecated
	public JSONObject getLoanDetail(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Name,Sub_Product_Type__c,loan__Loan_Amount__c,loan__Principal_Remaining__c,"
				+ "loan__Interest_Rate__c,loan__ACH_Debit_Amount__c,"
				+ "loan__Disbursed_Amount__c,loan__Number_of_Installments__c,Loan_Type_Indicator1__c,loan__Loan_Status__c"
				+ " from loan__Loan_Account__c where CRM_Account_Number__c='" + crmAccountNumber + "'";

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

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getCompleteJson());
		}

		responseJson.put("loans", loanDetails);

		return responseJson;
	}

	public JSONObject getServiceRequests(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Id,CL_Contract_No_LAI__c,Reference_LAI__c,CaseNumber,Case_Reason_sub__c,Status,Master_Case_Reason__c,"
				+ " Resolution_Comments_new__c , CreatedDate from Case" + " where Account.CRM_ACCOUNT_NO__c='" + crmAccountNumber
				+ "' ORDER BY createddate DESC";

		// and CreatedDate%3E date

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));
		JSONObject loanJson = getLoanAccountList(crmAccountNumber);

		if (null == requestsJson && null != loanJson)
			return null;

		JSONArray pending = new JSONArray();
		JSONArray closed = new JSONArray();

		if (requestsJson.getInt("totalSize") > 0) {
			JSONArray records = requestsJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);

				JSONObject request = new JSONObject();
				String parentId = current.optString("Id", Constants.NA);
				if (!parentId.equalsIgnoreCase(Constants.NA)) {

					request.put("id", parentId);
					request.put("caseNumber", current.getString("CaseNumber"));
					request.put("caseReason", current.optString("Case_Reason_sub__c", Constants.NA));
					request.put("resolutionComment", current.optString("Resolution_Comments_new__c", Constants.NA));
					request.put("masterCaseReason", current.optString("Master_Case_Reason__c", Constants.NA));
					request.put("createdDate", current.optString("CreatedDate", Constants.NA));

					// request.put("loanAccountNumber", current.optString("CL_Contract_No_LAI__c",
					// Constants.NA));

					String referenceLai = current.optString("Reference_LAI__c", Constants.NA);

					if (BasicUtils.isNotNullOrNA(referenceLai))
						request.put("loanAccountNumber", referenceLai);
					else
						request.put("loanAccountNumber", current.optString("CL_Contract_No_LAI__c", Constants.NA));

					if (current.getString("Status").equals("Closed")) {
						closed.put(request);
					} else {
						pending.put(request);
					}

				}

			}
		}

		JSONObject requestObject = new JSONObject();
		requestObject.put("pending", pending);
		requestObject.put("closed", closed);
		responseJson.put("serviceRequests", requestObject);
		responseJson.put("loans", loanJson.getJSONArray("loans"));

		return responseJson;

	}

	@Deprecated
	public JSONObject getRecentMonthlyPayments(String crmAccountNumber, String queryType) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "";

		if (queryType.equals(PaymentType.RECENT_MONTHLY_PAYMENT.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Payment_Date__c,loan__Payment_Amt__c"
					+ " from loan__Loan_account_Due_Details__c " + "where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "' " + "and loan__Payment_Satisfied__c=true"
					+ " ORDER BY loan__Payment_Date__c desc LIMIT 12";

		} else if (queryType.equals(PaymentType.MONTHLY_PAYMENTS_DUE.value)
				|| queryType.equals(PaymentType.__MONTHLY_PAYMENTS_DUE.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Due_Date__c,loan__Due_Amt__c "
					+ "from loan__Loan_account_Due_Details__c where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "'" + " and loan__DD_Primary_Flag__c=true and loan__Payment_Satisfied__c=false"
					+ " ORDER BY loan__Due_Date__c desc";

		} else if (queryType.equals(PaymentType.RECENT_PART_PAYMENTS.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Transaction_Date__c,loan__Transaction_Amount__c"
					+ " from loan__Loan_Payment_Transaction__c" + " where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "'"
					+ " and (Part_Payment_Type__c='part payments' or Part_Payment_Type__c='Subvention')"
					+ " and loan__Cleared__c=true ORDER BY loan__Transaction_Date__c desc";

		} else if (queryType.equals(PaymentType.AUTO_REPAY.value) || queryType.equals(PaymentType.AUTO_PREPAY.value)) {

			query = "select loan__Loan_Account__r.Name,loan__Transaction_Date__c,loan__Transaction_Amount__c"
					+ " from loan__Loan_Payment_Transaction__c" + " where loan__Loan_Account__r.CRM_Account_Number__c='"
					+ crmAccountNumber + "'" + " and loan__Cleared__c=true and Part_Payment_Type__c='Auto Pre Pay'"
					+ " ORDER BY loan__Transaction_Date__c desc";

		}

		JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));

		if (null == paymentJson)
			return null;

		JSONArray payments = new JSONArray();

		if (paymentJson.getInt("totalSize") > 0) {
			JSONArray records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);

				JSONObject payment = new JSONObject();

				payment.put("loanAccountNumber", current.getJSONObject("loan__Loan_Account__r").getString("Name"));

				if (queryType.equals(PaymentType.RECENT_MONTHLY_PAYMENT.value)) {

					payment.put("paymentAmount", current.getDouble("loan__Payment_Amt__c"));
					payment.put("completionDateTime", current.optString("loan__Payment_Date__c", Constants.NA));

				} else if (queryType.equals(PaymentType.MONTHLY_PAYMENTS_DUE.value)
						|| queryType.equals(PaymentType.__MONTHLY_PAYMENTS_DUE.value)) {

					payment.put("paymentAmount", current.getDouble("loan__Due_Amt__c"));
					payment.put("completionDateTime", current.optString("loan__Due_Date__c", Constants.NA));

				} else if (queryType.equals(PaymentType.RECENT_PART_PAYMENTS.value)) {

					payment.put("paymentAmount", current.getDouble("loan__Transaction_Amount__c"));
					payment.put("completionDateTime", current.optString("loan__Transaction_Date__c", Constants.NA));

				} else if (queryType.equals(PaymentType.AUTO_REPAY.value)
						|| queryType.equals(PaymentType.AUTO_PREPAY.value)) {

					payment.put("paymentAmount", current.getDouble("loan__Transaction_Amount__c"));
					payment.put("completionDateTime", current.optString("loan__Transaction_Date__c", Constants.NA));

				}

				payments.put(payment);

			}
		}

		responseJson.put("payments", payments);

		return responseJson;
	}

	public JSONObject getDisbursementDetail(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Name,Sub_Product_Type__c,loan__Loan_Status__c,loan__Disbursed_Amount__c,"
				+ "loan__Remaining_Loan_Amount__c from loan__Loan_Account__c " + "where CRM_Account_Number__c='"
				+ crmAccountNumber + "'";

		JSONObject disbursementJson = getSalesforceData(getModifiedQuery(query));

		query = "select loan__Loan_Account__r.Name,loan__Disbursal_Date__c,loan__Reference__c,"
				+ "loan__Disbursed_Amt__c from loan__Loan_Disbursal_Transaction__c "
				+ "where loan__Loan_Account__r.CRM_Account_Number__c='" + crmAccountNumber
				+ "' and loan__Cleared__c=true";

		JSONObject recentDisbursementJson = getSalesforceData(getModifiedQuery(query));

		if (null == disbursementJson || null == recentDisbursementJson)
			return null;

		JSONArray mainArray = new JSONArray();

		if (disbursementJson.getInt("totalSize") > 0) {
			JSONArray dRecords = disbursementJson.getJSONArray("records");
			for (int i = 0; i < dRecords.length(); i++) {
				JSONObject dCurrent = dRecords.getJSONObject(i);

				JSONObject disbursement = new JSONObject();
				String lAccountNumber = dCurrent.optString("Name", Constants.NA);
				if (!lAccountNumber.equalsIgnoreCase(Constants.NA)) {

					String loanType = dCurrent.optString("Sub_Product_Type__c", Constants.NA);
					String status = dCurrent.optString("loan__Loan_Status__c", Constants.NA);
					if (status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED))
						loanType += " (Closed)";

					disbursement.put("loanAccountNumber", lAccountNumber);
					disbursement.put("loanType", loanType);
					disbursement.put("disbursedAmount", dCurrent.optDouble("loan__Disbursed_Amount__c", 0.0));
					disbursement.put("remainingAmount", dCurrent.optDouble("loan__Remaining_Loan_Amount__c", 0.0));

					JSONArray recentDisbursements = new JSONArray();

					if (recentDisbursementJson.getInt("totalSize") > 0) {
						JSONArray iRecords = recentDisbursementJson.getJSONArray("records");
						for (int j = 0; j < iRecords.length(); j++) {
							JSONObject iCurrent = iRecords.getJSONObject(j);

							if (iCurrent.getJSONObject("loan__Loan_Account__r").getString("Name")
									.equalsIgnoreCase(dCurrent.getString("Name"))) {

								JSONObject disbursementItem = new JSONObject();
								disbursementItem.put("date", iCurrent.getString("loan__Disbursal_Date__c"));
								disbursementItem.put("amount", iCurrent.getDouble("loan__Disbursed_Amt__c"));
								disbursementItem.put("referenceNumber",
										iCurrent.optString("loan__Reference__c", Constants.NA));

								recentDisbursements.put(disbursementItem);

							}

						}
					}

					disbursement.put("recentDisbursements", recentDisbursements);
					mainArray.put(disbursement);

				}

			}
		}

		responseJson.put("disburesements", mainArray);

		return responseJson;
	}

	public JSONObject getUserProfile(User user) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Primary_Contact__r.Name,Primary_Contact__r.MobilePhone,Primary_Contact__r.Phone,"
				+ "Primary_Contact__r.MailingStreet,Primary_Contact__r.MailingPostalCode,Primary_Contact__r.MailingCity,"
				+ "Primary_Contact__r.MailingState,Primary_Contact__r.ID_Proof_2_Aadhar_Card__c,Primary_Contact__r.Aadhar_Card__c,"
				+ "Primary_Contact__r.ID_Proof_4_Driving_License__c,Primary_Contact__r.ID_Proof_1_PAN_Card__c,"
				+ "Primary_Contact__r.Email,CIBIL_Score__c,Co_Applicant_Name1__r.Name,"
				+ "Co_Applicant_Name1__r.MobilePhone,Opportunity_Branch_New__r.Name"
				+ " from Opportunity where CL_Contract_No_LAI__c = '" + user.loanAccountNumber + "'";

		JSONObject profileJson = getSalesforceData(getModifiedQuery(query));

		if (null == profileJson)
			return null;

		JSONObject profileData = new JSONObject();

		if (profileJson.getInt("totalSize") > 0) {
			JSONArray records = profileJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);

				JSONObject primaryDetail = new JSONObject();
				primaryDetail.put("name", current.getJSONObject("Primary_Contact__r").optString("Name", Constants.NA));
				primaryDetail.put("mobileNumber",
						current.getJSONObject("Primary_Contact__r").optString("MobilePhone", Constants.NA));
				primaryDetail.put("emailId",
						current.getJSONObject("Primary_Contact__r").optString("Email", Constants.NA));
				primaryDetail.put("imageUrl", user.imageUrl);

				JSONObject documents = new JSONObject();
				documents.put("panNumber",
						current.getJSONObject("Primary_Contact__r").optString("ID_Proof_1_PAN_Card__c", Constants.NA));

				String aadharNumber = current.getJSONObject("Primary_Contact__r").optString("Aadhar_Card__c",
						Constants.NA);

				if (!BasicUtils.isNotNullOrNA(aadharNumber)) {
					aadharNumber = current.getJSONObject("Primary_Contact__r").optString("ID_Proof_2_Aadhar_Card__c",
							Constants.NA);
				}

				if (!aadharNumber.equalsIgnoreCase(Constants.NA) && aadharNumber.length() > 8) {
					aadharNumber = aadharNumber.replace(aadharNumber.subSequence(0, 8), "********");
				}

				documents.put("aadharNumber", aadharNumber);

				documents.put("drivingLicenseNumber", current.getJSONObject("Primary_Contact__r")
						.optString("ID_Proof_4_Driving_License__c", Constants.NA));

				primaryDetail.put("documents", documents);

				JSONObject address = new JSONObject();
				address.put("street", current.getJSONObject("Primary_Contact__r").optString("MailingStreet", ""));
				address.put("city", current.getJSONObject("Primary_Contact__r").optString("MailingCity", ""));
				address.put("state", current.getJSONObject("Primary_Contact__r").optString("MailingState", ""));
				address.put("postalCode",
						current.getJSONObject("Primary_Contact__r").optString("MailingPostalCode", ""));
				address.put("mobileNumber", current.getJSONObject("Primary_Contact__r").optString("MobilePhone", ""));
				primaryDetail.put("address", address);

				profileData.put("primaryDetail", primaryDetail);

				profileData.put("cibilScore", current.optInt("CIBIL_Score__c", -1));
				profileData.put("branchName",
						current.getJSONObject("Opportunity_Branch_New__r").optString("Name", Constants.NA));

				JSONObject coApplicantJson = current.optJSONObject("Co_Applicant_Name1__r");
				JSONObject coApplicant = new JSONObject();

				if (null != coApplicantJson) {

					coApplicant.put("name", coApplicantJson.optString("Name", Constants.NA));
					coApplicant.put("mobileNumber", coApplicantJson.optString("MobilePhone", Constants.NA));

				}

				profileData.put("coApplicantDetail", coApplicant);

			}
		}

		responseJson.put("profile", profileData);

		return responseJson;

	}

	public boolean updateContactInformation(User user, String newMobileNumber) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Id from Contact where CRM_Account_Number__c='" + user.crmAccountNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return false;

		if (requestsJson.getInt("totalSize") > 0) {
			String contactId = requestsJson.getJSONArray("records").getJSONObject(0).optString("Id", Constants.NA);
			if (!contactId.equals(Constants.NA)) {

				JSONObject contactRequestObject = new JSONObject();
				contactRequestObject.put("MobilePhone", newMobileNumber);

				String uri = baseUri + "/sobjects/Contact/" + contactId;

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
					return updateContactInformation(user, newMobileNumber);
				} else {
					retryCount = 0;
				}

			}
		}

		return false;

	}

	public boolean updateContactMobile(User user, String newMobileNumber) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select Id from Contact where MobilePhone='" + user.mobileNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return false;

		if (requestsJson.getInt("totalSize") > 0) {
			String contactId = requestsJson.getJSONArray("records").getJSONObject(0).optString("Id", Constants.NA);
			if (!contactId.equals(Constants.NA)) {

				JSONObject contactRequestObject = new JSONObject();
				contactRequestObject.put("MobilePhone", newMobileNumber);

				String uri = baseUri + "/sobjects/Contact/" + contactId;

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
					return updateContactInformation(user, newMobileNumber);
				} else {
					retryCount = 0;
				}

			}
		}

		return false;

	}

	@Deprecated
	public Payment addPaymentReceipt(User user, Payment payment) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

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

			String completetionDateTime = DateTimeUtils.getDateForSalesforce(DateTimeUtils.getCurrentDateTimeInIST()); // payment.completionDateTime
			receiptObject.put("Receipt_Date__c", completetionDateTime);

			receiptObject.put("Receipt_Type_Nature_of_Payme__c", payment.paymentNature);

			if (payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.PART_PAYMENT.value))
				receiptObject.put("Prepayment_Type__c", payment.prePaymentType);
			else if (payment.paymentNature.equalsIgnoreCase(PaymentUtils.PaymentType.SERVICE_REQUEST.value))
				receiptObject.put("Nature_of_Payment_Payment_Sub_type_1__c", payment.paymentSubType);

			receiptObject.put("Insurance_Refund__c", "No");
			receiptObject.put("Paynimo_Response__c", payment.receiptData);
			receiptObject.put("Receipt_Amount__c", payment.paymentAmount);
			receiptObject.put("Cheque_No_Transaction_No__c", payment.paymentId);

			if (payment.loanType.equals("Housing Loan Receipt")) {
				receiptObject.put("RecordTypeId", "01290000000TAL9");
			} else {
				receiptObject.put("RecordTypeId", "01290000001Akms");
			}

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
				System.out.println("New receipt created with id: " + receiptId);
				payment.receiptNumber = getReceiptNumber(receiptId);

			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return addPaymentReceipt(user, payment);
			} else {
				retryCount = 0;
				System.out.println("Insertion unsuccessful. Status code returned is " + statusCode + " | error: "
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

	public String getReceiptNumber(String receiptId) throws Exception {

		String receiptNumber = Constants.NA;

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "Select Name from Cash_Receipt__c where id='" + receiptId + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return receiptNumber;

		if (requestsJson.getInt("totalSize") > 0)
			receiptNumber = requestsJson.getJSONArray("records").getJSONObject(0).getString("Name");

		return receiptNumber;

	}

	public JSONObject addServiceRequest(User user, JSONObject serviceRequestObject) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String uri = baseUri + "/sobjects/Case/";

		JSONObject responseObject = null;

		try {

			String attachment = serviceRequestObject.getString("attachment");
			serviceRequestObject.remove("attachment");
			String attachmentName = serviceRequestObject.getString("attachmentName");
			serviceRequestObject.remove("attachmentName");

			String lai = serviceRequestObject.optString(Constants.LOAN_ACCOUNT_NUMBER, Constants.NA);
			serviceRequestObject.remove(Constants.LOAN_ACCOUNT_NUMBER);

			if (lai.equalsIgnoreCase(Constants.NA))
				lai = user.loanAccountNumber;

			JSONObject metaData = getAccountNameAndOpportunity(lai);

			if (null == metaData || metaData.isEmpty()) {
				metaData = getAccountNameAndOpportunity(user.loanAccountNumber);
			}

			serviceRequestObject.put("Opportunity__c", metaData.getString("opportunityId"));
			serviceRequestObject.put("AccountId", metaData.getString("accountId"));
			serviceRequestObject.put("Reference_LAI__c", lai);
			String contactId = metaData.getString("contactId");
			
			if (BasicUtils.isNotNullOrNA(contactId))
				serviceRequestObject.put("ContactId", contactId);

			HttpClient httpClient = HttpClientBuilder.create().build();

			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);
			StringEntity body = new StringEntity(serviceRequestObject.toString(1));
			body.setContentType("application/json");
			httpPost.setEntity(body);

			HttpResponse response = httpClient.execute(httpPost);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 201) {
				retryCount = 0;
				String response_string = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(response_string);
				LoggerUtils.log("Case response: " + json);
				String caseId = json.getString("id");

				if (!attachment.equals(Constants.NA)) {

					JSONObject data = new JSONObject();
					data.put("Name", attachmentName);
					data.put("Body", attachment);
					data.put("parentId", caseId);

					JSONObject uploadResponse = uploadAttachment(data);
					if (null != uploadResponse) {
						responseObject = new JSONObject();
						responseObject.put("caseId", caseId);
					}

				} else {
					responseObject = new JSONObject();
					responseObject.put("caseId", caseId);
				}

				System.out.println("New case id from response: " + caseId);
			} else if (statusCode == 401 && retryCount < 3) {
				System.out.println("Query was unsuccessful. Access token was expired: " + statusCode);
				baseUri = null;
				oauthHeader = null;
				retryCount++;
				authenticate();
				return addServiceRequest(user, serviceRequestObject);
			} else {
				retryCount = 0;
				System.out.println("Response Object for addService: " + EntityUtils.toString(response.getEntity()));
				System.out.println("Insertion unsuccessful. Status code returned is " + statusCode);
			}
		} catch (JSONException e) {
			System.out.println("Issue creating JSON or processing results");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		return responseObject;
	}

	public JSONObject getAccountNameAndOpportunity(String loanAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "select loan__Contact__c , Opportunity__c,loan__Account__c , X_Sell_Products__c, loan__Loan_Product_Name__c from loan__Loan_Account__c where Name='"
				+ loanAccountNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return null;

		if (requestsJson.getInt("totalSize") > 0) {
			JSONArray records = requestsJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);

				String accountId = current.optString("loan__Account__c", Constants.NA);
				String opportunityId = current.optString("Opportunity__c", Constants.NA);
				String contactId = current.optString("loan__Contact__c", Constants.NA);
				String xSellProductId = current.optString("X_Sell_Products__c", Constants.NA);
				String loanProductType = current.optString("loan__Loan_Product_Name__c", Constants.NA);

				if (!accountId.equals(Constants.NA) && !opportunityId.equals(Constants.NA)) {

					responseJson.put("accountId", accountId);
					responseJson.put("opportunityId", opportunityId);
					responseJson.put("contactId", contactId);
					responseJson.put("xSellProductId", xSellProductId);
					responseJson.put("loanProductType", loanProductType);

					break;

				}

			}
		}

		return responseJson;

	}

	private JSONObject uploadAttachment(JSONObject jsonBody) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String uri = baseUri + "/sobjects/Attachment/";

		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri);
		httpPost.addHeader(oauthHeader);
		httpPost.addHeader(prettyPrintHeader);
		StringEntity body = new StringEntity(jsonBody.toString());
		body.setContentType("application/json");
		httpPost.setEntity(body);

		HttpResponse response = httpClient.execute(httpPost);
		String response_string = EntityUtils.toString(response.getEntity());
		JSONObject json = new JSONObject(response_string);

		int statusCode = response.getStatusLine().getStatusCode();
		LoggerUtils.log("Upload attachment status code: " + statusCode);

		return json;

	}

	public JSONObject getCLAttachmentList(String crmAccountNumber) throws Exception {

		JSONObject responseObject = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "SELECT Name,Customer_Name__c,Id,loan__Loan_Status__c FROM loan__Loan_Account__c"
				+ " where CRM_Account_Number__c = '" + crmAccountNumber + "'";

		JSONObject loanAccJson = getSalesforceData(getModifiedQuery(query));

		if (null == loanAccJson)
			return null;

		JSONArray documentArray = new JSONArray();

		if (loanAccJson.getInt("totalSize") > 0) {

			StringBuilder sb = new StringBuilder();

			JSONArray laRecords = loanAccJson.getJSONArray("records");
			for (int i = 0; i < laRecords.length(); i++) {
				JSONObject cRecord = laRecords.getJSONObject(i);
				String lai = cRecord.optString("Name", Constants.NA);
				if (!lai.equalsIgnoreCase(Constants.NA) && !lai.equalsIgnoreCase("null")) {
					if (!sb.toString().isEmpty())
						sb.append(",");
					sb.append("'" + cRecord.optString("Id", Constants.NA) + "'");
				}
			}

			if (!sb.toString().isEmpty()) {
				query = "SELECT Id,Name,ParentId,ContentType,CreatedDate FROM Attachment" + " where ParentId in ("
						+ sb.toString() + ") order by CreatedDate desc";

				JSONObject docJson = getSalesforceData(getModifiedQuery(query));

				if (docJson.getInt("totalSize") > 0) {
					JSONArray dRecords = docJson.getJSONArray("records");
					for (int i = 0; i < dRecords.length(); i++) {

						JSONObject current = dRecords.getJSONObject(i);

						JSONObject documentData = new JSONObject();

						documentData.put("parentId", current.getString("ParentId"));
						documentData.put("contentType", current.optString("ContentType", Constants.NA));
						documentData.put("documentId", current.getString("Id"));
						documentData.put("name", current.getString("Name"));
						documentData.put("datetime", current.getString("CreatedDate"));

						for (int j = 0; j < laRecords.length(); j++) {
							JSONObject cRecord = laRecords.getJSONObject(j);
							String cParentId = cRecord.optString("Id", Constants.NA);

							if (cParentId.equalsIgnoreCase(current.getString("ParentId"))) {
								documentData.put("lai", cRecord.optString("Name", Constants.NA));
								break;
							}

						}

						documentArray.put(documentData);
					}
				}

			}

		}

		responseObject.put("documents", documentArray);

		return responseObject;

	}

	public JSONObject getServiceRequestAttachment(String parentId) throws Exception {

		if (null == baseUri || null == oauthHeader)
			authenticate();

		JSONObject responseObject = new JSONObject();

		String query = "SELECT Id,Name,ParentId,ContentType FROM Attachment where ParentId='" + parentId + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return null;

		JSONArray documentArray = new JSONArray();

		if (requestsJson.getInt("totalSize") > 0) {
			JSONArray records = requestsJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {

				JSONObject current = records.getJSONObject(i);
				JSONObject documentData = new JSONObject();
				documentData.put("parentId", current.getString("ParentId"));
				documentData.put("contentType", current.optString("ContentType", Constants.NA));
				documentData.put("documentId", current.getString("Id"));
				documentData.put("body", Constants.NA); // current.getString("Body")
				documentData.put("name", current.getString("Name"));

				documentArray.put(documentData);
			}
		}

		responseObject.put("documents", documentArray);
		return responseObject;

	}

	public JSONObject getAttachmentData(String documentId) throws ClientProtocolException, Exception {

		JSONObject responseObject = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = baseUri + "/sobjects/Attachment/" + documentId + "/Body";
		System.out.println("Query URL: " + uri);
		HttpGet httpGet = new HttpGet(uri);
		System.out.println("oauthHeader2: " + oauthHeader);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);

		HttpResponse response = httpClient.execute(httpGet);

		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			retryCount = 0;
			try {

				byte[] encoded = Base64.encodeBase64(IOUtils.toByteArray(response.getEntity().getContent()));
				String encodedString = new String(encoded);
				responseObject.put("base64String", encodedString);

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
			return getAttachmentData(documentId);
		} else {
			retryCount = 0;
			System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
			System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
			System.out.println(getBody(response.getEntity().getContent()));
			// System.exit(-1);
			return null;
		}

		return responseObject;

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

	private String getModifiedQuery(String query) {
		return query.replace(" ", "+");
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

	public JSONObject getAccountIDFromCRM(String crmAccountNumber) throws Exception {

		String accountId = Constants.NA;
		String accountName = Constants.NA;
		var accountJson = new JSONObject();

		if (null == baseUri || null == oauthHeader)
			authenticate();

		String query = "Select Id, Name from Account where CRM_ACCOUNT_NO__c='" + crmAccountNumber + "'";

		JSONObject requestsJson = getSalesforceData(getModifiedQuery(query));

		if (null == requestsJson)
			return null;

		if (requestsJson.getInt("totalSize") > 0) {
			accountId = requestsJson.getJSONArray("records").getJSONObject(0).getString("Id");
		 	accountName = requestsJson.getJSONArray("records").getJSONObject(0).getString("Name");
		}

		
		accountJson.put("accountId", accountId);
		accountJson.put("accountName", accountName);
		
		
		return accountJson;

	}

}
