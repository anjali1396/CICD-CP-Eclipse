package v3.managers;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import models.Loan;
import models.User;
import models.payment.CPPaymentInfo;
import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;
import utils.DateTimeUtils.DateTimeFormat;
import utils.DateTimeUtils.DateTimeZone;
import utils.LoggerUtils;
import networking.SFConnection;

public class SalesForceManager {


	enum PaymentType {
		RECENT_MONTHLY_PAYMENT("Recent monthly payments"), MONTHLY_PAYMENTS_DUE("Monthly payments due"),
		__MONTHLY_PAYMENTS_DUE("Montly payments due"), RECENT_PART_PAYMENTS("Recent part payments"),
		AUTO_PREPAY("Auto Prepay"), AUTO_REPAY("Auto repay");

		public final String value;

		PaymentType(String value) {
			this.value = value;
		}
	}

	private final SFConnection sfConnection;

	public SalesForceManager() throws Exception {
		sfConnection = new SFConnection();

	}

	// ================= START OF ALL HOME IMPLEMENTATIONS ================== //
	// ======================================================================= //

	public JSONObject getUserDashboard(String crmAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();

		String query = "select Id,Name, Sub_Product_Type__c, loan__Principal_Remaining__c,"
				+ " X_Sell_Products__r.Insurance_Sum_Assured__c, Opportunity__c, Opportunity_Number__c,"
				+ " loan__ACH_Debit_Amount__c, loan__Loan_Status__c, loan__Loan_Amount__c from loan__Loan_Account__c"
				+ " where CRM_Account_Number__c='" + crmAccountNumber + "'"
				+ " and loan__Loan_Status__c in ('Active - Good Standing', 'Active - Bad Standing', 'Approved', "
				+ "'Closed - Obligations met', 'Closed- Written Off')";
		
		JSONObject loanJson = sfConnection.get(query);

		if (null == loanJson)
			return null;

		ArrayList<Loan> loans = new ArrayList<>();

		if (loanJson.getInt("totalSize") > 0) {

			final var records = loanJson.getJSONArray("records");

			for (int i = 0; i < records.length(); i++) {

				final var current = records.getJSONObject(i);

				final var loan = new Loan();
				loan.sfId = current.optString("Id", Constants.NA);
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

		final var accounts = new JSONArray();

		for (Loan loan : loans) {

			// a single loan account detail object
			final var account = new JSONObject();
			account.put("account", loan.getSummaryJson());

			// finally add single account detail to the accounts array
			accounts.put(account);

		}

		responseJson.put("accounts", accounts);

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

		
		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "Top_Up_CL_Contract_No_LAI__c, Debit_Amount_Top_Up__c, Top_Up_Insurance__c,"
				+ "Total_Debit_Amount_Topup__c,Top_Up_Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c WHERE Top_Up_CL_Contract_No_LAI__c IN (" + lai + ") "
				+ "and RecordType.DeveloperName = 'X_Sell_Product_Collection_Layout' "
				+ "and Clearance_Status__c not in ('Waived', 'No Status', 'Loan Closed', 'EMI Extension', 'Cleared' , 'EMI Moratorium' , 'EMI Moratorium - (Partial Cleared)', 'Restructured 2.0' , 'Restructured 2.0 - (Partial Cleared)') order by CreatedDate desc";

		
		JSONObject paymentJson = sfConnection.get(query);

		if (null == paymentJson)
			return null;

		final var payments = new ArrayList<CPPaymentInfo>();

		if (paymentJson.getInt("totalSize") > 0) {
			final var records = paymentJson.getJSONArray("records");
			for (int i = 0; i < records.length(); i++) {
				JSONObject current = records.getJSONObject(i);
				payments.add(getParsedCPPaymnetInfoForTL(current));

			}
		}

		return payments;
	}

	private ArrayList<CPPaymentInfo> getHomeLoanDetailsForDashboard(String lai) throws Exception {


		String query = "SELECT Name, Opportunity__c, CRM_Account_No__c, Due_Date__c, Month__c,"
				+ "CL_Contract_No_LAI__c, Debit_Amount_Housing__c, Debit_Amount_Insurance__c,"
				+ "Total_Debit_Amount__c, Payment_Excess_Shortfall__c,"
				+ "Partial_Cleared_Date__c, Partial_Cleared_Amount__c,"
				+ "Payment_Cleared_Date__c, Collected_Amount__c," + "Clearance_Status__c, Return_Reasons__c,"
				+ "First_Time_Bounce__c, Bounce_Charges_Paid__c, Reference_Number__c "
				+ "FROM Collection__c WHERE CL_Contract_No_LAI__c IN (" + lai + ") "
				+ "and RecordType.DeveloperName = 'Housing_Loan_Collections'"
				+ "and Clearance_Status__c not in ('Waived', 'No Status', 'Loan Closed', 'EMI Extension', 'Cleared' ,"
				+ " 'EMI Moratorium' , 'EMI Moratorium - (Partial Cleared)', 'Restructured 2.0' , 'Restructured 2.0 - (Partial Cleared)') order by CreatedDate desc";

		
		// JSONObject paymentJson = getSalesforceData(getModifiedQuery(query));
		JSONObject paymentJson = sfConnection.get(query);

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

	public boolean updateRegisteredInfo(User user) throws Exception {

		String query = "select AccountId from Contact where CRM_Account_Number__c='" + user.crmAccountNumber + "'";

		JSONObject requestsJson = sfConnection.get(query);

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

				String uri = SFConnection.baseUri + "/sobjects/Account/" + accountId;

				var sfResponse = sfConnection.patch(contactRequestObject, uri);

				if (sfResponse.isSuccess) {

					return true;
				} else {
					LoggerUtils
							.log("updateRegisteredInfo - failed to update Login data on SF: " + user.crmAccountNumber);

					return false;
				}

			}

		}
		return false;

	}

	public boolean updateLoginInfo(User user) throws Exception {


		String currentDateTime = DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd_T_HH_mm_ss_SSSZ, DateTimeZone.IST);
		String query = "select AccountId from Contact where CRM_Account_Number__c='" + user.crmAccountNumber + "'";

		JSONObject requestsJson = sfConnection.get(query);

		if (null == requestsJson)
			return false;

		if (requestsJson.getInt("totalSize") > 0) {
			String accountId = requestsJson.getJSONArray("records").getJSONObject(0).optString("AccountId",
					Constants.NA);
			if (!accountId.equals(Constants.NA)) {

				LoggerUtils.log("CurrentDateTime:" + currentDateTime);
				JSONObject contactRequestObject = new JSONObject();
				contactRequestObject.put("CP_Last_Login_Date__c", currentDateTime);

				if (!user.isRegistrationMarkedOnSF) {

					String regDateTime = DateTimeUtils.getDateForSalesforce(user.registrationDateTime);
					LoggerUtils.log("=====> registered datetime: " + regDateTime);
					contactRequestObject.put("CP_Registered__c", true);
					contactRequestObject.put("CP_Registration_Date__c", regDateTime);

				}

				String uri = SFConnection.baseUri + "/sobjects/Account/" + accountId;

				var sfResponse = sfConnection.patch(contactRequestObject, uri);

				if (sfResponse.isSuccess == true) {

					LoggerUtils.log("updateLoginInfo - Login data Successfully updated on SF");
					return true;
				} else {
					LoggerUtils.log("updateLoginInfo - failed to update Login data on SF: " + user.crmAccountNumber);

					return false;
				}

			}

		}
		return false;

	}

	public ArrayList<Loan> getLoanList(String crmAccountNumber) throws Exception {

		
		String query = "select Name,Sub_Product_Type__c,loan__Loan_Amount__c,loan__Principal_Remaining__c,"
				+ "loan__Interest_Rate__c,loan__ACH_Debit_Amount__c," + " X_Sell_Products__r.Insurance_Sum_Assured__c,"
				+ "loan__Disbursed_Amount__c,loan__Number_of_Installments__c,Loan_Type_Indicator1__c,loan__Loan_Status__c"
				+ " from loan__Loan_Account__c where CRM_Account_Number__c='" + crmAccountNumber + "'"
				+ " and loan__Loan_Status__c in ('Active - Good Standing', 'Active - Bad Standing', 'Approved', "
				+ "'Closed - Obligations met', 'Closed- Written Off')";
		
	
		JSONObject loanJson = sfConnection.get(query);

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


		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getCompleteJson());
		}


		return loans;
	}

	public ArrayList<Loan> getLoanDetail(String loanAccountNumber) throws Exception {

		var loans = new ArrayList<Loan>();
		var emiAmount = 0.0;

		final var query = "select Id,Name,Sub_Product_Type__c,loan__Loan_Amount__c,loan__Principal_Remaining__c,Latest_Quater_POS__c,"
				+ "loan__Interest_Rate__c,loan__ACH_Debit_Amount__c," + " X_Sell_Products__r.Insurance_Sum_Assured__c,"
				+ "loan__Disbursed_Amount__c,loan__Number_of_Installments__c,Loan_Type_Indicator1__c,loan__Loan_Status__c,loan__Disbursal_Status__c"
				+ " from loan__Loan_Account__c where Name='" + loanAccountNumber + "'";
		

		final var loanJson = sfConnection.get(query);

		if (null == loanJson)
			return loans;

		final var queryEmi = "select loan__RSS_Repayment_Amt__c from loan__Repayment_Schedule_Summary__c"
				+ " where loan__RSS_Loan_Account__r.Name='" + loanAccountNumber + "'"
				+ " and loan__RSS_Primary_flag__c = true order by loan__RSS_No_Of_Pmts__c desc limit 1";

		final var emiJson = sfConnection.get(queryEmi);

		if (null != emiJson) {

			if (emiJson.getInt("totalSize") > 0) {
				
				JSONArray records = emiJson.getJSONArray("records");
				// JSONObject attributes = records.getJSONObject("attributes");
				
//				 LoggerUtils.log("records " + records);
								
				JSONObject current = records.getJSONObject(0);
				//LoggerUtils.log("current " + current);

				emiAmount = current.optDouble("loan__RSS_Repayment_Amt__c", 0.0);

				//LoggerUtils.log("emiAmount " + emiAmount);

			}

		}

		if (loanJson.getInt("totalSize") > 0) {
			JSONArray records = loanJson.getJSONArray("records");

			JSONObject current = records.getJSONObject(0);

			Loan loan = new Loan();
			loan.accountNumber = current.optString("Name", Constants.NA);

			if (BasicUtils.isNotNullOrNA(loan.accountNumber)) {

				loan.sfId = current.optString("Id", Constants.NA);
				loan.loanType = current.optString("Sub_Product_Type__c", Constants.NA);
				loan.principalAmount = current.optDouble("loan__Principal_Remaining__c", 0.0);
				loan.lastFYPOS = current.optDouble("Latest_Quater_POS__c", 0.0);
				loan.debitAmount = current.optDouble("loan__ACH_Debit_Amount__c", 0.0);
				loan.loanAmount = current.optDouble("loan__Loan_Amount__c", 0.0);
				loan.paymentType = current.optString("Loan_Type_Indicator1__c", Constants.NA);
				loan.interestRate = current.optDouble("loan__Interest_Rate__c", 0.0);
				loan.disbursedAmount = current.optDouble("loan__Disbursed_Amount__c", 0.0);
				loan.numberOfInstallments = current.optInt("loan__Number_of_Installments__c", 0);
				loan.status = current.optString("loan__Loan_Status__c", Constants.NA);
				loan.disbursalStatus = current.optString("loan__Disbursal_Status__c", Constants.NA);
				loan.totalTenure = loan.numberOfInstallments;

				if (!loan.disbursalStatus.equals(Constants.FULLY_DISBURSED) || emiAmount == 0.0) {
					loan.remainingTenure = loan.numberOfInstallments;
				} else {

					var monthlyroi = loan.interestRate / 1200;

					final var calculatedTenure = ((Math.log(emiAmount)
							- Math.log(emiAmount - (loan.principalAmount * monthlyroi))) / Math.log(1 + monthlyroi));

					//int roundOffIntTenure = (int) calculatedTenure;
					
					int roundOffIntTenure = (int) Math.round(calculatedTenure);

					loan.remainingTenure = roundOffIntTenure;
				}

				if (loan.status.equalsIgnoreCase(Constants.LOAN_TYPE_CLOSED)) {
					loan.loanType += " (Closed)";
					loan.principalAmount = 0.0;
					loan.debitAmount = 0.0;
					loan.loanAmount = 0.0;
					loan.totalDebitAmount = 0.0;
				}

				JSONObject xSellProduct = current.optJSONObject("X_Sell_Products__r");
				if (null != xSellProduct) {
					double totalInsuredAmount = xSellProduct.optDouble("Insurance_Sum_Assured__c", -1);
					loan.totalInsuredAmount = totalInsuredAmount;
				}

				loans.add(loan);

			}
		} else {
			return loans;
		}

		loans = getLoansWithUpatedAmount(loans);

		JSONArray loanDetails = new JSONArray();
		for (Loan loan : loans) {
			loanDetails.put(loan.getCompleteJson());
		}


		return loans;
	}
	

	public JSONObject getDisbursementDetail(String loanAccountNumber) throws Exception {

		JSONObject responseJson = new JSONObject();


		String query = "select Name,Sub_Product_Type__c,loan__Loan_Status__c,loan__Disbursed_Amount__c,"
				+ "loan__Remaining_Loan_Amount__c from loan__Loan_Account__c " + "where Name='" + loanAccountNumber
				+ "'";

		final var disbursementJson = sfConnection.get(query);

		query = "select loan__Loan_Account__r.Name,loan__Disbursal_Date__c,loan__Reference__c,"
				+ "loan__Disbursed_Amt__c from loan__Loan_Disbursal_Transaction__c "
				+ "where loan__Loan_Account__r.Name='" + loanAccountNumber + "' and loan__Cleared__c=true";

		final var recentDisbursementJson = sfConnection.get(query);

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

}
